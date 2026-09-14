package com.isc.facebiometricservice;

import com.isc.facebiometricservice.config.BiometricProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BiometricProperties.class)
public class FaceBiometricServiceApplication {
    public static void main(String[] args) { SpringApplication.run(FaceBiometricServiceApplication.class,args); }
}
