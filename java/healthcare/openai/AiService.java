package healthcare.openai;

import java.time.Duration; // Duration import 추가

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.openai.api.OpenAiAudioApi; // Added for Voice enum
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus; // HttpStatus import 추가
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException; // WebClientResponseException import 추가

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry; // Retry import 추가

@Service
@Slf4j
public class AiService {
        // ##### 필드 #####
        @Autowired
        private ChatModel chatModel;

        @Autowired
        private healthcare.code.CodeService codeService;

        @Autowired
        private ToolCallbackProvider toolCallbackProvider;

        @Autowired
        private OpenAiAudioSpeechModel speechModel;

        @Autowired
        private OpenAiAudioTranscriptionModel transcriptionModel;

        // ##### 메소드 #####
        // ##### 메소드 #####
        public String generateText(String question) {
                return generateText(question, null);
        }

        public String generateText(String question, healthcare.user.SiteUser user) {
                // 코드 정보 가져오기
                String codeCsv = codeService.getAllCodesAsCsv();

                // 사용자 정보 구성
                String userInfoContext = "";
                if (user != null) {
                        userInfoContext = String.format(
                                        "\n[현재 사용자 정보]\n이름: %s\nID(Username): %s\n성별: %s\n전화번호: %s\n주소: %s\n" +
                                                        "참고: 예약 도구(makeReservation) 사용 시 위 사용자 정보를 인자로 사용하세요. 사용자에게 이름을 다시 묻지 마세요.\n",
                                        user.getUser_name(), user.getUserid(), user.getGender(), user.getTelp(),
                                        user.getAddr());
                }

                // 현재 시간 정보 구성
                String timeContext = String.format("\n[현재 시스템 시간]\n%s\n",
                                java.time.LocalDateTime.now()
                                                .format(java.time.format.DateTimeFormatter
                                                                .ofPattern("yyyy-MM-dd EEEE HH:mm")
                                                                .withLocale(java.util.Locale.KOREA)));

                // 시스템 메시지 생성
                SystemMessage systemMessage = SystemMessage.builder()
                                .text("사용자 질문에 대해 한국어로 답변을 해야 합니다."
                                		+ "\n(잠시만 기다려 주세요)라는 말은 하지 마세요."
                                		+ "\n증상과 진료과 정보 그리고 예약과 예약 확인을 위해서, 질문과 답변을 명확하게 요구하세요."
                                                + "\n참고 데이터는 현재 병원에 존재하는 과 목록입니다.\n환자가 입력한 증상에 알맞은 과로 안내해 주세요."
                                                + "\n예약 가능 시간은 평일 오전 8시 부터 오후 5시 까지, 토요일 오전 8시 부터 정오 12시 까지 입니다."
                                                + "\n단, 점심 시간은 오후 12시 30분 부터 오후 1시 30분 까지라서, 예약이 불가능한 시간대입니다."
                                                + "\n환자가 예약을 원하면 먼저 증상에 맞는 '세부 진료과(CodeId)'를 선정하세요. (예: 내과 -> I-001 사용. 그룹 코드 'I' 사용 금지)"
                                                + "\n- 특정 날짜를 지정하면 'checkAvailability'를 사용하세요."
                                                + "\n- 주말 예약, 이번 주 예약 등 기간이나 범위로 문의하면 'findAvailableDates'를 사용하세요."
                                                + "\n  - 예: '이번 주말' -> filter='WEEKEND', startDate=오늘"
                                                + "\n  - 예: '이번 주' -> filter='ALL', startDate=오늘, endDate=이번주 토요일"
                                                + "\n이후 예약이 가능하면 예약을 진행(makeReservation)해주세요."
                                                + "\n[중요] checkAvailability 또는 findAvailableDates 도구 호출 결과가 'Closed'로 시작하거나 'Available Dates' 목록을 반환하면, 그 내용에 대한 의미를 반드시 사용자에게 전달해야 합니다.\n[참고 데이터]\n"
                                                + codeCsv + userInfoContext + timeContext)
                                .build();

                // 사용자 메시지 생성
                UserMessage userMessage = UserMessage.builder()
                                .text(question)
                                .build();

                // 대화 옵션 설정
                OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                                .model("gpt-4o-mini")
                                .temperature(0.3)
                                .maxTokens(1000)
                                .toolCallbacks(this.toolCallbackProvider.getToolCallbacks())
                                .build();

                // 프롬프트 생성
                Prompt prompt = Prompt.builder()
                                .messages(systemMessage, userMessage)
                                .chatOptions(chatOptions)
                                .build();

                // LLM에게 요청하고 응답받기
                ChatResponse chatResponse = chatModel.call(prompt);
                AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
                String answer = assistantMessage.getText();

                return answer;
        }

        public Flux<String> generateStreamText(String question) {
                return generateStreamText(question, null);
        }

