package org.javenstudio.raptor.io.serializer;

import java.io.IOException;
import java.io.Serializable;

import org.javenstudio.raptor.io.RawComparator;


/**
 * <p>
 * A {@link RawComparator} that uses a {@link JavaSerialization}
 * {@link Deserializer} to deserialize objects that are then compared via
 * their {@link Comparable} interfaces.
 * </p>
 * @param <T>
 * @see JavaSerialization
 */
public class JavaSerializationComparator<T extends Serializable&Comparable<T>>
  extends DeserializerComparator<T> {

  public JavaSerializationComparator() throws IOException {
    super(new JavaSerialization.JavaSerializationDeserializer<T>());
  }

  public int compare(T o1, T o2) {
    return o1.compareTo(o2);
  }

}
