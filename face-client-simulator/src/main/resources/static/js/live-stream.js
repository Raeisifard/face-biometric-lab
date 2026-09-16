(function () {
  const state = { method: null, sessionId: null, timer: null, frames: 0, bytes: 0, busy: false };
  const referenceId = () => document.querySelector('.live-reference')?.value || 'user-123';
  const formatBytes = value => value < 1024 ? value + ' B' : (value / 1024).toFixed(1) + ' KB';
  async function json(url, options) { const response = await fetch(url, options); if (!response.ok) throw new Error(await response.text()); return response.json(); }

  function createPanel() {
    const section = document.createElement('section'); section.className = 'card live-panel';
    section.innerHTML = '<h2>2 · Live stream to server</h2><p class="live-method"></p>' +
      '<label>Reference ID <input class="live-reference" value="user-123"></label>' +
      '<label>Upload FPS <input class="live-fps" type="number" min="1" max="10" step="1" value="5"></label>' +
      '<div class="row"><button class="primary live-start">Start live session</button><button class="live-stop" disabled>Stop live session</button></div>' +
      '<p class="live-state">IDLE</p><p class="live-feedback">No server feedback yet</p>' +
      '<div class="metrics"><div><span>Frames</span><b class="live-frames">0</b></div><div><span>Bytes</span><b class="live-bytes">0 B</b></div><div><span>Latency</span><b class="live-latency">-</b></div></div>';
    document.querySelector('.grid').before(section); return section;
  }

  async function sendFrame(canvas, ui) {
    if (!state.sessionId || state.busy) return; state.busy = true;
    const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.78));
    if (!blob) { state.busy = false; return; }
    const form = new FormData(); form.append('frame', blob, 'frame.jpg'); const started = performance.now();
    try {
      const result = await json('/api/v1/simulator/live-stream/sessions/' + state.sessionId + '/frames', { method: 'POST', body: form });
      state.frames = result.frameCount || state.frames + 1; state.bytes += blob.size;
      ui.frames.textContent = state.frames; ui.bytes.textContent = formatBytes(state.bytes);
      ui.latency.textContent = Math.round(performance.now() - started) + ' ms';
      ui.feedback.textContent = result.feedbackCode + ': ' + result.message + ' (' + (result.livenessProgressPercent || 0) + '%)'; ui.state.textContent = result.state;
    } catch (error) { ui.state.textContent = 'PROCESSING_ERROR'; ui.feedback.textContent = error.message; } finally { state.busy = false; }
  }

  async function start(ui) {
    if (state.method !== 'LIVE_STREAM') return;
    const result = await json('/api/v1/simulator/live-stream/sessions?referenceId=' + encodeURIComponent(referenceId()), { method: 'POST' });
    state.sessionId = result.sessionId; state.frames = 0; state.bytes = 0; ui.state.textContent = result.processingState;
    ui.feedback.textContent = 'CAPTURE_CONTINUE: Session ' + state.sessionId; ui.start.disabled = true; ui.stop.disabled = false;
    const video = document.getElementById('preview'); const canvas = document.createElement('canvas'); canvas.width = video.videoWidth || 640; canvas.height = video.videoHeight || 480;
    const fps = Math.max(1, Math.min(10, Number(ui.fps.value) || 5));
    state.timer = setInterval(() => { canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height); sendFrame(canvas, ui); }, 1000 / fps);
  }

  async function stop(ui) {
    if (state.timer) clearInterval(state.timer); state.timer = null;
    if (state.sessionId) { const result = await json('/api/v1/simulator/live-stream/sessions/' + state.sessionId + '/complete', { method: 'POST' }); ui.state.textContent = result.processingState; ui.feedback.textContent = result.finalResult || 'CAPTURE_COMPLETE: live evidence submitted'; }
    state.sessionId = null; ui.start.disabled = false; ui.stop.disabled = true;
  }

  document.addEventListener('DOMContentLoaded', async () => {
    const live = createPanel(); const ui = { start: live.querySelector('.live-start'), stop: live.querySelector('.live-stop'), fps: live.querySelector('.live-fps'), state: live.querySelector('.live-state'), feedback: live.querySelector('.live-feedback'), frames: live.querySelector('.live-frames'), bytes: live.querySelector('.live-bytes'), latency: live.querySelector('.live-latency'), method: live.querySelector('.live-method') };
    try {
      const method = await json('/api/v1/simulator/live-stream/capture-method'); state.method = method.selectedMethod;
      ui.method.textContent = 'Server selected: ' + state.method + '. ' + method.message; document.querySelector('.method-badge').textContent = 'SERVER ' + state.method;
      if (state.method !== 'LIVE_STREAM') { live.hidden = true; document.querySelector('.card.method small').textContent = 'Server selected complete clip verification. Live streaming is disabled for this session.'; }
      else document.querySelector('.grid article:nth-child(2)').hidden = true;
    } catch (error) { ui.method.textContent = 'Could not read server capture method: ' + error.message; ui.start.disabled = true; }
    ui.start.addEventListener('click', () => start(ui).catch(error => { ui.state.textContent = 'PROCESSING_ERROR'; ui.feedback.textContent = error.message; }));
    ui.stop.addEventListener('click', () => stop(ui).catch(error => { ui.state.textContent = 'PROCESSING_ERROR'; ui.feedback.textContent = error.message; }));
  });
})();