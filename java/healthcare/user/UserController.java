package healthcare.user;

import java.security.Principal;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import healthcare.code.Code;
import healthcare.code.CodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final CodeService codeService;

    @GetMapping("/signup")
    public String signup(Model model, UserCreateForm userCreateForm) {
        return "user/signup_form";
    }

    @PostMapping("/signup")
    public String signup(@Valid UserCreateForm userCreateForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "user/signup_form";
        }

        if (!userCreateForm.getPasswd1().equals(userCreateForm.getPasswd2())) {
            bindingResult.rejectValue("passwd2", "passwordInCorrect",
                    "2개의 패스워드가 일치하지 않습니다.");
            return "user/signup_form";
        }

        try {
            userService.create(userCreateForm.getUserid(),
                    userCreateForm.getPasswd1(),
                    userCreateForm.getUser_name(), userCreateForm.getTelp(),
                    userCreateForm.getAddr(), userCreateForm.getGender());
        } catch (DataIntegrityViolationException e) {
            e.printStackTrace();
            bindingResult.reject("signupFailed", "이미 등록된 사용자입니다.");
            return "user/signup_form";
        } catch (Exception e) {
            e.printStackTrace();
            bindingResult.reject("signupFailed", e.getMessage());
            return "user/signup_form";
        }

        return "redirect:/";
    }

    @GetMapping("/login")
    public String login() {
        return "user/login_form";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/list")
    public String list(Model model, Principal principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            List<SiteUser> userList = this.userService.getList();
            model.addAttribute("userList", userList);
        } else {
            SiteUser user = this.userService.getUser(principal.getName());
            model.addAttribute("userList", List.of(user));
        }
        return "user/user_list";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/detail/{id}")
    public String detail(Model model, @PathVariable("id") Long id, Principal principal) {
        SiteUser userList = this.userService.getUser(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!userList.getUserid().equals(principal.getName()) &&
                auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        model.addAttribute("userList", userList);
        return "user/user_detail";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/delete/{id}")
    public String questionDelete(Principal principal, @PathVariable("id") Long id) {
        SiteUser userList = this.userService.getUser(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 관리자만 삭제 가능
        if (auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        this.userService.delete(userList);
        return "redirect:/";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String questionModify(Model model, @PathVariable("id") Long id, Principal principal) {
        // System.out.println("넘어온 ID @GetMapping: " + id);
        SiteUser userForm = this.userService.getUser(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 본인이 아니면서 AND 관리자 권한도 없는 경우에만 에러 발생 (즉, 관리자는 통과)
        if (!userForm.getUserid().equals(principal.getName()) &&
                auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        model.addAttribute("userForm", userForm);

        List<Code> codeListI = this.codeService.getList_I();
        model.addAttribute("codeListI", codeListI);
        List<Code> codeListS = this.codeService.getList_S();
        model.addAttribute("codeListS", codeListS);
        List<Code> codeListG = this.codeService.getList_G();
        model.addAttribute("codeListG", codeListG);

        return "user/user_form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify")
    public String questionModify(@Valid UserCreateForm userForm, BindingResult bindingResult,
            Principal principal,
            @org.springframework.web.bind.annotation.RequestParam(value = "codeId", required = false) Long codeId) {
        // if (bindingResult.hasErrors()) {
        // return "user_form";
        // }
        SiteUser user = this.userService.getUser(userForm.getUserid());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 본인이 아니면서 AND 관리자 권한도 없는 경우에만 에러 발생 (즉, 관리자는 통과)
        if (!user.getUserid().equals(principal.getName()) &&
                auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }

        System.out.println("===============================================" + userForm.getRole());

        Code code = null;
        if (codeId != null) {
            code = this.codeService.getCode(codeId);
        }

        String role = userForm.getRole();
        if (role == null || role.isEmpty()) {
            role = user.getRole();
        }

        this.userService.modify(user, userForm.getUser_name(), userForm.getTelp(), userForm.getAddr(),
                userForm.getGender(), role, code);

        // System.out.println(user.getUser_name() + user.getTelp());
        return String.format("redirect:/user/detail/%s", user.getId());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/withdraw/{id}")
    public String withdraw(Model model, @PathVariable("id") Long id, Principal principal) {
        SiteUser user = this.userService.getUser(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!user.getUserid().equals(principal.getName()) && !isAdmin) {
            return "redirect:/user/list";
        }
        model.addAttribute("id", id);
        return "user/user_withdraw";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/withdraw/{id}")
    public String withdraw(@PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestParam("password") String password,
            Principal principal, Model model) {
        SiteUser targetUser = this.userService.getUser(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // 1. Access Check
        if (!targetUser.getUserid().equals(principal.getName()) && !isAdmin) {
            return "redirect:/user/list";
        }

        // 2. Identify who is performing the action (Verifier)
        // If Admin is deleting another user, verify Admin's password.
        SiteUser verifier = isAdmin ? this.userService.getUser(principal.getName()) : targetUser;

        // 3. Verify Password
        // Note: verifier.passwd is encoded, password is raw. checkPassword handles
        // matches.
        if (this.userService.checkPassword(verifier, password)) {
            this.userService.delete(targetUser);

            // 4. Logout if Self-Deletion
            if (targetUser.getUserid().equals(principal.getName())) {
                return "redirect:/user/logout";
            }

            // If Admin deleted someone else
            return "redirect:/user/list";
        } else {
            model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
            model.addAttribute("id", id);
            return "user/user_withdraw";
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/password")
    public String passwordForm(Model model) {
        model.addAttribute("UserPassword", new UserPassword());
        return "user/user_passwd";
    }

    @PostMapping("/password")
    public String changePassword(
            Principal principal,
            @ModelAttribute("UserPassword") UserPassword form,
            Model model) {
        try {
            SiteUser user = userService.getUser(principal.getName());
            userService.changePassword(user, form);
            return "redirect:/user/detail/" + user.getId();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "user/user_passwd";
        }
    }
}