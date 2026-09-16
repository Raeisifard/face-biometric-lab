package com.isc.faceclientsimulator.api;

import com.isc.faceclientsimulator.client.LiveStreamServerClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/simulator/live-stream")
public class LiveStreamSimulatorController {
    private final LiveStreamServerClient client;

    public LiveStreamSimulatorController(LiveStreamServerClient client) {
        this.client = client;
    }

    @GetMapping("/capture-method")
    public Object captureMethod() {
        return client.captureMethod();
    }

    @PostMapping("/sessions")
    public Object createSession(@RequestParam String referenceId) {
        return client.createSession(referenceId);
    }

    @PostMapping(value = "/sessions/{sessionId}/frames", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object uploadFrame(@PathVariable String sessionId, @RequestPart("frame") MultipartFile frame) throws Exception {
        return client.uploadFrame(sessionId, frame.getBytes());
    }

    @PostMapping("/sessions/{sessionId}/complete")
    public Object complete(@PathVariable String sessionId) {
        return client.complete(sessionId);
    }
}