package com.example.demo.interview.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.interview.dao.InterviewAnswerDao;
import com.example.demo.interview.dao.InterviewSessionDao;
import com.example.demo.interview.dto.InterviewAnswers;
import com.example.demo.interview.dto.InterviewSession;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InterviewSessionService {

  // ChatClient
  private ChatClient chatClient;

  // DAO
  @Autowired
  private InterviewSessionDao interviewSessionDao;
  @Autowired
  private InterviewAnswerDao interviewAnswerDao;

  // 생성자
  public InterviewSessionService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // 면접 리포트 생성하기
  @Transactional
  public InterviewSession generateSessionReport(int sessionId) {

    // 세션별 모든 답변에 대한 피드백 조회
    List<InterviewAnswers> answers = interviewAnswerDao.selectAllAnswersBySessionId(sessionId);

    if (answers == null || answers.isEmpty()) {
      log.info("세션 {}에 대한 답이 없습니다.", sessionId);
      return null;
    }

    // 답변 피드백 요약 텍스트 구성
    StringBuilder sb = new StringBuilder();
    // extractSummary() 호출용
    ObjectMapper mapper = new ObjectMapper();

    for (int i=0; i<answers.size(); i++) {
      InterviewAnswers ans = answers.get(i);
      
      String summary = extractSummary(ans.getAnswerFeedback(), ans.getAnswerText(), mapper);

      sb.append("""
          Q%d: %s
          A%d 피드백 요약: %s  
      """.formatted(i + 1, ans.getQuestionId(), i + 1, summary));
    }

    String prompt = """
      당신은 취업 컨설턴트입니다.
      아래는 한 지원자의 면접 답변별 피드백 요약입니다.
      전체적으로 이 지원자의 면접 역량을 평가하고, 다음 JSON 형시으로 리포트를 작성하세요.
      
      {
        "overall_score": 0~100,
        "core_strengths": [],
        "improvement_points": [],
        "communication_score": 0~100,
        "technical_score": 0~100,
        "growth_advice": ""
      }

      면접 피드백 요약:
    """ + sb;

    // JSON 변환 (고수준)
    Map<String, Object> reportMap = chatClient.prompt()
      .user(prompt)
      .call()
      .entity(new MapOutputConverter());

    // Map → JSON 문자열 변환 (리포트 저장용 objectMapper)
    ObjectMapper objectMapper = new ObjectMapper();
    String reportFeedbackJson = null;
    try {
        reportFeedbackJson = objectMapper.writeValueAsString(reportMap);
    } catch (Exception e) {
        log.error("리포트 JSON 직렬화 오류", e);
    }

    // DB 저장
    InterviewSession session = new InterviewSession();
    session.setSessionId(sessionId);
    session.setReportFeedback(reportFeedbackJson);
    interviewSessionDao.updateSessionFeedback(session);

    return session;
  }

  // 세션 전체 조회
  public List<InterviewSession> getAllSessions() {
    return interviewSessionDao.selectAllInterviewSession();
  }

  // 세션 단건 조회
  public InterviewSession getSession(int sessionId) {
    return interviewSessionDao.selectInterviewSession(sessionId);
  }

  // 세션 생성
  @Transactional
  public void createSession(InterviewSession session) {
    interviewSessionDao.insertInterviewSession(session);
  }

  // 세션 삭제
  @Transactional
  public void removeSession(int sessionId) {
    interviewSessionDao.deleteInterviewSession(sessionId);
  }

  // JSON 문자열에서 "summary" 값을 꺼내기
  private String extractSummary(String feedbackJson, String fallback, ObjectMapper mapper) {
    if (feedbackJson == null || feedbackJson.isBlank()) {
      return fallback;
    }

    try {
      Map<String, Object> map = mapper.readValue(feedbackJson, Map.class);
      Object s = map.get("summary");
      return (s != null) ? String.valueOf(s) : fallback;
    } catch (Exception e) {
      log.warn("answerFeedback JSON 파싱 실패. fallback 사용", e);
      return fallback;
    }
      
  }
}
