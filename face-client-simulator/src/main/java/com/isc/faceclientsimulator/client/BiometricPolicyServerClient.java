package com.isc.faceclientsimulator.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class BiometricPolicyServerClient {
    private final RestClient rest;
    public BiometricPolicyServerClient(RestClient rest) { this.rest = rest; }
    public Object current(String method) { return rest.get().uri(uri -> uri.path("/api/v1/biometric/policy").queryParamIfPresent("method", java.util.Optional.ofNullable(method)).build()).retrieve().body(Object.class); }
    public Object createSession(String referenceId, String profile) {
        return rest.post().uri(uri -> uri.path("/api/v1/biometric/policy/sessions").queryParam("referenceId", referenceId).queryParamIfPresent("profile", java.util.Optional.ofNullable(profile)).build()).retrieve().body(Object.class);
    }
    public Object validate(Map<String,Object> request) {
        return rest.post().uri("/api/v1/biometric/policy/validate").body(request).retrieve().body(Object.class);
    }
}
