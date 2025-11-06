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
    for (int i=0; i<answers.size(); i++) {
      InterviewAnswers ans = answers.get(i);
      
      // 답변의 항목이 존재할 경우 summary 키의 값을 가져옴
      Object summary = (ans.getAiFeedback() != null)
              ? ans.getAiFeedback().get("summary")
              : ans.getAnswerText();

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

    // DB 저장
    InterviewSession session = new InterviewSession();
    session.setSessionId(sessionId);
    session.setReportFeedback(reportMap);
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

}
