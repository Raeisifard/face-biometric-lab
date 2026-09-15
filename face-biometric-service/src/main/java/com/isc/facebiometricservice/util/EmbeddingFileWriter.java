package com.isc.facebiometricservice.util;

import com.isc.facebiometricservice.api.EmbeddingPayload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class EmbeddingFileWriter {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private EmbeddingFileWriter() {
    }

    public static Path write(
            EmbeddingPayload payload,
            String directory
    ) throws IOException {

        Path outputDirectory = Paths.get(directory);
        Files.createDirectories(outputDirectory);

        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP);
        String safeUserId = sanitize(payload.userId());

        String fileName = timestamp + "_" + safeUserId + ".yml";
        Path outputFile = outputDirectory.resolve(fileName);

        StringBuilder yaml = new StringBuilder(8192);

        yaml.append(payload.userId()).append(":\n");
        yaml.append("  model-id: ").append(payload.modelId()).append("\n");
        yaml.append("  model-version: ").append(payload.modelVersion()).append("\n");
        yaml.append("  dimension: ").append(payload.dimension()).append("\n");
        yaml.append("  normalized: ").append(payload.normalized()).append("\n");
        yaml.append("  embedding:\n");

        for (float value : payload.embedding()) {
            yaml.append("    - ").append(Float.toString(value)).append("\n");
        }

        Files.writeString(
                outputFile,
                yaml.toString(),
                StandardCharsets.UTF_8
        );

        return outputFile;
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}