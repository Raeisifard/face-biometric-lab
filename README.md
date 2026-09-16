# Face Biometric Lab

A two-module Spring Boot project for developing a mobile-style face biometric flow.

## Modules

`face-client-simulator` is the Windows/browser-facing simulator. It uses the browser camera APIs to preview any camera the browser can access, samples frames, performs face detection, MiniFASNet V2 anti-spoofing plus temporal liveness checks, aligns the face and generates a 512-dimensional ArcFace embedding with ONNX Runtime. It can then simulate the request a Flutter app would send to the server.

`face-biometric-service` is the independent server-side service. It accepts a 512-D probe embedding, retrieves a registered reference embedding from Oracle, MongoDB or memory, validates the model/version contract and calculates cosine similarity.

## Architecture

```text
Browser / Windows camera
        |
        | getUserMedia()
        v
+--------------------------+
| face-client-simulator    |
|                          |
| webcam preview           |
| frame sampling           |
| YuNet face detector      |
| MiniFASNetV2 + temporal  |
| ArcFace w600k_r50        |
| 512-D embedding          |
+------------+-------------+
             |
             | JSON embedding
             v
+--------------------------+
| face-biometric-service   |
|                          |
| reference repository     |
| Oracle / Mongo / memory  |
| cosine similarity        |
| match decision            |
+--------------------------+
```

## Models

The zip intentionally does not include model binaries. The main public InsightFace pretrained packs are currently distributed for non-commercial research unless separately licensed, so the project documents model acquisition separately. The setup script can download the required files after the license acknowledgement step.

Initial baseline:

* Face detection: YuNet `face_detection_yunet_2023mar.onnx`
* Recognition: InsightFace `w600k_r50.onnx` from `buffalo_l` (512-D ArcFace-compatible embedding)
* Active/passive anti-spoofing: MiniFASNetV2 `2.7_80x80_MiniFASNetV2.onnx`
* Temporal liveness: movement challenge + multi-frame anti-spoof aggregation

These are deliberately behind interfaces so SCRFD, alternative ArcFace/AdaFace models, stronger liveness models and other detectors can be added later.

## Important security note

This project is a development simulator and biometric laboratory. The webcam liveness implementation is not a certified production anti-spoofing control. In a banking deployment it should be combined with secure capture, device/app integrity, CID/DID, protected transport, server-side policy and a validated PAD/liveness solution.

## Quick start

1. Install Java 21 and Maven 3.9+.
2. Run `scripts/install-models.ps1` in PowerShell from this repository after reviewing the model licensing notes.
3. Start the server service first:

   ```powershell
   mvn -pl face-biometric-service spring-boot:run
   ```

4. Start the client simulator in a second terminal:

   ```powershell
   mvn -pl face-client-simulator spring-boot:run
   ```

5. Open `http://localhost:8091` in Chrome or Edge.
6. Grant camera permission and select the desired Windows camera.
7. Click **Start camera**. The live preview is rendered by the browser.
8. The simulator samples the stream at the configured rate and sends frames to Spring Boot for YuNet detection and MiniFASNetV2 anti-spoofing.
9. Slowly move the head left/right until the temporal liveness requirement is satisfied.
10. Enroll a user, or verify against an existing reference embedding.

### Oracle profile

For the authoritative Oracle repository:

```powershell
$env:SPRING_PROFILES_ACTIVE="oracle"
mvn -pl face-biometric-service spring-boot:run
```

The default `memory` repository is useful for bringing up the full webcam flow before connecting to Oracle.

## Camera support

Camera access is implemented with the browser `MediaDevices.getUserMedia()` API. Therefore any webcam or camera device exposed to Windows and the browser can be selected from the camera dropdown. The simulator also accepts a local video file as a deterministic stream source for repeatable tests.

## Ports

* biometric service: `8090`
* client simulator: `8091`

## Maven

Use Maven 3.9+ from your local installation. A Maven Wrapper can be generated locally with `mvn -N wrapper:wrapper` when desired.

### Commands
````
mvn -pl face-biometric-service -Dtest=StoredVideoLivenessTest test
mvn -pl face-biometric-service "-Dtest=StoredVideoLivenessTest" "-Dbiometric.test.video=C:\behnam\java\face-biometric-lab\video-captures\user-123_20260916_155603_790.webm" test
````