package org.javenstudio.falcon.datum.table.store;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A {@link java.util.Set} of {@link KeyValue}s implemented on top of a
 * {@link java.util.concurrent.ConcurrentSkipListMap}.  Works like a
 * {@link java.util.concurrent.ConcurrentSkipListSet} in all but one regard:
 * An add will overwrite if already an entry for the added key.  In other words,
 * where CSLS does "Adds the specified element to this set if it is not already
 * present.", this implementation "Adds the specified element to this set EVEN
 * if it is already present overwriting what was there previous".  The call to
 * add returns true if no value in the backing map or false if there was an
 * entry with same key (though value may be different).
 * <p>Otherwise,
 * has same attributes as ConcurrentSkipListSet: e.g. tolerant of concurrent
 * get and set and won't throw ConcurrentModificationException when iterating.
 */
class KeyValueSkipListSet implements NavigableSet<KeyValue> {

  private final ConcurrentNavigableMap<KeyValue, KeyValue> mDelegatee;

  KeyValueSkipListSet(final KeyValue.KVComparator c) {
    this.mDelegatee = new ConcurrentSkipListMap<KeyValue, KeyValue>(c);
  }

  KeyValueSkipListSet(final ConcurrentNavigableMap<KeyValue, KeyValue> m) {
    this.mDelegatee = m;
  }

  /**
   * Iterator that maps Iterator calls to return the value component of the
   * passed-in Map.Entry Iterator.
   */
  static class MapEntryIterator implements Iterator<KeyValue> {
    private final Iterator<Map.Entry<KeyValue, KeyValue>> mIterator;

    MapEntryIterator(final Iterator<Map.Entry<KeyValue, KeyValue>> it) {
      this.mIterator = it;
    }

    @Override
    public boolean hasNext() {
      return this.mIterator.hasNext();
    }

    @Override
    public KeyValue next() {
      return this.mIterator.next().getValue();
    }

    @Override
    public void remove() {
      this.mIterator.remove();
    }
  }

  @Override
  public KeyValue ceiling(KeyValue e) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Iterator<KeyValue> descendingIterator() {
    return new MapEntryIterator(this.mDelegatee.descendingMap().entrySet().
      iterator());
  }

  @Override
  public NavigableSet<KeyValue> descendingSet() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public KeyValue floor(KeyValue e) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public SortedSet<KeyValue> headSet(final KeyValue toElement) {
    return headSet(toElement, false);
  }

  @Override
  public NavigableSet<KeyValue> headSet(final KeyValue toElement,
      boolean inclusive) {
    return new KeyValueSkipListSet(this.mDelegatee.headMap(toElement, inclusive));
  }

  @Override
  public KeyValue higher(KeyValue e) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Iterator<KeyValue> iterator() {
    return new MapEntryIterator(this.mDelegatee.entrySet().iterator());
  }

  @Override
  public KeyValue lower(KeyValue e) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public KeyValue pollFirst() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public KeyValue pollLast() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public SortedSet<KeyValue> subSet(KeyValue fromElement, KeyValue toElement) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public NavigableSet<KeyValue> subSet(KeyValue fromElement,
      boolean fromInclusive, KeyValue toElement, boolean toInclusive) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public SortedSet<KeyValue> tailSet(KeyValue fromElement) {
    return tailSet(fromElement, true);
  }

  @Override
  public NavigableSet<KeyValue> tailSet(KeyValue fromElement, boolean inclusive) {
    return new KeyValueSkipListSet(this.mDelegatee.tailMap(fromElement, inclusive));
  }

  @Override
  public Comparator<? super KeyValue> comparator() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public KeyValue first() {
    return this.mDelegatee.get(this.mDelegatee.firstKey());
  }

  @Override
  public KeyValue last() {
    return this.mDelegatee.get(this.mDelegatee.lastKey());
  }

  @Override
  public boolean add(KeyValue e) {
    return this.mDelegatee.put(e, e) == null;
  }

  @Override
  public boolean addAll(Collection<? extends KeyValue> c) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void clear() {
    this.mDelegatee.clear();
  }

  @Override
  public boolean contains(Object o) {
    //noinspection SuspiciousMethodCalls
    return this.mDelegatee.containsKey(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isEmpty() {
    return this.mDelegatee.isEmpty();
  }

  @Override
  public boolean remove(Object o) {
    return this.mDelegatee.remove(o) != null;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int size() {
    return this.mDelegatee.size();
  }

  @Override
  public Object[] toArray() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <T> T[] toArray(T[] a) {
    throw new UnsupportedOperationException("Not implemented");
  }
}
