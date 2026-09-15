package com.isc.faceclientsimulator.ai;

import com.isc.faceclientsimulator.config.SimulatorProperties;
import com.isc.faceclientsimulator.domain.FaceBoundingBox;
import com.isc.faceclientsimulator.domain.FaceDetectionResult;
import com.isc.faceclientsimulator.domain.FaceEmbedding;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class ArcFaceOnnxEmbeddingModel {
    private static final double[] TARGET = {38.2946, 51.6963, 73.5318, 51.5014, 56.0252, 71.7366};
    private final OnnxSession onnx;
    private final L2Normalizer normalizer;
    private final SimulatorProperties properties;

    public ArcFaceOnnxEmbeddingModel(SimulatorProperties properties, L2Normalizer normalizer) {
        this.properties = properties;
        this.normalizer = normalizer;
        this.onnx = new OnnxSession(properties.modelsDir(), properties.recognitionModelPath());
    }

    public FaceEmbedding generate(Mat frame, FaceDetectionResult detection) {
        if (!detection.faceDetected()) throw new IllegalArgumentException("No face detected");
        Mat aligned = align(frame, detection);
        try {
            float[] chw = toArcFaceTensor(aligned);
            float[] raw = onnx.run(chw, new long[]{1, 3, 112, 112});
            if (raw.length != 512) {
                throw new IllegalStateException("ArcFace model output must be 512-D, actual=" + raw.length);
            }
            float[] normalized = normalizer.normalize(raw);
            return new FaceEmbedding(normalized, 512,
                    properties.recognitionModelId(), properties.recognitionModelVersion(), true);
        } finally {
            aligned.release();
        }
    }

    private Mat align(Mat frame, FaceDetectionResult detection) {
        double[] src = {
                detection.landmarks().rightEye().x(), detection.landmarks().rightEye().y(),
                detection.landmarks().leftEye().x(), detection.landmarks().leftEye().y(),
                detection.landmarks().nose().x(), detection.landmarks().nose().y()
        };

        MatOfPoint2f srcPts = new MatOfPoint2f(
                new Point(src[0], src[1]),
                new Point(src[2], src[3]),
                new Point(src[4], src[5]));
        MatOfPoint2f dstPts = new MatOfPoint2f(
                new Point(TARGET[0], TARGET[1]),
                new Point(TARGET[2], TARGET[3]),
                new Point(TARGET[4], TARGET[5]));

        Mat transform = null;
        try {
            transform = Imgproc.getAffineTransform(srcPts, dstPts);
            Mat out = new Mat(112, 112, CvType.CV_8UC3);
            Imgproc.warpAffine(frame, out, transform, new Size(112, 112));
            return out;
        } finally {
            srcPts.release();
            dstPts.release();
            if (transform != null) {
                transform.release();
            }
        }
    }

    private float[] toArcFaceTensor(Mat image) {
        Mat f = new Mat();
        try {
            image.convertTo(f, CvType.CV_32FC3);
            float[] hwc = new float[112 * 112 * 3];
            f.get(0, 0, hwc);
            float[] chw = new float[3 * 112 * 112];
            int plane = 112 * 112;
            for (int y = 0; y < 112; y++) {
                for (int x = 0; x < 112; x++) {
                    int p = (y * 112 + x) * 3;
                    int q = y * 112 + x;
                    float b = hwc[p], g = hwc[p + 1], r = hwc[p + 2];
                    chw[q] = (r - 127.5f) / 127.5f;
                    chw[plane + q] = (g - 127.5f) / 127.5f;
                    chw[2 * plane + q] = (b - 127.5f) / 127.5f;
                }
            }
            return chw;
        } finally {
            f.release();
        }
    }
}
