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

import java.nio.file.Path;

@Component
public class YuNetFaceDetector {
    private static final int YUNET_OUTPUT_SIZE = 15;
    private static final int MIN_IMAGE_WIDTH = 32;
    private static final int MIN_IMAGE_HEIGHT = 32;
    private static final int MAX_IMAGE_WIDTH = 8192;
    private static final int MAX_IMAGE_HEIGHT = 8192;

    private final FaceDetectorYN detector;
    private final Path modelPath;
    private volatile boolean shutdown;

    public YuNetFaceDetector(SimulatorProperties properties) {
        this.modelPath = ModelPathResolver.resolve(
                properties.modelsDir(), properties.detectorModelPath());
        try {
            this.detector = FaceDetectorYN.create(
                    modelPath.toString(), "", new Size(320, 320),
                    properties.detectionThreshold(), 0.3f, 5000);
            if (this.detector == null) {
                throw new IllegalStateException("OpenCV returned a null YuNet detector.");
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load YuNet face detection model: " + modelPath, e);
        }
    }

    public synchronized FaceDetectionResult detect(Mat image) {
        long started = System.nanoTime();
        validateImage(image);
        if (shutdown) {
            throw new IllegalStateException("YuNetFaceDetector has already been shut down.");
        }
        try {
            detector.setInputSize(new Size(image.cols(), image.rows()));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid YuNet input image size: " + image.cols() + "x" + image.rows(), e);
        }

        Mat faces = new Mat();
        try {
            detector.detect(image, faces);
            if (faces.empty() || faces.rows() == 0) {
                return FaceDetectionResult.noFace(elapsed(started), 0);
            }
            if (faces.cols() < YUNET_OUTPUT_SIZE) {
                throw new IllegalStateException(
                        "Unexpected YuNet output shape: rows=" + faces.rows()
                                + ", cols=" + faces.cols() + ", type=" + faces.type());
            }

            int count = faces.rows();
            int best = -1;
            double bestScore = -1.0;
            for (int i = 0; i < count; i++) {
                float[] row = new float[YUNET_OUTPUT_SIZE];
                int copied = faces.get(i, 0, row);
                if (copied < YUNET_OUTPUT_SIZE) continue;
                double score = row[14];
                if (Double.isFinite(score) && score > bestScore) {
                    bestScore = score;
                    best = i;
                }
            }
            if (best < 0) {
                return FaceDetectionResult.noFace(elapsed(started), count);
            }

            float[] v = new float[YUNET_OUTPUT_SIZE];
            int copied = faces.get(best, 0, v);
            if (copied < YUNET_OUTPUT_SIZE) {
                throw new IllegalStateException("Unable to read complete YuNet detection row.");
            }
            validateDetectionValues(v);

            FaceBoundingBox box = new FaceBoundingBox(v[0], v[1], v[2], v[3]);
            FaceLandmarks landmarks = new FaceLandmarks(
                    new FaceLandmarks.Point2(v[4], v[5]),
                    new FaceLandmarks.Point2(v[6], v[7]),
                    new FaceLandmarks.Point2(v[8], v[9]),
                    new FaceLandmarks.Point2(v[10], v[11]),
                    new FaceLandmarks.Point2(v[12], v[13]));
            return new FaceDetectionResult(true, count, box, landmarks, bestScore, elapsed(started));
        } finally {
            faces.release();
        }
    }

    private void validateImage(Mat image) {
        if (image == null || image.empty()) {
            throw new IllegalArgumentException("YuNet cannot process a null or empty image.");
        }
        int width = image.cols();
        int height = image.rows();
        if (width < MIN_IMAGE_WIDTH || height < MIN_IMAGE_HEIGHT) {
            throw new IllegalArgumentException("Image is too small for YuNet: " + width + "x" + height);
        }
        if (width > MAX_IMAGE_WIDTH || height > MAX_IMAGE_HEIGHT) {
            throw new IllegalArgumentException("Image is too large for YuNet: " + width + "x" + height);
        }
        int channels = image.channels();
        if (channels != 1 && channels != 3 && channels != 4) {
            throw new IllegalArgumentException("Unsupported OpenCV image channel count: " + channels);
        }
    }

    private void validateDetectionValues(float[] v) {
        for (int i = 0; i < v.length; i++) {
            if (!Float.isFinite(v[i])) {
                throw new IllegalStateException("YuNet returned an invalid numeric value at index " + i);
            }
        }
        if (v[2] <= 0 || v[3] <= 0) {
            throw new IllegalStateException("YuNet returned an invalid face bounding box.");
        }
        if (v[14] < 0.0f || v[14] > 1.0f) {
            throw new IllegalStateException("YuNet returned an invalid confidence score: " + v[14]);
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        shutdown = true;
    }

    private long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
