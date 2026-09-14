# Webcam liveness

Version 1 combines two signals:

1. **MiniFASNetV2** frame-level anti-spoofing score. The model is 80x80 and follows the Silent-Face-Anti-Spoofing convention in which class 1 represents a real face.
2. **Temporal motion challenge** across a session window. The detector's five facial landmarks are tracked through sampled frames and the simulator requires observable movement before it accepts the session as live.

The final status is one of `COLLECTING`, `MOVE_HEAD`, `SUSPECTED_SPOOF`, `LIVE`, or `NO_FACE`.

This is intentionally a development/lab implementation. It is not a certified presentation-attack detector and must not be treated as sufficient for banking production. A production system should use a validated liveness/PAD model and secure camera capture, and combine that with device/app integrity and identity controls.
