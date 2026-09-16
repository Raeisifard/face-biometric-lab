package com.isc.faceclientsimulator.api;

import com.isc.faceclientsimulator.client.VideoVerificationServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulator/video-verification")
public class VideoVerificationSimulatorController {
 private static final Logger log=LoggerFactory.getLogger(VideoVerificationSimulatorController.class);
 private final VideoVerificationServerClient client;
 public VideoVerificationSimulatorController(VideoVerificationServerClient client){this.client=client;}
 @PostMapping(value="/verify",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Object verify(@RequestParam String referenceId,@RequestPart("clip") MultipartFile clip)throws Exception{String requestId=UUID.randomUUID().toString();log.info("Simulator video verification request: requestId={}, referenceId={}, filename={}, contentType={}, bytes={}",requestId,referenceId,clip.getOriginalFilename(),clip.getContentType(),clip.getSize());return client.verify(requestId,referenceId,clip.getBytes(),clip.getOriginalFilename()==null?"clip.webm":clip.getOriginalFilename());}
}
