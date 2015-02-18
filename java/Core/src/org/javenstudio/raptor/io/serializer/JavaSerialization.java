package org.javenstudio.raptor.io.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;


/**
 * <p>
 * An experimental {@link Serialization} for Java {@link Serializable} classes.
 * </p>
 * @see JavaSerializationComparator
 */
public class JavaSerialization implements Serialization<Serializable> {
  
  static class JavaSerializationDeserializer<T extends Serializable>
    implements Deserializer<T> {

    private ObjectInputStream ois;

    public void open(InputStream in) throws IOException {
      ois = new ObjectInputStream(in) {
        @Override protected void readStreamHeader() {
          // no header
        }
      };
    }
    
    @SuppressWarnings("unchecked")
    public T deserialize(T object) throws IOException {
      try {
        // ignore passed-in object
        return (T) ois.readObject();
      } catch (ClassNotFoundException e) {
        throw new IOException(e.toString());
      }
    }

    public void close() throws IOException {
      ois.close();
    }

  }
  
  static class JavaSerializationSerializer
    implements Serializer<Serializable> {

    private ObjectOutputStream oos;

    public void open(OutputStream out) throws IOException {
      oos = new ObjectOutputStream(out) {
        @Override protected void writeStreamHeader() {
          // no header
        }
      };
    }

    public void serialize(Serializable object) throws IOException {
      oos.reset(); // clear (class) back-references
      oos.writeObject(object);
    }

    public void close() throws IOException {
      oos.close();
    }

  }

  public boolean accept(Class<?> c) {
    return Serializable.class.isAssignableFrom(c);
  }

  public Deserializer<Serializable> getDeserializer(Class<Serializable> c) {
    return new JavaSerializationDeserializer<Serializable>();
  }

  public Serializer<Serializable> getSerializer(Class<Serializable> c) {
    return new JavaSerializationSerializer();
  }

}
