package com.isc.faceclientsimulator.ai;

import com.isc.faceclientsimulator.config.SimulatorProperties;
import com.isc.faceclientsimulator.domain.FaceBoundingBox;
import com.isc.faceclientsimulator.domain.FaceDetectionResult;
import com.isc.faceclientsimulator.domain.FaceLandmarks;
import jakarta.annotation.PreDestroy;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.objdetect.FaceDetectorYN;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class YuNetFaceDetector {
    private static final int YUNET_OUTPUT_SIZE = 15; /* * Reasonable limits for camera frames. * * These are validation limits, not YuNet model limits. * The actual detector input size is configured dynamically * from the incoming image. */
    private static final int MIN_IMAGE_WIDTH = 32;
    private static final int MIN_IMAGE_HEIGHT = 32;
    private static final int MAX_IMAGE_WIDTH = 8192;
    private static final int MAX_IMAGE_HEIGHT = 8192;
    private final FaceDetectorYN detector;
    private final String modelPath;
    private volatile boolean shutdown;

    /**
     * Creates and validates the YuNet detector.
     */
    public YuNetFaceDetector(SimulatorProperties properties) {
        this.modelPath = properties.detectorModelPath();
        validateModelPath(modelPath);
        try {
            this.detector = FaceDetectorYN.create(modelPath, "", new Size(320, 320), properties.detectionThreshold(), 0.3f, 5000);
            if (this.detector == null) {
                throw new IllegalStateException("OpenCV returned a null YuNet detector.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load YuNet face detection model: " + modelPath + ". Verify that the ONNX model is valid " + "and compatible with OpenCV FaceDetectorYN.", e);
        }
    }

    /**
     * Detects faces in a camera frame.
     */
    public synchronized FaceDetectionResult detect(Mat image) {
        long started = System.nanoTime();
        validateImage(image);
        if (shutdown) {
            throw new IllegalStateException("YuNetFaceDetector has already been shut down.");
        } /* * YuNet must receive the actual dimensions of the incoming * camera frame. */
        try {
            detector.setInputSize(new Size(image.cols(), image.rows()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid YuNet input image size: " + image.cols() + "x" + image.rows() + ".", e);
        }
        Mat faces = new Mat();
        try {
            detector.detect(image, faces); /* * No face detected. */
            if (faces.empty() || faces.rows() == 0) {
                return FaceDetectionResult.noFace(elapsed(started), 0);
            } /* * YuNet normally returns: * * rows = number of detected faces * cols = 15 * type = CV_32F */
            if (faces.cols() < YUNET_OUTPUT_SIZE) {
                throw new IllegalStateException("Unexpected YuNet output shape: rows=" + faces.rows() + ", cols=" + faces.cols() + ", type=" + faces.type() + ". Expected at least " + YUNET_OUTPUT_SIZE + " columns.");
            }
            int count = faces.rows(); /* * Find the highest-confidence face. */
            int best = -1;
            double bestScore = -1.0;
            for (int i = 0; i < count; i++) { /* * YuNet output is CV_32F, so float[] must be used. */
                float[] row = new float[YUNET_OUTPUT_SIZE];
                int copied = faces.get(i, 0, row);
                if (copied < YUNET_OUTPUT_SIZE) {
                    continue;
                }
                double score = row[14];
                if (Double.isFinite(score) && score > bestScore) {
                    bestScore = score;
                    best = i;
                }
            } /* * No valid detection row was returned. */
            if (best < 0) {
                return FaceDetectionResult.noFace(elapsed(started), count);
            } /* * Read the complete best detection. */
            float[] v = new float[YUNET_OUTPUT_SIZE];
            int copied = faces.get(best, 0, v);
            if (copied < YUNET_OUTPUT_SIZE) {
                throw new IllegalStateException("Unable to read complete YuNet detection row. " + "Expected " + YUNET_OUTPUT_SIZE + " values but received " + copied + ".");
            } /* * Validate numerical output before creating the domain * objects. */
            validateDetectionValues(v); /* * Bounding box: * * x, y, width, height */
            FaceBoundingBox box = new FaceBoundingBox(v[0], v[1], v[2], v[3]); /* * Five YuNet facial landmarks: * * right eye * left eye * nose * right mouth * left mouth */
            FaceLandmarks landmarks = new FaceLandmarks(new FaceLandmarks.Point2(v[4], v[5]), new FaceLandmarks.Point2(v[6], v[7]), new FaceLandmarks.Point2(v[8], v[9]), new FaceLandmarks.Point2(v[10], v[11]), new FaceLandmarks.Point2(v[12], v[13]));
            return new FaceDetectionResult(true, count, box, landmarks, bestScore, elapsed(started));
        } finally {
            faces.release();
        }
    }

    /**
     * Explicitly validates the incoming OpenCV image before it * reaches the native YuNet implementation.
     */
    private void validateImage(Mat image) {
        if (image == null) {
            throw new IllegalArgumentException("YuNet cannot process a null image.");
        }
        if (image.empty()) {
            throw new IllegalArgumentException("YuNet cannot process an empty image.");
        }
        int width = image.cols();
        int height = image.rows();
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid image dimensions: " + width + "x" + height + ". Width and height must be greater than zero.");
        }
        if (width < MIN_IMAGE_WIDTH || height < MIN_IMAGE_HEIGHT) {
            throw new IllegalArgumentException("Image is too small for YuNet: " + width + "x" + height + ". Minimum supported size is " + MIN_IMAGE_WIDTH + "x" + MIN_IMAGE_HEIGHT + ".");
        }
        if (width > MAX_IMAGE_WIDTH || height > MAX_IMAGE_HEIGHT) {
            throw new IllegalArgumentException("Image is too large for YuNet: " + width + "x" + height + ". Maximum supported size is " + MAX_IMAGE_WIDTH + "x" + MAX_IMAGE_HEIGHT + ".");
        } /* * YuNet expects a normal image with at least one channel. * OpenCV Mat channels are checked here before invoking the * native detector. */
        int channels = image.channels();
        if (channels <= 0) {
            throw new IllegalArgumentException("Invalid OpenCV image: " + "image has " + channels + " channels.");
        } /* * The camera simulator should normally provide BGR or * grayscale frames. Reject unusual channel counts early. */
        if (channels != 1 && channels != 3 && channels != 4) {
            throw new IllegalArgumentException("Unsupported OpenCV image channel count: " + channels + ". Expected 1, 3, or 4 channels.");
        }
    }

    /**
     * Validates the values returned by YuNet.
     */
    private void validateDetectionValues(float[] v) {
        for (int i = 0; i < v.length; i++) {
            if (!Float.isFinite(v[i])) {
                throw new IllegalStateException("YuNet returned an invalid numeric value " + "at output index " + i + ": " + v[i]);
            }
        } /* * Bounding box dimensions must be positive. */
        if (v[2] <= 0 || v[3] <= 0) {
            throw new IllegalStateException("YuNet returned an invalid face bounding box: " + "x=" + v[0] + ", y=" + v[1] + ", width=" + v[2] + ", height=" + v[3]);
        } /* * Confidence should be within the normal probability range. */
        if (v[14] < 0.0f || v[14] > 1.0f) {
            throw new IllegalStateException("YuNet returned an invalid confidence score: " + v[14]);
        }
    }

    /**
     * Validates the configured ONNX model before OpenCV attempts * to load it.
     */
    private void validateModelPath(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException("YuNet model path is not configured. " + "Configure the detector model path in " + "application.yml/application.properties.");
        }
        Path path;
        try {
            path = Path.of(configuredPath).toAbsolutePath().normalize();
        } catch (Exception e) {
            throw new IllegalStateException("Invalid YuNet model path: " + configuredPath, e);
        }
        if (!Files.exists(path)) {
            throw new IllegalStateException("YuNet model file not found: " + path);
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("YuNet model path is not a regular file: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new IllegalStateException("YuNet model file is not readable: " + path);
        }
        if (!path.toString().toLowerCase().endsWith(".onnx")) {
            throw new IllegalStateException("YuNet model must be an ONNX file: " + path);
        }
    }

    /**
     * Marks the detector as unavailable when Spring shuts down.
     *
     * FaceDetectorYN in OpenCV Java 4.9.0 does not expose an explicit
     * clear()/release() method, so native cleanup is left to the
     * OpenCV Java wrapper lifecycle.
     */
    @PreDestroy
    public synchronized void shutdown() {
        shutdown = true;
    }

    private long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}