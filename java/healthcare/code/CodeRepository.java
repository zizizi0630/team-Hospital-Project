package healthcare.code;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeRepository extends JpaRepository<Code, Long> {
	List<Code> findBycodeAuthor_useridLike(String codeId);

	Optional<Code> findBycodeId(String codeId);

	@Query("select distinct c from Code c where c.codeId LIKE :kw% ")
	// @Query("select distinct c from Code c where substring(c.codeId,1,1) = :kw ")
	List<Code> findBycodeKeyword(@Param("kw") String kw);

	@Query("SELECT c FROM Code c WHERE c.code_Name2 = :codeName2")
	Optional<Code> findByCode_Name2(@Param("codeName2") String codeName2);
}