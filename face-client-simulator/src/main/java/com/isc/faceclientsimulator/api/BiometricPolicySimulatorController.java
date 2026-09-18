package com.isc.faceclientsimulator.api;

import com.isc.faceclientsimulator.client.BiometricPolicyServerClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/simulator/policy")
public class BiometricPolicySimulatorController {
    private final BiometricPolicyServerClient client;
    public BiometricPolicySimulatorController(BiometricPolicyServerClient client) { this.client = client; }
    @GetMapping public Object current() { return client.current(); }
    @PostMapping("/sessions") public Object createSession(@RequestParam String referenceId, @RequestParam(required=false) String profile) { return client.createSession(referenceId, profile); }
    @PostMapping("/validate") public Object validate(@RequestBody Map<String,Object> request) { return client.validate(request); }
}
