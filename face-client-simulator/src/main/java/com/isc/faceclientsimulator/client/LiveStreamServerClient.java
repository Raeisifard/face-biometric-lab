package com.isc.faceclientsimulator.client;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class LiveStreamServerClient {
    private final RestClient rest;

    public LiveStreamServerClient(RestClient rest) {
        this.rest = rest;
    }

    public Object captureMethod() {
        return rest.get().uri("/api/v1/biometric/capture-method").retrieve().body(Object.class);
    }

    public Object createSession(String referenceId) {
        return rest.post().uri(uri -> uri.path("/api/v1/live-stream/sessions")
                .queryParam("customerReferenceId", referenceId)
                .queryParam("expectedCaptureMode", "LIVE_STREAM").build())
                .retrieve().body(Object.class);
    }

    public Object uploadFrame(String sessionId, byte[] frame) {
        var resource = new ByteArrayResource(frame) {
            @Override
            public String getFilename() {
                return "frame.jpg";
            }
        };
        var body = new LinkedMultiValueMap<String, Object>();
        body.add("frame", resource);
        return rest.post().uri("/api/v1/live-stream/sessions/{sessionId}/frames", sessionId)
                .contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(Object.class);
    }

    public Object complete(String sessionId) {
        return rest.post().uri("/api/v1/live-stream/sessions/{sessionId}/complete", sessionId)
                .retrieve().body(Object.class);
    }
}