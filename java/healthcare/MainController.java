package healthcare;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import healthcare.user.SiteUser;
import healthcare.user.UserService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class MainController {

    private final UserService userService;

    @GetMapping("/dtinfo")
    public String list(Model model) {
        List<SiteUser> doctorList = this.userService.getDoctorList();

        List<SiteUser> doctorListI = doctorList.stream()
                .filter(u -> u.getCode() != null && u.getCode().getCodeId().startsWith("I"))
                .toList();
        List<SiteUser> doctorListS = doctorList.stream()
                .filter(u -> u.getCode() != null && u.getCode().getCodeId().startsWith("S"))
                .toList();
        List<SiteUser> doctorListG = doctorList.stream()
                .filter(u -> u.getCode() != null && u.getCode().getCodeId().startsWith("G"))
                .toList();

        model.addAttribute("doctorListI", doctorListI);
        model.addAttribute("doctorListS", doctorListS);
        model.addAttribute("doctorListG", doctorListG);

        return "doctorInfo/doctor_info";
    }

    @GetMapping("/dtinfo/detail/{id}")
    public String detail(Model model, @PathVariable("id") Long id) {
        SiteUser siteUser = this.userService.getUser(id);
        model.addAttribute("siteUser", siteUser);

        return "doctorInfo/doctor_detail";
    }

    @GetMapping("/")
    public String root() {
        return "home_form";
    }
    
    @GetMapping("/codei_info")
    public String codeinfo1() {
        return "code_i_info";
    }

    @GetMapping("/codes_info")
    public String codeinfo2() {
        return "code_s_info";
    }
    
    @GetMapping("/codeg_info")
    public String codeinfo3() {
        return "code_g_info";
    }

    @GetMapping("/map")
    public String root2() {
        return "osm_map";
    }

    @GetMapping("/info")
    public String root3() {
        return "info";
    }
}