package com.isc.faceclientsimulator.ai;

import com.isc.faceclientsimulator.config.SimulatorProperties;
import com.isc.faceclientsimulator.domain.FaceBoundingBox;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class MiniFasNetV2LivenessModel {
    private final OnnxSession onnx;
    private final SimulatorProperties properties;

    public MiniFasNetV2LivenessModel(SimulatorProperties properties) {
        this.properties = properties;
        this.onnx = new OnnxSession(properties.livenessModelPath());
    }

    public double liveScore(Mat frame, FaceBoundingBox bbox) {
        Mat crop = crop(frame, bbox, 2.7);
        try {
            float[] input = toTensor(crop);
            float[] logits = onnx.run(input, new long[]{1,3,80,80});
            if (logits.length < 3) throw new IllegalStateException("MiniFASNetV2 output must contain at least 3 logits");
            double[] probs = softmax(logits);
            return probs[1]; // Silent-Face-Anti-Spoofing convention: class 1 is real.
        } finally { crop.release(); }
    }

    private Mat crop(Mat image, FaceBoundingBox box, double scale) {
        double safeScale = Math.min(Math.min((image.rows()-1)/Math.max(1.0, box.height()), (image.cols()-1)/Math.max(1.0, box.width())), scale);
        double nw=box.width()*safeScale, nh=box.height()*safeScale;
        double cx=box.centerX(), cy=box.centerY();
        int x1=(int)Math.max(0, Math.floor(cx-nw/2));
        int y1=(int)Math.max(0, Math.floor(cy-nh/2));
        int x2=(int)Math.min(image.cols()-1, Math.ceil(cx+nw/2));
        int y2=(int)Math.min(image.rows()-1, Math.ceil(cy+nh/2));
        Rect r = new Rect(x1,y1,Math.max(1,x2-x1+1),Math.max(1,y2-y1+1));
        Mat c = new Mat(image,r);
        Mat out = new Mat();
        Imgproc.resize(c,out,new Size(80,80));
        c.release();
        return out;
    }

    private float[] toTensor(Mat image) {
        Mat f = new Mat();
        image.convertTo(f, CvType.CV_32FC3);
        float[] hwc = new float[80*80*3];
        f.get(0,0,hwc); f.release();
        float[] chw = new float[80*80*3]; int plane=80*80;
        for (int y=0;y<80;y++) for (int x=0;x<80;x++) {
            int p=(y*80+x)*3; int q=y*80+x;
            chw[q]=hwc[p]; chw[plane+q]=hwc[p+1]; chw[2*plane+q]=hwc[p+2];
        }
        return chw;
    }

    private double[] softmax(float[] x) {
        double max=-Double.MAX_VALUE; for(float v:x) max=Math.max(max,v);
        double sum=0; double[] e=new double[x.length];
        for(int i=0;i<x.length;i++) { e[i]=Math.exp(x[i]-max); sum+=e[i]; }
        for(int i=0;i<x.length;i++) e[i]/=sum;
        return e;
    }
}
