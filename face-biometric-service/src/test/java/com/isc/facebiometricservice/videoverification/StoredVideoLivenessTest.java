package com.isc.facebiometricservice.videoverification;

import com.isc.facebiometricservice.biometric.CosineFaceMatcher;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.config.VideoVerificationProperties;
import nu.pattern.OpenCV;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diagnostic test for the server-side MiniFASNetV2 liveness pipeline.
 *
 * The test reads an already stored WebM clip from video-captures instead of
 * going through the HTTP/UI flow. It intentionally stops at the liveness
 * stage by supplying an empty reference repository.
 *
 * Override the clip with:
 *   -Dbiometric.test.video=C:/path/to/clip.webm
 */
class StoredVideoLivenessTest {

    private static final int EMBEDDING_DIMENSION = 512;

    static {
        // This test constructs VideoClipDecoder directly, without starting
        // Spring. The application normally loads OpenCV in VideoVerificationConfig,
        // so initialize the same bundled native library for this standalone test.
        OpenCV.loadLocally();
    }

    @Test
    void evaluatesLatestStoredVideoForLiveness() {
        Path video = configuredVideo();
        assertTrue(Files.isRegularFile(video), "Stored video does not exist: " + video);

        VideoVerificationProperties videoProperties = new VideoVerificationProperties(
                true,
                25 * 1024 * 1024L,
                3.0,
                5.0,
                4.0,
                80,
                1,
                0.45,
                true,
                0.50,
                5,
                "MEAN",
                "../models/detector/face_detection_yunet_2023mar.onnx",
                "../models/liveness/2.7_80x80_MiniFASNetV2.onnx",
                "../models/recognition/w600k_r50.onnx",
                "video-captures"
        );

        BiometricProperties biometricProperties = new BiometricProperties(
                "property",
                "arcface-512",
                "w600k-r50",
                "models/recognition/w600k_r50.onnx",
                EMBEDDING_DIMENSION,
                0.90,
                "COSINE",
                true,
                new BiometricProperties.Detector(false, "", 0.9),
                new BiometricProperties.Liveness(false, "", 0.5),
                new BiometricProperties.Oracle("", "", ""),
                new BiometricProperties.Mongo("", "", ""),
                new BiometricProperties.Cors("*"),
                Map.of()
        );

        VideoClipDecoder decoder = new VideoClipDecoder(videoProperties);
        VideoClipDecoder.DecodedClip clip = decoder.decode(video, videoProperties.sampleFps());

        try {
            VideoVerificationEngine engine = new VideoVerificationEngine(
                    videoProperties,
                    biometricProperties,
                    new CosineFaceMatcher(),
                    (referenceId, modelId, modelVersion) -> Optional.empty()
            );

            VideoVerificationEngine.Outcome outcome = engine.verify(
                    "stored-video-liveness-test",
                    clip,
                    "test-reference"
            );

            assertNotNull(outcome);
            assertTrue(outcome.decodedFrames() > 0, "No frames were decoded");
            assertNotNull(outcome.livenessScore(), "Liveness score was not produced");
            assertTrue(Double.isFinite(outcome.livenessScore()), "Liveness score must be finite");

            System.out.printf(
                    "Stored video liveness test: video=%s, decodedFrames=%d, livenessScore=%.9f, result=%s, reasons=%s%n",
                    video,
                    outcome.decodedFrames(),
                    outcome.livenessScore(),
                    outcome.result(),
                    outcome.reasons()
            );
        } finally {
            // VideoVerificationEngine.verify() releases decoded frame Mats.
        }
    }

    private Path configuredVideo() {
        String configured = System.getProperty("biometric.test.video");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }

        Path directory = findStoredVideoDirectory();
        try (var files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".webm"))
                    .max(Comparator.comparingLong(this::lastModified))
                    .orElseThrow(() -> new AssertionError("No .webm clips found in " + directory));
        } catch (Exception e) {
            throw new AssertionError("Could not inspect stored-video directory: " + directory, e);
        }
    }

    private Path findStoredVideoDirectory() {
        Path workingDirectory = Path.of(".").toAbsolutePath().normalize();
        Path[] candidates = {
                workingDirectory.resolve("video-captures"),
                workingDirectory.resolve("../video-captures").normalize(),
                workingDirectory.resolve("../../video-captures").normalize()
        };

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }

        throw new AssertionError(
                "Stored-video directory does not exist. Checked: " + String.join(", ",
                        java.util.Arrays.stream(candidates).map(Path::toString).toList())
        );
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception e) {
            return Long.MIN_VALUE;
        }
    }
}
