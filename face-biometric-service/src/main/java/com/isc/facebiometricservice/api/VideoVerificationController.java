package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.config.VideoVerificationProperties;
import com.isc.facebiometricservice.policy.BiometricPolicyService;
import com.isc.facebiometricservice.policy.BiometricPolicyViolationException;
import com.isc.facebiometricservice.videoverification.VideoClipDecoder;
import com.isc.facebiometricservice.videoverification.VideoVerificationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/biometric/video-verification")
public class VideoVerificationController {
    private static final Logger log = LoggerFactory.getLogger(VideoVerificationController.class);
    private final VideoVerificationProperties properties; private final BiometricProperties biometricProperties; private final FaceMatcher matcher; private final VideoClipDecoder decoder; private final VideoVerificationEngine engine; private final String configuredCaptureMethod; private final BiometricPolicyService policyService;
    public VideoVerificationController(VideoVerificationProperties properties,BiometricProperties biometricProperties,FaceMatcher matcher,VideoClipDecoder decoder,VideoVerificationEngine engine,BiometricPolicyService policyService,@Value("${biometric.capture-method:LIVE_STREAM}") String captureMethod){this.properties=properties;this.biometricProperties=biometricProperties;this.matcher=matcher;this.decoder=decoder;this.engine=engine;this.policyService=policyService;this.configuredCaptureMethod=captureMethod;}
    @PostMapping(value="/verify-clip",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerificationResponse> verify(@RequestParam(required=false)String requestId,@RequestParam String referenceId,@RequestParam(required=false)String captureMethod,@RequestPart("clip")MultipartFile clip){
        String id=requestId==null||requestId.isBlank()?UUID.randomUUID().toString():requestId;String selectedMethod=captureMethod==null||captureMethod.isBlank()?configuredCaptureMethod:captureMethod.toUpperCase();
        log.info("[VIDEO][VERIFY_RECEIVED] requestId={} referenceId={} captureMethod={} bytes={}",id,referenceId,selectedMethod,clip==null?0:clip.getSize());
        try { policyService.policyForMethod(selectedMethod); } catch (BiometricPolicyViolationException ex) { return http(response(id,referenceId,selectedMethod,VerificationStatus.INVALID_REQUEST,ex.code(),ex.getMessage(),null,List.of(ex.code()),null,null)); }
        if(!"FULL_CLIP".equals(selectedMethod)||!("FULL_CLIP".equalsIgnoreCase(configuredCaptureMethod)||"FREE_METHOD".equalsIgnoreCase(configuredCaptureMethod)))return http(response(id,referenceId,selectedMethod,VerificationStatus.INVALID_REQUEST,"CAPTURE_MODE_NOT_ALLOWED","The requested capture mode is not enabled.",null,List.of("CAPTURE_MODE_NOT_ALLOWED"),null,null));
        if(!properties.enabled())return http(response(id,referenceId,selectedMethod,VerificationStatus.INCONCLUSIVE,"VIDEO_VERIFICATION_DISABLED","Video verification is currently disabled.",null,List.of("VIDEO_VERIFICATION_DISABLED"),null,null));
        if(clip==null||clip.isEmpty())return http(response(id,referenceId,selectedMethod,VerificationStatus.INVALID_REQUEST,"INVALID_VIDEO","A non-empty video clip is required.",null,List.of("INVALID_VIDEO"),null,null));
        if(clip.getSize()>properties.maxClipBytes())return http(response(id,referenceId,selectedMethod,VerificationStatus.INVALID_REQUEST,"VIDEO_TOO_LARGE","The uploaded video exceeds the configured size limit.",null,List.of("VIDEO_TOO_LARGE"),null,null));
        try { policyService.validatePayload(policyService.currentPolicy(), clip.getSize()); } catch (BiometricPolicyViolationException ex) { return http(response(id,referenceId,selectedMethod,VerificationStatus.INVALID_REQUEST,ex.code(),ex.getMessage(),null,List.of(ex.code()),null,null)); }
        try{
            var decoded=decoder.decode(clip,properties.sampleFps(),referenceId); policyService.validateDuration(policyService.currentPolicy(), decoded.durationSeconds()); var outcome=engine.verify(id,decoded,referenceId);VerificationStatus status=VerificationStatus.valueOf(outcome.result());String code=primaryCode(outcome.result(),outcome.reasons());String message=message(status,code);
            var quality=new VerificationResponse.QualityLiveness(null,null,outcome.livenessScore());var metrics=new VerificationResponse.Metrics(outcome.processingMs(),outcome.decodedFrames(),outcome.recognitionFrames(),clip.getSize());
            return http(response(id,referenceId,selectedMethod,status,code,message,outcome.similarity(),outcome.reasons(),quality,metrics));
        }catch(BiometricPolicyViolationException e){String code=e.code();return http(response(id,referenceId,selectedMethod,VerificationStatus.INVALID_REQUEST,code,e.getMessage(),null,List.of(code),null,null));
        }catch(IllegalArgumentException e){String code=reason(e.getMessage());log.warn("[VIDEO][REJECTED] requestId={} referenceId={} code={}",id,referenceId,code);return http(response(id,referenceId,selectedMethod,VerificationStatus.INVALID_REQUEST,code,message(VerificationStatus.INVALID_REQUEST,code),null,List.of(code),null,null));}
        catch(Exception e){log.error("[VIDEO][PROCESSING_ERROR] requestId={} referenceId={}",id,referenceId,e);return http(response(id,referenceId,selectedMethod,VerificationStatus.PROCESSING_ERROR,"MODEL_ERROR","Biometric verification could not be completed.",null,List.of("MODEL_ERROR"),null,null));}
    }
    private VerificationResponse response(String requestId,String referenceId,String method,VerificationStatus status,String code,String message,Double similarity,List<String> reasons,VerificationResponse.QualityLiveness quality,VerificationResponse.Metrics metrics){int httpStatus=status==VerificationStatus.INVALID_REQUEST?400:status==VerificationStatus.PROCESSING_ERROR?500:200;return new VerificationResponse(requestId,referenceId,method,status,code,message,httpStatus,similarity,biometricProperties.threshold(),new VerificationResponse.ModelMetadata(biometricProperties.modelId(),biometricProperties.modelVersion(),biometricProperties.dimension(),matcher.algorithm()),quality==null?new VerificationResponse.QualityLiveness(null,null,null):quality,metrics==null?new VerificationResponse.Metrics(0L,null,null,null):metrics,reasons==null||reasons.isEmpty()?List.of(code):reasons);}
    private ResponseEntity<VerificationResponse> http(VerificationResponse response){return ResponseEntity.status(response.httpStatus()).body(response);}
    private String primaryCode(String result,List<String> reasons){if(reasons!=null&&!reasons.isEmpty())return reasons.get(0);return switch(result){case "MATCH"->"MATCH";case "NO_MATCH"->"SIMILARITY_BELOW_THRESHOLD";case "INCONCLUSIVE"->"VERIFICATION_INCONCLUSIVE";default->"PROCESSING_ERROR";};}
    private String message(VerificationStatus status,String code){return switch(code){case "MATCH"->"Biometric verification matched the enrolled reference.";case "SIMILARITY_BELOW_THRESHOLD"->"Biometric verification completed, but similarity is below the configured threshold.";case "REFERENCE_NOT_FOUND"->"No compatible biometric reference embedding is enrolled for this reference.";case "LIVENESS_FAILED"->"Liveness verification did not meet the configured threshold.";case "LOW_QUALITY"->"The captured biometric sample does not meet the required quality threshold.";case "NO_FACE"->"No face was detected in the submitted capture.";case "MULTIPLE_FACES"->"Multiple faces were detected in the submitted capture.";default->status==VerificationStatus.INVALID_REQUEST?"The biometric verification request is invalid.":"Biometric verification could not be completed.";};}
    private String reason(String value){return switch(value==null?"":value){case "VIDEO_TOO_LONG","VIDEO_TOO_LARGE","NO_FACE","MULTIPLE_FACES","FACE_TOO_SMALL","LIVENESS_FAILED","INVALID_VIDEO","LOW_LIGHT","BLUR","BAD_POSE","MODEL_ERROR","REFERENCE_NOT_FOUND","LOW_QUALITY","SIMILARITY_BELOW_THRESHOLD"->value;default->"INVALID_VIDEO";};}
}
