package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.config.Method1Properties;
import com.isc.facebiometricservice.method1.Method1VerificationEngine;
import com.isc.facebiometricservice.method1.VideoClipDecoder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/biometric/method-1")
public class Method1Controller {
 private final Method1Properties p; private final VideoClipDecoder decoder; private final Method1VerificationEngine engine;
 public Method1Controller(Method1Properties p,VideoClipDecoder decoder,Method1VerificationEngine engine){this.p=p;this.decoder=decoder;this.engine=engine;}
 @PostMapping(value="/verify-clip",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Method1Response verify(@RequestParam(required=false) String requestId,@RequestParam String referenceId,@RequestPart("clip") MultipartFile clip){
   String id=requestId==null||requestId.isBlank()?UUID.randomUUID().toString():requestId;
   if(!p.enabled()) return Method1Response.inconclusive(id,referenceId,"METHOD_DISABLED");
   if(clip==null||clip.isEmpty()) return Method1Response.invalid(id,referenceId,"INVALID_VIDEO");
   if(clip.getSize()>p.maxClipBytes()) return Method1Response.invalid(id,referenceId,"VIDEO_TOO_LARGE");
   try { var d=decoder.decode(clip,p.sampleFps()); var o=engine.verify(d,referenceId); return new Method1Response(id,referenceId,Status.valueOf(o.result()),o.similarity(),o.livenessScore(),o.processingMs(),o.decodedFrames(),o.recognitionFrames(),o.reasons()); }
   catch(IllegalArgumentException e){return Method1Response.invalid(id,referenceId,reason(e.getMessage()));}
   catch(Exception e){return new Method1Response(id,referenceId,Status.PROCESSING_ERROR,null,null,0,0,0,List.of("MODEL_ERROR"));}
 }
 private String reason(String s){return switch(s==null?"":s){case "VIDEO_TOO_LONG","VIDEO_TOO_LARGE","NO_FACE","MULTIPLE_FACES","FACE_TOO_SMALL","LIVENESS_FAILED","INVALID_VIDEO","LOW_LIGHT","BLUR","BAD_POSE","MODEL_ERROR"->s;default->"INVALID_VIDEO";};}
 public enum Status{MATCH,NO_MATCH,INCONCLUSIVE,INVALID_REQUEST,PROCESSING_ERROR}
 public record Method1Response(String requestId,String referenceId,Status result,Double similarity,Double livenessScore,long processingTimeMs,int decodedFrames,int recognitionFrames,List<String> reasonCodes){
  static Method1Response invalid(String r,String ref,String reason){return new Method1Response(r,ref,Status.INVALID_REQUEST,null,null,0,0,0,List.of(reason));}
  static Method1Response inconclusive(String r,String ref,String reason){return new Method1Response(r,ref,Status.INCONCLUSIVE,null,null,0,0,0,List.of(reason));}
 }
}
