package com.isc.faceclientsimulator.api;

import com.isc.faceclientsimulator.client.HybridMultiFrameVerificationServerClient;
import com.isc.faceclientsimulator.service.BiometricFrameProcessor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/simulator/hybrid-multi-frame")
public class HybridMultiFrameSimulatorController {
    private final HybridMultiFrameVerificationServerClient serverClient;
    private final BiometricFrameProcessor processor;
    private final ConcurrentHashMap<String, Boolean> sessions = new ConcurrentHashMap<>();

    public HybridMultiFrameSimulatorController(HybridMultiFrameVerificationServerClient serverClient,
                                                BiometricFrameProcessor processor) {
        this.serverClient = serverClient;
        this.processor = processor;
    }

    @PostMapping("/sessions")
    public Object create(@RequestParam String referenceId, @RequestParam(defaultValue = "4") int frames) {
        return serverClient.createSession(referenceId, frames);
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object analyze(@RequestParam String sessionId, @RequestParam int sequence,
                          @RequestPart("image") MultipartFile image) throws Exception {
        if (sessionId == null || sessionId.isBlank()) throw new IllegalArgumentException("sessionId is required");
        if (image == null || image.isEmpty()) throw new IllegalArgumentException("Image is required");
        sessions.putIfAbsent(sessionId, true);
        return processor.analyze(sessionId + "-hybrid-" + sequence, image.getBytes());
    }

    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object verify(@RequestParam String sessionId, @RequestParam String referenceId,
                         @RequestPart("images") List<MultipartFile> images) throws Exception {
        if (images == null || images.isEmpty()) throw new IllegalArgumentException("At least one image is required");
        List<byte[]> payloads = new ArrayList<>(images.size());
        List<Long> timestamps = new ArrayList<>(images.size());
        long now = System.currentTimeMillis();
        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            if (image == null || image.isEmpty()) throw new IllegalArgumentException("Image " + i + " is empty");
            payloads.add(image.getBytes());
            timestamps.add(now + i);
        }
        return serverClient.verify(sessionId, referenceId, payloads, timestamps);
    }
}
