package com.isc.faceclientsimulator.ai;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

public final class OpenCvImageCodec {
    private OpenCvImageCodec() {}

    public static Mat decode(byte[] bytes) {
        MatOfByte mob = new MatOfByte(bytes);
        try {
            Mat image = Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_COLOR);
            if (image.empty()) throw new IllegalArgumentException("Invalid image payload");
            return image;
        } finally { mob.release(); }
    }
}
