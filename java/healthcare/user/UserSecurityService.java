package healthcare.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserSecurityService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userid) throws UsernameNotFoundException {
        Optional<SiteUser> _siteUser = this.userRepository.findByuserid(userid);
        if (_siteUser.isEmpty()) {
            throw new UsernameNotFoundException("사용자를 찾을수 없습니다.");
        }
        SiteUser siteUser = _siteUser.get();
        List<GrantedAuthority> authorities = new ArrayList<>();
        String role = siteUser.getRole();
        if ("ADMIN".equalsIgnoreCase(role)) {
            authorities.add(new SimpleGrantedAuthority(UserRole.ADMIN.getValue()));
        } else if ("DOCTOR".equalsIgnoreCase(role)) {
            authorities.add(new SimpleGrantedAuthority(UserRole.DOCTOR.getValue()));
        } else if ("AI".equalsIgnoreCase(role)) {
            authorities.add(new SimpleGrantedAuthority(UserRole.AI.getValue()));
        } else {
            authorities.add(new SimpleGrantedAuthority(UserRole.PATIENT.getValue()));
        }
        return new User(siteUser.getUserid(), siteUser.getPasswd(), authorities);
    }
}