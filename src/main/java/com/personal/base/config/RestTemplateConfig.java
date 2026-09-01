package com.personal.base.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestTemplateConfig {

  @Bean
  public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    // Spring Boot 4's default converter auto-detection prefers the new Jackson 3
    // (tools.jackson.databind) converter, which cannot deserialize the Jackson 2
    // com.fasterxml.jackson.databind.JsonNode type this app's external HTTP calls
    // (MomoService, ZaloOaService) rely on. Force Jackson 2 explicitly.
    restTemplate.setMessageConverters(List.of(
            new FormHttpMessageConverter(),
            new StringHttpMessageConverter(),
            new MappingJackson2HttpMessageConverter()));
    return restTemplate;
  }
}
