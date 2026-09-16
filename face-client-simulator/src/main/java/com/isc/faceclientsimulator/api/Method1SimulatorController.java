package com.isc.faceclientsimulator.api;

import com.isc.faceclientsimulator.client.Method1ServerClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulator/method-1")
public class Method1SimulatorController {
 private final Method1ServerClient client;
 public Method1SimulatorController(Method1ServerClient client){this.client=client;}
 @PostMapping(value="/verify",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Object verify(@RequestParam String referenceId,@RequestPart("clip") MultipartFile clip)throws Exception{return client.verify(UUID.randomUUID().toString(),referenceId,clip.getBytes(),clip.getOriginalFilename()==null?"clip.webm":clip.getOriginalFilename());}
}
