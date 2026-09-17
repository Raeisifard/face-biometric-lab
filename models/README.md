# Model directory

Place model files here:

```text
models/
  detector/face_detection_yunet_2023mar.onnx
  recognition/w600k_r50.onnx
    recognition/w600k_mbf.onnx
  liveness/2.7_80x80_MiniFASNetV2.onnx
```

Run `scripts/install-models.ps1` to fetch them.

`w600k_r50.onnx` remains the server-side recognition model for Full Clip and Live Stream. `w600k_mbf.onnx` is the separate MobileFaceNet profile used by Client Embedding mode; its client preprocessing uses RGB values approximately `(pixel - 127.5) / 128.0` before L2 normalization. Do not mix reference embeddings between these profiles.

The public InsightFace model packs have a non-commercial-research restriction according to their current documentation. Obtain a suitable commercial license/model before any production banking use.
