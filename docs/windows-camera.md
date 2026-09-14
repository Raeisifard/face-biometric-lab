# Windows 11 camera flow

The client simulator does not open `VideoCapture(0)` in the JVM. The browser is responsible for camera access through `navigator.mediaDevices.getUserMedia()`. This is intentional: Chrome/Edge already know about USB webcams, integrated cameras, capture cards and other Windows camera devices exposed to the browser.

The UI enumerates `videoinput` devices after permission is granted and lets the operator choose a camera. The live stream is displayed in a `<video>` element. The simulator captures a low-rate JPEG sample from that video into a canvas and sends it to the local Spring Boot endpoint for AI analysis.

This design keeps the preview responsive and makes the frame-analysis boundary explicit, similar to a Flutter camera pipeline.

For a production mobile application the browser sampling layer is replaced by the Flutter camera implementation; the server-side vector API remains the same.
