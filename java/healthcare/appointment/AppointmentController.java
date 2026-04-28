package healthcare.appointment;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
@RequestMapping("/apmt")
public class AppointmentController {
    // private final CodeService codeService;
    private final healthcare.code.CodeRepository codeRepository;
    private final UserService userService;
    private final AppointmentService apmtService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public String list(Model model, Principal principal) {
        List<Appointment> apmtList = null;
        SiteUser siteUser = this.userService.getUser(principal.getName());
        // System.out.println(principal.getName());
        // System.out.println(siteUser.getUser_name());
        apmtList = this.apmtService.getList();

        model.addAttribute("apmtList", apmtList);
        return "apmt/apmt_list";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/insert")
    public String insert(@ModelAttribute("apmtForm") AppointmentForm apmtForm, Model model) {
        model.addAttribute("codeList", this.codeRepository.findAll());
        return "apmt/apmt_insert";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/insert")
    public String insert(@Valid @ModelAttribute("apmtForm") AppointmentForm apmtForm, BindingResult bindingResult,
            Principal principal, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("codeList", this.codeRepository.findAll());
            return "apmt/apmt_insert";
        }
        SiteUser siteUser = this.userService.getUser(principal.getName());
        this.apmtService.insert(apmtForm.getApmt_name(), apmtForm.getApmt_telp(),
                apmtForm.getApmt_addr(), apmtForm.getApmt_date(), apmtForm.getApmt_gender(),
                apmtForm.getCodeId(), apmtForm.getSymptoms(), siteUser);

        return "redirect:/apmt/list";
    }

    @GetMapping(value = "/detail/{id}")
    public String detail(Model model, @PathVariable("id") Long id) {
        Appointment apmt = this.apmtService.getAppointment(id);
        model.addAttribute("apmt", apmt);
        return "apmt/apmt_detail";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/delete/{id}")
    public String appointmentDelete(Principal principal, @PathVariable("id") Long id) {
        Appointment apmt = this.apmtService.getAppointment(id);

        this.apmtService.delete(apmt);

        return "redirect:/apmt/list";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/modify/{id}")
    public String appointmentModify(Model model, @PathVariable("id") Long id, Principal principal) {
        // System.out.println("넘어온 ID @GetMapping: " + id);
        Appointment apmt = this.apmtService.getAppointment(id);

        // Entity -> Form 변환 및 데이터 주입
        AppointmentForm apmtForm = new AppointmentForm();
        apmtForm.setId(apmt.getId());
        apmtForm.setApmt_name(apmt.getApmt_name());
        apmtForm.setApmt_telp(apmt.getApmt_telp());
        apmtForm.setApmt_addr(apmt.getApmt_addr());
        apmtForm.setApmt_gender(apmt.getApmt_gender());
        apmtForm.setApmt_date(apmt.getApmt_date());

        if (apmt.getCode() != null) {
            apmtForm.setCodeId(apmt.getCode().getCodeId());
        }
        apmtForm.setSymptoms(apmt.getSymptoms());

        model.addAttribute("codeList", this.codeRepository.findAll());
        model.addAttribute("apmtForm", apmtForm);
        return "apmt/apmt_modify";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/modify/{id}")
    public String appointmentModify(@Valid @ModelAttribute("apmtForm") AppointmentForm apmtForm,
            BindingResult bindingResult,
            Principal principal, @PathVariable("id") Long id, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("codeList", this.codeRepository.findAll());
            return "apmt/apmt_modify";
        }
        Appointment apmt = this.apmtService.getAppointment(id);

        this.apmtService.modify(apmt, apmtForm.getApmt_name(), apmtForm.getApmt_telp(), apmtForm.getApmt_date(),
                apmtForm.getApmt_addr(), apmtForm.getCodeId(), apmtForm.getSymptoms());
        return String.format("redirect:/apmt/detail/%s", id);
    }
}
