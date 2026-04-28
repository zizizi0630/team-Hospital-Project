package healthcare.code;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodeForm {
    @NotEmpty(message="코드는 필수항목입니다.")
    @Size(max=10)
	private String codeId;
	
    @NotEmpty(message="코드명칭은 필수항목입니다.")
    @Size(max=20)
	private String code_Name1;

    @NotEmpty(message="코드 상세설명은 필수항목입니다.")
    @Size(max=20)
	private String code_Name2;
    
    @NotEmpty(message="층수는 필수항목입니다.")
    @Size(max=20)
	private String floor;
    
    @NotEmpty(message="전화번호는 필수항목입니다.")
    @Size(max=20)
	private String telp;

}