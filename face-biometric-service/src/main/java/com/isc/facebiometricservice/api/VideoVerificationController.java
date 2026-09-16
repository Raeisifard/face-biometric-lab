package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.config.VideoVerificationProperties;
import com.isc.facebiometricservice.videoverification.VideoClipDecoder;
import com.isc.facebiometricservice.videoverification.VideoVerificationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/biometric/video-verification")
public class VideoVerificationController {
 private static final Logger log=LoggerFactory.getLogger(VideoVerificationController.class);
 private final VideoVerificationProperties p; private final VideoClipDecoder decoder; private final VideoVerificationEngine engine;
 public VideoVerificationController(VideoVerificationProperties p,VideoClipDecoder decoder,VideoVerificationEngine engine){this.p=p;this.decoder=decoder;this.engine=engine;}
 @PostMapping(value="/verify-clip",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public VideoVerificationResponse verify(@RequestParam(required=false)String requestId,@RequestParam String referenceId,@RequestPart("clip")MultipartFile clip){
   String id=requestId==null||requestId.isBlank()?UUID.randomUUID().toString():requestId;
   log.info("Video verification request: requestId={}, referenceId={}, filename={}, contentType={}, bytes={}",id,referenceId,clip==null?null:clip.getOriginalFilename(),clip==null?null:clip.getContentType(),clip==null?0:clip.getSize());
   if(!p.enabled()){log.warn("Video verification disabled: requestId={}",id);return VideoVerificationResponse.inconclusive(id,referenceId,"VIDEO_VERIFICATION_DISABLED");}
   if(clip==null||clip.isEmpty()){log.warn("Video verification rejected empty upload: requestId={}, referenceId={}",id,referenceId);return VideoVerificationResponse.invalid(id,referenceId,"INVALID_VIDEO");}
   if(clip.getSize()>p.maxClipBytes()){log.warn("Video verification rejected oversized upload: requestId={}, bytes={}, maxBytes={}",id,clip.getSize(),p.maxClipBytes());return VideoVerificationResponse.invalid(id,referenceId,"VIDEO_TOO_LARGE");}
   try{var d=decoder.decode(clip,p.sampleFps());var o=engine.verify(id,d,referenceId);return new VideoVerificationResponse(id,referenceId,Status.valueOf(o.result()),o.similarity(),o.livenessScore(),o.processingMs(),o.decodedFrames(),o.recognitionFrames(),o.reasons());}
   catch(IllegalArgumentException e){String reason=reason(e.getMessage());log.warn("Video verification rejected: requestId={}, referenceId={}, reason={}",id,referenceId,reason,e);return VideoVerificationResponse.invalid(id,referenceId,reason);}
   catch(Exception e){log.error("Video verification processing error: requestId={}, referenceId={}",id,referenceId,e);return new VideoVerificationResponse(id,referenceId,Status.PROCESSING_ERROR,null,null,0,0,0,List.of("MODEL_ERROR"));}
 }
 private String reason(String s){return switch(s==null?"":""+s){case "VIDEO_TOO_LONG","VIDEO_TOO_LARGE","NO_FACE","MULTIPLE_FACES","FACE_TOO_SMALL","LIVENESS_FAILED","INVALID_VIDEO","LOW_LIGHT","BLUR","BAD_POSE","MODEL_ERROR","REFERENCE_NOT_FOUND"->s;default->"INVALID_VIDEO";};}
 public enum Status{MATCH,NO_MATCH,INCONCLUSIVE,INVALID_REQUEST,PROCESSING_ERROR}
 public record VideoVerificationResponse(String requestId,String referenceId,Status result,Double similarity,Double livenessScore,long processingTimeMs,int decodedFrames,int recognitionFrames,List<String> reasonCodes){
  static VideoVerificationResponse invalid(String r,String ref,String reason){return new VideoVerificationResponse(r,ref,Status.INVALID_REQUEST,null,null,0,0,0,List.of(reason));}
  static VideoVerificationResponse inconclusive(String r,String ref,String reason){return new VideoVerificationResponse(r,ref,Status.INCONCLUSIVE,null,null,0,0,0,List.of(reason));}
 }
}
