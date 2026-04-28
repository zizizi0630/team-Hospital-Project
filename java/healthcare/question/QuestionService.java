package healthcare.question;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import healthcare.DataNotFoundException;
import healthcare.answer.Answer;
import healthcare.user.SiteUser;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor
@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final healthcare.answer.AiAnswerService aiAnswerService;
    public Page<Question> getList(int page, String kw, SiteUser user) {
        List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(Sort.Order.desc("createDate"));
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
        if (user != null && ( "ADMIN".equals(user.getRole()) || "DOCTOR".equals(user.getRole()))) {
            // 관리자인 경우 전체 조회
            return this.questionRepository.findAllByKeyword(kw, pageable);
        } else if (user != null) {
            // 일반 사용자인 경우 본인 질문만 조회
            return this.questionRepository.findAllByKeywordAndAuthorId(kw, user.getId(), pageable);
        } else {
            // 비로그인 사용자 (정책에 따라 다름, 여기서는 빈 페이지 반환하거나 전체 조회 중 선택)
            // 현재 구조상 컨트롤러에서 비로그인 접근을 막고 있지 않다면 빈 페이지 반환이 안전
            // 하지만 기존 로직상 목록은 로그인 없이 볼 수 있었다면 전체 조회를 유지하거나 정책 결정 필요
            // 요청사항: "Patient인 경우에는 본인의 유저 아이디와 일치하는... Admin인 경우에는 DB 자료 전부"
            // 비로그인에 대한 명시는 없었으나, 보통 목록은 공개 or 비공개.
            // 여기서는 'Patient'라고 명시했으므로 로그인한 일반 유저를 의미.
            // 비로그인은 조회 불가로 하거나, 전체 공개로 하거나.
            // 안전하게 비로그인은 빈 페이지 리턴 혹은 로그인 유도?
            // 질문 목록 자체는 로그인 없이 볼 수 있는 정책이었음. (Controller에 @PreAuthorize 없음)
            // 사용자 요청 맥락상 "자신의 질문만 보게 하고 싶다"는 private 게시판 성격.
            // 비로그인 유저는 볼 수 없어야 함이 맞을 듯. 빈 페이지 반환.
            return Page.empty(pageable);
        }
    }
    public List<Question> getList() {
        return this.questionRepository.findAll();
    }
    public Question getQuestion(Long id) {
        Optional<Question> question = this.questionRepository.findById(id);
        if (question.isPresent()) {
            return question.get();
        } else {
            throw new DataNotFoundException("question not found");
        }
    }
    public void create(String subject, String content, SiteUser user) {
        Question q = new Question();
        q.setSubject(subject);
        q.setContent(content);
        q.setCreateDate(LocalDateTime.now());
        q.setAuthor(user);
        this.questionRepository.save(q);
        // AI 자동 답변 생성 (비동기 호출)
        this.aiAnswerService.generateAndSaveAiAnswer(q);
    }
    public void modify(Question question, String subject, String content) {
        question.setSubject(subject);
        question.setContent(content);
        question.setModifyDate(LocalDateTime.now());
        this.questionRepository.save(question);
    }
    public void delete(Question question) {
        this.questionRepository.delete(question);
    }
    public void vote(Question question, SiteUser siteUser) {
        question.getVoter().add(siteUser);
        this.questionRepository.save(question);
    }
    private Specification<Question> search(String kw) {
        return new Specification<>() {
            private static final long serialVersionUID = 1L;
            @Override
            public Predicate toPredicate(Root<Question> q, CriteriaQuery<?> query, CriteriaBuilder cb) {
                query.distinct(true); // 중복을 제거
                Join<Question, SiteUser> u1 = q.join("author", JoinType.LEFT);
                Join<Question, Answer> a = q.join("answerList", JoinType.LEFT);
                Join<Answer, SiteUser> u2 = a.join("author", JoinType.LEFT);
                return cb.or(cb.like(q.get("subject"), "%" + kw + "%"), // 제목
                        cb.like(q.get("content"), "%" + kw + "%"), // 내용
                        cb.like(u1.get("username"), "%" + kw + "%"), // 질문 작성자
                        cb.like(a.get("content"), "%" + kw + "%"), // 답변 내용
                        cb.like(u2.get("username"), "%" + kw + "%")); // 답변 작성자
            }
        };
    }
}