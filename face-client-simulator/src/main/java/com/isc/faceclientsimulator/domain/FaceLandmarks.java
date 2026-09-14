package com.isc.faceclientsimulator.domain;

public record FaceLandmarks(
        Point2 rightEye,
        Point2 leftEye,
        Point2 nose,
        Point2 rightMouth,
        Point2 leftMouth
) {
    public record Point2(double x, double y) {}

    public double yawProxy(FaceBoundingBox box) {
        double eyeMidX = (rightEye.x() + leftEye.x()) / 2.0;
        double interEye = Math.max(1.0, Math.abs(leftEye.x() - rightEye.x()));
        return (nose.x() - eyeMidX) / interEye;
    }
}
