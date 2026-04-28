package healthcare.openai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/openai")
@Slf4j
public class AiController {
  // ##### 필드 #####
  @Autowired
  private AiService aiService;

  @Autowired
  private healthcare.user.UserService userService;

  // @Autowired
  // private AiServiceByChatClient aiService;

  // ##### 요청 매핑 메소드 #####
  @PostMapping(value = "/chat-model", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
  @ResponseBody // <-- 추가
  public String chatModel(@RequestParam("question") String question, java.security.Principal principal) {
    healthcare.user.SiteUser user = null;
    if (principal != null) {
      try {
        user = this.userService.getUser(principal.getName());
      } catch (Exception e) {
        log.warn("User not found for principal: {}", principal.getName());
      }
    }
    String answerText = aiService.generateText(question, user);
    return answerText;
  }

  @PostMapping(value = "/chat-model-stream", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_NDJSON_VALUE // 라인으로
                                                                                                                                                   // 구분된
                                                                                                                                                   // 청크
                                                                                                                                                   // 텍스트
  )
  public Flux<String> chatModelStream(@RequestParam("question") String question, java.security.Principal principal) {
    healthcare.user.SiteUser user = null;
    if (principal != null) {
      try {
        user = this.userService.getUser(principal.getName());
      } catch (Exception e) {
        log.warn("User not found for principal: {}", principal.getName());
      }
    }
    Flux<String> answerStreamText = aiService.generateStreamText(question, user);
    return answerStreamText;
  }

  // ##### 음성 합성 (TTS) #####
  @PostMapping(value = "/tts", produces = "audio/mp3")
  @ResponseBody
  public byte[] tts(@RequestParam("text") String text) {
    return aiService.generateSpeech(text);
  }

  // ##### 음성 인식 (STT) #####
  @PostMapping(value = "/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
  @ResponseBody
  public String stt(@RequestParam("file") MultipartFile file) {
    try {
      return aiService.transcribeAudio(file.getResource());
    } catch (Exception e) {
      log.error("STT Error", e);
      return "Error: " + e.getMessage();
    }
  }
}
