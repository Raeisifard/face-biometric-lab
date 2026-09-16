package com.isc.facebiometricservice.videoverification;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Component
public class VideoClipDecoder {
  private static final Logger log=LoggerFactory.getLogger(VideoClipDecoder.class);
  public DecodedClip decode(MultipartFile file,double sampleFps)throws IOException{log.info("Video upload received: filename={}, contentType={}, bytes={}, sampleFps={}",file.getOriginalFilename(),file.getContentType(),file.getSize(),sampleFps);Path tmp=Files.createTempFile("biometric-clip-",".video");try{file.transferTo(tmp);return decode(tmp,sampleFps);}finally{Files.deleteIfExists(tmp);}}
  public DecodedClip decode(Path path,double sampleFps){log.info("Video decode started: path={}, sampleFps={}",path,sampleFps);VideoCapture cap=new VideoCapture(path.toString());if(!cap.isOpened()){log.warn("Video decode failed: cannot open media, path={}",path);throw new IllegalArgumentException("INVALID_VIDEO");}double fps=cap.get(5),frames=cap.get(7),duration=fps>0?frames/fps:0;log.info("Video metadata: path={}, fps={}, reportedFrameCount={}, calculatedDurationSeconds={}",path,fps,frames,duration);int step=Math.max(1,(int)Math.round(Math.max(1,fps)/Math.max(.1,sampleFps)));List<Frame> out=new ArrayList<>();Mat m=new Mat();int i=0;try{while(cap.read(m)){if(i++%step==0)out.add(new Frame(i-1,m.clone()));}}finally{m.release();cap.release();}log.info("Video decode completed: path={}, decodedFrames={}, reportedFrameCount={}, fps={}, calculatedDurationSeconds={}, samplingStep={}",path,out.size(),frames,fps,duration,step);if(out.isEmpty())throw new IllegalArgumentException("INVALID_VIDEO");return new DecodedClip(duration,fps,(int)frames,out);}
  public record Frame(int index,Mat image){}
  public record DecodedClip(double durationSeconds,double fps,int totalFrames,List<Frame> frames){}
}
