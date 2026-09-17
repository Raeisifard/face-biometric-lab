package com.isc.facebiometricservice.streaming;

import com.isc.facebiometricservice.config.VideoVerificationProperties;
import com.isc.facebiometricservice.util.ModelPathResolver;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Component;
import ai.onnxruntime.*;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class OpenCvLiveFrameAnalyzer implements LiveFrameAnalyzer {
    private final FaceDetectorYN detector;
    private final OrtEnvironment environment = OrtEnvironment.getEnvironment();
    private final OrtSession liveness;
    private final double livenessThreshold;
    private final ConcurrentMap<String, Evidence> evidenceBySession = new ConcurrentHashMap<>();
    private static final int TEMPORAL_MIN_FRAMES = 5;
    private static final double TEMPORAL_MOTION_THRESHOLD = 0.015;

    public OpenCvLiveFrameAnalyzer(VideoVerificationProperties properties) {
        var model = ModelPathResolver.resolve(properties.detectorModelPath());
        detector = FaceDetectorYN.create(model.toString(), "", new Size(320, 320), 0.75f, 0.3f, 5000);
        try {
            liveness = environment.createSession(ModelPathResolver.resolve(properties.livenessModelPath()).toString(), new OrtSession.SessionOptions());
        } catch (OrtException exception) {
            throw new IllegalStateException("Live liveness model could not be initialized", exception);
        }
        livenessThreshold = properties.livenessThreshold();
    }

    @Override
    public Analysis analyze(byte[] frame) {
        return analyze("default", frame);
    }

    @Override
    public synchronized Analysis analyze(String sessionId, byte[] frame) {
        Mat image = Imgcodecs.imdecode(new MatOfByte(frame), Imgcodecs.IMREAD_COLOR);
        if (image.empty()) return new Analysis("INVALID_FRAME", "Frame could not be decoded as an image", null, 0);
        try {
            detector.setInputSize(image.size());
            Mat faces = new Mat();
            try {
                detector.detect(image, faces);
                int count = faces.rows();
                if (count == 0) return new Analysis("NO_FACE", "No face detected in frame", null, 0);
                if (count > 1) return new Analysis("MULTIPLE_FACES", "Exactly one face is required", null, count);
                float[] face = new float[15];
                faces.get(0, 0, face);
                double livenessScore = liveness(image, faces, 0);
                Evidence evidence = evidenceBySession.computeIfAbsent(sessionId, ignored -> new Evidence());
                evidence.accept(face[0] + face[2] / 2.0, face[1] + face[3] / 2.0, livenessScore);
                boolean temporalReady = evidence.frames >= TEMPORAL_MIN_FRAMES;
                double temporalMotion = evidence.motion();
                String message = temporalReady
                        ? "Face detected; temporal liveness evidence collected"
                        : "Face detected; collect temporal movement evidence";
                return new Analysis("GOOD_FRAME", message, livenessScore, 1, temporalMotion,
                    temporalReady
                        && temporalMotion >= TEMPORAL_MOTION_THRESHOLD
                        && evidence.averageLiveness() >= livenessThreshold);
            } finally {
                faces.release();
            }
        } finally {
            image.release();
        }
    }

    private double liveness(Mat image, Mat faces, int faceIndex) {
        float[] row = new float[15];
        faces.get(faceIndex, 0, row);
        int x = Math.max(0, (int) row[0]);
        int y = Math.max(0, (int) row[1]);
        int width = Math.min(image.cols() - x, Math.max(1, (int) row[2]));
        int height = Math.min(image.rows() - y, Math.max(1, (int) row[3]));
        Mat crop = new Mat(image, new org.opencv.core.Rect(x, y, width, height));
        Mat resized = new Mat();
        try {
            org.opencv.imgproc.Imgproc.resize(crop, resized, new Size(80, 80));
            float[] values = new float[80 * 80 * 3];
            Mat converted = new Mat();
            resized.convertTo(converted, org.opencv.core.CvType.CV_32FC3);
            converted.get(0, 0, values);
            converted.release();
            float[] chw = new float[values.length];
            int plane = 80 * 80;
            for (int pixel = 0; pixel < plane; pixel++) {
                chw[pixel] = values[pixel * 3];
                chw[plane + pixel] = values[pixel * 3 + 1];
                chw[2 * plane + pixel] = values[pixel * 3 + 2];
            }
            try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(chw), new long[]{1, 3, 80, 80});
                 OrtSession.Result output = liveness.run(Map.of(liveness.getInputNames().iterator().next(), tensor))) {
                Object value = output.get(0).getValue();
                float[] logits = value instanceof float[][] matrix ? matrix[0] : (float[]) value;
                double maximum = -Double.MAX_VALUE;
                for (float logit : logits) maximum = Math.max(maximum, logit);
                double sum = 0;
                for (float logit : logits) sum += Math.exp(logit - maximum);
                int selected = logits.length > 1 ? 1 : 0;
                return Math.exp(logits[selected] - maximum) / sum;
            }
        } catch (OrtException exception) {
            throw new IllegalStateException("LIVE_LIVENESS_MODEL_ERROR", exception);
        } finally {
            crop.release();
            resized.release();
        }
    }

    private static final class Evidence {
        private int frames;
        private double previousX;
        private double previousY;
        private double totalMotion;

        private void accept(double x, double y, double livenessScore) {
            if (frames > 0) totalMotion += Math.hypot(x - previousX, y - previousY);
            livenessTotal += livenessScore;
            previousX = x;
            previousY = y;
            frames++;
        }

        private double motion() {
            return frames < 2 ? 0 : Math.min(1, totalMotion / (frames * 320.0));
        }

        private double averageLiveness() {
            return livenessTotal / Math.max(1, frames);
        }

        private double livenessTotal;
    }
}