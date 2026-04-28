package healthcare.code;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import healthcare.user.SiteUser;
import healthcare.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
@RequestMapping("/code")
public class CodeController {

    private final CodeService codeService;
    private final UserService userService;

    @GetMapping("/list")
    public String list(Model model) {
        List<Code> codeList = null;
        codeList = this.codeService.getList();

        model.addAttribute("codeList", codeList);
        return "code/code_list";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/insert")
    public String insert(CodeForm codeForm) {
        return "code/code_insert";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/insert")
    public String insert(@Valid CodeForm codeForm, BindingResult bindingResult, Principal principal) {
        if (bindingResult.hasErrors()) {
            return "code/code_insert";
        }
        SiteUser siteUser = this.userService.getUser(principal.getName());
        if (principal.getName().equals("admin")) {
            this.codeService.insert(codeForm.getCodeId(), codeForm.getCode_Name1(),
                    codeForm.getCode_Name2(), codeForm.getFloor(), codeForm.getTelp(), siteUser);
        }

        return "redirect:/code/list";
    }

    @GetMapping(value = "/detail/{id}")
    public String detail(Model model, @PathVariable("id") Long id) {
        Code code = this.codeService.getCode(id);
        model.addAttribute("code", code);
        return "code/code_detail";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/delete/{id}")
    public String codeDelete(Principal principal, @PathVariable("id") Long id) {
        Code code = this.codeService.getCode(id);
        if (!principal.getName().equals("admin")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
        }

        if (principal.getName().equals("admin")) {
            this.codeService.delete(code);
        }

        return "redirect:/code/list";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String sungModify(Model model, @PathVariable("id") Long id, Principal principal) {
        // System.out.println("넘어온 ID @GetMapping: " + id);
        Code code = this.codeService.getCode(id);
        if (!code.getCodeAuthor().getUserid().equals(principal.getName())) {
            return "redirect:/code/list";
        }
        model.addAttribute("code", code);
        return "code/code_modify";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify")
    public String sungModify(@Valid CodeForm codeForm, BindingResult bindingResult,
            Principal principal) {
        Code code = this.codeService.getCodeId(codeForm.getCodeId());
        if (!code.getCodeAuthor().getUserid().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }

        if (principal.getName().equals("admin")) {
            this.codeService.modify(code, codeForm.getCodeId(), codeForm.getCode_Name1(), codeForm.getCode_Name2(),
                    codeForm.getFloor(), codeForm.getTelp());
        }
        return String.format("redirect:/code/detail/%s", code.getId());
    }

}