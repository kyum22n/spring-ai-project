package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

// {"url": "xxxxx", "b64_json": "xxxxx"} 
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Image {
  private String url;
  private String b64_json;
}
