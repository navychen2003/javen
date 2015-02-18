package org.javenstudio.raptor.bigdb.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A SortedMap implementation that uses Soft Reference values
 * internally to make it play well with the GC when in a low-memory
 * situation. Use as a cache where you also need SortedMap functionality.
 *
 * @param <K> key class
 * @param <V> value class
 */
public class SoftValueSortedMap<K,V> implements SortedMap<K,V> {

  private final SortedMap<K, SoftValue<K,V>> internalMap;
  @SuppressWarnings("rawtypes")
  private final ReferenceQueue rq = new ReferenceQueue();

  /** Constructor */
  public SoftValueSortedMap() {
    this(new TreeMap<K, SoftValue<K,V>>());
  }

  /**
   * Constructor
   * @param c comparator
   */
  public SoftValueSortedMap(final Comparator<K> c) {
    this(new TreeMap<K, SoftValue<K,V>>(c));
  }

  /** For headMap and tailMap support
   * @param original object to wrap
   */
  private SoftValueSortedMap(SortedMap<K,SoftValue<K,V>> original) {
    this.internalMap = original;
  }

  /**
   * Checks soft references and cleans any that have been placed on
   * ReferenceQueue.  Call if get/put etc. are not called regularly.
   * Internally these call checkReferences on each access.
   * @return How many references cleared.
   */
  @SuppressWarnings("unchecked")
  private int checkReferences() {
    int i = 0;
    for (Object obj; (obj = this.rq.poll()) != null;) {
      i++;
      //noinspection unchecked
      this.internalMap.remove(((SoftValue<K,V>)obj).key);
    }
    return i;
  }

  public synchronized V put(K key, V value) {
    checkReferences();
    SoftValue<K,V> oldValue = this.internalMap.put(key,
      new SoftValue<K,V>(key, value, this.rq));
    return oldValue == null ? null : oldValue.get();
  }

  @SuppressWarnings("rawtypes")
  public synchronized void putAll(Map map) {
    throw new RuntimeException("Not implemented");
  }

  public synchronized V get(Object key) {
    checkReferences();
    SoftValue<K,V> value = this.internalMap.get(key);
    if (value == null) {
      return null;
    }
    if (value.get() == null) {
      this.internalMap.remove(key);
      return null;
    }
    return value.get();
  }

  public synchronized V remove(Object key) {
    checkReferences();
    SoftValue<K,V> value = this.internalMap.remove(key);
    return value == null ? null : value.get();
  }

  public synchronized boolean containsKey(Object key) {
    checkReferences();
    return this.internalMap.containsKey(key);
  }

  public synchronized boolean containsValue(Object value) {
/*    checkReferences();
    return internalMap.containsValue(value);*/
    throw new UnsupportedOperationException("Don't support containsValue!");
  }

  public synchronized K firstKey() {
    checkReferences();
    return internalMap.firstKey();
  }

  public synchronized K lastKey() {
    checkReferences();
    return internalMap.lastKey();
  }

  public synchronized SoftValueSortedMap<K,V> headMap(K key) {
    checkReferences();
    return new SoftValueSortedMap<K,V>(this.internalMap.headMap(key));
  }

  public synchronized SoftValueSortedMap<K,V> tailMap(K key) {
    checkReferences();
    return new SoftValueSortedMap<K,V>(this.internalMap.tailMap(key));
  }

  public synchronized SoftValueSortedMap<K,V> subMap(K fromKey, K toKey) {
    checkReferences();
    return new SoftValueSortedMap<K,V>(this.internalMap.subMap(fromKey, toKey));
  }

  public synchronized boolean isEmpty() {
    checkReferences();
    return this.internalMap.isEmpty();
  }

  public synchronized int size() {
    checkReferences();
    return this.internalMap.size();
  }

  public synchronized void clear() {
    checkReferences();
    this.internalMap.clear();
  }

  public synchronized Set<K> keySet() {
    checkReferences();
    return this.internalMap.keySet();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public synchronized Comparator comparator() {
    return this.internalMap.comparator();
  }

  public synchronized Set<Map.Entry<K,V>> entrySet() {
    throw new RuntimeException("Not implemented");
  }

  public synchronized Collection<V> values() {
    checkReferences();
    Collection<SoftValue<K,V>> softValues = this.internalMap.values();
    ArrayList<V> hardValues = new ArrayList<V>();
    for(SoftValue<K,V> softValue : softValues) {
      hardValues.add(softValue.get());
    }
    return hardValues;
  }

  private static class SoftValue<K,V> extends SoftReference<V> {
    final K key;

    @SuppressWarnings({ "rawtypes", "unchecked" })
	SoftValue(K key, V value, ReferenceQueue q) {
      super(value, q);
      this.key = key;
    }
  }
}

