(function () {
  window.selectedCaptureMethod = 'FULL_CLIP';
  const originalFetch = window.fetch.bind(window);
  window.fetch = function (input, init) {
    const url = typeof input === 'string' ? input : input.url;
    if (url.includes('/video-verification/verify') && init && init.body instanceof FormData) {
      init.body.set('captureMethod', window.selectedCaptureMethod);
    }
    return originalFetch(input, init);
  };

  document.addEventListener('DOMContentLoaded', async () => {
    const method = await fetch('/api/v1/simulator/live-stream/capture-method').then(response => response.json()).catch(() => null);
    if (!method || method.selectedMethod !== 'FREE_METHOD') return;

    const live = document.querySelector('.live-panel');
    const clip = document.querySelector('.clip-panel');
    const liveButton = document.querySelector('.live-nav');
    const clipButton = document.querySelector('.clip-nav');
    const select = selected => {
      window.selectedCaptureMethod = selected;
      live.hidden = selected !== 'LIVE_STREAM';
      clip.hidden = selected !== 'FULL_CLIP';
      liveButton.classList.toggle('selected', selected === 'LIVE_STREAM');
      clipButton.classList.toggle('selected', selected === 'FULL_CLIP');
      document.querySelector('.method-badge').textContent = selected === 'LIVE_STREAM' ? 'LIVE STREAM ACTIVE' : 'FULL CLIP ACTIVE';
      document.querySelector('.method-note').textContent = 'Client-selected method for this session.';
    };
    liveButton.addEventListener('click', () => select('LIVE_STREAM'));
    clipButton.addEventListener('click', () => select('FULL_CLIP'));
    select('FULL_CLIP');
  });
})();
