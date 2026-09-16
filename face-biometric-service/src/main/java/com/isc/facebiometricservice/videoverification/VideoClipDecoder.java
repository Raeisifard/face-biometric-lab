package com.isc.facebiometricservice.videoverification;

import com.isc.facebiometricservice.config.VideoVerificationProperties;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class VideoClipDecoder {

    private static final Logger log = LoggerFactory.getLogger(VideoClipDecoder.class);
    private static final DateTimeFormatter STORAGE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final VideoVerificationProperties properties;

    public VideoClipDecoder(VideoVerificationProperties properties) {
        this.properties = properties;
    }

    public DecodedClip decode(MultipartFile file, double sampleFps, String referenceId) throws IOException {
        log.info("Video upload received: filename={}, contentType={}, bytes={}, sampleFps={}",
                file.getOriginalFilename(), file.getContentType(), file.getSize(), sampleFps);

        Path stored = storeForEvaluation(file, referenceId);
        Path tmp = Files.createTempFile("biometric-clip-", ".video");
        try {
            file.transferTo(tmp);
            return decode(tmp, sampleFps);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private Path storeForEvaluation(MultipartFile file, String referenceId) throws IOException {
        Path directory = Path.of(properties.storageDirectory()).toAbsolutePath().normalize();
        Files.createDirectories(directory);

        String safeReferenceId = sanitizeReferenceId(referenceId);
        String extension = extension(file.getOriginalFilename());
        Path target = directory.resolve(safeReferenceId + "_" + STORAGE_TIMESTAMP.format(LocalDateTime.now()) + extension)
                .normalize();

        if (!target.getParent().equals(directory)) {
            throw new IOException("Invalid video storage path");
        }

        file.transferTo(target);
        log.info("Video stored for evaluation: referenceId={}, path={}, bytes={}",
                referenceId, target, file.getSize());
        return target;
    }

    private String sanitizeReferenceId(String referenceId) {
        String value = referenceId == null ? "unknown" : referenceId.trim();
        value = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        return value.isBlank() ? "unknown" : value;
    }

    private String extension(String filename) {
        if (filename == null) {
            return ".webm";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return ".webm";
        }
        String value = filename.substring(dot).toLowerCase();
        return value.matches("\\.[a-z0-9]{1,8}") ? value : ".webm";
    }

    public DecodedClip decode(Path path, double sampleFps) {
        log.info("Video decode started: path={}, sampleFps={}", path, sampleFps);

        VideoCapture cap = new VideoCapture(path.toString());
        if (!cap.isOpened()) {
            log.warn("Video decode failed: cannot open media, path={}", path);
            throw new IllegalArgumentException("INVALID_VIDEO");
        }

        double reportedFps = cap.get(Videoio.CAP_PROP_FPS);
        double reportedFrameCount = cap.get(Videoio.CAP_PROP_FRAME_COUNT);
        double metadataDuration = calculateMetadataDuration(reportedFps, reportedFrameCount);

        log.info("Video metadata: path={}, fps={}, reportedFrameCount={}, calculatedDurationSeconds={}",
                path, reportedFps, reportedFrameCount, metadataDuration);

        int step = calculateSamplingStep(reportedFps, sampleFps);
        List<Frame> out = new ArrayList<>();
        List<Double> timestamps = new ArrayList<>();

        Mat m = new Mat();
        int decodedFrameCount = 0;

        try {
            while (cap.read(m)) {
                double timestampSeconds = readTimestampSeconds(cap);
                if (isFiniteNonNegative(timestampSeconds)) {
                    timestamps.add(timestampSeconds);
                }

                if (decodedFrameCount % step == 0) {
                    out.add(new Frame(decodedFrameCount, timestampSeconds, m.clone()));
                }
                decodedFrameCount++;
            }
        } finally {
            m.release();
            cap.release();
        }

        if (out.isEmpty()) {
            throw new IllegalArgumentException("INVALID_VIDEO");
        }

        DurationResolution durationResolution = resolveDuration(metadataDuration, timestamps, decodedFrameCount, reportedFps);
        int resolvedTotalFrames = isPositiveFinite(reportedFrameCount) ? safeFrameCount(reportedFrameCount) : decodedFrameCount;

        log.info("Video decode completed: path={}, decodedFrames={}, reportedFrameCount={}, fps={}, calculatedDurationSeconds={}, resolvedDurationSeconds={}, durationSource={}, samplingStep={}",
                path, out.size(), reportedFrameCount, reportedFps, metadataDuration, durationResolution.durationSeconds(), durationResolution.source(), step);

        if (!isPositiveFinite(durationResolution.durationSeconds())) {
            log.warn("Video duration unavailable: path={}, decodedFrameCount={}, reportedFrameCount={}, fps={}, timestampSamples={}",
                    path, decodedFrameCount, reportedFrameCount, reportedFps, timestamps.size());
            throw new IllegalArgumentException("INVALID_VIDEO");
        }

        return new DecodedClip(durationResolution.durationSeconds(), reportedFps, resolvedTotalFrames, out);
    }

    private int calculateSamplingStep(double fps, double sampleFps) {
        if (!isPositiveFinite(fps) || !isPositiveFinite(sampleFps)) return 1;
        return Math.max(1, (int) Math.round(fps / sampleFps));
    }

    private double calculateMetadataDuration(double fps, double frameCount) {
        if (!isPositiveFinite(fps) || !isPositiveFinite(frameCount)) return -1.0;
        double duration = frameCount / fps;
        return isPositiveFinite(duration) ? duration : -1.0;
    }

    private DurationResolution resolveDuration(double metadataDuration, List<Double> timestamps, int decodedFrameCount, double reportedFps) {
        if (isPositiveFinite(metadataDuration)) return new DurationResolution(metadataDuration, "CONTAINER_METADATA");
        if (timestamps.size() >= 2) {
            double last = timestamps.get(timestamps.size() - 1);
            double interval = medianPositiveInterval(timestamps);
            if (isFiniteNonNegative(last) && isPositiveFinite(interval)) {
                double duration = last + interval;
                if (isPositiveFinite(duration)) return new DurationResolution(duration, "FRAME_TIMESTAMPS");
            }
        }
        if (isPositiveFinite(reportedFps) && decodedFrameCount > 0 && reportedFps > 1.0) {
            double duration = decodedFrameCount / reportedFps;
            if (isPositiveFinite(duration)) return new DurationResolution(duration, "DECODED_FRAMES_AND_FPS");
        }
        return new DurationResolution(-1.0, "UNAVAILABLE");
    }

    private double medianPositiveInterval(List<Double> timestamps) {
        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < timestamps.size(); i++) {
            double delta = timestamps.get(i) - timestamps.get(i - 1);
            if (isPositiveFinite(delta)) intervals.add(delta);
        }
        if (intervals.isEmpty()) return -1.0;
        intervals.sort(Double::compareTo);
        int middle = intervals.size() / 2;
        if (intervals.size() % 2 == 0) return (intervals.get(middle - 1) + intervals.get(middle)) / 2.0;
        return intervals.get(middle);
    }

    private double readTimestampSeconds(VideoCapture cap) {
        double milliseconds = cap.get(Videoio.CAP_PROP_POS_MSEC);
        if (!Double.isFinite(milliseconds) || milliseconds < 0) return -1.0;
        return milliseconds / 1000.0;
    }

    private int safeFrameCount(double value) {
        if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(0, (int) Math.round(value));
    }

    private boolean isPositiveFinite(double value) { return Double.isFinite(value) && value > 0; }
    private boolean isFiniteNonNegative(double value) { return Double.isFinite(value) && value >= 0; }

    private record DurationResolution(double durationSeconds, String source) {}
    public record Frame(int index, double timestampSeconds, Mat image) {}
    public record DecodedClip(double durationSeconds, double fps, int totalFrames, List<Frame> frames) {}
}
