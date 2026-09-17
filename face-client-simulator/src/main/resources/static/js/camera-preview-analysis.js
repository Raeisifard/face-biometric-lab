(function () {
  const state = { sessionId: null, timer: null, busy: false };
  const log = (...values) => console.info('[CAMERA_PREVIEW]', ...values);

  async function json(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) throw new Error(await response.text());
    return response.json();
  }

  function selectedMethod() {
    return window.selectedCaptureMethod || document.querySelector('.method-badge')?.textContent || '';
  }

  function activeForPreview() {
    const method = selectedMethod();
    return method === 'FULL_CLIP' || method === 'LIVE_STREAM' || method.includes('FULL CLIP') || method.includes('LIVE STREAM');
  }

  function draw(result) {
    const video = document.getElementById('preview');
    const canvas = document.getElementById('camera-overlay');
    if (!video || !canvas) return;
    const width = video.videoWidth || 640;
    const height = video.videoHeight || 480;
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    context.clearRect(0, 0, width, height);
    const detection = result?.detection;
    if (!detection?.faceDetected || !detection.box) return;
    const live = Boolean(result.liveness?.frameLooksLive);
    context.strokeStyle = live ? '#32d583' : '#f5b94c';
    context.lineWidth = Math.max(3, width / 320);
    context.strokeRect(detection.box.x, detection.box.y, detection.box.width, detection.box.height);
    context.font = 'bold 14px sans-serif';
    context.fillStyle = context.strokeStyle;
    context.fillText(live ? 'LIVE' : (result.liveness?.status || 'FACE'), detection.box.x, Math.max(18, detection.box.y - 6));
  }

  async function analyze() {
    if (!state.sessionId || state.busy || !activeForPreview()) return;
    const video = document.getElementById('preview');
    if (!video || !video.videoWidth) return;
    state.busy = true;
    try {
      const canvas = document.createElement('canvas');
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
      const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.78));
      const form = new FormData();
      form.append('sessionId', state.sessionId);
      form.append('image', blob, 'preview-frame.jpg');
      const result = await json('/api/v1/simulator/frames', { method: 'POST', body: form });
      draw(result);
      log('preview response', { method: selectedMethod(), detection: result.detection, liveness: result.liveness, qualityScore: result.qualityScore });
      const stateElement = document.querySelector('.camera-state');
      if (stateElement) stateElement.textContent = result.liveness?.status || 'ANALYZING';
    } catch (error) {
      console.error('[CAMERA_PREVIEW] analysis error', error);
    } finally {
      state.busy = false;
    }
  }

  async function start() {
    if (!activeForPreview() || state.sessionId) return;
    try {
      const result = await json('/api/v1/simulator/sessions', { method: 'POST' });
      state.sessionId = result.sessionId;
      state.timer = setInterval(analyze, 300);
      log('preview analysis started', { sessionId: state.sessionId, method: selectedMethod() });
    } catch (error) {
      console.error('[CAMERA_PREVIEW] session start error', error);
    }
  }

  async function stop() {
    if (state.timer) clearInterval(state.timer);
    state.timer = null;
    if (state.sessionId) {
      const sessionId = state.sessionId;
      state.sessionId = null;
      await fetch('/api/v1/simulator/sessions/' + sessionId, { method: 'DELETE' }).catch(() => {});
      log('preview analysis stopped', { sessionId });
    }
    const canvas = document.getElementById('camera-overlay');
    if (canvas) canvas.getContext('2d').clearRect(0, 0, canvas.width, canvas.height);
  }

  document.addEventListener('DOMContentLoaded', () => {
    document.querySelector('.camera-card .primary')?.addEventListener('click', () => setTimeout(start, 250));
    document.querySelector('.camera-card button:not(.primary)')?.addEventListener('click', stop);
    document.querySelector('.live-nav')?.addEventListener('click', stop);
    document.querySelector('.clip-nav')?.addEventListener('click', stop);
    document.querySelector('.client-nav')?.addEventListener('click', stop);
  });
})();
