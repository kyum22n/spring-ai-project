package com.example.demo.interview.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.interview.dao.InterviewQuestionDao;
import com.example.demo.interview.dto.InterviewQuestions;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InterviewQuestionService {

  // ChatClient
  private ChatClient chatClient;

  // DAO
  @Autowired
  private InterviewQuestionDao interviewQuestionDao;

  // 생성자
  public InterviewQuestionService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // 모든 질문 목록
  public List<InterviewQuestions> getAllQuestions() {
    return interviewQuestionDao.selectAllQuestions();
  }

  // 면접 리포트에 해당하는 모든 질문 목록
  public List<InterviewQuestions> getAllQuestionsBySessionId(int sessionId) {
    return interviewQuestionDao.selectAllQuestionsBySessionId(sessionId);
  }

  // 질문 단건 조회
  public InterviewQuestions getOneInterviewQuestion(int questionId) {
    return interviewQuestionDao.selectInterviewQuestion(questionId);
  }

  // AI 질문 저장
  @Transactional
  public List<InterviewQuestions> generateAIQuestions(int sessionId, String type, String targetJob, String targetCompany) {

    // Prompt
    String prompt = """
      당신은 전문 면접관입니다.
      아래 조건에 맞는 예상 면접 질문 5개를 생성하세요.
      JSON 리스트 형식으로 출력하세요.
      ["질문1", "질문2", "질문3", "질문4", "질문5"]    

      - 면접 유형: %s
      - 희망 직무: %s
      - 지원 기업: %s
    """.formatted(type, targetJob, targetCompany);

    // AI 응답 + List<String> 형태로 받기
    List<String> aiQuestions = chatClient.prompt()
      .user(prompt)
      .call()
      .entity(new ListOutputConverter());

    if (aiQuestions != null || aiQuestions.isEmpty()) {
      throw new RuntimeException("AI 예상 면접 질문 생성 실패");
    }

    // DTO 변환 + DB 저장
    List<InterviewQuestions> savedQuestions = new ArrayList<>();

    for (String q : aiQuestions) {
      InterviewQuestions question = new InterviewQuestions();
      
      question.setSessionId(sessionId);
      question.setQuestionText(q);
      question.setCreatedAt(LocalDateTime.now());

      interviewQuestionDao.insertInterviewQuestion(question);

      savedQuestions.add(question);
    }

    return savedQuestions;

  }

  // 사용자 질문 추가
  @Transactional
  public void customQuestions(int sessionId, List<String> questionTexts) throws Exception {
    for (String qText : questionTexts) {
      InterviewQuestions question = new InterviewQuestions();
      
      question.setSessionId(sessionId);
      question.setQuestionText(qText);
      question.setCreatedAt(LocalDateTime.now());

      interviewQuestionDao.insertInterviewQuestion(question);
    }
  }

  // 질문 삭제
  public int removeInterviewQuestion(int questionId) {
    return interviewQuestionDao.deleteInterviewQuestion(questionId);
  }
}

