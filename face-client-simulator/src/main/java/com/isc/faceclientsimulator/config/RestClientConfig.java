package com.isc.faceclientsimulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient restClient(SimulatorProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.serverBaseUrl())
                .build();
    }
}
