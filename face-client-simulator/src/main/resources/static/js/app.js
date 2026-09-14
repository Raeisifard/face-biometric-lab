function simulatorApp(){
  return {
    cameras:[],deviceId:'',stream:null,running:false,sessionId:null,userId:'user-123',serverUrl:'http://localhost:8090',sampleRate:4,sourceLabel:'Camera',showHelp:null,
    timer:null,
    state:{status:'IDLE'},
    analysis:{detection:{faceDetected:false,confidence:0,box:null},liveness:{status:'NO_FACE',liveScore:0,temporalMotion:0,acceptedFrames:0,requiredFrames:12,instruction:'Start the camera.',frameLooksLive:false}},
    result:null,
    async init(){ await this.refreshCameras(); },
    statusClass(){return this.analysis.liveness.frameLooksLive?'status-live':(this.analysis.liveness.status==='SUSPECTED_SPOOF'?'status-bad':'')},
    async refreshCameras(){
      try{
        const temp=await navigator.mediaDevices.getUserMedia({video:true,audio:false}); temp.getTracks().forEach(t=>t.stop());
        const devices=await navigator.mediaDevices.enumerateDevices();
        this.cameras=devices.filter(d=>d.kind==='videoinput');
        if(!this.deviceId && this.cameras.length)this.deviceId=this.cameras[0].deviceId;
      }catch(e){this.state.status='CAMERA_PERMISSION_REQUIRED';}
    },
    async startCamera(){
      if(this.stream) this.stopCamera();
      const constraints={video:this.deviceId?{deviceId:{exact:this.deviceId},width:{ideal:1280},height:{ideal:720},frameRate:{ideal:15,max:30}}:{width:{ideal:1280},height:{ideal:720}},audio:false};
      try{
        this.stream=await navigator.mediaDevices.getUserMedia(constraints); const video=document.getElementById('preview'); video.srcObject=this.stream; await video.play();
        const s=await fetch('/api/v1/simulator/sessions',{method:'POST'}); const j=await s.json(); this.sessionId=j.sessionId; this.running=true; this.state.status='RUNNING';
        this.timer=setInterval(()=>this.sampleFrame(),1000/this.sampleRate);
      }catch(e){this.state.status='CAMERA_ERROR';alert(e.message);}
    },
    stopCamera(){if(this.timer)clearInterval(this.timer);this.timer=null;if(this.stream){this.stream.getTracks().forEach(t=>t.stop());this.stream=null}this.running=false;this.state.status='STOPPED'},
    async sampleFrame(){
      if(!this.running)return; const video=document.getElementById('preview'); if(video.readyState<2)return;
      const canvas=document.createElement('canvas');canvas.width=640;canvas.height=360;const ctx=canvas.getContext('2d');ctx.drawImage(video,0,0,640,360);
      const blob=await new Promise(r=>canvas.toBlob(r,'image/jpeg',0.72)); const fd=new FormData();fd.append('image',blob,'frame.jpg');
      try{const resp=await fetch('/api/v1/simulator/frames?sessionId='+encodeURIComponent(this.sessionId),{method:'POST',body:fd});const data=await resp.json();if(resp.ok){this.analysis=data;this.drawOverlay(data)}}catch(e){this.state.status='FRAME_ERROR'}
    },
    drawOverlay(data){
      const c=document.getElementById('overlay');const v=document.getElementById('preview');if(!v.videoWidth)return; c.width=v.videoWidth;c.height=v.videoHeight;const x=c.getContext('2d');x.clearRect(0,0,c.width,c.height);
      if(data.detection&&data.detection.box){const b=data.detection.box;const sx=c.width/640,sy=c.height/360;x.strokeStyle=data.liveness.frameLooksLive?'#22c55e':'#f59e0b';x.lineWidth=4;x.strokeRect(b.x*sx,b.y*sy,b.width*sx,b.height*sy);}
    },
    async enroll(){this.result=await this.captureAction('enroll')},
    async verify(){this.result=await this.captureAction('verify')},
    async captureAction(action){
      if(!this.sessionId){return {error:'Start camera first'}}
      if(!this.analysis.liveness.frameLooksLive){return {error:'Liveness has not passed',status:this.analysis.liveness.status}}
      const video=document.getElementById('preview');const c=document.createElement('canvas');c.width=640;c.height=360;c.getContext('2d').drawImage(video,0,0,640,360);const blob=await new Promise(r=>c.toBlob(r,'image/jpeg',0.9));const fd=new FormData();fd.append('image',blob,'capture.jpg');
      try{const resp=await fetch('/api/v1/simulator/'+action+'?sessionId='+encodeURIComponent(this.sessionId)+'&userId='+encodeURIComponent(this.userId),{method:'POST',body:fd});return await resp.json()}catch(e){return {error:e.message}}
    }
  }
}
document.addEventListener('alpine:init',()=>{});
