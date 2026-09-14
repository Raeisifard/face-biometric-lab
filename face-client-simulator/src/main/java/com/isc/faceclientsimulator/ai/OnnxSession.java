package com.isc.faceclientsimulator.ai;

import ai.onnxruntime.*;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class OnnxSession implements AutoCloseable {
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final String outputName;

    public OnnxSession(String modelPath) {
        try {
            Path p = Path.of(modelPath);
            if (!Files.isRegularFile(p)) throw new IllegalStateException("ONNX model not found: " + p.toAbsolutePath());
            environment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            session = environment.createSession(p.toString(), options);
            if (session.getInputNames().isEmpty() || session.getOutputNames().isEmpty()) throw new IllegalStateException("ONNX model has no input/output");
            inputName = session.getInputNames().iterator().next();
            outputName = session.getOutputNames().iterator().next();
        } catch (OrtException e) {
            throw new IllegalStateException("Unable to initialize ONNX Runtime model: " + modelPath, e);
        }
    }

    public synchronized float[] run(float[] input, long[] shape) {
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), shape);
             OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
            Object value = result.get(0).getValue();
            if (value instanceof float[][] matrix) return matrix[0];
            if (value instanceof float[] vector) return vector;
            throw new IllegalStateException("Unsupported ONNX output type: " + value.getClass());
        } catch (OrtException e) {
            throw new IllegalStateException("ONNX inference failed for output " + outputName, e);
        }
    }

    public OrtSession session() { return session; }

    @Override public void close() throws Exception { session.close(); }
}
