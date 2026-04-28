package healthcare.answer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import healthcare.DataNotFoundException;
import healthcare.openai.AiService;
import healthcare.question.Question;
import healthcare.user.SiteUser;
import healthcare.user.UserService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AiAnswerService {

    private final AnswerRepository answerRepository;
    private final AiService aiService;
    private final UserService userService;

    @Async
    public void generateAndSaveAiAnswer(Question question) {
        // 대화 히스토리 구성
        StringBuilder history = new StringBuilder();
        history.append("[질문]\n");
        history.append(String.format("제목: %s\n내용: %s\n\n", question.getSubject(), question.getContent()));

        history.append("[이전 답변 내역]\n");
        // DB에서 최신 답변 내역 조회
        List<Answer> answers = this.answerRepository.findByQuestionId(question.getId());
        if (answers != null) {
            for (Answer ans : answers) {
                String authorName = (ans.getAuthor() != null) ? ans.getAuthor().getUserid() : "익명";
                history.append(String.format("- [%s]: %s\n", authorName, ans.getContent()));
            }
        }

        // 날짜/시간 포맷터
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        history.append(String.format("[질문 생성 일시]: %s\n", question.getCreateDate().format(formatter)));

        if (answers != null && !answers.isEmpty()) {
            // 최신 답변 (리스트의 마지막 요소로 가정)
            Answer lastAnswer = answers.get(answers.size() - 1);
            LocalDateTime lastDate = lastAnswer.getModifyDate();
            if (lastDate == null) {
                lastDate = lastAnswer.getCreateDate();
            }
            history.append(String.format("[최신 답변 생성 일시]: %s\n", lastDate.format(formatter)));
        }
        history.append(String.format("[현재 시스템 일시]: %s\n\n", LocalDateTime.now().format(formatter)));

        // AI 답변 생성 요청 (히스토리 포함)
        String aiResponse = aiService.generateText(history.toString(), question.getAuthor());

        // AI 계정(ID: 4)을 답변 작성자로 설정
        SiteUser aiAuthor = null;
        try {
            aiAuthor = this.userService.getUser(4L);
        } catch (DataNotFoundException e) {
            // ID 4번 유저가 없을 경우 null 유지
        }

        // 답변 저장 (AnswerService.create를 호출하지 않고 직접 저장하여 순환 참조 방지)
        Answer answer = new Answer();
        answer.setContent(aiResponse + "\n\n*추가적인 질문이 있으실까요?");
        answer.setCreateDate(LocalDateTime.now());
        answer.setQuestion(question);
        answer.setAuthor(aiAuthor);
        this.answerRepository.save(answer);
    }
}
