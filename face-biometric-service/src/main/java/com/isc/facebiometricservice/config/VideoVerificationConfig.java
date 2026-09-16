package com.isc.facebiometricservice.config;

import nu.pattern.OpenCV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VideoVerificationProperties.class)
public class VideoVerificationConfig {
    private static final Logger log=LoggerFactory.getLogger(VideoVerificationConfig.class);
    static { OpenCV.loadLocally(); log.info("OpenCV native library loaded for video verification"); }
}
