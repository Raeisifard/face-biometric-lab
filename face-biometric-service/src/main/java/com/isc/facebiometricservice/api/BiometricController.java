package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import com.isc.facebiometricservice.service.BiometricVerificationService;
import com.isc.facebiometricservice.service.FaceMatchingService;
import com.isc.facebiometricservice.util.EmbeddingFileWriter;
import com.isc.facebiometricservice.util.EmbeddingLogger;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/biometric")
@Validated
public class BiometricController {
    private static final Logger log = LoggerFactory.getLogger(BiometricController.class);
    private final FaceMatchingService service;
    private final BiometricVerificationService verificationService;
    private final BiometricProperties properties;

    public BiometricController(FaceMatchingService service, BiometricVerificationService verificationService,
                               BiometricProperties properties) {
        this.service = service;
        this.verificationService = verificationService;
        this.properties = properties;
    }

    @PostMapping("/verify")
    public VerificationResponse verify(@Valid @RequestBody VerificationRequest request) {
        return verificationService.verify(request);
    }

    @PostMapping("/enroll-embedding")
    public VerifyResponse enroll(@Valid @RequestBody EmbeddingPayload p) {
        require(p);
        FaceEmbedding e = new FaceEmbedding(p.embedding(), p.dimension(), p.modelId(), p.modelVersion(), p.normalized());
        service.enroll(p.userId(), e);
        try {
            Path file = EmbeddingFileWriter.write(p, "biometric-embeddings");
            log.info("[BIOMETRIC] Reference embedding written to {}", file.toAbsolutePath());
        } catch (IOException ex) {
            log.error("[BIOMETRIC] Failed to write reference embedding for user={}", p.userId(), ex);
        }
        return new VerifyResponse(p.userId(), true, 1.0, properties.threshold(), "ENROLL",
                e.modelId(), e.modelVersion(), 0);
    }

    @PostMapping("/verify-embedding")
    public VerifyResponse verifyLegacy(@Valid @RequestBody EmbeddingPayload p) {
        require(p);
        FaceEmbedding e = new FaceEmbedding(p.embedding(), p.dimension(), p.modelId(), p.modelVersion(), p.normalized());
        var r = service.verify(p.userId(), e);
        return new VerifyResponse(p.userId(), r.matched(), r.similarity(), r.threshold(), r.algorithm(),
                e.modelId(), e.modelVersion(), r.processingTimeMs());
    }

    @GetMapping("/models")
    public ModelResponse models() {
        return new ModelResponse(properties.modelId(), properties.modelVersion(), properties.dimension(),
                properties.algorithm(), properties.threshold());
    }

    private void require(EmbeddingPayload p) {
        if (p == null || p.userId() == null || p.userId().isBlank()) throw new IllegalArgumentException("userId is required");
        if (p.embedding() == null || p.embedding().length != properties.dimension())
            throw new IllegalArgumentException("Embedding must contain " + properties.dimension() + " values");
    }

    public record ModelResponse(String modelId, String modelVersion, int dimension, String algorithm, double threshold) {}
}
