package com.isc.faceclientsimulator;

import com.isc.faceclientsimulator.config.SimulatorProperties;
import nu.pattern.OpenCV;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SimulatorProperties.class)
public class FaceClientSimulatorApplication {

    public static void main(String[] args) {
        OpenCV.loadLocally();
        SpringApplication.run(FaceClientSimulatorApplication.class, args);
    }
}
