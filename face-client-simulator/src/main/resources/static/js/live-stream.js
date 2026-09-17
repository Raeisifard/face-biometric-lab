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
      '<div class="row"><button class="primary live-start">Start live session</button><button class="live-stop" disabled>Stop session</button><button class="live-continue" hidden>Continue capture</button></div>' +
      '<div class="live-summary"><div><span>Server state</span><strong class="live-state">IDLE</strong></div><div><span>Frames</span><strong class="live-frames">0</strong></div><div><span>Latency</span><strong class="live-latency">-</strong></div><div><span>Upload</span><strong class="live-bytes">0 B</strong></div></div>' +
      '<div class="feedback-box"><span class="feedback-icon">●</span><div><strong class="live-feedback">Waiting for session</strong><small class="live-reason">No server response yet</small></div></div>' +
      '<div class="clip-result live-result"><div class="section-heading"><p class="eyebrow">SERVER PIPELINE</p><span class="state-pill live-result-status">PENDING</span></div>' +
      '<div class="pipeline live-pipeline"><span data-step="upload">Upload</span><span data-step="decode">Decode</span><span data-step="detect">Detect</span><span data-step="quality">Quality</span><span data-step="live">Liveness</span><span data-step="select">Select</span><span data-step="recognition">Recognition</span><span data-step="match">Match</span></div>' +
      '<div class="clip-result-grid"><div><span>Code</span><strong class="live-code">-</strong></div><div><span>Similarity</span><strong class="live-similarity">-</strong></div><div><span>Liveness</span><strong class="live-liveness">-</strong></div><div><span>Decoded frames</span><strong class="live-decoded">0</strong></div><div><span>Recognition frames</span><strong class="live-recognition">0</strong></div><div><span>Processing</span><strong class="live-processing">-</strong></div><div><span>Round trip</span><strong class="live-round-trip">-</strong></div></div>' +
      '<div class="reason-box"><b>Message</b><span class="live-message">-</span></div><div class="reason-box"><b>Reason codes</b><span class="live-reasons">None</span></div></div>' +
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
      if (result.livenessScore != null) ui.liveness.textContent = result.livenessScore;
      ui.detected.textContent = result.detectedFaces ?? 0; ui.decoded.textContent = result.frameCount ?? state.frames; ui.recognition.textContent = result.feedbackCode === 'GOOD_FRAME' ? result.frameCount : ui.recognition.textContent;
      ui.feedback.textContent = result.feedbackCode; ui.reason.textContent = result.message; ui.serverState.textContent = result.state; ui.pill.textContent = result.state;
      addEvent(ui, result.feedbackCode, result.message, result.feedbackCode === 'GOOD_FRAME' || result.feedbackCode === 'LIVENESS_PROGRESS' ? 'success' : 'warning');
    } catch (error) { ui.serverState.textContent = 'PROCESSING_ERROR'; ui.feedback.textContent = 'PROCESSING_ERROR'; ui.reason.textContent = error.message; addEvent(ui, 'PROCESSING_ERROR', error.message, 'error'); } finally { state.busy = false; }
  }

  async function start(ui) {
    const resuming = Boolean(state.sessionId);
    const result = resuming ? { sessionId: state.sessionId, processingState: 'CAPTURING' } : await json('/api/v1/simulator/live-stream/sessions?referenceId=' + encodeURIComponent(referenceId()), { method: 'POST' });
    state.sessionId = result.sessionId; state.frames = resuming ? state.frames : 0; state.bytes = resuming ? state.bytes : 0; ui.start.disabled = true; ui.stop.disabled = false; ui.continue.hidden = true; ui.serverState.textContent = result.processingState; ui.pill.textContent = result.processingState;
    ui.feedback.textContent = 'CAPTURE_CONTINUE'; ui.reason.textContent = 'Session ' + result.sessionId + ' started'; addEvent(ui, 'SESSION_STARTED', 'Server accepted session for ' + referenceId(), 'success');
    const video = document.getElementById('preview'); const canvas = document.createElement('canvas'); canvas.width = video.videoWidth || 640; canvas.height = video.videoHeight || 480; const fps = Math.max(1, Math.min(10, Number(ui.fps.value) || 5));
    state.timer = setInterval(() => { canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height); sendFrame(canvas, ui); }, 1000 / fps);
  }

  async function stop(ui) {
    if (state.timer) clearInterval(state.timer); state.timer = null;
    if (!state.sessionId) return;
    const sessionId = state.sessionId; state.sessionId = null;
    const started = performance.now();
    const result = await json('/api/v1/simulator/live-stream/sessions/' + sessionId + '/complete', { method: 'POST' });
    applyVerificationResult(ui, result, Math.round(performance.now() - started));
    if (result.result === 'INCONCLUSIVE' || result.code === 'RECOGNITION_PENDING' || result.code === 'TEMPORAL_EVIDENCE_PENDING') { state.sessionId = sessionId; ui.start.disabled = true; ui.continue.hidden = false; } else { ui.start.disabled = false; }
    ui.stop.disabled = true;
  }

  function applyVerificationResult(ui, result, roundTripMs) {
    const quality = result.quality || {}; const metrics = result.metrics || {};
    const pending = result.result === 'INCONCLUSIVE';
    const failed = result.result === 'NO_MATCH' || result.result === 'INVALID_REQUEST' || result.result === 'PROCESSING_ERROR' || result.code === 'LIVENESS_FAILED';
    const terminal = !pending;
    ui.serverState.textContent = result.result || 'INCONCLUSIVE'; ui.pill.textContent = result.result || 'INCONCLUSIVE';
    ui.feedback.textContent = result.code || result.result || 'INCONCLUSIVE'; ui.reason.textContent = result.message || 'Server completed processing';
    ui.code.textContent = result.code || '-'; ui.message.textContent = result.message || '-'; ui.status.textContent = result.result || 'PENDING'; ui.status.className = 'state-pill live-result-status ' + (failed ? 'failed' : pending ? 'pending' : 'done');
    ui.similarity.textContent = result.similarity == null ? '-' : result.similarity; ui.liveness.textContent = quality.livenessScore == null ? '-' : quality.livenessScore;
    ui.decoded.textContent = metrics.decodedFrames ?? 0; ui.recognition.textContent = metrics.recognitionFrames ?? 0; ui.processing.textContent = metrics.processingTimeMs ? metrics.processingTimeMs + ' ms' : '-'; ui.roundTrip.textContent = roundTripMs + ' ms';
    ui.reasons.textContent = result.reasonCodes && result.reasonCodes.length ? result.reasonCodes.join(', ') : 'None';
    const steps = pending ? ['upload', 'decode', 'detect', 'quality', 'live', 'select'] : ['upload', 'decode', 'detect', 'quality', 'live', 'select', 'recognition', 'match'];
    ui.pipeline.forEach(step => { step.className = result.reasonCodes?.length && result.reasonCodes.some(code => code.includes(step.dataset.step.toUpperCase())) ? 'failed' : terminal ? 'done' : steps.includes(step.dataset.step) ? 'done' : 'pending'; });
    ui.final.className = 'final-result ' + (failed ? 'failed' : pending ? 'pending' : 'success'); ui.finalStatus.textContent = failed ? 'Verification failed' : pending ? 'Recognition pending' : 'Verification complete'; ui.finalReason.textContent = result.message || 'No message supplied';
    addEvent(ui, result.code || result.result || 'VERIFICATION_PENDING', result.message || 'Server completed processing', failed ? 'error' : pending ? 'warning' : 'success');
  }

  document.addEventListener('DOMContentLoaded', async () => {
    const panel = livePanel(); const ui = { start: panel.querySelector('.live-start'), stop: panel.querySelector('.live-stop'), continue: panel.querySelector('.live-continue'), fps: panel.querySelector('.live-fps'), serverState: panel.querySelector('.live-state'), pill: panel.querySelector('.live-state-pill'), feedback: panel.querySelector('.live-feedback'), reason: panel.querySelector('.live-reason'), frames: panel.querySelector('.live-frames'), bytes: panel.querySelector('.live-bytes'), latency: panel.querySelector('.live-latency'), events: panel.querySelector('.live-events'), final: panel.querySelector('.final-result'), finalStatus: panel.querySelector('.final-status'), finalReason: panel.querySelector('.final-reason'), status: panel.querySelector('.live-result-status'), code: panel.querySelector('.live-code'), message: panel.querySelector('.live-message'), similarity: panel.querySelector('.live-similarity'), liveness: panel.querySelector('.live-liveness'), detected: panel.querySelector('.live-detected'), decoded: panel.querySelector('.live-decoded'), recognition: panel.querySelector('.live-recognition'), processing: panel.querySelector('.live-processing'), roundTrip: panel.querySelector('.live-round-trip'), reasons: panel.querySelector('.live-reasons'), pipeline: panel.querySelectorAll('.live-pipeline span') };
    try {
      const method = await json('/api/v1/simulator/live-stream/capture-method'); state.method = method.selectedMethod; document.querySelector('.method-badge').textContent = state.method === 'LIVE_STREAM' ? 'LIVE STREAM ACTIVE' : 'FULL CLIP ACTIVE';
      document.querySelector('.live-nav').classList.toggle('selected', state.method === 'LIVE_STREAM'); document.querySelector('.clip-nav').classList.toggle('selected', state.method === 'FULL_CLIP');
      document.querySelector('.live-method').textContent = 'Server selected ' + state.method + '. ' + method.message;
      const live = document.querySelector('.live-panel'); const clip = document.querySelector('.clip-panel'); live.hidden = state.method !== 'LIVE_STREAM'; clip.hidden = state.method !== 'FULL_CLIP';
      addEvent(ui, 'SERVER_METHOD', state.method, 'info');
    } catch (error) { document.querySelector('.method-badge').textContent = 'SERVER OFFLINE'; addEvent(ui, 'SERVER_OFFLINE', error.message, 'error'); ui.start.disabled = true; }
    ui.start.addEventListener('click', () => start(ui).catch(error => { ui.feedback.textContent = 'PROCESSING_ERROR'; ui.reason.textContent = error.message; }));
    ui.continue.addEventListener('click', () => start(ui).catch(error => { ui.feedback.textContent = 'PROCESSING_ERROR'; ui.reason.textContent = error.message; }));
    ui.stop.addEventListener('click', () => stop(ui).catch(error => { ui.feedback.textContent = 'PROCESSING_ERROR'; ui.reason.textContent = error.message; }));
  });
})();
