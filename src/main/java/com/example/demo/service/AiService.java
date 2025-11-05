package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.dto.OpenAiImageEditResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AiService {
  private ChatClient chatClient;

  @Autowired
  private ImageModel imageModel;

  public AiService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // 이미지 분석
  public Flux<String> imageAnalysis(String question, String contentType, byte[] bytes) {

    // 시스템 메시지 생성
    SystemMessage systemMessage = SystemMessage.builder()
      .text("""
        당신은 이미지 분석 전문가입니다.
        사용자의 질문에 맞게 이미지를 분석하고 답변을 한국어로 하세요.
      """)
      .build();

    // 리소스 객체 생성(바이트 배열 처리)
    Resource resource = new ByteArrayResource(bytes);

    // 미디어 객체 생성
    // 방법 1
    // Media media = new Media(MimeType.valueOf(contentType), resource);
    // 방법 2
    Media media = Media.builder()
      .mimeType(MimeType.valueOf(contentType))
      .data(resource)
      .build();

    // 사용자 메시지 생성
    UserMessage userMessage = UserMessage.builder()
      .text(question)
      .media(media)
      .build();

    // Stream으로 프롬프트 생성
    Flux<String> fluxString = chatClient.prompt()
      .messages(systemMessage, userMessage)
      .stream()
      .content();

    return fluxString;
    
  }

  // 이미지 생성
  public String generateImage(String description) {
    String englishDescription = koToEn(description);

    // 방법 1
    List<ImageMessage> imageMessageList = new ArrayList<>();
    ImageMessage imageMessage = new ImageMessage(englishDescription);
    imageMessageList.add(imageMessage);
    
    // 방법 2
    // ImageMessage imageMessage = new ImageMessage(englishDescription);
    // List<ImageMessage> listImageMessages = List.of(imageMessage);

    // ImageOptions imageOptions = OpenAiImageOptions.builder()
    //   .model("dall-e-3")  // default
    //   .responseFormat("b64_json") // 또는 url
    //   .width(1024)
    //   .height(1024)
    //   .N(1)
    //   .build();
    
    ImageOptions imageOptions = OpenAiImageOptions.builder()
      .model("gpt-image-1")
      .width(1536)
      .height(1024)
      .N(1)
      .build();

    ImagePrompt imagePrompt = new ImagePrompt(imageMessageList, imageOptions);

    ImageResponse imageResponse = imageModel.call(imagePrompt);
    
    String b64Json = imageResponse.getResult().getOutput().getB64Json();

    return b64Json;
  }

  // 한글 -> 영어 번역
  public String koToEn(String str) {
    String translatedStr = chatClient.prompt()
      .system("당신은 번역사입니다. 사용자의 한국어 질문을 영어 질문으로 변환시키세요.")
      .user(str)
      .call()
      .content();

    return translatedStr;
  }

  // 이미지 편집
  @Value("${spring.ai.openai.api-key}")
  private String openAiApikey;

  public String editImage(String description, byte[] originalImage, byte[] maskImage) {
    WebClient webClient = WebClient.builder()
      .baseUrl("https://api.openai.com/v1/images/edits")
      .defaultHeader("Authorization", "Bearer " + openAiApikey)
      .exchangeStrategies(ExchangeStrategies.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(1024*1024*20))
        .build()
      )
      .build();

    // 파일 이름 & Content type 얻기(Content type은 파일 이름에서 확장자를 통해 얻을 수 있음)
    // 익명 자식 객체를 상속받아 메소드를 재정의
    Resource originalResource = new ByteArrayResource(originalImage) {
      @Override
      public String getFilename() {
        return "original.png";
      }
    };
    Resource maskResource = new ByteArrayResource(maskImage) {
      @Override
      public String getFilename() {
        return "mask.png";
      }
    };

    MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
    multiValueMap.add("model", "gpt-image-1");  //문자파트
    multiValueMap.add("image", originalResource);  //파일파트
    multiValueMap.add("mask", maskResource); //파일파트
    multiValueMap.add("prompt", koToEn(description)); //문자파트
    multiValueMap.add("n", "1");  //문자파트
    multiValueMap.add("size", "1536x1024"); //문자파트
    multiValueMap.add("quality", "low"); //문자파트

    // 이미지 하나를 요청하기 때문에 Flux가 아니라 Mono를 사용(비동기)
    // {"data": [{"url": "xxxxx", "b64_json": "xxxxx"}, ... ]}  
    Mono<OpenAiImageEditResponse> mono = webClient.post()
      .contentType(MediaType.MULTIPART_FORM_DATA) // 텍스트 + 이미지
      .body(BodyInserters.fromMultipartData(multiValueMap)) // 멀티파트 데이터로 body에 넣어줌
      .retrieve()
      .bodyToMono(OpenAiImageEditResponse.class); // 해당 객체 타입으로 비동기로 오는 데이터를 받음

    // 비동기 응답이 완료될 때까지 기다리고 완료된 객체 얻기
    OpenAiImageEditResponse editResponse = mono.block(); // 비동기를 동기 방식으로 기다리기
    String b64Json = editResponse.getData().get(0).getB64_json();

    return b64Json;
  }
}
