package healthcare.appointment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import healthcare.schedule.ScheduleTools;
import healthcare.user.SiteUser;
import healthcare.user.UserService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentTools {

    private final AppointmentService appointmentService;
    private final UserService userService;
    private final ScheduleTools scheduleTools; // Replaced ScheduleRepository with ScheduleTools

    @Tool(description = "Make a clinic appointment reservation. Requires date (YYYY-MM-DDTHH:MM) and username. Optional: codeId (department code, e.g., 'I-001', 'SUR') and symptoms. User details are automatically fetched.")
    public String makeReservation(String date, String username, String codeId, String symptoms) {
        try {
            SiteUser user = this.userService.getUser(username);
            if (user == null) {
                return "Error: User not found with username: " + username;
            }

            LocalDateTime apmtDate = LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // [New] Check for Hospital/Department Holidays using centralized ScheduleTools
            // logic
            // We pass the exact datetime string to check availability for that specific
            // slot
            String availability = scheduleTools.checkAvailability(date, symptoms, codeId, null);
            if (availability.startsWith("Closed")) {
                return "Error: Cannot make reservation. " + availability;
            }

            // Fetch user details from DB

            // Fetch user details from DB
            String name = user.getUser_name();
            String phone = user.getTelp();
            String address = user.getAddr();
            String gender = user.getGender();

            if (name == null || phone == null || address == null || gender == null) {
                return "Error: User profile is incomplete (missing name, phone, address, or gender). Please update profile first.";
            }

            this.appointmentService.insert(name, phone, address, apmtDate, gender, codeId, symptoms, user);

            return "Reservation successfully created for " + name + " on " + date + " (Dept: " + codeId + ", Symptoms: "
                    + symptoms + ")";
        } catch (Exception e) {
            return "Error creating reservation: " + e.getMessage();
        }
    }

    @Tool(description = "Check if the user has any existing reservations. Requires username.")
    public String checkReservation(String username) {
        try {
            SiteUser user = this.userService.getUser(username);
            if (user == null) {
                return "Error: User not found with username: " + username;
            }

            List<Appointment> appointments = this.appointmentService.getList(user);

            // Filter queries for today and future dates
            LocalDateTime startOfToday = java.time.LocalDate.now().atStartOfDay();
            List<Appointment> upcomingAppointments = appointments.stream()
                    .filter(a -> !a.getApmt_date().isBefore(startOfToday))
                    .collect(java.util.stream.Collectors.toList());

            if (upcomingAppointments == null || upcomingAppointments.isEmpty()) {
                return "No upcoming reservations found for user: " + username;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(upcomingAppointments.size()).append(" upcoming reservation(s) for ")
                    .append(username)
                    .append(":\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Appointment apmt : upcomingAppointments) {
                sb.append("- Date: ").append(apmt.getApmt_date().format(formatter))
                        .append(", Name: ").append(apmt.getApmt_name())
                        .append(", Dept: ").append(apmt.getCode() != null ? apmt.getCode().getCodeId() : "N/A")
                        .append(", Symptoms: ").append(apmt.getSymptoms() != null ? apmt.getSymptoms() : "N/A")
                        .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error checking reservations: " + e.getMessage();
        }
    }
}
