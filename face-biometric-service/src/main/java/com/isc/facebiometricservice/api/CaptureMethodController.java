package com.isc.facebiometricservice.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/biometric")
public class CaptureMethodController {
    private final String selectedMethod;

    public CaptureMethodController(@Value("${biometric.capture-method:LIVE_STREAM}") String selectedMethod) {
        this.selectedMethod = selectedMethod.toUpperCase();
    }

    @GetMapping("/capture-method")
    public CaptureMethodResponse captureMethod() {
        return new CaptureMethodResponse(selectedMethod,
                selectedMethod.equals("LIVE_STREAM")
                        ? "Server selected incremental frame verification"
                        : "Server selected complete clip verification");
    }

    public record CaptureMethodResponse(String selectedMethod, String message) {
    }
}