package com.isc.facebiometricservice.service;

import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import com.isc.facebiometricservice.repository.MemoryFaceEmbeddingRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FaceMatchingServiceTest {
  private final BiometricProperties p = new BiometricProperties("memory","arcface-512","v1",512,0.5,"COSINE",true,null,null,new BiometricProperties.Cors("*"));
  @Test void identicalVectorsMatch(){
    var repo=new MemoryFaceEmbeddingRepository(); var s=new FaceMatchingService(repo,p); float[] v=new float[512]; v[0]=1;
    s.enroll("u",new FaceEmbedding(v,512,"arcface-512","v1",true)); var r=s.verify("u",new FaceEmbedding(v,512,"arcface-512","v1",true));
    assertTrue(r.matched()); assertEquals(1.0,r.similarity(),1e-9);
  }
  @Test void differentModelIsRejected(){
    assertThrows(IllegalArgumentException.class,()->new FaceMatchingService(new MemoryFaceEmbeddingRepository(),p).verify("u",new FaceEmbedding(new float[512],512,"other","v1",true)));
  }
}
