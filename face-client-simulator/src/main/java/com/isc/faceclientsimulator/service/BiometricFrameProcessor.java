package com.isc.faceclientsimulator.service;

import com.isc.faceclientsimulator.ai.*;
import com.isc.faceclientsimulator.config.SimulatorProperties;
import com.isc.faceclientsimulator.domain.*;
import com.isc.faceclientsimulator.liveness.TemporalLivenessEngine;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

@Service
public class BiometricFrameProcessor {
    private static final double MIN_QUALITY_SCORE = 0.45;
    private final YuNetFaceDetector detector;
    private final MiniFasNetV2LivenessModel liveness;
    private final ArcFaceOnnxEmbeddingModel embedding;
    private final MobileFaceNetOnnxEmbeddingModel clientEmbedding;
    private final TemporalLivenessEngine temporal;
    private final SimulatorProperties properties;

    public BiometricFrameProcessor(YuNetFaceDetector detector,
                                    MiniFasNetV2LivenessModel liveness,
                                    ArcFaceOnnxEmbeddingModel embedding,
                                    MobileFaceNetOnnxEmbeddingModel clientEmbedding,
                                    TemporalLivenessEngine temporal,
                                    SimulatorProperties properties) {
        this.detector=detector; this.liveness=liveness; this.embedding=embedding; this.clientEmbedding=clientEmbedding; this.temporal=temporal; this.properties=properties;
    }

    public FrameAnalysis analyze(String sessionId, byte[] bytes) {
        long started=System.nanoTime();
        Mat image=OpenCvImageCodec.decode(bytes);
        try {
            FaceDetectionResult detection=detector.detect(image);
            if(!detection.faceDetected()) {
                TemporalLivenessEngine.Result r=temporal.accept(sessionId,detection,0);
                return new FrameAnalysis(sessionId,detection,toDto(r,0,System.nanoTime()-started),false,0);
            }
            double qualityScore = quality(image, detection);
            if (qualityScore < MIN_QUALITY_SCORE) {
                var result = new LivenessFrameResult("LOW_QUALITY", false, 0, 0, 0, properties.livenessMinFrames(),
                        "Improve lighting and hold the camera steady", (System.nanoTime() - started) / 1_000_000);
                return new FrameAnalysis(sessionId, detection, result, false, qualityScore);
            }
            double liveScore=liveness.liveScore(image,detection.box());
            TemporalLivenessEngine.Result r=temporal.accept(sessionId,detection,liveScore);
            return new FrameAnalysis(sessionId,detection,toDto(r,liveScore,System.nanoTime()-started),r.live(),qualityScore);
        } finally { image.release(); }
    }

    public void resetSession(String sessionId) { temporal.reset(sessionId); }

    public FaceEmbedding generateEmbedding(byte[] bytes) {
        Mat image=OpenCvImageCodec.decode(bytes);
        try {
            FaceDetectionResult detection=detector.detect(image);
            if(!detection.faceDetected()) throw new IllegalArgumentException("No face detected");
            return embedding.generate(image,detection);
        } finally { image.release(); }
    }

    public FaceEmbedding generateClientEmbedding(byte[] bytes) {
        Mat image=OpenCvImageCodec.decode(bytes);
        try {
            FaceDetectionResult detection=detector.detect(image);
            if(!detection.faceDetected()) throw new IllegalArgumentException("No face detected");
            return clientEmbedding.generate(image,detection);
        } finally { image.release(); }
    }

    private LivenessFrameResult toDto(TemporalLivenessEngine.Result r,double liveScore,long nanos) {
        return new LivenessFrameResult(r.status(),r.live(),liveScore,r.temporalMotion(),r.acceptedFrames(),r.requiredFrames(),r.instruction(),nanos/1_000_000);
    }

    private double quality(Mat image, FaceDetectionResult detection) {
        int x = Math.max(0, (int) detection.box().x());
        int y = Math.max(0, (int) detection.box().y());
        int width = Math.min(image.cols() - x, Math.max(1, (int) detection.box().width()));
        int height = Math.min(image.rows() - y, Math.max(1, (int) detection.box().height()));
        Mat crop = new Mat(image, new Rect(x, y, width, height));
        Mat gray = new Mat();
        Mat laplacian = new Mat();
        try {
            Imgproc.cvtColor(crop, gray, Imgproc.COLOR_BGR2GRAY);
            MatOfDouble mean = new MatOfDouble();
            MatOfDouble deviation = new MatOfDouble();
            org.opencv.core.Core.meanStdDev(gray, mean, deviation);
            Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);
            MatOfDouble lapMean = new MatOfDouble();
            MatOfDouble lapDeviation = new MatOfDouble();
            org.opencv.core.Core.meanStdDev(laplacian, lapMean, lapDeviation);
            double blur = Math.min(1, Math.pow(lapDeviation.get(0, 0)[0], 2) / 400.0);
            double light = 1 - Math.min(1, Math.abs(mean.get(0, 0)[0] - 128) / 128);
            double size = Math.min(1, Math.min(detection.box().width() / image.cols(), detection.box().height() / image.rows()) / .18);
            return .45 * blur + .35 * light + .20 * size;
        } finally {
            crop.release();
            gray.release();
            laplacian.release();
        }
    }
}
