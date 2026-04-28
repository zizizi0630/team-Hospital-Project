package healthcare.schedule;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import healthcare.user.SiteUser;
import healthcare.user.UserService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final UserService userService;
    private final healthcare.appointment.AppointmentService appointmentService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("")
    public String calendar(Principal principal) {

        SiteUser user = this.userService.getUser(principal.getName());

        if ("ADMIN".equals(user.getRole()) || "DOCTOR".equals(user.getRole())) {

        } else {
            return "redirect:/";
        }

        return "schedule/calendar";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api")
    @ResponseBody
    public List<Map<String, Object>> getEvents() {
        List<Schedule> schedules = this.scheduleService.getAllEvents();
        List<healthcare.appointment.Appointment> appointments = this.appointmentService.getList();
        List<Map<String, Object>> events = new ArrayList<>();

        // 1. Add Schedule Events
        for (Schedule s : schedules) {
            Map<String, Object> event = new HashMap<>();
            event.put("id", "S_" + s.getId()); // Prefix to distinguish
            event.put("title", s.getTitle());
            event.put("start", s.getStartDateTime());
            event.put("end", s.getEndDateTime());
            event.put("allDay", s.isAllDay());
            event.put("color", s.getColor());
            if (s.getAuthor() != null) {
                event.put("authorId", s.getAuthor().getUserid());
                event.put("authorName", s.getAuthor().getUser_name());
            }
            events.add(event);
        }

        // 2. Add Appointment Events (Aggregated)
        Map<String, List<Map<String, Object>>> groupedAppointments = new HashMap<>();

        for (healthcare.appointment.Appointment a : appointments) {
            String date = a.getApmt_date().toLocalDate().toString();
            String codePrefix = "OTHERS";
            String majorName = "기타";
            String color = "#17a2b8";

            if (a.getCode() != null && a.getCode().getCodeId() != null) {
                // Use Name from DB (code_Name2 serves as the Category Name)
                if (a.getCode().getCode_Name2() != null && !a.getCode().getCode_Name2().isEmpty()) {
                    majorName = a.getCode().getCode_Name2();
                }

                String prefix = a.getCode().getCodeId().substring(0, 1).toUpperCase();
                switch (prefix) {
                    case "I":
                        codePrefix = "I";
                        if (majorName.equals("기타"))
                            majorName = "내과";
                        color = "#007bff";
                        break;
                    case "S":
                        codePrefix = "S";
                        if (majorName.equals("기타"))
                            majorName = "외과";
                        color = "#28a745";
                        break;
                    case "G":
                        codePrefix = "G";
                        if (majorName.equals("기타"))
                            majorName = "기타전문과";
                        color = "#fd7e14";
                        break;
                    default:
                        break;
                }
            }

            String groupKey = date + "_" + codePrefix;

            // Prepare Appointment Details
            Map<String, Object> apmtDetail = new HashMap<>();
            apmtDetail.put("id", a.getId());
            apmtDetail.put("time", a.getApmt_date().toLocalTime().toString());
            apmtDetail.put("patientName", a.getApmt_name());
            apmtDetail.put("symptoms", a.getSymptoms());
            apmtDetail.put("codeId", a.getCode() != null ? a.getCode().getCodeId() : "Unknown");
            apmtDetail.put("codeName", a.getCode() != null ? a.getCode().getCode_Name1() : "Unknown");

            // Initialize Group if not exists
            if (!groupedAppointments.containsKey(groupKey)) {
                List<Map<String, Object>> list = new ArrayList<>();
                groupedAppointments.put(groupKey, list);

                // create the Summary Event placeholder (will add to 'events' later)
                Map<String, Object> summaryEvent = new HashMap<>();
                summaryEvent.put("id", "Group_" + groupKey);
                summaryEvent.put("title", majorName + " 예약목록");
                summaryEvent.put("start", date); // All Day
                summaryEvent.put("allDay", true);
                summaryEvent.put("color", color);
                summaryEvent.put("extendedProps", new HashMap<String, Object>());
                ((Map<String, Object>) summaryEvent.get("extendedProps")).put("category", majorName);
                ((Map<String, Object>) summaryEvent.get("extendedProps")).put("appointmentList", list); // Reference to
                                                                                                        // the list

                events.add(summaryEvent);
            }

            // Add to the list
            groupedAppointments.get(groupKey).add(apmtDetail);
        }

        // Update titles with counts
        for (Map<String, Object> event : events) {
            if (event.get("id").toString().startsWith("Group_")) {
                List<?> list = (List<?>) ((Map<String, Object>) event.get("extendedProps")).get("appointmentList");
                event.put("title", event.get("title") + " (" + list.size() + "건)");
            }
        }

        return events;
    }

    @GetMapping("/list")
    public String list(org.springframework.ui.Model model) {

        List<Schedule> scheduleList = this.scheduleService.getAllEvents();
        model.addAttribute("scheduleList", scheduleList);
        return "schedule/list";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/update")
    @ResponseBody
    public String updateEvent(@RequestParam("id") Long id,
            @RequestParam("title") String title,
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr,
            @RequestParam(value = "allDay", defaultValue = "false") boolean allDay,
            org.springframework.security.core.Authentication authentication) {

        Schedule schedule = this.scheduleService.getSchedule(id);

        boolean isWriter = schedule.getAuthor().getUserid().equals(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isWriter && !isAdmin) {
            return "unauthorized";
        }

        LocalDateTime start = LocalDateTime.parse(startStr, DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime end = null;
        if (endStr != null && !endStr.isEmpty()) {
            end = LocalDateTime.parse(endStr, DateTimeFormatter.ISO_DATE_TIME);
        }

        this.scheduleService.modify(schedule, title, start, end, allDay);
        return "success";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/delete")
    @ResponseBody
    public String deleteEvent(@RequestParam("id") Long id,
            org.springframework.security.core.Authentication authentication) {
        Schedule schedule = this.scheduleService.getSchedule(id);

        boolean isWriter = schedule.getAuthor().getUserid().equals(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isWriter && !isAdmin) {
            return "unauthorized";
        }

        this.scheduleService.delete(schedule);
        return "success";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @ResponseBody
    public String createEvent(@RequestParam("title") String title,
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr,
            @RequestParam(value = "allDay", defaultValue = "false") boolean allDay,
            Principal principal) {

        SiteUser siteUser = this.userService.getUser(principal.getName());

        LocalDateTime start = LocalDateTime.parse(startStr, DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime end = null;
        if (endStr != null && !endStr.isEmpty()) {
            end = LocalDateTime.parse(endStr, DateTimeFormatter.ISO_DATE_TIME);
        }

        this.scheduleService.create(title, start, end, allDay, siteUser);
        return "success";
    }
}
