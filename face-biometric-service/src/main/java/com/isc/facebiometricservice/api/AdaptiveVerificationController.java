package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.adaptive.AdaptiveVerificationService;
import com.isc.facebiometricservice.adaptive.AdaptiveVerificationService.AdaptiveVerificationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/biometric/adaptive")
public class AdaptiveVerificationController {
    private final AdaptiveVerificationService service;
    public AdaptiveVerificationController(AdaptiveVerificationService service) { this.service = service; }

    @PostMapping("/sessions")
    public ResponseEntity<?> create(@RequestParam String referenceId,
                                    @RequestParam(required = false) String profile,
                                    @RequestParam(required = false) String requestedMethod) {
        try { return ResponseEntity.ok(service.create(referenceId, profile, requestedMethod)); }
        catch (AdaptiveVerificationException ex) { return bad(ex); }
        catch (RuntimeException ex) { return ResponseEntity.badRequest().body(new ErrorResponse(ex.getClass().getSimpleName(), ex.getMessage())); }
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> get(@PathVariable String sessionId) {
        try { return ResponseEntity.ok(service.get(sessionId)); } catch (AdaptiveVerificationException ex) { return bad(ex); }
    }

    @PostMapping(value = "/sessions/{sessionId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> image(@PathVariable String sessionId, @RequestPart("image") MultipartFile image) {
        try { return ResponseEntity.ok(service.verifyImage(sessionId, image == null ? null : image.getBytes())); } catch (AdaptiveVerificationException ex) { return bad(ex); } catch (Exception ex) { return error(ex); }
    }

    @PostMapping(value = "/sessions/{sessionId}/frames", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> frames(@PathVariable String sessionId, @RequestPart("frames") List<MultipartFile> frames) {
        try { return ResponseEntity.ok(service.verifyFrames(sessionId, frames == null ? List.of() : frames.stream().map(this::bytes).toList())); } catch (AdaptiveVerificationException ex) { return bad(ex); } catch (Exception ex) { return error(ex); }
    }

    @PostMapping(value = "/sessions/{sessionId}/clip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> clip(@PathVariable String sessionId, @RequestPart("clip") MultipartFile clip) {
        try { return ResponseEntity.ok(service.verifyClip(sessionId, clip)); } catch (AdaptiveVerificationException ex) { return bad(ex); } catch (Exception ex) { return error(ex); }
    }

    @PostMapping("/sessions/{sessionId}/embedding")
    public ResponseEntity<?> embedding(@PathVariable String sessionId, @RequestBody EmbeddingPayload payload) {
        try { return ResponseEntity.ok(service.verifyEmbedding(sessionId, payload)); } catch (AdaptiveVerificationException ex) { return bad(ex); } catch (Exception ex) { return error(ex); }
    }

    private byte[] bytes(MultipartFile f) { try { return f.getBytes(); } catch (Exception e) { throw new IllegalArgumentException("INVALID_FRAME", e); } }
    private ResponseEntity<?> bad(AdaptiveVerificationException ex) { return ResponseEntity.badRequest().body(new ErrorResponse(ex.code(), ex.getMessage())); }
    private ResponseEntity<?> error(Exception ex) { return ResponseEntity.internalServerError().body(new ErrorResponse("ADAPTIVE_PROCESSING_ERROR", "Adaptive verification could not be completed.")); }
    public record ErrorResponse(String code, String message) {}
}
