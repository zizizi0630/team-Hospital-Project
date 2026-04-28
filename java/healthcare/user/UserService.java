package healthcare.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import healthcare.DataNotFoundException;
import healthcare.code.Code;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<SiteUser> getDoctorList() {
        return this.userRepository.findByRoleInOrderByCode_IdAsc(
                java.util.Arrays.asList("DOCTOR", "ROLE_DOCTOR", "doctor", "role_doctor"));
    }

    public long countDoctorsByCode(String codeId) {
        return this.userRepository.countByRoleInAndCode_CodeId(
                java.util.Arrays.asList("DOCTOR", "ROLE_DOCTOR", "doctor", "role_doctor"),
                codeId);
    }

    public SiteUser create(String userid, String passwd,
            String user_name, String telp, String addr, String gender) {
        SiteUser user = new SiteUser();
        user.setUserid(userid);
        // user.setPasswd(passwd);
        user.setPasswd(passwordEncoder.encode(passwd));
        user.setUser_name(user_name);
        user.setTelp(telp);
        user.setGender(gender);
        user.setAddr(addr);

        if (this.userRepository.count() == 0) {
            user.setRole("ADMIN");
        } else {
            user.setRole("PATIENT");
        }

        this.userRepository.save(user);
        return user;
    }

    public List<SiteUser> getList() {
        return this.userRepository.findAll(Sort.by(Sort.Direction.ASC, "userid"));
    }

    public SiteUser getUser(String userid) {
        Optional<SiteUser> siteUser = this.userRepository.findByuserid(userid);
        if (siteUser.isPresent()) {
            return siteUser.get();
        } else {
            throw new DataNotFoundException("siteuser not found");
        }
    }

    public SiteUser getUser(Long id) {
        Optional<SiteUser> userList = this.userRepository.findById(id);
        if (userList.isPresent()) {
            return userList.get();
        } else {
            throw new DataNotFoundException("user not found");
        }
    }

    @Transactional
    public void delete(SiteUser siteUser) {
        // 1. Question 작성자 해제 (Question.java 참조)
        if (siteUser.getQuestionList() != null) {
            siteUser.getQuestionList().forEach(q -> q.setAuthor(null));
        }

        // 2. Answer 작성자 해제 (Answer.java 참조)
        if (siteUser.getAnswerList() != null) {
            siteUser.getAnswerList().forEach(a -> a.setAuthor(null));
        }

        // 3. Schedule 작성자 해제 (Schedule.java 참조)
        if (siteUser.getScheduleList() != null) {
            siteUser.getScheduleList().forEach(s -> s.setAuthor(null));
        }

        // 4. Appointment 작성자 해제 (Appointment.java 참조)
        if (siteUser.getAppointmentList() != null) {
            siteUser.getAppointmentList().forEach(ap -> ap.setApmtAuthor(null));
        }

        // 5. Code 작성자 해제 (추가됨 - Code.java 참조)
        if (siteUser.getCodeList() != null) {
            siteUser.getCodeList().forEach(c -> c.setCodeAuthor(null));
        }

        // 6. 모든 관계가 정리되었으므로 유저 삭제 실행
        this.userRepository.delete(siteUser);
    }

    public void modify(SiteUser user, String user_name, String telp, String addr, String gender, String role,
            Code code) {
        user.setUser_name(user_name);
        user.setTelp(telp);
        user.setGender(gender);
        user.setAddr(addr);
        System.out.println("**********************************" + role);
        user.setRole(role);
        user.setCode(code);
        this.userRepository.save(user);
    }

    public boolean checkPassword(SiteUser user, String password) {
        return this.passwordEncoder.matches(password, user.getPasswd());
    }

    @Transactional
    public void changePassword(SiteUser user, UserPassword form) {

        // 현재 비번
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPasswd())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비번
        if (!form.getNewPassword().equals(form.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }
        
        user.setPasswd(passwordEncoder.encode(form.getNewPassword()));
    }
}