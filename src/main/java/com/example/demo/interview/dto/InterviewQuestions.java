package com.example.demo.interview.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InterviewQuestions {
  private Integer questionId; //면접 질문
  private Integer sessionId;  //면접
  private String questionText;  //면접 질문 내용
  private LocalDateTime createdAt;  //생성일
}
