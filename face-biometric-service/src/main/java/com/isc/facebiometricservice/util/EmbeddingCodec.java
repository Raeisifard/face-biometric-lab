package com.isc.facebiometricservice.util;

import org.springframework.stereotype.Component;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Component
public class EmbeddingCodec {
    public byte[] encode(float[] values){ByteBuffer b=ByteBuffer.allocate(values.length*4).order(ByteOrder.BIG_ENDIAN);for(float v:values)b.putFloat(v);return b.array();}
    public float[] decode(byte[] data){if(data==null||data.length%4!=0)throw new IllegalArgumentException("Invalid float32 embedding BLOB");ByteBuffer b=ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);float[] r=new float[data.length/4];for(int i=0;i<r.length;i++)r[i]=b.getFloat();return r;}
}
