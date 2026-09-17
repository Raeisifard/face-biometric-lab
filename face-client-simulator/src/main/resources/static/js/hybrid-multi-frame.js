window.initHybridMultiFrame = function () {
    const $ = id => document.getElementById(id);
    if (!$('hmf-start') || window.__hmfInitialized) return;
    window.__hmfInitialized = true;

    let running = false;
    let sessionId = null;
    let frames = [];
    let timer = null;
    let samples = 0;
    let busy = false;
    let modalIndex = -1;

    const log = (code, message) => {
        const box = $('hmf-events');
        if (!box) return;
        const row = document.createElement('div');
        row.className = 'hybrid-event info';
        row.innerHTML = '<span><b></b><small></small></span><time></time>';
        row.querySelector('b').textContent = code;
        row.querySelector('small').textContent = message;
        row.querySelector('time').textContent = new Date().toLocaleTimeString();
        box.prepend(row);
        while (box.children.length > 12) box.lastChild.remove();
        console.info('[HYBRID-MULTI][' + code + ']', message);
    };

    const setStatus = status => $('hmf-status').textContent = status;

    const ensureModal = () => {
        if ($('hmf-modal')) return;
        const modal = document.createElement('div');
        modal.id = 'hmf-modal';
        modal.className = 'hmf-modal';
        modal.hidden = true;
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');
        modal.setAttribute('aria-label', 'Selected frame viewer');
        modal.innerHTML = `
            <div class="hmf-modal-dialog">
                <button type="button" class="hmf-modal-close" id="hmf-modal-close" aria-label="Close image viewer" title="Close">×</button>
                <div class="hmf-modal-image-wrap">
                    <button type="button" class="hmf-modal-prev" id="hmf-modal-prev" aria-label="Previous frame" title="Previous frame">‹</button>
                    <img class="hmf-modal-image" id="hmf-modal-image" alt="Selected face frame">
                    <button type="button" class="hmf-modal-next" id="hmf-modal-next" aria-label="Next frame" title="Next frame">›</button>
                </div>
                <div class="hmf-modal-caption">
                    <span id="hmf-modal-info"></span>
                    <span class="hmf-modal-counter" id="hmf-modal-counter"></span>
                </div>
                <div class="hmf-modal-help">Use Previous / Next, or ← / → keys. Press Esc to close.</div>
            </div>`;
        document.body.appendChild(modal);

        $('hmf-modal-close').addEventListener('click', closeModal);
        $('hmf-modal-prev').addEventListener('click', () => moveModal(-1));
        $('hmf-modal-next').addEventListener('click', () => moveModal(1));
        modal.addEventListener('click', event => {
            if (event.target === modal) closeModal();
        });
    };

    const updateModal = () => {
        if (modalIndex < 0 || modalIndex >= frames.length) return;
        const frame = frames[modalIndex];
        $('hmf-modal-image').src = frame.url;
        $('hmf-modal-image').alt = 'Selected frame ' + (modalIndex + 1);
        $('hmf-modal-info').textContent = 'Frame #' + (modalIndex + 1) + ' · quality ' + Number(frame.quality || 0).toFixed(3);
        $('hmf-modal-counter').textContent = (modalIndex + 1) + ' / ' + frames.length;
        $('hmf-modal-prev').disabled = frames.length < 2;
        $('hmf-modal-next').disabled = frames.length < 2;
    };

    const openModal = index => {
        if (!frames.length || index < 0 || index >= frames.length) return;
        ensureModal();
        modalIndex = index;
        updateModal();
        $('hmf-modal').hidden = false;
        document.body.style.overflow = 'hidden';
        $('hmf-modal-close').focus();
    };

    const closeModal = () => {
        const modal = $('hmf-modal');
        if (!modal) return;
        modal.hidden = true;
        document.body.style.overflow = '';
        modalIndex = -1;
    };

    const moveModal = delta => {
        if (!frames.length) return;
        modalIndex = (modalIndex + delta + frames.length) % frames.length;
        updateModal();
    };

    const clearFrames = () => {
        closeModal();
        frames.forEach(frame => URL.revokeObjectURL(frame.url));
        frames = [];
        $('hmf-frames').innerHTML = '';
        $('hmf-selected').textContent = '0';
        $('hmf-selected-label').textContent = '0 images selected';
        $('hmf-quality').textContent = '-';
        $('hmf-verify').disabled = true;
    };

    const render = () => {
        const strip = $('hmf-frames');
        strip.innerHTML = '';

        frames.forEach((frame, index) => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'multi-frame-item';
            button.title = 'Open frame ' + (index + 1);
            button.setAttribute('aria-label', 'Open selected frame ' + (index + 1));

            const image = document.createElement('img');
            image.className = 'multi-frame-thumb';
            image.src = frame.url;
            image.alt = 'Selected frame ' + (index + 1);
            image.loading = 'eager';
            image.decoding = 'async';

            const label = document.createElement('span');
            label.className = 'multi-frame-label';
            label.textContent = '#' + (index + 1) + ' · q=' + Number(frame.quality || 0).toFixed(3);

            button.append(image, label);
            button.addEventListener('click', () => openModal(index));
            strip.appendChild(button);
        });

        $('hmf-selected').textContent = frames.length;
        $('hmf-selected-label').textContent = frames.length + ' image' + (frames.length === 1 ? '' : 's') + ' selected';
        if (frames.length) {
            $('hmf-quality').textContent = Math.max(...frames.map(frame => Number(frame.quality) || 0)).toFixed(3);
        }
        $('hmf-verify').disabled = !sessionId || frames.length !== Number($('hmf-target').textContent);
    };

    const createSession = async () => {
        const reference = $('hmf-reference').value.trim();
        const count = Number($('hmf-count').value);
        const response = await fetch('/api/v1/simulator/hybrid-multi-frame/sessions?referenceId=' + encodeURIComponent(reference) + '&frames=' + count, {method: 'POST'});
        const data = await response.json();
        if (!response.ok || !data.sessionId) throw new Error(data.message || 'Session creation failed');
        sessionId = data.sessionId;
        $('hmf-target').textContent = data.expectedFrames || count;
        $('hmf-aggregation').textContent = data.aggregation || 'MEAN';
        return data;
    };

    const sample = async () => {
        if (!running || busy || frames.length >= Number($('hmf-target').textContent)) return;
        const video = $('preview');
        if (!video || !video.videoWidth) return;

        busy = true;
        samples++;
        $('hmf-samples').textContent = samples;

        try {
            const canvas = document.createElement('canvas');
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            canvas.getContext('2d').drawImage(video, 0, 0);
            const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.84));
            if (!blob) throw new Error('Frame encoding failed');

            const sequence = samples - 1;
            const timestamp = Date.now();
            const form = new FormData();
            form.append('sessionId', sessionId);
            form.append('sequence', String(sequence));
            form.append('image', blob, 'hybrid-sample.jpg');

            const response = await fetch('/api/v1/simulator/hybrid-multi-frame/analyze', {method: 'POST', body: form});
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || 'Frame analysis failed');

            const quality = Number(data.qualityScore || 0);
            const detection = data.detection || {};
            const liveness = data.liveness || {};
            $('hmf-face').textContent = detection.faceDetected
                ? (detection.faceCount > 1 ? 'Multiple faces' : 'Detected')
                : 'No face';
            $('hmf-current-quality').textContent = quality.toFixed(3);
            $('hmf-live').textContent = liveness.status || 'Checking';

            if (detection.faceDetected && detection.faceCount === 1 && quality >= 0.45 && liveness.frameLooksLive !== false) {
                frames.push({blob, url: URL.createObjectURL(blob), quality, timestamp, sequence});
                render();
                log('FRAME_SELECTED', 'Selected frame ' + frames.length + ' · quality=' + quality.toFixed(3));
            } else {
                log('FRAME_REJECTED', 'Sample ' + samples + ' did not meet client-side selection criteria');
            }

            if (frames.length >= Number($('hmf-target').textContent)) {
                running = false;
                clearInterval(timer);
                timer = null;
                setStatus('FRAME_SET_READY');
                log('CAPTURE_COMPLETE', 'Selected ' + frames.length + ' frames; ready for server verification');
            }
        } catch (error) {
            log('FRAME_ANALYSIS_ERROR', error.message);
        } finally {
            busy = false;
        }
    };

    const start = async () => {
        if (running) return;
        const video = $('preview');
        if (!video || !video.srcObject) {
            setStatus('CAMERA_REQUIRED');
            log('CAMERA_REQUIRED', 'Start the camera first');
            return;
        }
        if (!$('hmf-reference').value.trim()) {
            setStatus('REFERENCE_REQUIRED');
            log('REFERENCE_REQUIRED', 'Reference ID is required');
            return;
        }

        try {
            clearFrames();
            samples = 0;
            $('hmf-samples').textContent = '0';
            await createSession();
            running = true;
            setStatus('CAPTURING');
            log('SESSION_CREATED', 'Server session ' + sessionId);
            timer = setInterval(sample, 450);
        } catch (error) {
            setStatus('ERROR');
            log('SESSION_CREATE_FAILED', error.message);
        }
    };

    const stop = () => {
        running = false;
        if (timer) clearInterval(timer);
        timer = null;
        setStatus(frames.length ? 'FRAME_SET_READY' : 'STOPPED');
        log('CAPTURE_STOPPED', 'Selected ' + frames.length + ' frames');
    };

    const verify = async () => {
        if (frames.length !== Number($('hmf-target').textContent) || !sessionId) return;
        running = false;
        if (timer) clearInterval(timer);
        timer = null;
        setStatus('UPLOADING');
        $('hmf-verify').disabled = true;

        const form = new FormData();
        const orderedFrames = [...frames].sort((a, b) => a.sequence - b.sequence);
        orderedFrames.forEach(frame => {
            form.append('images', frame.blob, 'hybrid-frame-' + frame.sequence + '.jpg');
            form.append('sequenceNumbers', String(frame.sequence));
            form.append('captureTimestamps', String(frame.timestamp));
        });

        const startTime = performance.now();
        try {
            const response = await fetch('/api/v1/simulator/hybrid-multi-frame/verify?sessionId=' + encodeURIComponent(sessionId) + '&referenceId=' + encodeURIComponent($('hmf-reference').value.trim()), {method: 'POST', body: form});
            const data = await response.json();
            data.clientRoundTripMs = Math.round(performance.now() - startTime);

            const box = $('hmf-result');
            box.hidden = false;
            box.innerHTML = '<div class="section-heading"><div><p class="eyebrow">SERVER VERIFICATION</p><h3></h3></div><span class="state-pill"></span></div>' +
                '<div class="clip-result-grid"><div><span>Code</span><strong data-f="code"></strong></div><div><span>Similarity</span><strong data-f="similarity"></strong></div><div><span>Liveness</span><strong data-f="liveness"></strong></div><div><span>Quality</span><strong data-f="quality"></strong></div><div><span>Submitted frames</span><strong data-f="submitted"></strong></div><div><span>Recognition frames</span><strong data-f="recognition"></strong></div><div><span>Server processing</span><strong data-f="processing"></strong></div><div><span>Round trip</span><strong data-f="roundtrip"></strong></div></div>' +
                '<div class="reason-box"><b>Message</b><span data-f="message"></span></div><div class="reason-box"><b>Reason codes</b><span data-f="reasons"></span></div>' +
                '<details class="diagnostic-details"><summary>Raw server response</summary><pre data-f="raw"></pre></details>';

            box.querySelector('h3').textContent = data.result || 'INCONCLUSIVE';
            box.querySelector('.state-pill').textContent = data.result || 'INCONCLUSIVE';
            const quality = data.quality || {};
            const metrics = data.metrics || {};
            box.querySelector('[data-f=code]').textContent = data.code || '-';
            box.querySelector('[data-f=similarity]').textContent = data.similarity ?? '-';
            box.querySelector('[data-f=liveness]').textContent = quality.livenessScore ?? '-';
            box.querySelector('[data-f=quality]').textContent = quality.qualityScore ?? '-';
            box.querySelector('[data-f=submitted]').textContent = metrics.decodedFrames ?? frames.length;
            box.querySelector('[data-f=recognition]').textContent = metrics.recognitionFrames ?? '-';
            box.querySelector('[data-f=processing]').textContent = metrics.processingTimeMs != null ? metrics.processingTimeMs + ' ms' : '-';
            box.querySelector('[data-f=roundtrip]').textContent = data.clientRoundTripMs + ' ms';
            box.querySelector('[data-f=message]').textContent = data.message || '-';
            box.querySelector('[data-f=reasons]').textContent = (data.reasonCodes || []).join(', ') || 'None';
            box.querySelector('[data-f=raw]').textContent = JSON.stringify(data, null, 2);

            setStatus(data.result || 'INCONCLUSIVE');
            log('SERVER_RESULT', 'result=' + (data.result || '-') + ' · similarity=' + (data.similarity ?? '-'));
            sessionId = null;
        } catch (error) {
            $('hmf-verify').disabled = false;
            setStatus('SERVER_ERROR');
            log('SERVER_REQUEST_FAILED', error.message);
        }
    };

    ensureModal();
    $('hmf-start').onclick = start;
    $('hmf-stop').onclick = stop;
    $('hmf-reset').onclick = () => {
        stop();
        sessionId = null;
        clearFrames();
        samples = 0;
        $('hmf-samples').textContent = '0';
        $('hmf-result').hidden = true;
        $('hmf-status').textContent = 'READY';
        $('hmf-face').textContent = 'Waiting';
        $('hmf-live').textContent = 'Waiting';
        $('hmf-current-quality').textContent = '-';
        log('RESET', 'Hybrid multi-frame capture reset');
    };
    $('hmf-verify').onclick = verify;
    $('hmf-count').onchange = () => {
        if (!running) $('hmf-target').textContent = $('hmf-count').value;
    };

    document.addEventListener('keydown', event => {
        if (!$('hmf-modal') || $('hmf-modal').hidden) return;
        if (event.key === 'Escape') closeModal();
        else if (event.key === 'ArrowLeft') moveModal(-1);
        else if (event.key === 'ArrowRight') moveModal(1);
    });
};
