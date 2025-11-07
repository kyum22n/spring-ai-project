package com.example.demo.interview.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.interview.dto.InterviewAnswers;
import com.example.demo.interview.dto.InterviewQuestions;
import com.example.demo.interview.dto.InterviewSession;
import com.example.demo.interview.service.InterviewAnswerService;
import com.example.demo.interview.service.InterviewQuestionService;
import com.example.demo.interview.service.InterviewSessionService;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/interview")
public class InterviewController {

  // Service
  @Autowired
  private InterviewSessionService interviewSessionService;
  @Autowired
  private InterviewQuestionService interviewQuestionService;
  @Autowired
  private InterviewAnswerService interviewAnswerService;
  
  // 면접 리포트 생성 (면접 종료 후)
  @PostMapping(
    value = "/make-report",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public InterviewSession makeReport(@RequestParam("sessionId") int sessionId) {
    return interviewSessionService.generateSessionReport(sessionId);
  }

  // 리포트 전체 조회
  @GetMapping("/report-list")
  public List<InterviewSession> reportList() {  
    return interviewSessionService.getAllSessions();
  }

  // 리포트 단건 조회
  @GetMapping("/report-one")
  public InterviewSession reportOne(@RequestParam("sessionId") int sessionId) {
    return interviewSessionService.getSession(sessionId);
  }

  // 면접 세션 생성
  @PostMapping("/make-session")
  public void makeSession(InterviewSession interviewSession) {
   interviewSessionService.createSession(interviewSession);
  }
  
  // 면접 리포트 삭제
  @DeleteMapping("/delete-report")
  public void deleteReport(@RequestParam("sessionId") int sessionId) {
    interviewSessionService.removeSession(sessionId);
  }

  // 질문 전체 조회
  @GetMapping("/question-list")
  public List<InterviewQuestions> questionList() {
    return interviewQuestionService.getAllQuestions();
  }

  // 면접 리포트에 해당하는 모든 질문
  @GetMapping("/report-question-list")
  public List<InterviewQuestions> reportQuestionList(@RequestParam("sessionId") int sessionId) {
    return interviewQuestionService.getAllQuestionsBySessionId(sessionId);
  }

  // 질문 단건 조회
  @GetMapping("/question-one")
  public InterviewQuestions questionOne(@RequestParam("questionId") int questionId) {
    return interviewQuestionService.getOneInterviewQuestion(questionId);
  }
  
  // AI 질문 생성 + 저장
  @PostMapping(
    value = "/make-ai-question",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  public List<InterviewQuestions> makeAIQuestion(
    @RequestParam("sessionId")int sessionId,
    @RequestParam("type")String type,
    @RequestParam("targetJob")String targetJob,
    @RequestParam("targetCompany")String targetCompany
  ) {
    return interviewQuestionService.generateAIQuestions(sessionId, type, targetJob, targetCompany);
  }
  
  // 사용자 질문 추가 + 저장
  @PostMapping("/add-custom-question")
  public void addCustomQuestion(
    @RequestParam("sessionId") int sessionId, 
    @RequestBody List<String> questionTexts
  ) throws Exception {
    interviewQuestionService.customQuestions(sessionId, questionTexts);
  }
  
  @DeleteMapping("/delete-question")
  public void deleteQuestion(@RequestParam("questionId") int questionId) {
    interviewQuestionService.removeInterviewQuestion(questionId);
  }
  
  // 면접 답변 업로드 + AI 피드백 생성
  @PostMapping(
    value = "/submitAnswer",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  public void submitAnswer(
    @RequestParam("audioFile") MultipartFile audioFile,
    @RequestParam("imagFile") MultipartFile imagFile,
    @RequestParam("questionId") int questionId,
    @RequestParam("sessionId") int sessionId
  ) throws Exception {
    interviewAnswerService.processInterviewAnswers(audioFile, imagFile, questionId, sessionId);
  }
  

  // 면접 리포츠에 해당하는 답변 목록 조회
  @GetMapping("/answer-list")
  public List<InterviewAnswers> answerList(@RequestParam("sessionId") int sessionId) {
    return interviewAnswerService.getAllAnswersBySessionId(sessionId);
  }

  // 면접 답변 단건 조회
  @GetMapping("/answer-one")
  public InterviewAnswers answerOne(@RequestParam("answerId") int answerId) {
    return interviewAnswerService.getOneInterviewAnswer(answerId);
  }
  
  // 답변 삭제
  @DeleteMapping("/delete-answer")
  public void deleteAnswer(@RequestParam("answerId") int answerId) {
    interviewAnswerService.removeInterviewAnswer(answerId);
  }
  
}
