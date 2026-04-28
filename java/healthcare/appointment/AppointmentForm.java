package healthcare.appointment;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppointmentForm {

    private Long id;

    @NotEmpty(message = "이름은 필수항목입니다.")
    @Size(max = 10)
    private String apmt_name;

    @NotEmpty(message = "전화번호는 필수항목입니다.")
    @Size(max = 20)
    private String apmt_telp;

    @NotEmpty(message = "주소는 필수항목입니다.")
    @Size(max = 20)
    private String apmt_addr;

    @NotEmpty(message = "성별은 필수항목입니다.")
    @Size(max = 20)
    private String apmt_gender;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @jakarta.validation.constraints.NotNull(message = "예약 날짜는 필수항목입니다.")
    private LocalDateTime apmt_date;

    // 진료과 코드 (ID)
    private String codeId;

    // 증상
    private String symptoms;
}
