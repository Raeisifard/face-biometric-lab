package com.isc.facebiometricservice.videoverification;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class VideoVerificationScoreAggregator {
 public double aggregate(List<Double> scores,String strategy){if(scores==null||scores.isEmpty())return 0;if("MAX".equalsIgnoreCase(strategy))return Collections.max(scores);if("MEDIAN".equalsIgnoreCase(strategy)){var x=new ArrayList<>(scores);Collections.sort(x);return x.get(x.size()/2);}return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);}
}
