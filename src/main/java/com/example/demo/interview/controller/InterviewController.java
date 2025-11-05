package com.example.demo.interview.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/interview")
public class InterviewController {

  // 질문 목록 조회
  @GetMapping("/question-list")
  public String getMethodName(@RequestParam String param) {
      return new String();
  }
  
  // 면접 리포트 목록 조회
  @GetMapping("/report-list")
  public String getMethodName(@RequestParam String param) {
      return new String();
  }

  
  
}
