package com.example.demo.interview.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.interview.dto.InterviewSession;

@Mapper
public interface InterviewSessionDao {
  // 면접 조회(전체)
  List<InterviewSession> selectAllInterviewSession();
  
  // 면접 조회(단건)
  InterviewSession selectInterviewSession(int sessionId);
  
  // 면접 생성
  int insertInterviewSession(InterviewSession interviewSession);

  // 면접 요약/피드백 생성
  int updateSessionFeedback(InterviewSession interviewSession);
  
  // 면접 삭제
  int deleteInterviewSession(int sessionId);
}
