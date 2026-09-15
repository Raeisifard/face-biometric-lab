package com.isc.faceclientsimulator.ai;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Resolves ONNX models without depending on the JVM working directory. */
public final class ModelPathResolver {
    private static final String MODELS_PROPERTY = "face.simulator.models-dir";
    private static final String MODELS_ENV = "FACE_SIMULATOR_MODELS_DIR";

    private ModelPathResolver() {
    }

    public static Path resolve(String modelsDir, String modelPath) {
        if (modelPath == null || modelPath.isBlank()) {
            throw new IllegalArgumentException("ONNX model path must not be null or empty");
        }

        String normalizedModel = normalize(modelPath);

        Path absolute = tryAbsolute(normalizedModel);
        if (absolute != null) {
            return absolute;
        }

        if (normalizedModel.startsWith("classpath:/")) {
            return materializeClasspath(normalizedModel.substring("classpath:/".length()));
        }

        String rootSetting = firstNonBlank(
                modelsDir,
                System.getProperty(MODELS_PROPERTY),
                System.getenv(MODELS_ENV));

        if (rootSetting != null) {
            Path configuredRoot = Path.of(normalize(rootSetting)).toAbsolutePath().normalize();
            Path found = findUnderRoot(configuredRoot, normalizedModel);
            if (found != null) {
                return found;
            }
        }

        for (Path root : discoverRoots()) {
            Path found = findFromBase(root, normalizedModel);
            if (found != null) {
                return found;
            }
        }

        Path classpath = tryMaterializeClasspath(stripLeadingSlash(normalizedModel));
        if (classpath != null) {
            return classpath;
        }

        throw new IllegalStateException(buildNotFoundMessage(normalizedModel, rootSetting));
    }

    public static Path resolve(String modelPath) {
        return resolve(null, modelPath);
    }

    private static Path tryAbsolute(String value) {
        try {
            Path path = Path.of(value);
            if (!path.isAbsolute()) {
                return null;
            }
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                throw new IllegalStateException("ONNX model not found or not readable: " + path.toAbsolutePath());
            }
            return path.normalize();
        } catch (InvalidPathException e) {
            throw new IllegalStateException("Invalid ONNX model path: " + value, e);
        }
    }

    private static Path findUnderRoot(Path root, String modelPath) {
        if (!Files.isDirectory(root)) {
            return null;
        }

        String clean = cleanModelPath(modelPath);
        Path candidate = root.resolve(clean).normalize();
        if (isFile(candidate)) {
            return candidate;
        }

        Path modelsChild = root.resolve("models").resolve(clean).normalize();
        return isFile(modelsChild) ? modelsChild : null;
    }

    private static Path findFromBase(Path base, String modelPath) {
        if (base == null || !Files.isDirectory(base)) {
            return null;
        }

        String clean = cleanModelPath(modelPath);
        Path direct = base.resolve(modelPath).normalize();
        if (isFile(direct)) {
            return direct;
        }

        Path cleanPath = base.resolve(clean).normalize();
        if (isFile(cleanPath)) {
            return cleanPath;
        }

        Path models = base.resolve("models").resolve(clean).normalize();
        return isFile(models) ? models : null;
    }

    private static List<Path> discoverRoots() {
        List<Path> roots = new ArrayList<>();
        Path working = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        addRoot(roots, working);
        addRoot(roots, working.resolve("models"));

        try {
            URI location = ModelPathResolver.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            Path code = Path.of(location).toAbsolutePath().normalize();
            if (Files.isRegularFile(code)) {
                code = code.getParent();
            }

            Path current = code;
            for (int i = 0; i < 10 && current != null; i++) {
                addRoot(roots, current);
                addRoot(roots, current.resolve("models"));
                current = current.getParent();
            }
        } catch (Exception ignored) {
            // user.dir remains the fallback discovery root.
        }
        return roots;
    }

    private static void addRoot(List<Path> roots, Path path) {
        if (path == null) {
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (!roots.contains(normalized)) {
            roots.add(normalized);
        }
    }

    private static Path materializeClasspath(String resource) {
        Path path = tryMaterializeClasspath(resource);
        if (path == null) {
            throw new IllegalStateException("ONNX model classpath resource not found: " + resource);
        }
        return path;
    }

    private static Path tryMaterializeClasspath(String resource) {
        String clean = stripLeadingSlash(resource);
        try (InputStream input = ModelPathResolver.class.getClassLoader().getResourceAsStream(clean)) {
            if (input == null) {
                return null;
            }
            String fileName = Path.of(clean).getFileName().toString();
            Path temp = Files.createTempFile("face-client-simulator-", "-" + fileName);
            Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            temp.toFile().deleteOnExit();
            return temp;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to materialize ONNX model from classpath: " + clean, e);
        }
    }

    private static boolean isFile(Path path) {
        return Files.isRegularFile(path) && Files.isReadable(path);
    }

    private static String cleanModelPath(String value) {
        String result = value;
        while (result.startsWith("./")) {
            result = result.substring(2);
        }
        while (result.startsWith("../")) {
            result = result.substring(3);
        }
        if (result.startsWith("models/")) {
            result = result.substring("models/".length());
        }
        return result;
    }

    private static String stripLeadingSlash(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    private static String normalize(String value) {
        return value.trim().replace('\\', '/');
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String buildNotFoundMessage(String model, String configuredRoot) {
        StringBuilder message = new StringBuilder("ONNX model not found: ")
                .append(model)
                .append(System.lineSeparator())
                .append("The resolver does not depend on ./ or ../. Searched application/project locations:")
                .append(System.lineSeparator());
        for (Path root : discoverRoots()) {
            message.append("  - ").append(root).append(System.lineSeparator());
        }
        if (configuredRoot != null) {
            message.append("Configured model directory: ").append(configuredRoot).append(System.lineSeparator());
        }
        message.append("You may set FACE_SIMULATOR_MODELS_DIR to an absolute model directory.");
        return message.toString();
    }
}
