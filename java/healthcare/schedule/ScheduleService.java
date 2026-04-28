package healthcare.schedule;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import healthcare.user.SiteUser;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public List<Schedule> getAllEvents() {
        return this.scheduleRepository.findAll();
    }

    public Schedule create(String title, LocalDateTime start, LocalDateTime end, boolean allDay, SiteUser author) {
        Schedule schedule = new Schedule();
        schedule.setTitle(title);
        schedule.setStartDateTime(start);
        schedule.setEndDateTime(end);
        schedule.setAllDay(allDay);
        schedule.setAuthor(author);
        schedule.setColor(determineColor(title, author));
        this.scheduleRepository.save(schedule);
        return schedule;
    }

    public Schedule getSchedule(Long id) {
        return this.scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid schedule Id:" + id));
    }

    public void modify(Schedule schedule, String title, LocalDateTime start, LocalDateTime end, boolean allDay) {
        schedule.setTitle(title);
        schedule.setStartDateTime(start);
        schedule.setEndDateTime(end);
        schedule.setAllDay(allDay);
        // Recalculate color in case title or author changed (though author usually
        // doesn't change)
        schedule.setColor(determineColor(title, schedule.getAuthor()));
        this.scheduleRepository.save(schedule);
    }

    private String determineColor(String title, SiteUser author) {
        if (title != null && title.contains("휴무")) {
            return "#ed0a3f"; // Red
        }

        if (author != null && author.getCode() != null) {
            String codeId = author.getCode().getCodeId();
            if (codeId.startsWith("I")) {
                return "#3788d8"; // Blue (Internal Medicine)
            } else if (codeId.startsWith("S")) {
                return "#fd7e14"; // Orange (Surgery)
            } else if (codeId.startsWith("G")) {
                return "#28a745"; // Green (General/Other)
            }
        }
        return "#28a745"; // Default Green
    }

    public void delete(Schedule schedule) {
        this.scheduleRepository.delete(schedule);
    }
}
