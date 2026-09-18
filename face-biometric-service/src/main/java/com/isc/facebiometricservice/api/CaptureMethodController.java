package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.policy.BiometricPolicyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/biometric")
public class CaptureMethodController {
    private final BiometricPolicyService policyService;

    public CaptureMethodController(BiometricPolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/capture-method")
    public CaptureMethodResponse captureMethod() {
        var policy = policyService.currentPolicy();
        return new CaptureMethodResponse(
                policy.method().wireValue(),
                "Server-issued biometric policy controls method selection; the client cannot override it.",
                List.of(policy.method().wireValue()),
                policy.policyId(),
                policy.version(),
                policy.profile(),
                policy.fallbackMethod());
    }

    public record CaptureMethodResponse(String selectedMethod, String message, List<String> availableMethods,
                                        String policyId, long policyVersion, String profile, String fallbackMethod) {
    }
}
