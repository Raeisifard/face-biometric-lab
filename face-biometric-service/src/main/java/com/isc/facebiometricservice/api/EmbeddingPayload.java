package com.isc.facebiometricservice.api;
public record EmbeddingPayload(String userId,float[] embedding,int dimension,String modelId,String modelVersion,boolean normalized){}
