(function(){
window.selectedCaptureMethod='FULL_CLIP';
window.biometricPolicy=null;
window.biometricPolicySessionId=null;

const originalFetch=window.fetch.bind(window);
window.fetch=function(input,init){
    const url=typeof input==='string'?input:input.url;
    if(url.includes('/video-verification/verify')&&init&&init.body instanceof FormData){
        init.body.set('captureMethod',window.selectedCaptureMethod);
    }
    return originalFetch(input,init);
};

function ensureMultiFrameUi(){
    const sidebar=document.querySelector('.method-sidebar');
    const content=document.querySelector('.method-content');
    if(!sidebar||!content||document.querySelector('.hybrid-multi-nav'))return;
    const button=document.createElement('button');
    button.type='button';button.className='method-nav hybrid-multi-nav';
    button.innerHTML='<span class="method-icon">◇◇</span><span><strong>Hybrid multi-frame</strong><small>Server-selected sequence</small></span><i class="method-check">●</i>';
    sidebar.insertBefore(button,sidebar.querySelector('.sidebar-note'));
    const panel=document.createElement('article');
    panel.className='card hybrid-multi-panel';panel.hidden=true;
    panel.innerHTML='<div class="section-heading"><div><p class="eyebrow">METHOD 5 · HYBRID MULTI-FRAME</p><h2>Capture → Select frames → Verify</h2></div><span class="state-pill" id="hmf-status">READY</span></div>'+
        '<p class="method-description">The simulator performs lightweight capture analysis and selects multiple high-quality frames. The server independently repeats detection, quality, liveness and recognition before aggregating evidence.</p>'+
        '<div class="hybrid-stepbar"><span class="active">1 · Capture</span><span>2 · Select</span><span>3 · Server re-validation</span><span>4 · Result</span></div>'+
        '<div class="hybrid-grid"><div class="hybrid-left"><div class="panel-title"><strong>Selected frames</strong><small id="hmf-selected-label">0 images selected</small></div><div id="hmf-frames" class="multi-frame-strip"></div>'+
        '<div class="hybrid-metrics"><div><span>Target frames</span><strong id="hmf-target">4</strong></div><div><span>Samples</span><strong id="hmf-samples">0</strong></div><div><span>Selected</span><strong id="hmf-selected">0</strong></div><div><span>Best quality</span><strong id="hmf-quality">-</strong></div></div>'+
        '<div class="row"><button id="hmf-start" class="primary">Capture frames</button><button id="hmf-stop">Stop capture</button><button id="hmf-reset">Reset</button></div></div>'+
        '<div class="hybrid-right"><div class="panel-title"><strong>Capture configuration</strong><small>Server policy is authoritative</small></div><label>Reference ID<input id="hmf-reference" value="test-person-01" placeholder="user-123"></label>'+
        '<label>Number of frames<select id="hmf-count" disabled><option>1</option><option>2</option><option selected>4</option><option>5</option><option>6</option><option>7</option><option>8</option></select></label>'+
        '<div class="quality-grid"><div><span>Face detection</span><b id="hmf-face">Waiting</b></div><div><span>Quality</span><b id="hmf-current-quality">-</b></div><div><span>Liveness</span><b id="hmf-live">Waiting</b></div><div><span>Server aggregation</span><b id="hmf-aggregation">MEAN</b></div></div>'+
        '<button id="hmf-verify" class="primary wide" disabled>Send selected frames to server</button><p class="upload-note">The server receives only the selected images plus ordered sequence/timestamp metadata.</p></div></div>'+
        '<div id="hmf-result" class="hybrid-result" hidden></div><div id="hmf-events" class="hybrid-events"></div>';
    content.appendChild(panel);
}

function setPolicyText(id,value){const e=document.getElementById(id);if(e)e.textContent=value;}
function formatBytes(n){if(!n)return'0 B';if(n<1024)return n+' B';if(n<1048576)return(n/1024).toFixed(1)+' KB';return(n/1048576).toFixed(2)+' MB';}

function renderPolicy(policy){
    window.biometricPolicy=policy;
    setPolicyText('policy-profile',policy.profile||'-');
    setPolicyText('policy-method',policy.method||'-');
    setPolicyText('policy-version',(policy.policyId||'-')+' / v'+(policy.version??'-'));
    setPolicyText('policy-duration',policy.maxDurationSeconds?policy.minDurationSeconds+'–'+policy.maxDurationSeconds+' s':'Not duration-bound');
    setPolicyText('policy-frames',policy.requiredFrameCount>0?String(policy.requiredFrameCount):'Not fixed');
    setPolicyText('policy-fps',policy.uploadFps?String(policy.uploadFps):'N/A');
    setPolicyText('policy-payload',formatBytes(policy.maxPayloadBytes));
    setPolicyText('policy-liveness',(policy.livenessMode||'NONE')+(policy.livenessRequired?' · required':''));
    setPolicyText('policy-quality',policy.minQualityScore!=null?String(policy.minQualityScore):'-');
    setPolicyText('policy-model',(policy.recognitionModelId||'-')+' / '+(policy.recognitionModelVersion||'-'));
    setPolicyText('policy-threshold',policy.recognitionThreshold!=null?String(policy.recognitionThreshold):'-');
    setPolicyText('policy-fallback',policy.fallbackMethod||'None');
    const state=document.getElementById('policy-state');if(state)state.textContent='SERVER ISSUED';
    const target=document.getElementById('hmf-target');if(target&&policy.requiredFrameCount)target.textContent=policy.requiredFrameCount;
    const count=document.getElementById('hmf-count');if(count&&policy.requiredFrameCount)count.value=String(policy.requiredFrameCount);
}

async function createPolicySession(){
    const reference=(document.getElementById('hmf-reference')?.value||'test-person-01').trim();
    if(!reference)return;
    const response=await fetch('/api/v1/simulator/policy/sessions?referenceId='+encodeURIComponent(reference),{method:'POST'});
    const data=await response.json();
    if(!response.ok||!data.sessionId)throw new Error(data.message||data.code||'Policy session creation failed');
    window.biometricPolicySessionId=data.sessionId;
    setPolicyText('policy-session-id','Session '+data.sessionId);
    setPolicyText('policy-expiry',data.expiresAt?new Date(data.expiresAt).toLocaleTimeString():'-');
    return data;
}

async function runPolicyTest(kind){
    const out=document.getElementById('policy-diagnostic-result');
    if(!window.biometricPolicySessionId)await createPolicySession();
    const p=window.biometricPolicy;
    const request={sessionId:window.biometricPolicySessionId,method:p.method,frameCount:p.requiredFrameCount,payloadBytes:Math.min(p.maxPayloadBytes,1),durationSeconds:Math.max(p.minDurationSeconds,0),livenessScore:p.livenessRequired?p.livenessThreshold:1,modelId:p.recognitionModelId,modelVersion:p.recognitionModelVersion};
    if(kind==='method')request.method=p.method==='FULL_CLIP'?'LIVE_STREAM':'FULL_CLIP';
    if(kind==='frames')request.frameCount=(p.requiredFrameCount||1)+1;
    if(kind==='payload')request.payloadBytes=p.maxPayloadBytes+1;
    if(kind==='duration')request.durationSeconds=p.maxDurationSeconds+1;
    if(kind==='liveness')request.livenessScore=0;
    if(kind==='model'){request.modelId='wrong-model';request.modelVersion='wrong-version';}
    out.textContent='Testing '+kind+'...';
    try{
        const response=await fetch('/api/v1/simulator/policy/validate',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(request)});
        const data=await response.json();
        out.textContent=JSON.stringify({test:kind,httpStatus:response.status,...data},null,2);
    }catch(e){out.textContent=JSON.stringify({test:kind,error:e.message},null,2);}
}

function applyServerMethod(method){
    const panels={LIVE_STREAM:document.querySelector('.live-panel'),FULL_CLIP:document.querySelector('.clip-panel'),CLIENT_EMBEDDING:document.querySelector('.client-panel'),HYBRID_SINGLE_FRAME:document.querySelector('.hybrid-panel'),HYBRID_MULTI_FRAME:document.querySelector('.hybrid-multi-panel')};
    const buttons={LIVE_STREAM:document.querySelector('.live-nav'),FULL_CLIP:document.querySelector('.clip-nav'),CLIENT_EMBEDDING:document.querySelector('.client-nav'),HYBRID_SINGLE_FRAME:document.querySelector('.hybrid-nav'),HYBRID_MULTI_FRAME:document.querySelector('.hybrid-multi-nav')};
    window.selectedCaptureMethod=method;
    Object.keys(panels).forEach(k=>{if(panels[k]){panels[k].hidden=k!==method;panels[k].style.display=k===method?'':''}});
    Object.keys(buttons).forEach(k=>{
        const b=buttons[k];if(!b)return;
        const allowed=k===method;
        b.classList.toggle('selected',allowed);
        b.classList.toggle('policy-blocked',!allowed);
        b.setAttribute('aria-disabled',String(!allowed));
        if(!allowed)b.title='Disabled by the server-issued biometric policy';
        else b.title='Server-selected capture method';
    });
    const badge=document.querySelector('.method-badge');if(badge)badge.textContent=method+' · SERVER POLICY';
    const note=document.querySelector('.method-note');if(note)note.textContent='Server policy controls method selection. Client method override is rejected.';
    if(window.clientEmbeddingSelect)window.clientEmbeddingSelect(method);
}

async function loadPolicy(){
    ensureMultiFrameUi();
    try{
        const response=await fetch('/api/v1/simulator/policy');
        const policy=await response.json();
        if(!response.ok)throw new Error(policy.message||'Policy request failed');
        renderPolicy(policy);
        applyServerMethod(policy.method);
        await createPolicySession();
        document.querySelectorAll('[data-policy-test]').forEach(b=>{b.disabled=false;b.onclick=()=>runPolicyTest(b.dataset.policyTest);});
    }catch(e){
        const state=document.getElementById('policy-state');if(state)state.textContent='ERROR';
        setPolicyText('policy-diagnostic-result',e.message);
    }
}

function init(){
    ensureMultiFrameUi();
    const layout=document.querySelector('.app-layout'),sidebar=document.querySelector('.method-sidebar');
    if(layout&&sidebar&&!sidebar.querySelector('.sidebar-toggle')){
        const toggle=document.createElement('button');toggle.type='button';toggle.className='sidebar-toggle';toggle.setAttribute('aria-label','Collapse navigation');toggle.title='Collapse navigation';toggle.textContent='☰';sidebar.prepend(toggle);
        toggle.addEventListener('click',()=>{const collapsed=layout.classList.toggle('nav-collapsed');toggle.setAttribute('aria-label',collapsed?'Expand navigation':'Collapse navigation');toggle.title=collapsed?'Expand navigation':'Collapse navigation';});
    }
    document.querySelectorAll('.method-nav').forEach(button=>button.addEventListener('click',event=>{
        if(button.getAttribute('aria-disabled')==='true'){event.preventDefault();return;}
    }));
    document.getElementById('policy-session')?.addEventListener('click',async()=>{try{await createPolicySession();}catch(e){setPolicyText('policy-diagnostic-result',e.message);}});
    loadPolicy().then(()=>{if(window.initHybridMultiFrame)window.initHybridMultiFrame();});
}
document.addEventListener('DOMContentLoaded',init);
})();