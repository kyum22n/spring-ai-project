package com.example.demo.interview.service;

import java.util.List;

import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import com.example.demo.interview.dao.InterviewAnswerDao;
import com.example.demo.interview.dao.InterviewQuestionDao;
import com.example.demo.interview.dao.InterviewSessionDao;
import com.example.demo.interview.dto.InterviewAnswers;

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
    return interviewAnswerDao.selectInterviewAnswers(answerId);
  }

  // 답변 + 피드백 저장========================================================

  // STT
  public String submitInterviewAnswer(String fileName, byte[] bytes) {

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
  
  // 이미지 분석
  public Flux<String> imageAnalysis(String answer, String contentType, byte[] bytes) {

    // 시스템 메시지
    SystemMessage systemMessage = SystemMessage.builder()
      .text("""
        당신은 이미지 분석 전문가입니다.
        사용자의 답변에 맞게 이미지를 분석하고 어쩌구 하세요.
      """)
      .build();

    // 이미지 데이터를 ByteArrayResource로 생성
    Resource resource = new ByteArrayResource(bytes);

    // 미디어 객체 생성
    Media media = Media.builder()
      .mimeType(MimeType.valueOf(contentType))
      .data(resource)
      .build();

    // 사용자 메시지
    UserMessage userMessage = UserMessage.builder()
      .text(answer)
      .media(media)
      .build();

    // Stream으로 프롬프트 생성
    Flux<String> fluxString = chatClient.prompt()
      .messages(systemMessage, userMessage)
      .stream()
      .content();

    return fluxString;
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
