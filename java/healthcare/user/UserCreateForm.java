package healthcare.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateForm {
    @Size(min = 3, max = 15)
    @NotEmpty(message = "사용자ID는 필수항목입니다.")
    private String userid;

    @NotEmpty(message = "비밀번호는 필수항목입니다.")
    private String passwd1;

    @NotEmpty(message = "비밀번호 확인은 필수항목입니다.")
    private String passwd2;

    @NotEmpty(message = "이름은 필수항목입니다.")
    private String user_name;

    @NotEmpty(message = "연락처는 필수항목입니다.")
    private String telp;
    
    @NotEmpty(message = "주소는 필수항목입니다.")
    private String addr;

    @NotEmpty(message = "성별선택은 필수항목입니다.")
    private String gender;
    
    private String role;
}