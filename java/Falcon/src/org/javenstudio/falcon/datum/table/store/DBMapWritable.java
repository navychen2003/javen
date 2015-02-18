package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import org.javenstudio.raptor.conf.Configurable;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.util.ReflectionUtils;

/**
 * A Writable Map.
 * Like {@link org.javenstudio.raptor.io.MapWritable} but dumb. It will fail
 * if passed a value type that it has not already been told about. Its  been
 * primed with bigdb Writables and byte[].  Keys are always byte arrays.
 *
 * @param <K> <byte[]> key  TODO: Parameter K is never used, could be removed.
 * @param <V> value Expects a Writable or byte[].
 */
public class DBMapWritable<K,V>
    implements SortedMap<byte[],V>, Configurable, Writable, CodeToClassAndBack {
  
  private AtomicReference<Configuration> mConf = null;
  private SortedMap<byte[], V> mInstance = null;

  /**
   * The default contructor where a TreeMap is used
   **/
   public DBMapWritable(){
     this(new TreeMap<byte[], V>(Bytes.BYTES_COMPARATOR));
   }

  /**
   * Contructor where another SortedMap can be used
   *
   * @param map the SortedMap to be used
   */
  public DBMapWritable(SortedMap<byte[], V> map){
    mConf = new AtomicReference<Configuration>();
    mInstance = map;
  }

  /** @return the conf */
  @Override
  public Configuration getConf() {
    return mConf.get();
  }

  /** @param conf the conf to set */
  @Override
  public void setConf(Configuration conf) {
    this.mConf.set(conf);
  }

  @Override
  public void clear() {
    mInstance.clear();
  }

  @Override
  public boolean containsKey(Object key) {
    return mInstance.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return mInstance.containsValue(value);
  }

  @Override
  public Set<Entry<byte[], V>> entrySet() {
    return mInstance.entrySet();
  }

  @Override
  public V get(Object key) {
    return mInstance.get(key);
  }

  @Override
  public boolean isEmpty() {
    return mInstance.isEmpty();
  }

  @Override
  public Set<byte[]> keySet() {
    return mInstance.keySet();
  }

  @Override
  public int size() {
    return mInstance.size();
  }

  @Override
  public Collection<V> values() {
    return mInstance.values();
  }

  @Override
  public void putAll(Map<? extends byte[], ? extends V> m) {
    this.mInstance.putAll(m);
  }

  @Override
  public V remove(Object key) {
    return this.mInstance.remove(key);
  }

  @Override
  public V put(byte[] key, V value) {
    return this.mInstance.put(key, value);
  }

  @Override
  public Comparator<? super byte[]> comparator() {
    return this.mInstance.comparator();
  }

  @Override
  public byte[] firstKey() {
    return this.mInstance.firstKey();
  }

  @Override
  public SortedMap<byte[], V> headMap(byte[] toKey) {
    return this.mInstance.headMap(toKey);
  }

  @Override
  public byte[] lastKey() {
    return this.mInstance.lastKey();
  }

  @Override
  public SortedMap<byte[], V> subMap(byte[] fromKey, byte[] toKey) {
    return this.mInstance.subMap(fromKey, toKey);
  }

  @Override
  public SortedMap<byte[], V> tailMap(byte[] fromKey) {
    return this.mInstance.tailMap(fromKey);
  }

  // Writable

  /** @return the Class class for the specified id */
  @SuppressWarnings("boxing")
  protected Class<?> getClass(byte id) {
    return CODE_TO_CLASS.get(id);
  }

  /** @return the id for the specified Class */
  @SuppressWarnings("boxing")
  protected byte getId(Class<?> clazz) {
    Byte b = CLASS_TO_CODE.get(clazz);
    if (b == null) {
      throw new NullPointerException("Nothing for : " + clazz);
    }
    return b;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return this.mInstance.toString();
  }

  @Override
  public void write(DataOutput out) throws IOException {
    // Write out the number of entries in the map
    out.writeInt(this.mInstance.size());
    
    // Then write out each key/value pair
    for (Map.Entry<byte[], V> e : mInstance.entrySet()) {
      Bytes.writeByteArray(out, e.getKey());
      Byte id = getId(e.getValue().getClass());
      out.writeByte(id);
      
      Object value = e.getValue();
      if (value instanceof byte[]) {
        Bytes.writeByteArray(out, (byte[])value);
      } else {
        ((Writable)value).write(out);
      }
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public void readFields(DataInput in) throws IOException {
    // First clear the map.  Otherwise we will just accumulate
    // entries every time this method is called.
    this.mInstance.clear();
    
    // Read the number of entries in the map
    int entries = in.readInt();
    
    // Then read each key/value pair
    for (int i = 0; i < entries; i++) {
      byte[] key = Bytes.readByteArray(in);
      byte id = in.readByte();
      Class clazz = getClass(id);
      V value = null;
      
      if (clazz.equals(byte[].class)) {
        byte[] bytes = Bytes.readByteArray(in);
        value = (V)bytes;
        
      } else {
        Writable w = (Writable)ReflectionUtils.
          newInstance(clazz, getConf());
        w.readFields(in);
        value = (V)w;
      }
      
      this.mInstance.put(key, value);
    }
  }
}
