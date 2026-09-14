package com.isc.facebiometricservice.api;
public record VerifyResponse(String userId,boolean matched,double similarity,double threshold,String algorithm,String modelId,String modelVersion,long processingTimeMs){}
