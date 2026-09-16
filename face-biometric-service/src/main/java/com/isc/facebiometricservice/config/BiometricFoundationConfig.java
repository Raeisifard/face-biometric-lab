package com.isc.facebiometricservice.config;

import com.isc.facebiometricservice.biometric.CosineFaceMatcher;
import com.isc.facebiometricservice.biometric.FaceMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BiometricFoundationConfig {
    @Bean
    FaceMatcher faceMatcher() {
        return new CosineFaceMatcher();
    }
}
