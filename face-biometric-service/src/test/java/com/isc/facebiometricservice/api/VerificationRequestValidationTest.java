package com.isc.facebiometricservice.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VerificationRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test void requestIdAndReferenceIdAreRequired() {
        var request = new VerificationRequest("", "", new VerificationRequest.CaptureData("EMBEDDING", List.of(), true), null, null, null);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test void captureTypeIsRequired() {
        var request = new VerificationRequest("req", "customer", new VerificationRequest.CaptureData("", List.of(), true), null, null, null);
        assertFalse(validator.validate(request).isEmpty());
    }
}
