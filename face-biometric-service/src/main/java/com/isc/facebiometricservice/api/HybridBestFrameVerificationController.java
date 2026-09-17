package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.videoverification.VideoVerificationEngine;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.springframework.http.MediaType;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;import java.time.Instant;import java.util.List;import java.util.Map;import java.util.UUID;import java.util.concurrent.ConcurrentHashMap;import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/v1/biometric/hybrid-verification")
public class HybridBestFrameVerificationController {
    private static final Logger log=LoggerFactory.getLogger(HybridBestFrameVerificationController.class);
    private static final Duration SESSION_TTL=Duration.ofMinutes(2);
    private static final long MAX_IMAGE_BYTES=8L*1024*1024;
    private final VideoVerificationEngine engine;
    private final BiometricProperties biometricProperties;
    private final FaceMatcher matcher;
    private final Map<String,Session> sessions=new ConcurrentHashMap<>();

    public HybridBestFrameVerificationController(VideoVerificationEngine engine,BiometricProperties biometricProperties,FaceMatcher matcher){
        this.engine=engine;this.biometricProperties=biometricProperties;this.matcher=matcher;
        log.info("[HYBRID][INIT] Hybrid best-frame verification controller initialized; sessionTtl={}s maxImageBytes={}",SESSION_TTL.toSeconds(),MAX_IMAGE_BYTES);
    }

    @PostMapping("/sessions")
    public SessionResponse createSession(@RequestParam String referenceId){
        if(referenceId==null||referenceId.isBlank())throw new IllegalArgumentException("referenceId is required");
        String id=UUID.randomUUID().toString();Instant exp=Instant.now().plus(SESSION_TTL);
        sessions.put(id,new Session(id,referenceId,exp,false));
        log.info("[HYBRID][SESSION_CREATED] sessionId={} referenceId={} expiresAt={}",id,referenceId,exp);
        return new SessionResponse(id,referenceId,exp);
    }

