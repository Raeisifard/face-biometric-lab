package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import com.isc.facebiometricservice.service.FaceMatchingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/biometric")
@Validated
public class BiometricController {
    private final FaceMatchingService service; private final BiometricProperties properties;
    public BiometricController(FaceMatchingService service,BiometricProperties properties){this.service=service;this.properties=properties;}

    @PostMapping("/enroll-embedding")
    public VerifyResponse enroll(@Valid @RequestBody EmbeddingPayload p){
        require(p); FaceEmbedding e=new FaceEmbedding(p.embedding(),p.dimension(),p.modelId(),p.modelVersion(),p.normalized()); service.enroll(p.userId(),e);
        return new VerifyResponse(p.userId(),true,1.0,properties.threshold(),"ENROLL",e.modelId(),e.modelVersion(),0);
    }

    @PostMapping("/verify-embedding")
    public VerifyResponse verify(@Valid @RequestBody EmbeddingPayload p){
        require(p); FaceEmbedding e=new FaceEmbedding(p.embedding(),p.dimension(),p.modelId(),p.modelVersion(),p.normalized()); var r=service.verify(p.userId(),e);
        return new VerifyResponse(p.userId(),r.matched(),r.similarity(),r.threshold(),r.algorithm(),e.modelId(),e.modelVersion(),r.processingTimeMs());
    }

    @GetMapping("/models")
    public ModelResponse models(){return new ModelResponse(properties.modelId(),properties.modelVersion(),properties.dimension(),properties.algorithm(),properties.threshold());}

    private void require(EmbeddingPayload p){if(p==null||p.userId()==null||p.userId().isBlank())throw new IllegalArgumentException("userId is required"); if(p.embedding()==null||p.embedding().length!=properties.dimension())throw new IllegalArgumentException("Embedding must contain "+properties.dimension()+" values");}
    public record ModelResponse(String modelId,String modelVersion,int dimension,String algorithm,double threshold){}
}
