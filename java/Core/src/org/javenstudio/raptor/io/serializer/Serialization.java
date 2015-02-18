package org.javenstudio.raptor.io.serializer;


/**
 * <p>
 * Encapsulates a {@link Serializer}/{@link Deserializer} pair.
 * </p>
 * @param <T>
 */
public interface Serialization<T> {
  
  /**
   * Allows clients to test whether this {@link Serialization}
   * supports the given class.
   */
  boolean accept(Class<?> c);
  
  /**
   * @return a {@link Serializer} for the given class.
   */
  Serializer<T> getSerializer(Class<T> c);

  /**
   * @return a {@link Deserializer} for the given class.
   */
  Deserializer<T> getDeserializer(Class<T> c);
}
