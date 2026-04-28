package healthcare.answer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import healthcare.DataNotFoundException;
import healthcare.question.Question;
import healthcare.user.SiteUser;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AnswerService {

    private final AnswerRepository answerRepository;
    // AiService, UserService 제거 -> AiAnswerService로 이동
    private final AiAnswerService aiAnswerService;

    public Answer create(Question question, String content, SiteUser author) {
        Answer answer = new Answer();
        answer.setContent(content);
        answer.setCreateDate(LocalDateTime.now());
        answer.setQuestion(question);
        answer.setAuthor(author);
        this.answerRepository.save(answer);

        // 작성자가 AI(ID:4)가 아닐 경우에만 AI 자동 답변 트리거 - 별도 서비스로 위임
        if (author != null && (author.getId() == null || !author.getId().equals(4L))) {
            this.aiAnswerService.generateAndSaveAiAnswer(question);
        }

        return answer;
    }

    // createAiAnswer 메소드 제거됨 (AiAnswerService로 이동)

    public Answer getAnswer(Long id) {
        Optional<Answer> answer = this.answerRepository.findById(id);
        if (answer.isPresent()) {
            return answer.get();
        } else {
            throw new DataNotFoundException("answer not found");
        }
    }

    public void modify(Answer answer, String content) {
        answer.setContent(content);
        answer.setModifyDate(LocalDateTime.now());
        this.answerRepository.save(answer);
    }

    public void delete(Answer answer) {
        this.answerRepository.delete(answer);
    }

    public void vote(Answer answer, SiteUser siteUser) {
        answer.getVoter().add(siteUser);
        this.answerRepository.save(answer);
    }
}