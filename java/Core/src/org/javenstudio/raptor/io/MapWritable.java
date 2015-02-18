package org.javenstudio.raptor.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.javenstudio.raptor.classification.InterfaceAudience;
import org.javenstudio.raptor.classification.InterfaceStability;
import org.javenstudio.raptor.util.ReflectionUtils;

/**
 * A Writable Map.
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class MapWritable extends AbstractMapWritable
  implements Map<Writable, Writable> {

  private Map<Writable, Writable> instance;
  
  /** Default constructor. */
  public MapWritable() {
    super();
    this.instance = new HashMap<Writable, Writable>();
  }
  
  /**
   * Copy constructor.
   * 
   * @param other the map to copy from
   */
  public MapWritable(MapWritable other) {
    this();
    copy(other);
  }
  
  /** {@inheritDoc} */
  public void clear() {
    instance.clear();
  }

  /** {@inheritDoc} */
  public boolean containsKey(Object key) {
    return instance.containsKey(key);
  }

  /** {@inheritDoc} */
  public boolean containsValue(Object value) {
    return instance.containsValue(value);
  }

  /** {@inheritDoc} */
  public Set<Map.Entry<Writable, Writable>> entrySet() {
    return instance.entrySet();
  }

  /** {@inheritDoc} */
  public Writable get(Object key) {
    return instance.get(key);
  }
  
  /** {@inheritDoc} */
  public boolean isEmpty() {
    return instance.isEmpty();
  }

  /** {@inheritDoc} */
  public Set<Writable> keySet() {
    return instance.keySet();
  }

  /** {@inheritDoc} */
  public Writable put(Writable key, Writable value) {
    addToMap(key.getClass());
    addToMap(value.getClass());
    return instance.put(key, value);
  }

  /** {@inheritDoc} */
  public void putAll(Map<? extends Writable, ? extends Writable> t) {
    for (Map.Entry<? extends Writable, ? extends Writable> e: t.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  /** {@inheritDoc} */
  public Writable remove(Object key) {
    return instance.remove(key);
  }

  /** {@inheritDoc} */
  public int size() {
    return instance.size();
  }

  /** {@inheritDoc} */
  public Collection<Writable> values() {
    return instance.values();
  }
  
  // Writable
  
  /** {@inheritDoc} */
  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
    
    // Write out the number of entries in the map
    
    out.writeInt(instance.size());

    // Then write out each key/value pair
    
    for (Map.Entry<Writable, Writable> e: instance.entrySet()) {
      out.writeByte(getId(e.getKey().getClass()));
      e.getKey().write(out);
      out.writeByte(getId(e.getValue().getClass()));
      e.getValue().write(out);
    }
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    
    // First clear the map.  Otherwise we will just accumulate
    // entries every time this method is called.
    this.instance.clear();
    
    // Read the number of entries in the map
    
    int entries = in.readInt();
    
    // Then read each key/value pair
    
    for (int i = 0; i < entries; i++) {
      Writable key = (Writable) ReflectionUtils.newInstance(getClass(
          in.readByte()), getConf());
      
      key.readFields(in);
      
      Writable value = (Writable) ReflectionUtils.newInstance(getClass(
          in.readByte()), getConf());
      
      value.readFields(in);
      instance.put(key, value);
    }
  }
}