    @PostMapping(value="/verify",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public VerificationResponse verify(@RequestParam String sessionId,@RequestParam String referenceId,@RequestPart("image") MultipartFile image){
        String requestId=UUID.randomUUID().toString();
        log.info("[HYBRID][VERIFY_RECEIVED] requestId={} sessionId={} referenceId={} bytes={} contentType={}",requestId,sessionId,referenceId,image==null?0:image.getSize(),image==null?null:image.getContentType());
        Session s=sessions.get(sessionId);
        if(s==null)return invalid(requestId,referenceId,"SESSION_NOT_FOUND","The verification session was not found.");
        if(s.expiresAt().isBefore(Instant.now())){sessions.remove(sessionId);return invalid(requestId,referenceId,"SESSION_EXPIRED","The verification session has expired.");}
        if(!s.referenceId().equals(referenceId))return invalid(requestId,referenceId,"REFERENCE_ID_MISMATCH","The reference ID does not match the verification session.");
        if(image==null||image.isEmpty())return invalid(requestId,referenceId,"INVALID_IMAGE","A non-empty image is required.");
        if(image.getSize()>MAX_IMAGE_BYTES)return invalid(requestId,referenceId,"IMAGE_TOO_LARGE","The uploaded image exceeds the configured size limit.");
        String ct=image.getContentType();if(ct==null||!ct.startsWith("image/"))return invalid(requestId,referenceId,"INVALID_IMAGE_TYPE","The uploaded content is not a supported image.");
        if(!consume(sessionId))return invalid(requestId,referenceId,"SESSION_REPLAYED","The verification session has already been consumed.");

        try{
            log.info("[HYBRID][ENGINE_START] requestId={} referenceId={} singleFrame=true",requestId,referenceId);
            var o=engine.verifySingleImage(requestId,image.getBytes(),referenceId);
            VerificationStatus status=VerificationStatus.valueOf(o.result());
            String code=primaryCode(o.result(),o.reasonCodes());
            String message=message(status,code);
            log.info("[HYBRID][ENGINE_RESULT] requestId={} result={} code={} similarity={} quality={} liveness={} processingMs={} reasons={}",requestId,o.result(),code,o.similarity(),o.qualityScore(),o.livenessScore(),o.processingMs(),o.reasonCodes());
            return response(requestId,referenceId,status,code,message,o.similarity(),o.qualityScore(),o.livenessScore(),o.processingMs(),image.getSize(),o.reasonCodes());
        }catch(IllegalArgumentException e){
            String code=reason(e.getMessage());log.warn("[HYBRID][ENGINE_REJECTED] requestId={} reason={}",requestId,code);
            return invalid(requestId,referenceId,code,message(VerificationStatus.INVALID_REQUEST,code));
        }catch(Exception e){
            log.error("[HYBRID][ENGINE_ERROR] requestId={} referenceId={}",requestId,referenceId,e);
            return response(requestId,referenceId,VerificationStatus.PROCESSING_ERROR,"MODEL_ERROR","Biometric verification could not be completed.",null,null,null,0,image.getSize(),List.of("MODEL_ERROR"));
        }
    }

    private boolean consume(String id){AtomicBoolean accepted=new AtomicBoolean(false);sessions.computeIfPresent(id,(k,s)->{if(s.consumed())return s;accepted.set(true);return new Session(s.sessionId(),s.referenceId(),s.expiresAt(),true);});return accepted.get();}

    private VerificationResponse invalid(String requestId,String referenceId,String code,String message){
        log.info("[HYBRID][INVALID_REQUEST] requestId={} referenceId={} code={}",requestId,referenceId,code);
        return response(requestId,referenceId,VerificationStatus.INVALID_REQUEST,code,message,null,null,null,0,null,List.of(code));
    }

    private VerificationResponse response(String requestId,String referenceId,VerificationStatus status,String code,String message,
                                          Double similarity,Double qualityScore,Double livenessScore,long processingMs,long uploadedBytes,List<String> reasons){
        int httpStatus=status==VerificationStatus.INVALID_REQUEST?400:status==VerificationStatus.PROCESSING_ERROR?500:200;
        return new VerificationResponse(requestId,referenceId,"HYBRID_SINGLE_FRAME",status,code,message,httpStatus,similarity,biometricProperties.threshold(),
                new VerificationResponse.ModelMetadata(biometricProperties.modelId(),biometricProperties.modelVersion(),biometricProperties.dimension(),matcher.algorithm()),
                new VerificationResponse.QualityLiveness(qualityScore,null,livenessScore),
                new VerificationResponse.Metrics(processingMs,null,null,uploadedBytes),reasons==null||reasons.isEmpty()?List.of(code):reasons);
    }

    private String primaryCode(String result,List<String> reasons){
        if(reasons!=null&&!reasons.isEmpty())return reasons.get(0);
        return switch(result){case "MATCH"->"MATCH";case "NO_MATCH"->"SIMILARITY_BELOW_THRESHOLD";case "INCONCLUSIVE"->"VERIFICATION_INCONCLUSIVE";default->"PROCESSING_ERROR";};
    }

    private String message(VerificationStatus status,String code){
        return switch(code){
            case "MATCH"->"Biometric verification matched the enrolled reference.";
            case "SIMILARITY_BELOW_THRESHOLD"->"Biometric verification completed, but similarity is below the configured threshold.";
            case "REFERENCE_NOT_FOUND"->"No compatible biometric reference embedding is enrolled for this reference.";
            case "MODEL_MISMATCH"->"The biometric sample and reference use incompatible models.";
            case "LIVENESS_FAILED"->"Liveness verification did not meet the configured threshold.";
            case "LOW_QUALITY"->"The captured biometric sample does not meet the required quality threshold.";
            case "NO_FACE"->"No face was detected in the submitted capture.";
            case "MULTIPLE_FACES"->"Multiple faces were detected in the submitted capture.";
            default->status==VerificationStatus.INVALID_REQUEST?"The biometric verification request is invalid.":"Biometric verification could not be completed.";
        };
    }

    private String reason(String value){
        return switch(value==null?"":value){case "INVALID_IMAGE","SUSPICIOUS_IMAGE_DIMENSIONS","IMAGE_TOO_LARGE","INVALID_IMAGE_TYPE","NO_FACE","MULTIPLE_FACES","FACE_TOO_SMALL","LOW_QUALITY","LIVENESS_FAILED","REFERENCE_NOT_FOUND","MODEL_ERROR","SIMILARITY_BELOW_THRESHOLD"->value;default->"INVALID_IMAGE";};
    }

    public record SessionResponse(String sessionId,String referenceId,Instant expiresAt){}
    private record Session(String sessionId,String referenceId,Instant expiresAt,boolean consumed){}
}
