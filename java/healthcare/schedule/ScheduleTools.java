package healthcare.schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.time.format.TextStyle;
import java.util.Locale;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import healthcare.user.UserService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduleTools {

    private final ScheduleRepository scheduleRepository;
    private final UserService userService;
    private final healthcare.appointment.AppointmentRepository appointmentRepository;
    private final healthcare.code.CodeRepository codeRepository;

    @Tool(description = "Check if a specific department is available on a given date. " +
            "Requires date (YYYY-MM-DD) or datetime (YYYY-MM-DDTHH:MM). Optional: codeId (e.g., 'I-001'), doctorName. "
            +
            "Returns 'Available' or 'Closed: [Reason]'. If closed, tries to suggest the next available date within 7 days.")
    public String checkAvailability(String date, String symptom, String codeId, String doctorName) {
        try {
            LocalDateTime requestTime = parseRequestTime(date);
            healthcare.code.Code code = resolveCode(codeId);

            if (code == null && (codeId != null && !codeId.isEmpty())) {
                return "Error: Invalid codeId or Department Name (" + codeId + ").";
            }

            String status = checkDateStatus(requestTime, code);

            if (status.startsWith("Closed")) {
                // If closed, try to find next available date within 7 days
                LocalDateTime nextDate = requestTime.plusDays(1);

                for (int i = 1; i <= 7; i++) {
                    String nextStatus = checkDateStatus(nextDate, code);
                    if (nextStatus.equals("Available")) {
                        String dayOfWeek = nextDate.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL,
                                java.util.Locale.KOREAN);
                        return status + " (Next available date: " + nextDate.toLocalDate() + " (" + dayOfWeek + "))";
                    }
                    nextDate = nextDate.plusDays(1);
                }
                return status + " (No available dates found within next 7 days)";
            }

            return status;

        } catch (Exception e) {
            return "Error checking availability: " + e.getMessage();
        }
    }

    @Tool(description = "Search for available dates within a specific range. " +
            "Requires codeId and date range (startDate, endDate in YYYY-MM-DD). " +
            "Optional: filter ('WEEKEND' for Sat/Sun only, 'ALL' for consecutive days). " +
            "Returns a list of all available dates in the range.")
    public String findAvailableDates(String codeId, String startDate, String endDate, String filter) {
        try {
            LocalDateTime start = parseRequestTime(startDate);
            LocalDateTime end = parseRequestTime(endDate);
            StringBuilder availableDates = new StringBuilder();

            healthcare.code.Code code = resolveCode(codeId);
            if (code == null && (codeId != null && !codeId.isEmpty())) {
                return "Error: Invalid codeId or Department Name (" + codeId + ").";
            }

            // Limit range to avoid excessive queries (max 31 days)
            if (java.time.temporal.ChronoUnit.DAYS.between(start, end) > 31) {
                end = start.plusDays(31);
            }

            LocalDateTime current = start;
            while (!current.isAfter(end)) {
                boolean checkThisDay = true;
                java.time.DayOfWeek dow = current.getDayOfWeek();

                // Apply Filters
                if ("WEEKEND".equalsIgnoreCase(filter)) {
                    if (dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY) {
                        checkThisDay = false;
                    }
                }

                if (checkThisDay) {
                    String status = checkDateStatus(current, code);
                    if ("Available".equals(status)) {
                        String dayOfWeek = current.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL,
                                java.util.Locale.KOREAN);
                        availableDates.append(current.toLocalDate()).append(" (").append(dayOfWeek).append("), ");
                    }
                }
                current = current.plusDays(1);
            }

            if (availableDates.length() == 0) {
                return "No available dates found in the specified range.";
            }
            return "Available Dates: " + availableDates.substring(0, availableDates.length() - 2);

        } catch (Exception e) {
            return "Error searching available dates: " + e.getMessage();
        }
    }

    private LocalDateTime parseRequestTime(String date) {
        if (date.contains("T")) {
            return LocalDateTime.parse(date);
        } else {
            return java.time.LocalDate.parse(date).atStartOfDay();
        }
    }

    private healthcare.code.Code resolveCode(String codeIdentifier) {
        if (codeIdentifier == null || codeIdentifier.isEmpty())
            return null;

        // 1. Try Lookup by ID
        healthcare.code.Code code = codeRepository.findBycodeId(codeIdentifier).orElse(null);

        // 2. Fallback: Lookup by Name
        if (code == null) {
            code = codeRepository.findByCode_Name2(codeIdentifier).orElse(null);
        }
        return code;
    }

    private String checkDateStatus(LocalDateTime requestTime, healthcare.code.Code code) {
        // 0. Check Basic Hospital Hours (Sunday Closed)
        if (requestTime.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            return "Closed: Sunday (Hospital Closed)";
        }

        // [New] Hardcoded Lunch Time Check (12:30 ~ 13:30)
        java.time.LocalTime time = requestTime.toLocalTime();
        if (!time.isBefore(java.time.LocalTime.of(12, 30)) && time.isBefore(java.time.LocalTime.of(13, 30))) {
            return "Closed: 점심 시간 (Lunch Time 12:30 - 13:30)";
        }

        LocalDateTime startOfDay = requestTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        // 1. Check ADMIN Schedules (Global Rules)
        List<Schedule> adminSchedules = scheduleRepository.findByAuthor_RoleAndStartDateTimeBetween("ADMIN",
                startOfDay, endOfDay);

        for (Schedule s : adminSchedules) {
            // Check "휴무" (Holiday)
            if ("휴무".equals(s.getTitle())) {
                // Check if it covers the request time.
                // For holidays, if it's on the day, the whole day is closed.
                // The provided snippet removes the isOverlapping check for "휴무" here, implying
                // the whole day is affected.
                return "Closed: Hospital Holiday (" + s.getTitle() + ")";
            }

            // Check "점심" (Lunch)
            if ("점심".equals(s.getTitle())) {
                if (isOverlapping(s, requestTime)) {
                    return "Closed: Lunch Time (" + s.getStartDateTime().toLocalTime() + " - "
                            + s.getEndDateTime().toLocalTime() + ")";
                }
            }
        }

        // 2. Check Department Availability (Doctor Count)
        if (code != null) {
            String codeId = code.getCodeId();
            long totalDoctors = userService.countDoctorsByCode(codeId);

            if (totalDoctors == 0) {
                return "Closed: 해당 과에 등록된 의사가 없습니다 (No doctors found). 관리자에게 문의하세요.";
            }

            // Get all DOCTOR schedules for this department on this day
            List<Schedule> deptSchedules = scheduleRepository.findByCodeIdAndDateRange(codeId, startOfDay,
                    endOfDay);

            // Count how many doctors are OFF at the requested time
            long doctorsOnLeave = 0;
            StringBuilder leaveDetails = new StringBuilder(); // This is kept for potential future use or debugging, but
                                                              // not used in the final return string as per snippet.

            for (Schedule s : deptSchedules) {
                // Only consider schedules titled "휴무" for doctors
                if ("휴무".equals(s.getTitle())) {
                    if (isOverlapping(s, requestTime)) {
                        doctorsOnLeave++;
                        // Collect details: "Dr. Name (Reason)" - Reason is likely "휴무" but let's just
                        // say "휴무" or "Leave"
                        // If we want the title, "휴무". If event has description? Title is "휴무".
                        String docName = s.getAuthor().getUser_name();
                        leaveDetails.append(docName).append(": ").append(s.getTitle()).append(", ");
                    }
                }
            }

            // If total doctors are 5, and 5 are on leave -> Closed.
            if ((totalDoctors - doctorsOnLeave) <= 0) {
                // String details = leaveDetails.length() > 0 ? leaveDetails.substring(0,
                // leaveDetails.length() - 2) : "All doctors on leave";
                // Simplified reason as user requested:
                return "Closed: 해당 과의 모든 의료진이 휴무입니다 (All doctors are on leave).";
            }
            // return "Available: " + (totalDoctors - doctorsOnLeave) + " doctors working.";
            return "Available";
        }

        return "Available";
    }

    private boolean isOverlapping(Schedule s, LocalDateTime requestTime) {
        // Simple containment check for Point-in-time request
        // If requestTime matches StartTime exactly, consider it inside?
        // Usually appointments start at StartTime.
        // If 'Closed' is 12:00-13:00. Request 12:00 -> Closed.
        // s.start <= request < s.end
        String title = (s.getTitle() != null) ? s.getTitle() : "";

        // Special case for '휴무' - assumes blocked for the whole duration
        // Special case for '점심' - blocking

        return !requestTime.isBefore(s.getStartDateTime()) && requestTime.isBefore(s.getEndDateTime());
    }

    @Tool(description = "Check standard hospital holidays for a given date.")
    public String checkHospitalSchedule(String date) {
        return checkAvailability(date, null, null, null);
    }

    @Tool(description = "Get available 10-minute time slots for a specific date and department. " +
            "Requires date (YYYY-MM-DD or YYYY-MM-DDTHH:mm) and codeId. " +
            "Returns a summarized string of available time ranges (e.g., '09:00 ~ 12:00, 13:30 ~ 15:00') excluding lunch, holidays, and booked slots.")
    public String getAvailableTimeSlots(String date, String codeId) {
        try {
            if (codeId == null || codeId.isEmpty()) {
                return "Error: codeId is required to find available slots.";
            }

            LocalDateTime requestDate = parseRequestTime(date);
            LocalDateTime startOfDay = requestDate.toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

            // 1. Basic Check (Sunday)
            if (requestDate.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                return "Closed: Sunday (Hospital Closed)";
            }

            // Fetch Code Entity (Unified Resolution)
            healthcare.code.Code code = resolveCode(codeId);

            if (code == null)
                return "Error: Invalid codeId or Department Name (" + codeId + ").";

            // Use the authoritative ID from the found entity
            codeId = code.getCodeId();

            // 2. Determine Working Hours
            // Weekday: 09:00 - 18:00
            // Saturday: 09:00 - 13:00
            int startHour = 9;
            int endHour = (requestDate.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) ? 13 : 18;

            // 3. Fetch Admin Schedules (Holidays, Lunch)
            List<Schedule> adminSchedules = scheduleRepository.findByAuthor_RoleAndStartDateTimeBetween("ADMIN",
                    startOfDay, endOfDay);

            for (Schedule s : adminSchedules) {
                if ("휴무".equals(s.getTitle())) {
                    return "Labor Holiday (Hospital Closed): " + s.getTitle();
                }
            }

            // 4. Fetch Department Schedules (Doctor Leaves) & Count Doctors
            long totalDoctors = userService.countDoctorsByCode(codeId);
            if (totalDoctors == 0)
                return "Closed: No doctors in this department.";

            List<Schedule> deptSchedules = scheduleRepository.findByCodeIdAndDateRange(codeId, startOfDay, endOfDay);

            // 5. Fetch Existing Appointments (Booked Slots)
            List<healthcare.appointment.Appointment> appointments = appointmentRepository
                    .findAllByCodeAndDateBetween(code, startOfDay, endOfDay);
            java.util.Set<java.time.LocalTime> bookedTimes = appointments.stream()
                    .map(a -> a.getApmt_date().toLocalTime())
                    .collect(java.util.stream.Collectors.toSet());

            List<String> availableSlots = new java.util.ArrayList<>();
            LocalDateTime cursor = requestDate.toLocalDate().atTime(startHour, 0);
            LocalDateTime closeTime = requestDate.toLocalDate().atTime(endHour, 0);

            while (cursor.isBefore(closeTime)) {
                // Check if time is booked
                if (bookedTimes.contains(cursor.toLocalTime())) {
                    cursor = cursor.plusMinutes(10);
                    continue;
                }

                // Check Admin Blocking (Lunch)
                boolean blockedByAdmin = false;
                java.time.LocalTime t = cursor.toLocalTime();
                // Lunch 12:30 - 13:30 (Hardcoded rule as per requirement)
                if ((t.isAfter(java.time.LocalTime.of(12, 29)) && t.isBefore(java.time.LocalTime.of(13, 30)))) {
                    blockedByAdmin = true;
                }

                if (!blockedByAdmin) {
                    for (Schedule s : adminSchedules) {
                        if (isOverlapping(s, cursor)) {
                            blockedByAdmin = true;
                            break;
                        }
                    }
                }

                if (blockedByAdmin) {
                    cursor = cursor.plusMinutes(10);
                    continue;
                }

                // Check Doctor Availability
                long doctorsOnLeave = 0;
                for (Schedule s : deptSchedules) {
                    if ("휴무".equals(s.getTitle()) && isOverlapping(s, cursor)) {
                        doctorsOnLeave++;
                    }
                }

                if ((totalDoctors - doctorsOnLeave) > 0) {
                    availableSlots.add(cursor.toLocalTime().toString());
                }

                cursor = cursor.plusMinutes(10);
            }

            if (availableSlots.isEmpty()) {
                return "No available slots for " + date + ".";
            }

            // Grouping logic
            StringBuilder summary = new StringBuilder("Available Slots: ");
            if (availableSlots.isEmpty())
                return summary.toString();

            java.time.LocalTime startRange = java.time.LocalTime.parse(availableSlots.get(0));
            java.time.LocalTime prev = startRange;

            for (int i = 1; i < availableSlots.size(); i++) {
                java.time.LocalTime current = java.time.LocalTime.parse(availableSlots.get(i));
                if (prev.plusMinutes(10).equals(current)) {
                    // Continuous
                    prev = current;
                } else {
                    // Break in continuity
                    summary.append(startRange).append(" ~ ").append(prev).append(", ");
                    startRange = current;
                    prev = current;
                }
            }
            summary.append(startRange).append(" ~ ").append(prev);

            return summary.toString();

        } catch (Exception e) {
            return "Error calculating slots: " + e.getMessage();
        }
    }
}
