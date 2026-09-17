package com.isc.facebiometricservice.repository;

import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository @ConditionalOnProperty(name="biometric.repository-type",havingValue="property")
public class PropertyFaceEmbeddingRepository implements FaceEmbeddingRepository {
 private static final Logger log=LoggerFactory.getLogger(PropertyFaceEmbeddingRepository.class);private final BiometricProperties properties;public PropertyFaceEmbeddingRepository(BiometricProperties properties){this.properties=properties;}
 @Override public void save(String userId,FaceEmbedding embedding){throw new UnsupportedOperationException("Enrollment is disabled when biometric.repository-type=property");}
 @Override public Optional<FaceEmbedding> find(String userId,String modelId,String modelVersion){Optional<FaceEmbedding> reference=findByReferenceId(userId);if(reference.isEmpty())return Optional.empty();FaceEmbedding e=reference.get();if(!modelId.equals(e.modelId())||!modelVersion.equals(e.modelVersion())){log.warn("Reference lookup failed: repository=property, referenceId={}, requestedModel={}/{}, configuredModel={}/{}, reason=MODEL_MISMATCH",userId,modelId,modelVersion,e.modelId(),e.modelVersion());return Optional.empty();}log.debug("Reference lookup succeeded: repository=property, referenceId={}, dimension={}, model={}/{}, normalized={}",userId,e.dimension(),e.modelId(),e.modelVersion(),e.normalized());return Optional.of(e);}
 @Override public Optional<FaceEmbedding> findByReferenceId(String userId){if(properties.referenceEmbeddings()==null){log.warn("Reference lookup failed: repository=property, referenceId={}, reason=REFERENCE_EMBEDDINGS_NOT_CONFIGURED",userId);return Optional.empty();}BiometricProperties.ReferenceEmbedding reference=properties.referenceEmbeddings().get(userId);if(reference==null){log.warn("Reference lookup failed: repository=property, referenceId={}, reason=REFERENCE_NOT_FOUND",userId);return Optional.empty();}return Optional.of(new FaceEmbedding(reference.embedding(),reference.dimension(),reference.modelId(),reference.modelVersion(),reference.normalized()));}
}
