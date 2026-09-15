package com.isc.faceclientsimulator.ai;

import ai.onnxruntime.*;

import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Map;

public final class OnnxSession implements AutoCloseable {
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final String outputName;
    private final Path resolvedModelPath;

    public OnnxSession(String modelPath) {
        this(null, modelPath);
    }

    public OnnxSession(String modelsDir, String modelPath) {
        this.resolvedModelPath = ModelPathResolver.resolve(modelsDir, modelPath);
        try {
            environment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            session = environment.createSession(resolvedModelPath.toString(), options);
            if (session.getInputNames().isEmpty() || session.getOutputNames().isEmpty()) {
                throw new IllegalStateException("ONNX model has no input/output: " + resolvedModelPath);
            }
            inputName = session.getInputNames().iterator().next();
            outputName = session.getOutputNames().iterator().next();
        } catch (OrtException e) {
            throw new IllegalStateException(
                    "Unable to initialize ONNX Runtime model: " + resolvedModelPath, e);
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
            throw new IllegalStateException("ONNX inference failed for " + resolvedModelPath + ", output " + outputName, e);
        }
    }

    public OrtSession session() {
        return session;
    }

    public Path resolvedModelPath() {
        return resolvedModelPath;
    }

    @Override
    public void close() throws Exception {
        session.close();
    }
}
