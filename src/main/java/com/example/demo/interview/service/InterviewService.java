package com.example.demo.interview.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InterviewService {

  // ChatClient
  private ChatClient chatClient;

  // 생성자
  public InterviewService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // 모든 면접 리포트 목록 조회
  
}
