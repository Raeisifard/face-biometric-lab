package com.isc.facebiometricservice.domain;

public record SimilarityResult(double similarity,double threshold,boolean matched,String algorithm,long processingTimeMs){}
