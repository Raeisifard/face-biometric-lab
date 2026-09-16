(function () {
  const state = { method: null, sessionId: null, timer: null, frames: 0, bytes: 0, busy: false };
  const formatBytes = value => value < 1024 ? value + ' B' : (value / 1024).toFixed(1) + ' KB';
  const referenceId = () => document.querySelector('.live-reference')?.value || 'user-123';
  async function json(url, options) { const response = await fetch(url, options); if (!response.ok) throw new Error(await response.text()); return response.json(); }
  const addEvent = (ui, title, detail, type) => { const item = document.createElement('li'); item.className = type || 'info'; item.innerHTML = '<span class="event-mark">●</span><span><b>' + title + '</b><small>' + detail + '</small></span>'; ui.events.prepend(item); };

  function livePanel() {
    const panel = document.querySelector('.live-panel');
    panel.innerHTML = '<div class="section-heading"><div><p class="eyebrow">METHOD 2</p><h2>Live stream</h2></div><span class="state-pill live-state-pill">Ready</span></div>' +
      '<p class="live-method"></p><label>Reference ID<input class="live-reference" value="user-123"></label>' +
      '<label>Upload FPS <input class="live-fps" type="number" min="1" max="10" step="1" value="5"></label>' +
      '<div class="row"><button class="primary live-start">Start live session</button><button class="live-stop" disabled>Stop session</button></div>' +
      '<div class="live-summary"><div><span>Server state</span><strong class="live-state">IDLE</strong></div><div><span>Frames</span><strong class="live-frames">0</strong></div><div><span>Latency</span><strong class="live-latency">-</strong></div><div><span>Upload</span><strong class="live-bytes">0 B</strong></div></div>' +
      '<div class="feedback-box"><span class="feedback-icon">●</span><div><strong class="live-feedback">Waiting for session</strong><small class="live-reason">No server response yet</small></div></div>' +
      '<div class="process-columns"><div><p class="eyebrow">PROCESS TIMELINE</p><ul class="live-events"></ul></div><div><p class="eyebrow">FINAL RESULT</p><div class="final-result pending"><strong class="final-status">Pending</strong><small class="final-reason">Capture has not completed</small></div></div></div>';
    return panel;
  }

  async function sendFrame(canvas, ui) {
    if (!state.sessionId || state.busy) return; state.busy = true;
    const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.78));
    if (!blob) { state.busy = false; return; }
    const form = new FormData(); form.append('frame', blob, 'frame.jpg'); const started = performance.now();
    try {
      const result = await json('/api/v1/simulator/live-stream/sessions/' + state.sessionId + '/frames', { method: 'POST', body: form });
      state.frames = result.frameCount || state.frames + 1; state.bytes += blob.size;
      ui.frames.textContent = state.frames; ui.bytes.textContent = formatBytes(state.bytes); ui.latency.textContent = Math.round(performance.now() - started) + ' ms';
      ui.feedback.textContent = result.feedbackCode; ui.reason.textContent = result.message; ui.serverState.textContent = result.state; ui.pill.textContent = result.state;
      addEvent(ui, result.feedbackCode, result.message, result.feedbackCode === 'GOOD_FRAME' || result.feedbackCode === 'LIVENESS_PROGRESS' ? 'success' : 'warning');
    } catch (error) { ui.serverState.textContent = 'PROCESSING_ERROR'; ui.feedback.textContent = 'PROCESSING_ERROR'; ui.reason.textContent = error.message; addEvent(ui, 'PROCESSING_ERROR', error.message, 'error'); } finally { state.busy = false; }
  }

  async function start(ui) {
    const result = await json('/api/v1/simulator/live-stream/sessions?referenceId=' + encodeURIComponent(referenceId()), { method: 'POST' });
    state.sessionId = result.sessionId; state.frames = 0; state.bytes = 0; ui.start.disabled = true; ui.stop.disabled = false; ui.serverState.textContent = result.processingState; ui.pill.textContent = result.processingState;
    ui.feedback.textContent = 'CAPTURE_CONTINUE'; ui.reason.textContent = 'Session ' + result.sessionId + ' started'; addEvent(ui, 'SESSION_STARTED', 'Server accepted session for ' + referenceId(), 'success');
    const video = document.getElementById('preview'); const canvas = document.createElement('canvas'); canvas.width = video.videoWidth || 640; canvas.height = video.videoHeight || 480; const fps = Math.max(1, Math.min(10, Number(ui.fps.value) || 5));
    state.timer = setInterval(() => { canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height); sendFrame(canvas, ui); }, 1000 / fps);
  }

  async function stop(ui) {
    if (state.timer) clearInterval(state.timer); state.timer = null;
    if (!state.sessionId) return;
    const sessionId = state.sessionId; state.sessionId = null;
    const result = await json('/api/v1/simulator/live-stream/sessions/' + sessionId + '/complete', { method: 'POST' });
    ui.serverState.textContent = result.processingState; ui.pill.textContent = result.processingState; ui.feedback.textContent = result.finalResult || 'VERIFICATION_COMPLETE'; ui.reason.textContent = result.finalResult || 'Server completed processing';
    const failed = (result.finalResult || '').startsWith('VERIFICATION_FAILED'); ui.final.className = 'final-result ' + (failed ? 'failed' : 'success'); ui.finalStatus.textContent = failed ? 'Verification failed' : 'Verification complete'; ui.finalReason.textContent = result.finalResult || 'No reason supplied';
    addEvent(ui, failed ? 'VERIFICATION_FAILED' : 'VERIFICATION_COMPLETE', result.finalResult || 'Server completed processing', failed ? 'error' : 'success'); ui.start.disabled = false; ui.stop.disabled = true;
  }

  document.addEventListener('DOMContentLoaded', async () => {
    const panel = livePanel(); const ui = { start: panel.querySelector('.live-start'), stop: panel.querySelector('.live-stop'), fps: panel.querySelector('.live-fps'), serverState: panel.querySelector('.live-state'), pill: panel.querySelector('.live-state-pill'), feedback: panel.querySelector('.live-feedback'), reason: panel.querySelector('.live-reason'), frames: panel.querySelector('.live-frames'), bytes: panel.querySelector('.live-bytes'), latency: panel.querySelector('.live-latency'), events: panel.querySelector('.live-events'), final: panel.querySelector('.final-result'), finalStatus: panel.querySelector('.final-status'), finalReason: panel.querySelector('.final-reason') };
    try {
      const method = await json('/api/v1/simulator/live-stream/capture-method'); state.method = method.selectedMethod; document.querySelector('.method-badge').textContent = state.method === 'LIVE_STREAM' ? 'LIVE STREAM ACTIVE' : 'FULL CLIP ACTIVE';
      document.querySelector('.live-nav').classList.toggle('selected', state.method === 'LIVE_STREAM'); document.querySelector('.clip-nav').classList.toggle('selected', state.method === 'FULL_CLIP');
      document.querySelector('.live-method').textContent = 'Server selected ' + state.method + '. ' + method.message;
      const live = document.querySelector('.live-panel'); const clip = document.querySelector('.clip-panel'); live.hidden = state.method !== 'LIVE_STREAM'; clip.hidden = state.method !== 'FULL_CLIP';
      addEvent(ui, 'SERVER_METHOD', state.method, 'info');
    } catch (error) { document.querySelector('.method-badge').textContent = 'SERVER OFFLINE'; addEvent(ui, 'SERVER_OFFLINE', error.message, 'error'); ui.start.disabled = true; }
    ui.start.addEventListener('click', () => start(ui).catch(error => { ui.feedback.textContent = 'PROCESSING_ERROR'; ui.reason.textContent = error.message; }));
    ui.stop.addEventListener('click', () => stop(ui).catch(error => { ui.feedback.textContent = 'PROCESSING_ERROR'; ui.reason.textContent = error.message; }));
  });
})();
