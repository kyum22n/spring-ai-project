package com.example.demo.interview.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.interview.dto.InterviewAnswers;

@Mapper
public interface InterviewAnswerDao {
  // 모든 답변 목록
  List<InterviewAnswers> selectAllAnswers();
  
  // 면접 리포트에 해당하는 답변 목록
  List<InterviewAnswers> selectAllAnswers(int sessionId);

  // 답변 목록 단건 조회
  InterviewAnswers selecInterviewAnswers(int answerId);

  // 답변 + 피드백 저장
  int insertInterviewAnswer(InterviewAnswers interviewAnswer);

  // 피드백(JSONB)만 갱신 (AI 분석 결과 반영)
  int updateAnswerFeedback(InterviewAnswers interviewAnswers);

  // 답변 삭제
  int deleteInterviewAnswer(int answerId);
}
