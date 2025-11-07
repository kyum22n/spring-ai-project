package com.example.demo.interview.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Data;

@Data
public class InterviewSession {
  private Integer sessionId;  //면접
  private Integer userId; //사용자
  private String type;  //면접 유형(종합/직무)
  private String targetJob;  //희망 직무
  private String targetCompany; //희망 기업
  private String reportFeedback; //면접 피드백
  private LocalDateTime createdAt;  //생성일
}
