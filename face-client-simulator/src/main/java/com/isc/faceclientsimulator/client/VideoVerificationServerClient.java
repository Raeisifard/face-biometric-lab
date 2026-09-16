package com.isc.faceclientsimulator.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class VideoVerificationServerClient {
 private static final Logger log=LoggerFactory.getLogger(VideoVerificationServerClient.class);
 private final RestClient rest;
 public VideoVerificationServerClient(RestClient rest){this.rest=rest;}
 public Object verify(String requestId,String referenceId,byte[] clip,String filename){
  log.info("Sending video verification clip: requestId={}, referenceId={}, filename={}, bytes={}",requestId,referenceId,filename,clip.length);
  long start=System.nanoTime();
  var resource=new ByteArrayResource(clip){@Override public String getFilename(){return filename;}};
  var body=new LinkedMultiValueMap<String,Object>();body.add("requestId",requestId);body.add("referenceId",referenceId);body.add("clip",resource);
  try{Object response=rest.post().uri("/api/v1/biometric/video-verification/verify-clip").contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(Object.class);log.info("Video verification response received: requestId={}, roundTripMs={}",requestId,(System.nanoTime()-start)/1_000_000);return response;}catch(RuntimeException e){log.error("Video verification request failed: requestId={}, referenceId={}, roundTripMs={}",requestId,referenceId,(System.nanoTime()-start)/1_000_000,e);throw e;}
 }
}
