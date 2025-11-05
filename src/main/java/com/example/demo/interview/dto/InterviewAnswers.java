package com.example.demo.interview.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Data;

@Data
public class InterviewAnswers {
  private Integer answerId; //답변
  private Integer questionId; //질문
  private Integer sessionId;  //면접
  private String answerText;  //STT 변환 텍스트(답변 내용)
  private Map<String, Object> aiFeedback; //피드백(질문 단위)
  private LocalDateTime createdAt;  //생성일
}
