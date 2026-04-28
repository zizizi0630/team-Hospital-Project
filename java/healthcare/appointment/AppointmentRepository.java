package healthcare.appointment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
	// List<Appointment> findByapmtAuthor_useridLike(String apmtId);
	// Optional<Appointment> findByapmtId(String apmtId);

	/*
	 * @Query("select distinct c from Appointment c where c.id LIKE :kw% ")
	 * //@Query("select distinct c from Code c where substring(c.codeId,1,1) = :kw "
	 * )
	 * List<Appointment> findByappointmentKeyword(@Param("kw") String kw);
	 */

	List<Appointment> findByApmtAuthor(healthcare.user.SiteUser author);

	@Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.code = :code AND a.apmt_date = :date")
	boolean existsByCodeAndDate(@Param("code") healthcare.code.Code code, @Param("date") java.time.LocalDateTime date);

	@Query("SELECT a FROM Appointment a WHERE a.code = :code AND a.apmt_date BETWEEN :start AND :end")
	List<Appointment> findAllByCodeAndDateBetween(@Param("code") healthcare.code.Code code,
			@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}
