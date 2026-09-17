package com.isc.faceclientsimulator.client;

import com.isc.faceclientsimulator.domain.FaceEmbedding;import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.springframework.stereotype.Component;import org.springframework.web.client.RestClient;

@Component
public class BiometricServerClient {
 private final RestClient restClient;private static final Logger log=LoggerFactory.getLogger(BiometricServerClient.class);public BiometricServerClient(RestClient restClient){this.restClient=restClient;}
 public com.isc.faceclientsimulator.api.SimulatorController.ServerStatusResponse health(){try{var response=restClient.get().uri("/actuator/health").retrieve().body(HealthResponse.class);log.info("[SIM->BIOMETRIC] Health OK status={}",response!=null?response.status():"null");return new com.isc.faceclientsimulator.api.SimulatorController.ServerStatusResponse(true,response!=null?response.status():"UNKNOWN","Biometric service reachable");}catch(Exception ex){log.warn("[SIM->BIOMETRIC] Health check failed: {}",ex.getMessage());return new com.isc.faceclientsimulator.api.SimulatorController.ServerStatusResponse(false,"DOWN",ex.getClass().getSimpleName()+": "+ex.getMessage());}}
 public ServerEnrollResponse enroll(String userId,FaceEmbedding e){log.info("[SIM->BIOMETRIC] POST /api/v1/biometric/enroll-embedding user={} model={} version={}",userId,e.modelId(),e.modelVersion());var response=restClient.post().uri("/api/v1/biometric/enroll-embedding").body(new ServerEmbeddingPayload(userId,e.values(),e.dimension(),e.modelId(),e.modelVersion(),e.normalized())).retrieve().body(ServerEnrollResponse.class);log.info("[SIM<-BIOMETRIC] enroll response user={} enrolled={}",userId,response!=null?response.enrolled():null);return response;}
 public ServerVerifyResponse verify(String userId,FaceEmbedding e){log.info("[SIM->BIOMETRIC] POST /api/v1/biometric/verify-embedding user={} model={} version={}",userId,e.modelId(),e.modelVersion());var response=restClient.post().uri("/api/v1/biometric/verify-embedding").body(new ServerEmbeddingPayload(userId,e.values(),e.dimension(),e.modelId(),e.modelVersion(),e.normalized())).retrieve().onStatus(status->status.isError(),(request,response)->{}).body(ServerVerifyResponse.class);log.info("[SIM<-BIOMETRIC] verify response user={} result={} code={} similarity={}",userId,response!=null?response.result():null,response!=null?response.code():null,response!=null?response.similarity():null);return response;}
 public ServerEnrollResponse enrollClient(String userId,FaceEmbedding e){return enroll(userId,e);}
 public ServerVerifyResponse verifyClient(String userId,FaceEmbedding e){return verify(userId,e);}
 private record HealthResponse(String status){}
}
