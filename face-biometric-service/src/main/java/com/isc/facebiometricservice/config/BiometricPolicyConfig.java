package com.isc.facebiometricservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BiometricPolicyProperties.class)
public class BiometricPolicyConfig {
}
