package com.isc.faceclientsimulator.api;

import com.isc.faceclientsimulator.domain.FaceEmbedding;
import com.isc.faceclientsimulator.service.BiometricFrameProcessor;
import com.isc.faceclientsimulator.client.BiometricServerClient;
import com.isc.faceclientsimulator.client.ServerVerifyResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/simulator")
public class SimulatorController {
    private final BiometricFrameProcessor processor;
    private final BiometricServerClient serverClient;
    private final ConcurrentHashMap<String,Boolean> sessions = new ConcurrentHashMap<>();

    public SimulatorController(BiometricFrameProcessor processor,BiometricServerClient serverClient){this.processor=processor;this.serverClient=serverClient;}

    @GetMapping("/models")
    public ModelsResponse models() {
        return new ModelsResponse("YuNet", "ArcFace/InsightFace w600k_r50", "MiniFASNetV2", 512);
    }

    @PostMapping("/sessions")
    public SessionResponse createSession() {
        String id=UUID.randomUUID().toString(); sessions.put(id,true); return new SessionResponse(id);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public void delete(@PathVariable String sessionId){sessions.remove(sessionId); processor.resetSession(sessionId);}

    @PostMapping(value="/frames",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object frame(@RequestParam String sessionId,@RequestPart("image") MultipartFile image) throws Exception {
        requireSession(sessionId); requireImage(image);
        return processor.analyze(sessionId,image.getBytes());
    }

    @PostMapping(value="/embedding",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public FaceEmbeddingResponse embedding(@RequestPart("image") MultipartFile image) throws Exception {
        requireImage(image); FaceEmbedding e=processor.generateEmbedding(image.getBytes());
        return new FaceEmbeddingResponse(e.modelId(),e.modelVersion(),e.dimension(),e.normalized(),e.values());
    }

    @PostMapping(value="/enroll",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ServerVerifyResponse enroll(@RequestParam String sessionId,@RequestParam String userId,@RequestPart("image") MultipartFile image) throws Exception {
        requireSession(sessionId); requireImage(image);
        var analysis=processor.analyze(sessionId,image.getBytes());
        if(!analysis.liveness().frameLooksLive()) throw new IllegalStateException("Liveness has not passed: "+analysis.liveness().status());
        return serverClient.enroll(userId,processor.generateEmbedding(image.getBytes()));
    }

    @PostMapping(value="/verify",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ServerVerifyResponse verify(@RequestParam String sessionId,@RequestParam String userId,@RequestPart("image") MultipartFile image) throws Exception {
        requireSession(sessionId); requireImage(image);
        var analysis=processor.analyze(sessionId,image.getBytes());
        if(!analysis.liveness().frameLooksLive()) throw new IllegalStateException("Liveness has not passed: "+analysis.liveness().status());
        return serverClient.verify(userId,processor.generateEmbedding(image.getBytes()));
    }

    private void requireSession(String id){if(!sessions.containsKey(id))throw new IllegalArgumentException("Unknown session: "+id);}
    private void requireImage(MultipartFile file){if(file==null||file.isEmpty())throw new IllegalArgumentException("Image is required"); if(!StringUtils.hasText(file.getContentType())||!file.getContentType().startsWith("image/"))throw new IllegalArgumentException("Only image uploads are accepted");}

    public record SessionResponse(String sessionId){}
    public record ModelsResponse(String detector,String recognition,String liveness,int embeddingDimension){}
    public record FaceEmbeddingResponse(String modelId,String modelVersion,int dimension,boolean normalized,float[] embedding){}
}
