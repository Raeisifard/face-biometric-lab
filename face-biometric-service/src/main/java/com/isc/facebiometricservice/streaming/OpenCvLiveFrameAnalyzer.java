package com.isc.facebiometricservice.streaming;

import com.isc.facebiometricservice.config.VideoVerificationProperties;
import com.isc.facebiometricservice.util.ModelPathResolver;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Component;

@Component
public class OpenCvLiveFrameAnalyzer implements LiveFrameAnalyzer {
    private final FaceDetectorYN detector;

    public OpenCvLiveFrameAnalyzer(VideoVerificationProperties properties) {
        var model = ModelPathResolver.resolve(properties.detectorModelPath());
        detector = FaceDetectorYN.create(model.toString(), "", new Size(320, 320), 0.75f, 0.3f, 5000);
    }

    @Override
    public Analysis analyze(byte[] frame) {
        Mat image = Imgcodecs.imdecode(new MatOfByte(frame), Imgcodecs.IMREAD_COLOR);
        if (image.empty()) return new Analysis("INVALID_FRAME", "Frame could not be decoded as an image");
        try {
            detector.setInputSize(image.size());
            Mat faces = new Mat();
            try {
                detector.detect(image, faces);
                int count = faces.rows();
                if (count == 0) return new Analysis("NO_FACE", "No face detected in frame");
                if (count > 1) return new Analysis("MULTIPLE_FACES", "Exactly one face is required");
                return new Analysis("GOOD_FRAME", "One face detected");
            } finally {
                faces.release();
            }
        } finally {
            image.release();
        }
    }
}