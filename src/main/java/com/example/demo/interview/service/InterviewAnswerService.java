package com.example.demo.interview.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.interview.dao.InterviewAnswerDao;
import com.example.demo.interview.dao.InterviewQuestionDao;
import com.example.demo.interview.dao.InterviewSessionDao;
import com.example.demo.interview.dto.InterviewAnswers;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class InterviewAnswerService {

  // ChatClient
  private ChatClient chatClient;
  // STT
  private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

  // DAO
  @Autowired
  private InterviewSessionDao interviewSessionDao;
  @Autowired
  private InterviewQuestionDao interviewQuestionDao;
  @Autowired
  private InterviewAnswerDao interviewAnswerDao;


  // 생성자
  public InterviewAnswerService(
    ChatClient.Builder chatClientBuilder,
    OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel
  ) {
    this.chatClient = chatClientBuilder.build();
    this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
  }

  // 모든 답변 목록
  public List<InterviewAnswers> getAllAnswers() {
    return interviewAnswerDao.selectAllAnswers();
  }

  // 면접 리포트에 해당하는 답변 목록
  public List<InterviewAnswers> getAllAnswersBySessionId(int sessionId) {
    return interviewAnswerDao.selectAllAnswersBySessionId(sessionId);
  }

  // 답변 단건 조회
  public InterviewAnswers getOneInterviewAnswer(int answerId) {
    InterviewAnswers answer = interviewAnswerDao.selectInterviewAnswers(answerId);
    return answer;
  }

  // 답변 + 피드백 저장===============================================================================================

  /*===================
    1. 답변 + 피드백 저장
  ====================*/
  @Transactional
  public InterviewAnswers processInterviewAnswers(MultipartFile audioFile, MultipartFile imageFile, int questionId, int sessionId) throws Exception {

    String transcript = stt(audioFile.getOriginalFilename(), audioFile.getBytes()); //음석 인식 결과
    String visualFeedback = analyzeImage(imageFile); //시각적 피드백
    
    // AI 피드백 생성
    Map<String, Object> answerFeedbackMap = generateAIFeedback(transcript, visualFeedback); //통합 피드백demoApplication

    // Map -> JSON 문자열 변환(MyBatis)
    ObjectMapper objectMapper = new ObjectMapper();
    String answerFeedback = objectMapper.writeValueAsString(objectMapper);
    
    // DB 저장
    InterviewAnswers answer = new InterviewAnswers();
    answer.setQuestionId(questionId);
    answer.setSessionId(sessionId);
    answer.setAnswerText(transcript);
    // JSON을 String으로 전달
    answer.setAnswerFeedback(answerFeedback);
    answer.setCreatedAt(LocalDateTime.now());
    
    interviewAnswerDao.insertInterviewAnswer(answer);
    
    return answer;
    
  }
  
  /*===============
  2. STT
  ================*/
  public String stt(String fileName, byte[] bytes) {

    // 음성 데이터를 ByteArrayResource로 생성
    Resource audiResource = new ByteArrayResource(bytes) {
      // 파일 이름&포맷 전달
      @Override
      public String getFilename() {
        return fileName;
      }
    };
    
    // AudioTranscriptionOptions 생성해서 옵션 지정, 힌트 제공
    AudioTranscriptionOptions audioTranscriptionOptions = OpenAiAudioTranscriptionOptions.builder()
      .model("whisper-1")
      .language("ko")
      .build();
      
      // Prompt
      AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audiResource, audioTranscriptionOptions);
      
      // LLM에 프롬프트 전달 + 응답
      AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(prompt);
      String text = response.getResult().getOutput();
      
      return text;
    } 

  /*===============
    3. 이미지 분석
  ================*/
  private String analyzeImage(MultipartFile file) throws Exception {
    
    // 이미지 데이터를 ByteArrayResource로 생성
    Resource resource = new ByteArrayResource(file.getBytes()){
      @Override
      public String getFilename() {
        return file.getOriginalFilename();
      }
    };

    // 미디어 객체 생성
    Media media = Media.builder()
      .mimeType(MimeType.valueOf(file.getContentType()))
      .data(resource)
      .build();

    // 시스템 메시지
    SystemMessage systemMessage = SystemMessage.builder()
      .text("""
        당신은 면접관입니다.
        지원자의 표정, 시선, 감정 상태를 분석하세요.
      """)
      .build();

    // 사용자 메시지
    UserMessage userMessage = UserMessage.builder()
      .text("이 지원자의 표정, 인상, 시선, 감정 상태를 100자 이내로 요약해줘")
      .media(media)
      .build();

    // Stream으로 프롬프트 생성
    String result = chatClient.prompt()
      .messages(systemMessage, userMessage)
      .call()
      .content();

    return result;
  }

  /*=================
    4. AI 피드백 생성
  ==================*/
  private Map<String, Object> generateAIFeedback(String transcript, String visualFeedback) {
    
    // 시스템 메시지
    String systemPrompt = """
      당신은 면접 피드백 전문가입니다.
      다음 지원자의 답변 내용을 평가하고, 말의 논리성, 태도, 어조, 표현력, 핵심 전달력을 5가지 관점에서 피드백하세요.
      각 항목은 한 문장으로 구체적으로 설명해주세요.
    """;

    // 사용자 메시지
    String userPrompt = "답변 내용:\n" + transcript;
    if (visualFeedback != null) {
      userPrompt += "\n\n시각 분석 결과:\n" + visualFeedback;
    }

    // LLM에 프롬프트 전달
    Map<String, Object> aiFeedback = chatClient.prompt()
      .system(systemPrompt)
      .user(userPrompt)
      .call()
      .entity(new MapOutputConverter());

    return aiFeedback;
  }

  // ===========================================================================

  // 피드백 갱신
  public int modifyAnswerFeedback(InterviewAnswers interviewAnswers) {
    return interviewAnswerDao.updateAnswerFeedback(interviewAnswers);
  }

  // 답변 삭제
  public int removeInterviewAnswer(int answerId) {
    return interviewAnswerDao.deleteInterviewAnswer(answerId);
  }
}
