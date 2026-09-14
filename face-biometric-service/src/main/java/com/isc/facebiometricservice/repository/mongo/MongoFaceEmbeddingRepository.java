package com.isc.facebiometricservice.repository.mongo;

import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import com.isc.facebiometricservice.repository.FaceEmbeddingRepository;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@ConditionalOnProperty(name="biometric.repository-type", havingValue="mongo")
public class MongoFaceEmbeddingRepository implements FaceEmbeddingRepository {
    private final MongoTemplate mongo; private final String collection;
    public MongoFaceEmbeddingRepository(MongoTemplate mongo,BiometricProperties p){this.mongo=mongo;this.collection=p.mongo().collection();}
    public void save(String userId,FaceEmbedding e){
        Document d=new Document("userId",userId).append("modelId",e.modelId()).append("modelVersion",e.modelVersion()).append("dimension",e.dimension()).append("normalized",e.normalized()).append("embedding",toList(e.values())).append("status","ACTIVE");
        mongo.getCollection(collection).replaceOne(new Document("userId",userId).append("modelId",e.modelId()).append("modelVersion",e.modelVersion()),d,new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }
    public Optional<FaceEmbedding> find(String userId,String modelId,String modelVersion){
        Document d=mongo.getCollection(collection).find(new Document("userId",userId).append("modelId",modelId).append("modelVersion",modelVersion).append("status","ACTIVE")).first();
        if(d==null)return Optional.empty();
        List<?> list=d.getList("embedding",Object.class); float[] v=new float[list.size()]; for(int i=0;i<list.size();i++)v[i]=((Number)list.get(i)).floatValue();
        return Optional.of(new FaceEmbedding(v,d.getInteger("dimension"),modelId,modelVersion,Boolean.TRUE.equals(d.getBoolean("normalized"))));
    }
    private List<Float> toList(float[] v){List<Float> r=new ArrayList<>(v.length);for(float x:v)r.add(x);return r;}
}
