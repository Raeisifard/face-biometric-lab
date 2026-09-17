package com.isc.faceclientsimulator.ai;

import com.isc.faceclientsimulator.config.SimulatorProperties;
import com.isc.faceclientsimulator.domain.FaceDetectionResult;
import com.isc.faceclientsimulator.domain.FaceEmbedding;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class MobileFaceNetOnnxEmbeddingModel {
    private static final double[] TARGET = {38.2946, 51.6963, 73.5318, 51.5014, 56.0252, 71.7366};
    private final OnnxSession onnx;
    private final L2Normalizer normalizer;
    private final SimulatorProperties properties;

    public MobileFaceNetOnnxEmbeddingModel(SimulatorProperties properties, L2Normalizer normalizer) {
        this.properties = properties;
        this.normalizer = normalizer;
        this.onnx = new OnnxSession(properties.modelsDir(), properties.clientRecognitionModelPath());
    }

    public FaceEmbedding generate(Mat frame, FaceDetectionResult detection) {
        if (!detection.faceDetected()) throw new IllegalArgumentException("No face detected");
        Mat aligned = align(frame, detection);
        try {
            float[] raw = onnx.run(toMobileFaceNetTensor(aligned), new long[]{1, 3, 112, 112});
            if (raw.length != 512) throw new IllegalStateException("MobileFaceNet model output must be 512-D, actual=" + raw.length);
            return new FaceEmbedding(normalizer.normalize(raw), 512,
                    properties.clientRecognitionModelId(), properties.clientRecognitionModelVersion(), true);
        } finally {
            aligned.release();
        }
    }

    private Mat align(Mat frame, FaceDetectionResult detection) {
        MatOfPoint2f source = new MatOfPoint2f(
                new Point(detection.landmarks().rightEye().x(), detection.landmarks().rightEye().y()),
                new Point(detection.landmarks().leftEye().x(), detection.landmarks().leftEye().y()),
                new Point(detection.landmarks().nose().x(), detection.landmarks().nose().y()));
        MatOfPoint2f target = new MatOfPoint2f(
                new Point(TARGET[0], TARGET[1]), new Point(TARGET[2], TARGET[3]), new Point(TARGET[4], TARGET[5]));
        Mat transform = Imgproc.getAffineTransform(source, target);
        Mat output = new Mat();
        try {
            Imgproc.warpAffine(frame, output, transform, new Size(112, 112));
            return output;
        } finally {
            source.release();
            target.release();
            transform.release();
        }
    }

    /** MobileFaceNet profile: RGB bytes centered at 127.5 with divisor 128.0. */
    private float[] toMobileFaceNetTensor(Mat image) {
        Mat converted = new Mat();
        try {
            image.convertTo(converted, CvType.CV_32FC3);
            float[] hwc = new float[112 * 112 * 3];
            converted.get(0, 0, hwc);
            float[] chw = new float[hwc.length];
            int plane = 112 * 112;
            for (int pixel = 0; pixel < plane; pixel++) {
                int source = pixel * 3;
                chw[pixel] = (hwc[source + 2] - 127.5f) / 128.0f;
                chw[plane + pixel] = (hwc[source + 1] - 127.5f) / 128.0f;
                chw[2 * plane + pixel] = (hwc[source] - 127.5f) / 128.0f;
            }
            return chw;
        } finally {
            converted.release();
        }
    }
}