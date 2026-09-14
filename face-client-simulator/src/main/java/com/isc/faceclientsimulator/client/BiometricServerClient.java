package com.isc.faceclientsimulator.client;

import com.isc.faceclientsimulator.domain.FaceEmbedding;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BiometricServerClient {
    private final RestClient restClient;
    public BiometricServerClient(RestClient restClient){this.restClient=restClient;}

    public ServerVerifyResponse enroll(String userId, FaceEmbedding e) {
        return restClient.post().uri("/api/v1/biometric/enroll-embedding")
                .body(new ServerEmbeddingPayload(userId,e.values(),e.dimension(),e.modelId(),e.modelVersion(),e.normalized()))
                .retrieve().body(ServerVerifyResponse.class);
    }

    public ServerVerifyResponse verify(String userId, FaceEmbedding e) {
        return restClient.post().uri("/api/v1/biometric/verify-embedding")
                .body(new ServerEmbeddingPayload(userId,e.values(),e.dimension(),e.modelId(),e.modelVersion(),e.normalized()))
                .retrieve().body(ServerVerifyResponse.class);
    }
}
