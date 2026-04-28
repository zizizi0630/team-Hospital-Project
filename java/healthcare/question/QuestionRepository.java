package healthcare.question;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface QuestionRepository extends JpaRepository<Question, Long> {
	Question findBySubject(String subject);
	Question findBySubjectAndContent(String subject, String content);
	List<Question> findBySubjectLike(String subject);
	Page<Question> findAll(Pageable pageable);
	// Page<Question> findAll(Specification<Question> spec, Pageable pageable);
	@Query("select "
			+ "distinct q "
			+ "from Question q "
			+ "left outer join SiteUser u1 on q.author=u1 "
			+ "left outer join Answer a on a.question=q "
			+ "left outer join SiteUser u2 on a.author=u2 "
			+ "where "
			+ "   q.subject like %:kw% "
			+ "   or q.content like %:kw% "
			+ "   or u1.userid like %:kw% "
			+ "   or a.content like %:kw% "
			+ "   or u2.userid like %:kw% ")
	Page<Question> findAllByKeyword(@Param("kw") String kw, Pageable pageable);
	@Query("select "
			+ "distinct q "
			+ "from Question q "
			+ "left outer join SiteUser u1 on q.author=u1 "
			+ "left outer join Answer a on a.question=q "
			+ "left outer join SiteUser u2 on a.author=u2 "
			+ "where "
			+ "   q.author.id = :authorId "
			+ "   and ("
			+ "       q.subject like %:kw% "
			+ "       or q.content like %:kw% "
			+ "       or u1.userid like %:kw% "
			+ "       or a.content like %:kw% "
			+ "       or u2.userid like %:kw% "
			+ "   )")
	Page<Question> findAllByKeywordAndAuthorId(@Param("kw") String kw, @Param("authorId") Long authorId,
			Pageable pageable);
}