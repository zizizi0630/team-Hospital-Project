package healthcare.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<SiteUser, Long> {
    Optional<SiteUser> findByuserid(String userid);

    List<SiteUser> findByRoleOrderByCode_IdAsc(String role);

    List<SiteUser> findByRoleInOrderByCode_IdAsc(java.util.Collection<String> roles);

    long countByRoleAndCode_CodeId(String role, String codeId);

    long countByRoleInAndCode_CodeId(java.util.Collection<String> roles, String codeId);
}