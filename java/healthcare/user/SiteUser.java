package healthcare.user;

import java.util.List;

import healthcare.answer.Answer;
import healthcare.appointment.Appointment;
import healthcare.code.Code;
import healthcare.question.Question;
import healthcare.schedule.Schedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class SiteUser {

    @ManyToOne
    @jakarta.persistence.JoinColumn(name = "code_id")
    private Code code;

    @Column(name = "code_id", insertable = false, updatable = false)
    private Long code_id;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String userid;

    private String passwd;

    @Column(length = 20)
    private String user_name;

    @Column(length = 20)
    private String telp;

    @Column(length = 200)
    private String addr;

    @Column(length = 20)
    private String gender;

    private String role;

    @Column(columnDefinition = "TEXT")
    private String content1;

    @Column(columnDefinition = "TEXT")
    private String content2;

    @Column(columnDefinition = "TEXT")
    private String content3;
    
    @OneToMany(mappedBy = "author")
    private List<Question> questionList;

    @OneToMany(mappedBy = "author")
    private List<Answer> answerList;
    
    @OneToMany(mappedBy = "author") // Schedule의 author 필드와 매핑
    private List<Schedule> scheduleList;

    @OneToMany(mappedBy = "apmtAuthor") // Appointment의 apmtAuthor 필드와 매핑
    private List<Appointment> appointmentList;
    
    @OneToMany(mappedBy = "codeAuthor")
    private List<Code> codeList;
}