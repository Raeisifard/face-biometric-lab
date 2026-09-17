package com.isc.faceclientsimulator.client;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class HybridVerificationServerClient {
    private final RestClient rest;

    public HybridVerificationServerClient(RestClient rest) {
        this.rest = rest;
    }

    public Object createSession(String referenceId) {
        return rest.post()
                .uri(uri -> uri.path("/api/v1/biometric/hybrid-verification/sessions")
                        .queryParam("referenceId", referenceId).build())
                .retrieve().body(Object.class);
    }

    public Object verify(String sessionId, String referenceId, byte[] image) {
        var resource = new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return "best-frame.jpg";
            }
        };
        var body = new LinkedMultiValueMap<String, Object>();
        body.add("image", resource);
        return rest.post()
                .uri(uri -> uri.path("/api/v1/biometric/hybrid-verification/verify")
                        .queryParam("sessionId", sessionId)
                        .queryParam("referenceId", referenceId).build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve().body(Object.class);
    }
}
