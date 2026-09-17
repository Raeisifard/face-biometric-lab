(function () {
  const state = { sessionId: null, timer: null, busy: false, frames: 0 };

  function panel() {
    const target = document.querySelector('.client-panel');
    target.innerHTML = '<div class="section-heading"><div><p class="eyebrow">CLIENT EMBEDDING</p><h2>Local biometric pipeline</h2></div><span class="state-pill client-state-pill">Ready</span></div>' +
      '<p class="method-description">YuNet, quality, temporal liveness, alignment, and MobileFaceNet run in the simulator. The server receives only the final 512-D embedding.</p>' +
      '<label>Reference ID<input class="client-reference" value="user-123"></label>' +
      '<div class="row"><button class="primary client-start">Start client session</button><button class="client-stop" disabled>Stop session</button><button class="primary client-verify" disabled>Verify embedding</button></div>' +
      '<div class="live-summary"><div><span>Local state</span><strong class="client-live-state">IDLE</strong></div><div><span>Frames</span><strong class="client-frames">0</strong></div><div><span>Detected faces</span><strong class="client-faces">0</strong></div><div><span>Quality</span><strong class="client-quality">-</strong></div><div><span>Liveness</span><strong class="client-liveness">-</strong></div><div><span>Temporal motion</span><strong class="client-motion">-</strong></div><div><span>Embedding profile</span><strong>MobileFaceNet</strong></div></div>' +
      '<div class="feedback-box"><span class="feedback-icon">●</span><div><strong class="client-feedback">Waiting for session</strong><small class="client-reason">No local analysis yet</small></div></div>' +
      '<div class="clip-result client-result"><div class="section-heading"><p class="eyebrow">SERVER COMPARISON</p><span class="state-pill client-result-status">PENDING</span></div><div class="pipeline client-pipeline"><span class="pending">Local pipeline</span><span class="pending">Embedding upload</span><span class="pending">Reference lookup</span><span class="pending">Cosine match</span></div><div class="reason-box"><b>Trust boundary</b><span>CLIENT_GENERATED embedding; similarity is SERVER_VERIFIED</span></div><pre class="client-json" hidden></pre></div>';
    return target;
  }

  async function json(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) throw new Error(await response.text());
    return response.json();
  }

  function imageBlob() {
    const video = document.getElementById('preview');
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
    return new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.82));
  }

  async function analyze(ui) {
    if (!state.sessionId || state.busy) return;
    state.busy = true;
    try {
      const blob = await imageBlob();
      const form = new FormData();
      form.append('sessionId', state.sessionId);
      form.append('image', blob, 'client-frame.jpg');
      const result = await json('/api/v1/simulator/frames', { method: 'POST', body: form });
      state.frames = result.liveness.acceptedFrames;
      ui.frames.textContent = state.frames;
      ui.faces.textContent = result.detection.faceDetected() ? '1' : '0';
      ui.liveness.textContent = result.liveness.liveScore.toFixed(3);
      ui.motion.textContent = result.liveness.temporalMotion.toFixed(3);
      ui.quality.textContent = result.qualityScore.toFixed(3);
      ui.state.textContent = result.liveness.status;
      ui.pill.textContent = result.liveness.status;
      ui.feedback.textContent = result.liveness.status;
      ui.reason.textContent = result.liveness.instruction;
      ui.verify.disabled = !result.liveness.frameLooksLive;
    } catch (error) {
      ui.state.textContent = 'PROCESSING_ERROR';
      ui.feedback.textContent = 'PROCESSING_ERROR';
      ui.reason.textContent = error.message;
    } finally {
      state.busy = false;
    }
  }

  async function start(ui) {
    const result = await json('/api/v1/simulator/sessions', { method: 'POST' });
    state.sessionId = result.sessionId;
    state.frames = 0;
    ui.start.disabled = true;
    ui.stop.disabled = false;
    ui.verify.disabled = true;
    ui.state.textContent = 'COLLECTING';
    ui.feedback.textContent = 'COLLECTING';
    ui.reason.textContent = 'Move slowly left and right';
    state.timer = setInterval(() => analyze(ui), 250);
  }

  async function stop(ui) {
    if (state.timer) clearInterval(state.timer);
    state.timer = null;
    ui.stop.disabled = true;
    ui.start.disabled = false;
  }

  async function verify(ui) {
    if (!state.sessionId) return;
    const blob = await imageBlob();
    const form = new FormData();
    form.append('sessionId', state.sessionId);
    form.append('userId', ui.reference.value || 'user-123');
    form.append('image', blob, 'client-embedding.jpg');
    ui.resultStatus.textContent = 'VERIFYING';
    try {
      const result = await json('/api/v1/simulator/client-verify', { method: 'POST', body: form });
      ui.resultStatus.textContent = result.matched ? 'MATCH' : 'NO_MATCH';
      ui.resultStatus.className = 'state-pill client-result-status ' + (result.matched ? 'done' : 'failed');
      ui.pipeline.forEach(step => { step.className = result.matched ? 'done' : 'failed'; });
      ui.json.hidden = false;
      ui.json.textContent = JSON.stringify(result, null, 2);
    } catch (error) {
      ui.resultStatus.textContent = 'INCONCLUSIVE';
      ui.resultStatus.className = 'state-pill client-result-status failed';
      ui.json.hidden = false;
      ui.json.textContent = error.message;
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    const target = panel();
    const ui = {
      start: target.querySelector('.client-start'), stop: target.querySelector('.client-stop'), verify: target.querySelector('.client-verify'),
      reference: target.querySelector('.client-reference'), state: target.querySelector('.client-live-state'), pill: target.querySelector('.client-state-pill'),
      frames: target.querySelector('.client-frames'), faces: target.querySelector('.client-faces'), quality: target.querySelector('.client-quality'), liveness: target.querySelector('.client-liveness'), motion: target.querySelector('.client-motion'),
      feedback: target.querySelector('.client-feedback'), reason: target.querySelector('.client-reason'), resultStatus: target.querySelector('.client-result-status'), pipeline: target.querySelectorAll('.client-pipeline span'), json: target.querySelector('.client-json')
    };
    window.clientEmbeddingSelect = selected => {
      target.hidden = selected !== 'CLIENT_EMBEDDING';
      document.querySelector('.client-nav').classList.toggle('selected', selected === 'CLIENT_EMBEDDING');
    };
    ui.start.addEventListener('click', () => start(ui).catch(error => { ui.reason.textContent = error.message; }));
    ui.stop.addEventListener('click', () => stop(ui));
    ui.verify.addEventListener('click', () => verify(ui));
  });
})();
