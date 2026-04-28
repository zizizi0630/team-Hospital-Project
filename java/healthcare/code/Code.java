package healthcare.code;

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
public class Code {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	//@Column(unique = true)
	@Column(length = 10)
	private String codeId;
	
	@Column(length = 20)
	private String code_Name1;

	@Column(length = 20)
	private String code_Name2;

	private LocalDateTime code_Date;

	@ManyToOne
    private SiteUser codeAuthor;

	@Column(length = 20)
	private String floor;

	@Column(length = 20)
	private String telp;
}
