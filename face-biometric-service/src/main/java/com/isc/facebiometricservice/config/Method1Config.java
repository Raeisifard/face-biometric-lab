package com.isc.facebiometricservice.config;

import nu.pattern.OpenCV;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Method1Properties.class)
public class Method1Config {
    static {
        OpenCV.loadLocally();
    }
}
