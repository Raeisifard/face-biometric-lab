package com.isc.facebiometricservice.config;

import org.opencv.core.Core;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Method1Properties.class)
public class Method1Config {
 static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }
}
