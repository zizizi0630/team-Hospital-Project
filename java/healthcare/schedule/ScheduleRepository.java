package healthcare.schedule;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

        // Find schedules within a date range
        List<Schedule> findByStartDateTimeBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

        // Find schedules for a specific author within a date range
        List<Schedule> findByAuthorAndStartDateTimeBetween(healthcare.user.SiteUser author,
                        java.time.LocalDateTime start,
                        java.time.LocalDateTime end);

        // Find schedules for a specific department (Code) within a date range
        @Query("select s from Schedule s where s.author.code.codeId = :codeId and s.startDateTime between :start and :end")
        List<Schedule> findByCodeIdAndDateRange(@Param("codeId") String codeId,
                        @Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

        // Find schedules by Role (e.g., ADMIN) within a date range
        List<Schedule> findByAuthor_RoleAndStartDateTimeBetween(String role, java.time.LocalDateTime start,
                        java.time.LocalDateTime end);
}