        public Flux<String> generateStreamText(String question, healthcare.user.SiteUser user) {
                // 코드 정보 가져오기
                String codeCsv = codeService.getAllCodesAsCsv();

                // 사용자 정보 구성
                String userInfoContext = "";
                if (user != null) {
                        userInfoContext = String.format(
                                        "\n[현재 사용자 정보]\n이름: %s\nID(Username): %s\n성별: %s\n전화번호: %s\n주소: %s\n" +
                                                        "참고: 예약 도구(makeReservation) 사용 시 위 사용자 정보를 인자로 사용하세요. 사용자에게 이름을 다시 묻지 마세요.\n",
                                        user.getUser_name(), user.getUserid(), user.getGender(), user.getTelp(),
                                        user.getAddr());
                }

                // 현재 시간 정보 구성
                String timeContext = String.format("\n[현재 시스템 시간]\n%s\n",
                                java.time.LocalDateTime.now().format(
                                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE HH:mm")
                                                                .withLocale(java.util.Locale.KOREA)));

                // 시스템 메시지 생성
                SystemMessage systemMessage = SystemMessage.builder()
                                .text("사용자 질문에 대해 한국어로 답변을 해야 합니다."
                                                + "\n예약 가능 시간은 평일 오전 8시 부터 오후 5시 까지, 토요일 오전 8시 부터 정오 12시 까지 입니다."
                                                + "\n단, 점심 시간은 오후 12시 30분 부터 오후 1시 30분 까지라서, 예약이 불가능한 시간대입니다."
                                                + "\n[중요] 예약은 반드시 10분 단위(00분, 10분, 20분...)로만 가능합니다. (예: 10:15 불가, 10:10 또는 10:20 가능)"
                                                + "\n환자가 예약을 원하면 먼저 증상에 맞는 '세부 진료과(CodeId)'를 선정하세요. (예: 내과 -> I-001 사용. 그룹 코드 'I' 사용 금지)"
                                                + "\n- 특정 날짜를 지정하면 'checkAvailability'를 사용하세요."
                                                + "\n- 주말 예약, 이번 주 예약 등 기간이나 범위로 문의하면 'findAvailableDates'를 사용하세요."
                                                + "\n이후 예약이 가능하면 예약을 진행(makeReservation)해주세요."
                                                + "\n[중요] checkAvailability 또는 findAvailableDates 도구 호출 결과가 'Closed'로 시작하거나 'Available Dates' 목록을 반환하면, 그 내용을 반드시 사용자에게 그대로 전달해야 합니다. 임의로 요약하거나 생략하지 마세요.\n\n[참고 데이터]\n"
                                                + codeCsv + userInfoContext
                                                + timeContext)
                                .build();

                // 사용자 메시지 생성
                UserMessage userMessage = UserMessage.builder()
                                .text(question)
                                .build();

                // 대화 옵션 설정: gpt-4o -> gpt-4o-mini로 변경 (Rate Limit 완화)
                // 대화 옵션 설정: gpt-4o -> gpt-4o-mini로 변경 (Rate Limit 완화)
                OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                                .model("gpt-4o-mini") // gpt-4o -> gpt-4o-mini로 변경!
                                .temperature(0.3)
                                .maxTokens(1000)
                                .toolCallbacks(this.toolCallbackProvider.getToolCallbacks())
                                .build();

                // 프롬프트 생성
                Prompt prompt = Prompt.builder()
                                .messages(systemMessage, userMessage)
                                .chatOptions(chatOptions)
                                .build();

                // LLM에게 요청하고 응답받기
                Flux<ChatResponse> fluxResponse = chatModel.stream(prompt)
                                // 429 에러 발생 시 재시도 로직 추가
                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5)) // 최대 3번, 최소 5초 간격으로 재시도
                                                .filter(this::isRateLimitError) // 429 에러만 필터링
                                                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                                        log.error("Rate limit exceeded after retries: {}",
                                                                        retrySignal.failure().getMessage());
                                                        // 최종 실패 시 RuntimeException 대신 Flux의 에러 핸들링으로 넘김
                                                        return retrySignal.failure();
                                                }));

                Flux<String> fluxString = fluxResponse.map(chatResponse -> {
                        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
                        String chunk = assistantMessage.getText();
                        if (chunk == null)
                                chunk = "";
                        return chunk;
                });

                // 최종적으로 재시도 후에도 실패했을 때 사용자에게 안내 메시지 스트리밍
                return fluxString.onErrorResume(throwable -> {
                        log.error("스트림 최종 실패. Rate Limit 초과 또는 기타 오류: {}", throwable.getMessage());
                        // 사용자에게 보여줄 에러 메시지를 Flux 형태로 반환하여 화면에 표시
                        return Flux.just("[API 에러] API 사용량 제한 초과 또는 서버 오류: 잠시 후 다시 시도해주세요.\n");
                });

        }

        // ##### 음성 합성 (TTS) #####
        public byte[] generateSpeech(String text) {
                OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                                .model("tts-1")
                                .voice(OpenAiAudioApi.SpeechRequest.Voice.ONYX)
                                .speed(1.25)
                                .build();
                TextToSpeechPrompt speechPrompt = new TextToSpeechPrompt(text, speechOptions);
                TextToSpeechResponse response = speechModel.call(speechPrompt);
                return response.getResult().getOutput();
        }

        // ##### 음성 인식 (STT) #####
        public String transcribeAudio(Resource audioFile) {
                OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                                .model("whisper-1")
                                .language("ko")
                                .build();
                AudioTranscriptionPrompt transcriptionPrompt = new AudioTranscriptionPrompt(audioFile,
                                transcriptionOptions);
                AudioTranscriptionResponse response = transcriptionModel.call(transcriptionPrompt);
                return response.getResult().getOutput();
        }

        /**
         * WebClientResponseException에서 429 Too Many Requests 에러인지 확인하는 헬퍼 메서드
         */
        private boolean isRateLimitError(Throwable throwable) {
                if (throwable instanceof WebClientResponseException) {
                        return ((WebClientResponseException) throwable).getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
                }
                return false;
        }
}