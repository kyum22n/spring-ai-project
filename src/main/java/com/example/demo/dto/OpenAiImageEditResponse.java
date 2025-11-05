package com.example.demo.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

// {"data": [{"url": "xxxxx", "b64_json": "xxxxx"}, ... ]}  
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiImageEditResponse {
  private List<Image> data;
}


