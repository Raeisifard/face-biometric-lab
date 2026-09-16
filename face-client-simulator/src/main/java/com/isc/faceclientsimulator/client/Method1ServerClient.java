package com.isc.faceclientsimulator.client;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class Method1ServerClient {
 private final RestClient rest;
 public Method1ServerClient(RestClient rest){this.rest=rest;}
 public Object verify(String requestId,String referenceId,byte[] clip,String filename){
  var resource=new ByteArrayResource(clip){@Override public String getFilename(){return filename;}};
  var body=new LinkedMultiValueMap<String,Object>();body.add("requestId",requestId);body.add("referenceId",referenceId);body.add("clip",resource);
  return rest.post().uri("/api/v1/biometric/method-1/verify-clip").contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(Object.class);
 }
}
