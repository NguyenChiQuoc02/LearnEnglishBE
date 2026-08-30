package com.personal.base.config.logging;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebFilterConfig {

  // Registered directly (not via @Component) so it is added to the servlet
  // container exactly once, running before Spring Security, and therefore
  // timing the full request including auth/authorization checks.
  @Bean
  public FilterRegistrationBean<RequestTimingFilter> requestTimingFilter() {
    FilterRegistrationBean<RequestTimingFilter> registration = new FilterRegistrationBean<>(new RequestTimingFilter());
    registration.addUrlPatterns("/api/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
