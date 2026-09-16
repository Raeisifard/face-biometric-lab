package com.isc.facebiometricservice.videoverification;

import com.isc.facebiometricservice.biometric.CosineFaceMatcher;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.config.VideoVerificationProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import nu.pattern.OpenCV;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diagnostic test for the server-side MiniFASNetV2 liveness and ArcFace
 * 1:1 verification pipeline.
 *
 * The test uses a fixed reference identity/model contract copied from the
 * property-backed test data. It does not search for a referenceId at runtime.
 * The embedding values themselves are read from the existing test resource.
 *
 * Override the clip with:
 *   -Dbiometric.test.video=C:/path/to/clip.webm
 */
class StoredVideoLivenessTest {

    private static final int EMBEDDING_DIMENSION = 512;
    private static final String REFERENCE_RESOURCE = "biometric-embeddings.yml";

    // Fixed test reference taken from biometric-embeddings.yml.
    private static final String REFERENCE_ID = "user-123";
    private static final String MODEL_ID = "arcface-512";
    private static final String MODEL_VERSION = "w600k-r50";

    static {
        // This test constructs VideoClipDecoder directly, without starting
        // Spring. The application normally loads OpenCV in VideoVerificationConfig,
        // so initialize the same bundled native library for this standalone test.
        OpenCV.loadLocally();
    }

    @Test
    void evaluatesLatestStoredVideoForLivenessAndRecognition() {
        Path video = configuredVideo();
        assertTrue(Files.isRegularFile(video), "Stored video does not exist: " + video);

        BiometricProperties biometricProperties = new BiometricProperties(
                "property",
                MODEL_ID,
                MODEL_VERSION,
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

        FaceEmbedding reference = loadReferenceEmbedding();
        VideoClipDecoder decoder = new VideoClipDecoder(videoProperties);
        VideoClipDecoder.DecodedClip clip = decoder.decode(video, videoProperties.sampleFps());

        try {
            VideoVerificationEngine engine = new VideoVerificationEngine(
                    videoProperties,
                    biometricProperties,
                    new CosineFaceMatcher(),
                    (referenceId, modelId, modelVersion) ->
                            REFERENCE_ID.equals(referenceId)
                                    && MODEL_ID.equals(modelId)
                                    && MODEL_VERSION.equals(modelVersion)
                                    ? Optional.of(reference)
                                    : Optional.empty()
            );

            VideoVerificationEngine.Outcome outcome = engine.verify(
                    "stored-video-liveness-test",
                    clip,
                    REFERENCE_ID
            );

            assertNotNull(outcome);
            assertTrue(outcome.decodedFrames() > 0, "No frames were decoded");
            assertNotNull(outcome.livenessScore(), "Liveness score was not produced");
            assertTrue(Double.isFinite(outcome.livenessScore()), "Liveness score must be finite");
            assertNotNull(outcome.similarity(), "Recognition similarity was not produced");
            assertTrue(Double.isFinite(outcome.similarity()), "Recognition similarity must be finite");
            assertTrue(outcome.recognitionFrames() > 0, "No recognition frames were selected");

            System.out.printf(
                    "Stored video verification test: video=%s, referenceId=%s, decodedFrames=%d, recognitionFrames=%d, livenessScore=%.9f, similarity=%.9f, result=%s, reasons=%s%n",
                    video,
                    REFERENCE_ID,
                    outcome.decodedFrames(),
                    outcome.recognitionFrames(),
                    outcome.livenessScore(),
                    outcome.similarity(),
                    outcome.result(),
                    outcome.reasons()
            );
        } finally {
            // VideoVerificationEngine.verify() releases decoded frame Mats.
        }
    }

    /**
     * Reads only the embedding values from the existing property test resource.
     * The identity and model contract are intentionally hard-coded above so
     * this test never has to discover which referenceId to use.
     */
    private FaceEmbedding loadReferenceEmbedding() {
        String yaml;
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(REFERENCE_RESOURCE)) {
            if (input == null) {
                throw new AssertionError("Reference embedding resource not found: " + REFERENCE_RESOURCE);
            }
            yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AssertionError("Could not read reference embedding resource: " + REFERENCE_RESOURCE, e);
        }

        List<Float> values = new java.util.ArrayList<>(EMBEDDING_DIMENSION);
        boolean inEmbedding = false;

        for (String raw : yaml.split("\\R")) {
            String trimmed = raw.trim();

            if (trimmed.equals("embedding:")) {
                inEmbedding = true;
                continue;
            }

            if (!inEmbedding) {
                continue;
            }

            if (trimmed.startsWith("- ")) {
                values.add(Float.valueOf(trimmed.substring(2).trim()));
                continue;
            }

            if (!trimmed.isEmpty() && !Character.isWhitespace(raw.charAt(0))) {
                break;
            }
        }

        assertTrue(values.size() == EMBEDDING_DIMENSION,
                "Reference embedding for " + REFERENCE_ID + " must contain exactly "
                        + EMBEDDING_DIMENSION + " values but contains " + values.size());

        float[] embedding = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            embedding[i] = values.get(i);
        }
        return new FaceEmbedding(embedding, EMBEDDING_DIMENSION, MODEL_ID, MODEL_VERSION, true);
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
