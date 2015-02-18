package org.javenstudio.raptor.io.serializer;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.Configured;
import org.javenstudio.raptor.util.ReflectionUtils;
import org.javenstudio.raptor.util.StringUtils;


/**
 * <p>
 * A factory for {@link Serialization}s.
 * </p>
 */
public class SerializationFactory extends Configured {
  private static final Logger LOG = Logger.getLogger(SerializationFactory.class);

  private static final String[] DEFAULT_SERIALIZATIONS = new String[] { 
	  	WritableSerialization.class.getName(), 
	  	//JavaSerialization.class.getName()
	  };
  
  private List<Serialization<?>> serializations = new ArrayList<Serialization<?>>();
  
  /**
   * <p>
   * Serializations are found by reading the <code>io.serializations</code>
   * property from <code>conf</code>, which is a comma-delimited list of
   * classnames. 
   * </p>
   */
  public SerializationFactory(Configuration conf) {
    super(conf);
    String[] serializations = conf.getStrings("io.serializations", DEFAULT_SERIALIZATIONS);
    for (String serializerName : serializations) {
      add(conf, serializerName);
    }
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void add(Configuration conf, String serializationName) {
    try {
      Class<? extends Serialization> serializionClass =
        (Class<? extends Serialization>) conf.getClassByName(serializationName);
      serializations.add((Serialization)
          ReflectionUtils.newInstance(serializionClass, getConf()));
    } catch (ClassNotFoundException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("Serilization class not found: " + StringUtils.stringifyException(e));
    }
  }

  public <T> Serializer<T> getSerializer(Class<T> c) {
    return getSerialization(c).getSerializer(c);
  }

  public <T> Deserializer<T> getDeserializer(Class<T> c) {
    return getSerialization(c).getDeserializer(c);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <T> Serialization<T> getSerialization(Class<T> c) {
    for (Serialization serialization : serializations) {
      if (serialization.accept(c)) {
        return (Serialization<T>) serialization;
      }
    }
    return null;
  }
}
