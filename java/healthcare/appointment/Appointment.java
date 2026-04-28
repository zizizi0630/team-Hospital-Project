package healthcare.appointment;

import java.time.LocalDateTime;

import healthcare.user.SiteUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String apmt_name;

    @Column(length = 20)
    private String apmt_telp;

    @Column(length = 255)
    private String apmt_addr;

    @Column(length = 20)
    private String apmt_gender;

    private LocalDateTime apmt_date;

    private LocalDateTime C_U_Date;

    @ManyToOne
    private healthcare.code.Code code;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @ManyToOne
    private SiteUser apmtAuthor;
}
