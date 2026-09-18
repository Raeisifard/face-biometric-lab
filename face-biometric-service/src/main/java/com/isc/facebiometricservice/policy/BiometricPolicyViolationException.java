package com.isc.facebiometricservice.policy;

public class BiometricPolicyViolationException extends RuntimeException {
    private final String code;
    public BiometricPolicyViolationException(String code, String message) { super(message); this.code = code; }
    public String code() { return code; }
}
