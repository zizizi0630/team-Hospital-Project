package healthcare.code;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import healthcare.DataNotFoundException;
import healthcare.user.SiteUser;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CodeService {

    private final CodeRepository codeRepository;

    public List<Code> getList() {
        return this.codeRepository.findAll();
    }

    // userid를 키워드로 검색하는 서비스 메서드
    public List<Code> searchCodeByUserid(String userid) {
        // 실제 LIKE 쿼리를 위해 % 와일드카드를 추가합니다.
        String searchKeyword = "%" + userid + "%";
        return codeRepository.findBycodeAuthor_useridLike(searchKeyword);
    }
    
    public List<Code> getList_I() {
        return this.codeRepository.findBycodeKeyword("I");
    }

    public List<Code> getList_S() {
        return this.codeRepository.findBycodeKeyword("S");
    }

    public List<Code> getList_G() {
        return this.codeRepository.findBycodeKeyword("G");
    }
    
    public Code insert(String codeId, String code_Name1, String code_Name2, String floor, String telp, SiteUser user) {
        Code code = new Code();
        code.setCodeId(codeId);
        code.setCode_Name1(code_Name1);
        code.setCode_Name2(code_Name2);
        code.setFloor(floor);
        code.setTelp(telp);
        code.setCode_Date(LocalDateTime.now());
        code.setCodeAuthor(user);
        this.codeRepository.save(code);
        return code;
    }

    public Code getCode(Long id) {
        Optional<Code> code = this.codeRepository.findById(id);
        if (code.isPresent()) {
            return code.get();
        } else {
            throw new DataNotFoundException("code not found");
        }
    }

    public Code getCodeId(String codeId) {
        Optional<Code> code = this.codeRepository.findBycodeId(codeId);
        if (code.isPresent()) {
            return code.get();
        } else {
            throw new DataNotFoundException("code not found");
        }
    }

    public void delete(Code code) {
        this.codeRepository.delete(code);
    }

    public void modify(Code code, String codeId, String code_Name1, String code_Name2, String floor, String telp) {
        code.setCodeId(codeId);
        code.setCode_Name1(code_Name1);
        code.setCode_Name2(code_Name2);
        code.setFloor(floor);
        code.setTelp(telp);
        code.setCode_Date(LocalDateTime.now());
        this.codeRepository.save(code);
    }
    
    public String getAllCodesAsCsv() {
        List<Code> allCodes = this.codeRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("CodeId,CodeName,Floor,Telp,Description\n"); // Header (Optional, but good for AI)
        for (Code code : allCodes) {
            sb.append(String.format("%s,%s,%s,%s,%s\n",
                    code.getCodeId(),
                    code.getCode_Name1(),
            		code.getFloor() != null ? code.getFloor() : "",
            		code.getTelp() != null ? code.getTelp() : "",
            		code.getCode_Name2() != null ? code.getCode_Name2() : ""));
        }
        return sb.toString();
    }
}