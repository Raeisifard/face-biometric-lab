package com.isc.faceclientsimulator.api;

import com.isc.faceclientsimulator.domain.FaceEmbedding;
import com.isc.faceclientsimulator.service.BiometricFrameProcessor;
import com.isc.faceclientsimulator.client.BiometricServerClient;
import com.isc.faceclientsimulator.client.HybridVerificationServerClient;
import com.isc.faceclientsimulator.client.ServerVerifyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/simulator")
public class SimulatorController {
    private static final Logger log = LoggerFactory.getLogger(SimulatorController.class);
    private final BiometricFrameProcessor processor;
    private final BiometricServerClient serverClient;
    private final HybridVerificationServerClient hybridClient;
    private final ConcurrentHashMap<String,Boolean> sessions = new ConcurrentHashMap<>();

    public SimulatorController(BiometricFrameProcessor processor,BiometricServerClient serverClient,
                                HybridVerificationServerClient hybridClient){this.processor=processor;this.serverClient=serverClient;this.hybridClient=hybridClient;}

    @GetMapping("/models")
    public ModelsResponse models() {
        log.info("[SIM] Model information requested");
        return new ModelsResponse("YuNet", "MobileFaceNet w600k_mbf (client); ArcFace w600k_r50 (server modes)", "MiniFASNetV2 + temporal", 512);
    }

    @GetMapping("/server-status")
    public ServerStatusResponse serverStatus() {
        return serverClient.health();
    }

    @PostMapping("/sessions")
    public SessionResponse createSession() {
        String id=UUID.randomUUID().toString(); sessions.put(id,true);
        log.info("[SIM] Created processing session {}", id);
        return new SessionResponse(id);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public void delete(@PathVariable String sessionId){sessions.remove(sessionId); processor.resetSession(sessionId); log.info("[SIM] Deleted processing session {}",sessionId);}

    @PostMapping(value="/frames",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object frame(@RequestParam String sessionId,@RequestPart("image") MultipartFile image) throws Exception {
        requireSession(sessionId); requireImage(image);
        var result=processor.analyze(sessionId,image.getBytes());
        log.info("[SIM_API] frame session={} faceDetected={} faceCount={} livenessStatus={} live={} quality={} frames={}",sessionId,result.detection().faceDetected(),result.detection().faceCount(),result.liveness().status(),result.liveness().frameLooksLive(),result.qualityScore(),result.liveness().acceptedFrames());
        return result;
    }

    @PostMapping(value="/embedding",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public FaceEmbeddingResponse embedding(@RequestPart("image") MultipartFile image) throws Exception {
        requireImage(image); FaceEmbedding e=processor.generateEmbedding(image.getBytes());
        log.info("[SIM] Generated embedding model={} version={} dimension={}",e.modelId(),e.modelVersion(),e.dimension());
        return new FaceEmbeddingResponse(e.modelId(),e.modelVersion(),e.dimension(),e.normalized(),e.values());
    }

    @PostMapping(value="/enroll",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ServerVerifyResponse enroll(@RequestParam String sessionId,@RequestParam String userId,@RequestPart("image") MultipartFile image) throws Exception {
        requireSession(sessionId); requireImage(image);
        log.info("[SIM] Enroll requested user={} session={}",userId,sessionId);
        var analysis=processor.analyze(sessionId,image.getBytes());
        if(!analysis.liveness().frameLooksLive()) throw new IllegalStateException("Liveness has not passed: "+analysis.liveness().status());
        FaceEmbedding embedding=processor.generateEmbedding(image.getBytes());
        log.info("[SIM] Calling biometric service enroll user={} model={} dimension={}",userId,embedding.modelId(),embedding.dimension());
        return serverClient.enroll(userId,embedding);
    }

    @PostMapping(value="/verify",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ServerVerifyResponse verify(@RequestParam String sessionId,@RequestParam String userId,@RequestPart("image") MultipartFile image) throws Exception {
        requireSession(sessionId); requireImage(image);
        log.info("[SIM] Verify requested user={} session={}",userId,sessionId);
        var analysis=processor.analyze(sessionId,image.getBytes());
        if(!analysis.liveness().frameLooksLive()) throw new IllegalStateException("Liveness has not passed: "+analysis.liveness().status());
        FaceEmbedding embedding=processor.generateEmbedding(image.getBytes());
        log.info("[SIM] Calling biometric service verify user={} model={} dimension={}",userId,embedding.modelId(),embedding.dimension());
        return serverClient.verify(userId,embedding);
    }

    @PostMapping(value="/client-embedding", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public FaceEmbeddingResponse clientEmbedding(@RequestPart("image") MultipartFile image) throws Exception {
        requireImage(image);
        FaceEmbedding embedding = processor.generateClientEmbedding(image.getBytes());
        return new FaceEmbeddingResponse(embedding.modelId(), embedding.modelVersion(), embedding.dimension(), embedding.normalized(), embedding.values());
    }

    @PostMapping(value="/client-verify", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ServerVerifyResponse clientVerify(@RequestParam String sessionId, @RequestParam String userId,
                                             @RequestPart("image") MultipartFile image) throws Exception {
        requireSession(sessionId); requireImage(image);
        var analysis = processor.analyze(sessionId, image.getBytes());
        if (!analysis.liveness().frameLooksLive()) throw new IllegalStateException("Temporal liveness has not passed: " + analysis.liveness().status());
        FaceEmbedding embedding = processor.generateClientEmbedding(image.getBytes());
        log.info("[SIM_API] client verify session={} user={} model={} version={} dimension={} normalized={}", sessionId, userId, embedding.modelId(), embedding.modelVersion(), embedding.dimension(), embedding.normalized());
        return serverClient.verifyClient(userId, embedding);
    }

    @PostMapping(value="/client-enroll", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ServerVerifyResponse clientEnroll(@RequestParam String sessionId, @RequestParam String userId,
                                              @RequestPart("image") MultipartFile image) throws Exception {
        requireSession(sessionId); requireImage(image);
        var analysis = processor.analyze(sessionId, image.getBytes());
        if (!analysis.liveness().frameLooksLive()) throw new IllegalStateException("Temporal liveness has not passed: " + analysis.liveness().status());
        FaceEmbedding embedding = processor.generateClientEmbedding(image.getBytes());
        return serverClient.enrollClient(userId, embedding);
    }

    @PostMapping("/hybrid/sessions")
    public Object hybridSession(@RequestParam String referenceId) {
        return hybridClient.createSession(referenceId);
    }

    @PostMapping(value="/hybrid/verify", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object hybridVerify(@RequestParam String sessionId, @RequestParam String referenceId,
                               @RequestPart("image") MultipartFile image) throws Exception {
        requireImage(image);
        return hybridClient.verify(sessionId, referenceId, image.getBytes());
    }

    private void requireSession(String id){if(!sessions.containsKey(id))throw new IllegalArgumentException("Unknown session: "+id);}
    private void requireImage(MultipartFile file){if(file==null||file.isEmpty())throw new IllegalArgumentException("Image is required"); if(!StringUtils.hasText(file.getContentType())||!file.getContentType().startsWith("image/"))throw new IllegalArgumentException("Only image uploads are accepted");}

    public record SessionResponse(String sessionId){}
    public record ModelsResponse(String detector,String recognition,String liveness,int embeddingDimension){}
    public record FaceEmbeddingResponse(String modelId,String modelVersion,int dimension,boolean normalized,float[] embedding){}
    public record ServerStatusResponse(boolean reachable,String status,String message){}
}
