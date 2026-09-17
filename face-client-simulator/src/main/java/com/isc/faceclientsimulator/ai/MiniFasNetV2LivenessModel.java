package com.isc.faceclientsimulator.ai;

import com.isc.faceclientsimulator.config.SimulatorProperties;
import com.isc.faceclientsimulator.domain.FaceBoundingBox;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MiniFasNetV2LivenessModel {
    private static final Logger log = LoggerFactory.getLogger(MiniFasNetV2LivenessModel.class);

    private static final int INPUT_CHANNELS = 3;
    private static final int INPUT_HEIGHT = 80;
    private static final int INPUT_WIDTH = 80;
    private static final int INPUT_ELEMENTS = INPUT_CHANNELS * INPUT_HEIGHT * INPUT_WIDTH;
    private static final int OUTPUT_CLASSES = 3;
    private static final long[] INPUT_SHAPE = {1, INPUT_CHANNELS, INPUT_HEIGHT, INPUT_WIDTH};

    private final OnnxSession onnx;
    private final SimulatorProperties properties;

    public MiniFasNetV2LivenessModel(SimulatorProperties properties) {
        this.properties = properties;
        this.onnx = new OnnxSession(properties.modelsDir(), properties.livenessModelPath());

        log.info("MiniFASNetV2 model initialized: path={}, inputName={}, outputName={}, expectedInputShape=[1,3,80,80], expectedInputRange=[0,255] BGR, expectedOutputClasses=3",
                onnx.resolvedModelPath(), onnx.session().getInputNames().iterator().next(),
                onnx.session().getOutputNames().iterator().next());
    }

    public double liveScore(Mat frame, FaceBoundingBox bbox) {
        Mat crop = crop(frame, bbox, 2.7);
        try {
            float[] input = toTensor(crop);
            validateInputContract(input, INPUT_SHAPE);

            log.debug("MiniFASNetV2 inference input: crop={}x{}x{}, tensorLength={}, min={}, max={}, shape=[1,3,80,80]",
                    crop.cols(), crop.rows(), crop.channels(), input.length, min(input), max(input));

            float[] logits = onnx.run(input, INPUT_SHAPE);
            validateOutputContract(logits);

            double[] probs = softmax(logits);
            double liveScore = probs[1];
            log.debug("MiniFASNetV2 inference output: logits=[{}, {}, {}], probabilities=[{}, {}, {}], liveScore={}",
                    logits[0], logits[1], logits[2], probs[0], probs[1], probs[2], liveScore);
            return liveScore;
        } finally {
            crop.release();
        }
    }

    static void validateInputContract(float[] input, long[] shape) {
        if (shape == null || shape.length != 4
                || shape[0] != 1 || shape[1] != INPUT_CHANNELS
                || shape[2] != INPUT_HEIGHT || shape[3] != INPUT_WIDTH) {
            throw new IllegalStateException("MiniFASNetV2 input shape must be [1,3,80,80], got " + java.util.Arrays.toString(shape));
        }
        if (input == null || input.length != INPUT_ELEMENTS) {
            throw new IllegalStateException("MiniFASNetV2 input must contain exactly " + INPUT_ELEMENTS + " float values");
        }
        for (float value : input) {
            if (!Float.isFinite(value) || value < 0.0f || value > 255.0f) {
                throw new IllegalStateException("MiniFASNetV2 input values must be finite and in [0,255]");
            }
        }
    }

    static void validateOutputContract(float[] output) {
        if (output == null || output.length != OUTPUT_CLASSES) {
            throw new IllegalStateException("MiniFASNetV2 output must contain exactly 3 logits, got "
                    + (output == null ? "null" : output.length));
        }
        for (float value : output) {
            if (!Float.isFinite(value)) {
                throw new IllegalStateException("MiniFASNetV2 output logits must be finite");
            }
        }
    }

    private Mat crop(Mat image, FaceBoundingBox box, double scale) {
        double safeScale = Math.min(
                Math.min((image.rows() - 1) / Math.max(1.0, box.height()),
                        (image.cols() - 1) / Math.max(1.0, box.width())), scale);
        double nw = box.width() * safeScale;
        double nh = box.height() * safeScale;
        double cx = box.centerX(), cy = box.centerY();
        int x1 = (int) Math.max(0, Math.floor(cx - nw / 2));
        int y1 = (int) Math.max(0, Math.floor(cy - nh / 2));
        int x2 = (int) Math.min(image.cols() - 1, Math.ceil(cx + nw / 2));
        int y2 = (int) Math.min(image.rows() - 1, Math.ceil(cy + nh / 2));
        Rect r = new Rect(x1, y1, Math.max(1, x2 - x1 + 1), Math.max(1, y2 - y1 + 1));
        Mat c = new Mat(image, r);
        Mat out = new Mat();
        try {
            Imgproc.resize(c, out, new Size(INPUT_WIDTH, INPUT_HEIGHT));
            return out;
        } finally {
            c.release();
        }
    }

    private float[] toTensor(Mat image) {
        if (image.channels() != INPUT_CHANNELS) {
            throw new IllegalStateException("MiniFASNetV2 expects a 3-channel BGR crop, got channels=" + image.channels());
        }

        Mat f = new Mat();
        try {
            image.convertTo(f, CvType.CV_32FC3);
            float[] hwc = new float[INPUT_ELEMENTS];
            f.get(0, 0, hwc);
            float[] chw = new float[INPUT_ELEMENTS];
            int plane = INPUT_HEIGHT * INPUT_WIDTH;
            for (int y = 0; y < INPUT_HEIGHT; y++) {
                for (int x = 0; x < INPUT_WIDTH; x++) {
                    int p = (y * INPUT_WIDTH + x) * INPUT_CHANNELS;
                    int q = y * INPUT_WIDTH + x;
                    chw[q] = hwc[p];
                    chw[plane + q] = hwc[p + 1];
                    chw[2 * plane + q] = hwc[p + 2];
                }
            }
            return chw;
        } finally {
            f.release();
        }
    }

    private double[] softmax(float[] x) {
        double max = -Double.MAX_VALUE;
        for (float v : x) max = Math.max(max, v);
        double sum = 0;
        double[] e = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            e[i] = Math.exp(x[i] - max);
            sum += e[i];
        }
        for (int i = 0; i < x.length; i++) e[i] /= sum;
        return e;
    }

    private float min(float[] values) {
        float result = Float.POSITIVE_INFINITY;
        for (float value : values) result = Math.min(result, value);
        return result;
    }

    private float max(float[] values) {
        float result = Float.NEGATIVE_INFINITY;
        for (float value : values) result = Math.max(result, value);
        return result;
    }
}
