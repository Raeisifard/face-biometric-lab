package com.isc.facebiometricservice.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final BiometricProperties p; public WebConfig(BiometricProperties p){this.p=p;}
  @Override public void addCorsMappings(CorsRegistry r){r.addMapping("/api/**").allowedOrigins(p.cors().allowedOrigins().split(",")).allowedMethods("GET","POST","OPTIONS").allowedHeaders("*");}
}
