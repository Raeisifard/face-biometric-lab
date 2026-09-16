package com.isc.facebiometricservice.method1;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Component
public class VideoClipDecoder {
  public DecodedClip decode(MultipartFile file, double sampleFps) throws IOException {
    Path tmp=Files.createTempFile("biometric-clip-", ".video");
    try { file.transferTo(tmp); return decode(tmp, sampleFps); }
    finally { Files.deleteIfExists(tmp); }
  }
  public DecodedClip decode(Path path,double sampleFps){
    VideoCapture cap=new VideoCapture(path.toString());
    if(!cap.isOpened()) throw new IllegalArgumentException("INVALID_VIDEO");
    double fps=cap.get(5), frames=cap.get(7), duration=fps>0?frames/fps:0;
    int step=Math.max(1,(int)Math.round(Math.max(1,fps)/Math.max(.1,sampleFps)));
    List<Frame> out=new ArrayList<>(); Mat m=new Mat(); int i=0;
    try { while(cap.read(m)){ if(i++%step==0) out.add(new Frame(i-1,m.clone())); } }
    finally {m.release();cap.release();}
    if(out.isEmpty()) throw new IllegalArgumentException("INVALID_VIDEO");
    return new DecodedClip(duration,fps,(int)frames,out);
  }
  public record Frame(int index, Mat image){}
  public record DecodedClip(double durationSeconds,double fps,int totalFrames,List<Frame> frames){}
}
