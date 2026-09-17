package com.isc.facebiometricservice.repository;

import com.isc.facebiometricservice.domain.FaceEmbedding;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.stereotype.Repository;import java.util.Optional;import java.util.concurrent.ConcurrentHashMap;

@Repository @ConditionalOnProperty(name="biometric.repository-type",havingValue="memory",matchIfMissing=true)
public class MemoryFaceEmbeddingRepository implements FaceEmbeddingRepository {
 private final ConcurrentHashMap<String,FaceEmbedding> store=new ConcurrentHashMap<>();private String key(String u,String m,String v){return u+"|"+m+"|"+v;}
 public void save(String userId,FaceEmbedding embedding){store.put(key(userId,embedding.modelId(),embedding.modelVersion()),embedding);}
 public Optional<FaceEmbedding> find(String userId,String modelId,String modelVersion){return Optional.ofNullable(store.get(key(userId,modelId,modelVersion)));}
 @Override public Optional<FaceEmbedding> findByReferenceId(String userId){return store.values().stream().filter(e->userId.equals(storeKeyUserId(e))).findFirst();}
 private String storeKeyUserId(FaceEmbedding embedding){return store.entrySet().stream().filter(e->e.getValue()==embedding).map(e->e.getKey().substring(0,e.getKey().indexOf('|'))).findFirst().orElse(null);}
}
