package com.isc.faceclientsimulator.client;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class HybridMultiFrameVerificationServerClient {
    private final RestClient rest;

    public HybridMultiFrameVerificationServerClient(RestClient rest) {
        this.rest = rest;
    }

    public Object createSession(String referenceId, int requestedFrames) {
        return rest.post().uri(uri -> uri.path("/api/v1/biometric/hybrid-multi-frame-verification/sessions")
                .queryParam("referenceId", referenceId)
                .queryParam("requestedFrames", requestedFrames)
                .build()).retrieve().body(Object.class);
    }

    public Object verify(String sessionId, String referenceId, List<byte[]> images, List<Long> timestamps) {
        var body = new LinkedMultiValueMap<String, Object>();
        for (int i = 0; i < images.size(); i++) {
            byte[] image = images.get(i);
            int index = i;
            body.add("images", new ByteArrayResource(image) {
                @Override public String getFilename() { return "hybrid-frame-" + index + ".jpg"; }
            });
            body.add("sequenceNumbers", String.valueOf(i));
            body.add("captureTimestamps", String.valueOf(timestamps.get(i)));
        }
        return rest.post().uri(uri -> uri.path("/api/v1/biometric/hybrid-multi-frame-verification/verify")
                .queryParam("sessionId", sessionId)
                .queryParam("referenceId", referenceId)
                .build()).contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve()
                .onStatus(status -> status.isError(), (request, response) -> {})
                .body(Object.class);
    }
}
