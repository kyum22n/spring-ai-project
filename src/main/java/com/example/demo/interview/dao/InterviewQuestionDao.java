package com.example.demo.interview.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.interview.dto.InterviewQuestions;

@Mapper
public interface InterviewQuestionDao {
  // 모든 질문 목록
  List<InterviewQuestions> selectAllQuestions();
  
  // 면접 리포트에 해당하는 모든 질문 목록
  List<InterviewQuestions> selectAllQuestionsBySessionId(int sessionId);

  // 질문 단건 조회
  InterviewQuestions selectInterviewQuestion(int questionId);

  // 질문 저장
  int insertInterviewQuestion(InterviewQuestions interviewQuestions);

  // 질문 삭제
  int deleteInterviewQuestion(int questionId);
}
