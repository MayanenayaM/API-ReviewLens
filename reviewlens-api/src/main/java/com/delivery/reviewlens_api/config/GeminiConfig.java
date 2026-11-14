package com.delivery.reviewlens_api.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiConfig {
  
  @Bean
  public RestClient geminiRestClient(
          @Value("${gemini.api.url}") String baseUrl,
          @Value("${gemini.api.key}") String apiKey,
          @Value("${gemini.api.timeout-segundos:10}") long timeoutSegundos) {

      HttpClient httpClient = HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(5))
              .build();

      JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
      requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSegundos));

      return RestClient.builder()
              .baseUrl(baseUrl)
              .defaultHeader("x-goog-api-key", apiKey)
              .requestFactory(requestFactory)
              .build();
    }
}
