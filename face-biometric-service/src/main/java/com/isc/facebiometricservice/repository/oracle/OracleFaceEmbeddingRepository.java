package com.isc.facebiometricservice.repository.oracle;

import com.isc.facebiometricservice.domain.FaceEmbedding;
import com.isc.facebiometricservice.repository.FaceEmbeddingRepository;
import com.isc.facebiometricservice.util.EmbeddingCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name="biometric.repository-type", havingValue="oracle")
public class OracleFaceEmbeddingRepository implements FaceEmbeddingRepository {
    private final JdbcTemplate jdbc; private final EmbeddingCodec codec;
    public OracleFaceEmbeddingRepository(JdbcTemplate jdbc,EmbeddingCodec codec){this.jdbc=jdbc;this.codec=codec;}
    @Override public void save(String userId,FaceEmbedding e){
        int updated=jdbc.update("UPDATE FACE_BIOMETRIC_PROFILE SET EMBEDDING_DATA=?, EMBEDDING_DIMENSION=?, NORMALIZED=?, UPDATED_AT=SYSTIMESTAMP WHERE USER_ID=? AND MODEL_ID=? AND MODEL_VERSION=?",
                codec.encode(e.values()),e.dimension(),e.normalized()?1:0,userId,e.modelId(),e.modelVersion());
        if(updated==0) jdbc.update("INSERT INTO FACE_BIOMETRIC_PROFILE (USER_ID,MODEL_ID,MODEL_VERSION,EMBEDDING_DIMENSION,EMBEDDING_DATA,NORMALIZED,STATUS,CREATED_AT,UPDATED_AT) VALUES (?,?,?,?,?,?,?,SYSTIMESTAMP,SYSTIMESTAMP)",userId,e.modelId(),e.modelVersion(),e.dimension(),codec.encode(e.values()),e.normalized()?1:0,"ACTIVE");
    }
    @Override public Optional<FaceEmbedding> find(String userId,String modelId,String modelVersion){
        var rows=jdbc.query("SELECT EMBEDDING_DATA,EMBEDDING_DIMENSION,NORMALIZED FROM FACE_BIOMETRIC_PROFILE WHERE USER_ID=? AND MODEL_ID=? AND MODEL_VERSION=? AND STATUS='ACTIVE'",(rs,n)->new FaceEmbedding(codec.decode(rs.getBytes(1)),rs.getInt(2),modelId,modelVersion,rs.getInt(3)==1),userId,modelId,modelVersion);
        return rows.stream().findFirst();
    }
}
