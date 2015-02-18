package org.javenstudio.raptor.io.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.Configured;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.util.ReflectionUtils;


/**
 * A {@link Serialization} for {@link Writable}s that delegates to
 * {@link Writable#write(java.io.DataOutput)} and
 * {@link Writable#readFields(java.io.DataInput)}.
 */
public class WritableSerialization extends Configured 
  implements Serialization<Writable> {
  
  static class WritableDeserializer extends Configured 
    implements Deserializer<Writable> {

    private Class<?> writableClass;
    private DataInputStream dataIn;
    
    public WritableDeserializer(Configuration conf, Class<?> c) {
      setConf(conf);
      this.writableClass = c;
    }
    
    public void open(InputStream in) {
      if (in instanceof DataInputStream) {
        dataIn = (DataInputStream) in;
      } else {
        dataIn = new DataInputStream(in);
      }
    }
    
    public Writable deserialize(Writable w) throws IOException {
      Writable writable;
      if (w == null) {
        writable 
          = (Writable) ReflectionUtils.newInstance(writableClass, getConf());
      } else {
        writable = w;
      }
      writable.readFields(dataIn);
      return writable;
    }

    public void close() throws IOException {
      dataIn.close();
    }
    
  }
  
  static class WritableSerializer implements Serializer<Writable> {

    private DataOutputStream dataOut;
    
    public void open(OutputStream out) {
      if (out instanceof DataOutputStream) {
        dataOut = (DataOutputStream) out;
      } else {
        dataOut = new DataOutputStream(out);
      }
    }

    public void serialize(Writable w) throws IOException {
      w.write(dataOut);
    }

    public void close() throws IOException {
      dataOut.close();
    }

  }

  public boolean accept(Class<?> c) {
    return Writable.class.isAssignableFrom(c);
  }

  public Deserializer<Writable> getDeserializer(Class<Writable> c) {
    return new WritableDeserializer(getConf(), c);
  }

  public Serializer<Writable> getSerializer(Class<Writable> c) {
    return new WritableSerializer();
  }

}
