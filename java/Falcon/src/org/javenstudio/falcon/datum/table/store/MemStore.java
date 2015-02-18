package org.javenstudio.falcon.datum.table.store;

import java.rmi.UnexpectedException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.javenstudio.common.util.Logger;

/**
 * The MemStore holds in-memory modifications to the Store.  Modifications
 * are {@link KeyValue}s.  When asked to flush, current memstore is moved
 * to snapshot and is cleared.  We continue to serve edits out of new memstore
 * and backing snapshot until flusher reports in that the flush succeeded. At
 * this point we let the snapshot go.
 * TODO: Adjust size of the memstore when we remove items because they have
 * been deleted.
 * TODO: With new KVSLS, need to make sure we update HeapSize with difference
 * in KV size.
 */
public class MemStore implements HeapSize {
  private static final Logger LOG = Logger.getLogger(MemStore.class);

  // MemStore.  Use a KeyValueSkipListSet rather than SkipListSet because of the
  // better semantics.  The Map will overwrite if passed a key it already had
  // whereas the Set will not add new KV if key is same though value might be
  // different.  Value is not important -- just make sure always same
  // reference passed.
  private volatile KeyValueSkipListSet mKvset;

  // Snapshot of memstore.  Made for flusher.
  private volatile KeyValueSkipListSet mSnapshot;

  private final ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();

  private final KeyValue.KVComparator mComparator;

  // Used comparing versions -- same r/c and ts but different type.
  @SuppressWarnings("unused")
  private final KeyValue.KVComparator mComparatorIgnoreType;

  // Used comparing versions -- same r/c and type but different timestamp.
  @SuppressWarnings("unused")
  private final KeyValue.KVComparator mComparatorIgnoreTimestamp;

  // Used to track own heapSize
  private final AtomicLong mSize;

  private TimeRangeTracker mTimeRangeTracker;
  private TimeRangeTracker mSnapshotTimeRangeTracker;

  /**
   * Default constructor. Used for tests.
   */
  public MemStore() {
    this(KeyValue.COMPARATOR);
  }

  /**
   * Constructor.
   * @param c Comparator
   */
  public MemStore(final KeyValue.KVComparator c) {
    this.mComparator = c;
    this.mComparatorIgnoreTimestamp =
      this.mComparator.getComparatorIgnoringTimestamps();
    this.mComparatorIgnoreType = this.mComparator.getComparatorIgnoringType();
    this.mKvset = new KeyValueSkipListSet(c);
    this.mSnapshot = new KeyValueSkipListSet(c);
    this.mTimeRangeTracker = new TimeRangeTracker();
    this.mSnapshotTimeRangeTracker = new TimeRangeTracker();
    this.mSize = new AtomicLong(DEEP_OVERHEAD);
  }

  protected void dump() {
	if (LOG.isInfoEnabled()) {
      for (KeyValue kv: this.mKvset) {
        LOG.info("dump: " + kv);
      }
      for (KeyValue kv: this.mSnapshot) {
        LOG.info("dump: " + kv);
      }
	}
  }

  /**
   * Creates a snapshot of the current memstore.
   * Snapshot must be cleared by call to {@link #clearSnapshot(SortedSet<KeyValue>)}
   * To get the snapshot made by this method, use {@link #getSnapshot()}
   */
  protected void snapshot() {
    this.mLock.writeLock().lock();
    try {
      // If snapshot currently has entries, then flusher failed or didn't call
      // cleanup.  Log a warning.
      if (!this.mSnapshot.isEmpty()) {
    	if (LOG.isWarnEnabled()) {
          LOG.warn("Snapshot called again without clearing previous. " +
            "Doing nothing. Another ongoing flush or did we fail last attempt?");
    	}
      } else {
        if (!this.mKvset.isEmpty()) {
          this.mSnapshot = this.mKvset;
          this.mKvset = new KeyValueSkipListSet(this.mComparator);
          this.mSnapshotTimeRangeTracker = this.mTimeRangeTracker;
          this.mTimeRangeTracker = new TimeRangeTracker();
          // Reset heap to not include any keys
          this.mSize.set(DEEP_OVERHEAD);
        }
      }
    } finally {
      this.mLock.writeLock().unlock();
    }
  }

  /**
   * Return the current snapshot.
   * Called by flusher to get current snapshot made by a previous
   * call to {@link #snapshot()}
   * @return Return snapshot.
   * @see {@link #snapshot()}
   * @see {@link #clearSnapshot(SortedSet<KeyValue>)}
   */
  protected KeyValueSkipListSet getSnapshot() {
    return this.mSnapshot;
  }

  /**
   * The passed snapshot was successfully persisted; it can be let go.
   * @param ss The snapshot to clean out.
   * @throws UnexpectedException
   * @see {@link #snapshot()}
   */
  protected void clearSnapshot(final SortedSet<KeyValue> ss)
      throws UnexpectedException {
    this.mLock.writeLock().lock();
    try {
      if (this.mSnapshot != ss) {
        throw new UnexpectedException("Current snapshot is " +
          this.mSnapshot + ", was passed " + ss);
      }
      // OK. Passed in snapshot is same as current snapshot.  If not-empty,
      // create a new snapshot and let the old one go.
      if (!ss.isEmpty()) {
        this.mSnapshot = new KeyValueSkipListSet(this.mComparator);
        this.mSnapshotTimeRangeTracker = new TimeRangeTracker();
      }
    } finally {
      this.mLock.writeLock().unlock();
    }
  }

  /**
   * Write an update
   * @param kv
   * @return approximate size of the passed key and value.
   */
  protected long add(final KeyValue kv) {
    long s = -1;
    this.mLock.readLock().lock();
    try {
      s = heapSizeChange(kv, this.mKvset.add(kv));
      this.mTimeRangeTracker.includeTimestamp(kv);
      this.mSize.addAndGet(s);
    } finally {
      this.mLock.readLock().unlock();
    }
    return s;
  }

  /**
   * Write a delete
   * @param delete
   * @return approximate size of the passed key and value.
   */
  protected long delete(final KeyValue delete) {
    long s = 0;
    this.mLock.readLock().lock();
    try {
      s += heapSizeChange(delete, this.mKvset.add(delete));
      this.mTimeRangeTracker.includeTimestamp(delete);
    } finally {
      this.mLock.readLock().unlock();
    }
    this.mSize.addAndGet(s);
    return s;
  }

  /**
   * @param kv Find the row that comes after this one.  If null, we return the
   * first.
   * @return Next row or null if none found.
   */
  protected KeyValue getNextRow(final KeyValue kv) {
    this.mLock.readLock().lock();
    try {
      return getLowest(getNextRow(kv, this.mKvset), getNextRow(kv, this.mSnapshot));
    } finally {
      this.mLock.readLock().unlock();
    }
  }

  /**
   * @param a
   * @param b
   * @return Return lowest of a or b or null if both a and b are null
   */
  private KeyValue getLowest(final KeyValue a, final KeyValue b) {
    if (a == null) return b;
    if (b == null) return a;
    return mComparator.compareRows(a, b) <= 0 ? a : b;
  }

  /**
   * @param key Find row that follows this one.  If null, return first.
   * @param map Set to look in for a row beyond <code>row</code>.
   * @return Next row or null if none found.  If one found, will be a new
   * KeyValue -- can be destroyed by subsequent calls to this method.
   */
  private KeyValue getNextRow(final KeyValue key,
      final NavigableSet<KeyValue> set) {
    KeyValue result = null;
    SortedSet<KeyValue> tail = key == null? set: set.tailSet(key);
    
    // Iterate until we fall into the next row; i.e. move off current row
    for (KeyValue kv : tail) {
      if (mComparator.compareRows(kv, key) <= 0)
        continue;
      
      // Note: Not suppressing deletes or expired cells.  Needs to be handled
      // by higher up functions.
      result = kv;
      break;
    }
    
    return result;
  }

  /**
   * @param state column/delete tracking state
   */
  protected void getRowKeyAtOrBefore(final GetClosestRowBeforeTracker state) {
    this.mLock.readLock().lock();
    try {
      getRowKeyAtOrBefore(mKvset, state);
      getRowKeyAtOrBefore(mSnapshot, state);
    } finally {
      this.mLock.readLock().unlock();
    }
  }

  /**
   * @param set
   * @param state Accumulates deletes and candidates.
   */
  private void getRowKeyAtOrBefore(final NavigableSet<KeyValue> set,
      final GetClosestRowBeforeTracker state) {
    if (set.isEmpty()) {
      return;
    }
    if (!walkForwardInSingleRow(set, state.getTargetKey(), state)) {
      // Found nothing in row.  Try backing up.
      getRowKeyBefore(set, state);
    }
  }

  /**
   * Walk forward in a row from <code>firstOnRow</code>.  Presumption is that
   * we have been passed the first possible key on a row.  As we walk forward
   * we accumulate deletes until we hit a candidate on the row at which point
   * we return.
   * @param set
   * @param firstOnRow First possible key on this row.
   * @param state
   * @return True if we found a candidate walking this row.
   */
  private boolean walkForwardInSingleRow(final SortedSet<KeyValue> set,
      final KeyValue firstOnRow, final GetClosestRowBeforeTracker state) {
    boolean foundCandidate = false;
    SortedSet<KeyValue> tail = set.tailSet(firstOnRow);
    if (tail.isEmpty()) return foundCandidate;
    
    for (Iterator<KeyValue> i = tail.iterator(); i.hasNext();) {
      KeyValue kv = i.next();
      
      // Did we go beyond the target row? If so break.
      if (state.isTooFar(kv, firstOnRow)) break;
      if (state.isExpired(kv)) {
        i.remove();
        continue;
      }
      
      // If we added something, this row is a contender. break.
      if (state.handle(kv)) {
        foundCandidate = true;
        break;
      }
    }
    
    return foundCandidate;
  }

  /**
   * Walk backwards through the passed set a row at a time until we run out of
   * set or until we get a candidate.
   * @param set
   * @param state
   */
  private void getRowKeyBefore(NavigableSet<KeyValue> set,
      final GetClosestRowBeforeTracker state) {
    KeyValue firstOnRow = state.getTargetKey();
    for (Member p = memberOfPreviousRow(set, state, firstOnRow);
        p != null; p = memberOfPreviousRow(p.mSet, state, firstOnRow)) {
      // Make sure we don't fall out of our table.
      if (!state.isTargetTable(p.mKv)) break;
      // Stop looking if we've exited the better candidate range.
      if (!state.isBetterCandidate(p.mKv)) break;
      // Make into firstOnRow
      firstOnRow = new KeyValue(p.mKv.getRow(), DBConstants.LATEST_TIMESTAMP);
      // If we find something, break;
      if (walkForwardInSingleRow(p.mSet, firstOnRow, state)) 
        break;
    }
  }

  /**
   * Given the specs of a column, update it, first by inserting a new record,
   * then removing the old one.  Since there is only 1 KeyValue involved, the memstoreTS
   * will be set to 0, thus ensuring that they instantly appear to anyone. The underlying
   * store will ensure that the insert/delete each are atomic. A scanner/reader will either
   * get the new value, or the old value and all readers will eventually only see the new
   * value after the old was removed.
   *
   * @param row
   * @param family
   * @param qualifier
   * @param newValue
   * @param now
   * @return
   */
  public long updateColumnValue(byte[] row,
                                byte[] family,
                                byte[] qualifier,
                                long newValue,
                                long now) {
    this.mLock.readLock().lock();
    try {
      KeyValue firstKv = KeyValue.createFirstOnRow(
          row, family, qualifier);
      
      // create a new KeyValue with 'now' and a 0 memstoreTS == immediately visible
      KeyValue newKv;
      // Is there a KeyValue in 'snapshot' with the same TS? If so, upgrade the timestamp a bit.
      SortedSet<KeyValue> snSs = mSnapshot.tailSet(firstKv);
      
      if (!snSs.isEmpty()) {
        KeyValue snKv = snSs.first();
        // is there a matching KV in the snapshot?
        if (snKv.matchingRow(firstKv) && snKv.matchingQualifier(firstKv)) {
          if (snKv.getTimestamp() == now) {
            // poop,
            now += 1;
          }
        }
      }

      // logic here: the new ts MUST be at least 'now'. But it could be larger if necessary.
      // But the timestamp should also be max(now, mostRecentTsInMemstore)

      // so we cant add the new KV w/o knowing what's there already, but we also
      // want to take this chance to delete some kvs. So two loops (sad)

      SortedSet<KeyValue> ss = mKvset.tailSet(firstKv);
      Iterator<KeyValue> it = ss.iterator();
      
      while (it.hasNext()) {
        KeyValue kv = it.next();

        // if this isnt the row we are interested in, then bail:
        if (!firstKv.matchingColumn(family,qualifier) || !firstKv.matchingRow(kv)) {
          break; // rows dont match, bail.
        }

        // if the qualifier matches and it's a put, just RM it out of the kvset.
        if (firstKv.matchingQualifier(kv)) {
          // to be extra safe we only remove Puts that have a memstoreTS==0
          if (kv.getType() == KeyValue.Type.Put.getCode()) {
            now = Math.max(now, kv.getTimestamp());
          }
        }
      }

      // add the new value now. this might have the same TS as an existing KV, thus confusing
      // readers slightly for a MOMENT until we erase the old one (and thus old value).
      newKv = new KeyValue(row, family, qualifier,
          now, Bytes.toBytes(newValue));
      long addedSize = add(newKv);

      // remove extra versions.
      ss = mKvset.tailSet(firstKv);
      it = ss.iterator();
      
      while (it.hasNext()) {
        KeyValue kv = it.next();

        if (kv == newKv) {
          // ignore the one i just put in (heh)
          continue;
        }

        // if this isnt the row we are interested in, then bail:
        if (!firstKv.matchingColumn(family,qualifier) || !firstKv.matchingRow(kv)) {
          break; // rows dont match, bail.
        }

        // if the qualifier matches and it's a put, just RM it out of the kvset.
        if (firstKv.matchingQualifier(kv)) {
          // to be extra safe we only remove Puts that have a memstoreTS==0
          if (kv.getType() == KeyValue.Type.Put.getCode()) {
            // false means there was a change, so give us the size.
            addedSize -= heapSizeChange(kv, true);

            it.remove();
          }
        }
      }

      return addedSize;
    } finally {
      this.mLock.readLock().unlock();
    }
  }

  /**
   * Immutable data structure to hold member found in set and the set it was
   * found in.  Include set because it is carrying context.
   */
  private static class Member {
    private final KeyValue mKv;
    private final NavigableSet<KeyValue> mSet;
    
    Member(final NavigableSet<KeyValue> s, final KeyValue kv) {
      this.mKv = kv;
      this.mSet = s;
    }
  }

  /**
   * @param set Set to walk back in.  Pass a first in row or we'll return
   * same row (loop).
   * @param state Utility and context.
   * @param firstOnRow First item on the row after the one we want to find a
   * member in.
   * @return Null or member of row previous to <code>firstOnRow</code>
   */
  private Member memberOfPreviousRow(NavigableSet<KeyValue> set,
      final GetClosestRowBeforeTracker state, final KeyValue firstOnRow) {
    NavigableSet<KeyValue> head = set.headSet(firstOnRow, false);
    if (head.isEmpty()) return null;
    
    for (Iterator<KeyValue> i = head.descendingIterator(); i.hasNext();) {
      KeyValue found = i.next();
      if (state.isExpired(found)) {
        i.remove();
        continue;
      }
      return new Member(head, found);
    }
    
    return null;
  }

  /**
   * @return scanner on memstore and snapshot in this order.
   */
  protected List<KeyValueScanner> getScanners() {
    this.mLock.readLock().lock();
    try {
      return Collections.<KeyValueScanner>singletonList(
          new MemStoreScanner());
    } finally {
      this.mLock.readLock().unlock();
    }
  }

  /**
   * Check if this memstore may contain the required keys
   * @param scan
   * @return False if the key definitely does not exist in this Memstore
   */
  public boolean shouldSeek(Scan scan) {
    return mTimeRangeTracker.includesTimeRange(scan.getTimeRange()) ||
        mSnapshotTimeRangeTracker.includesTimeRange(scan.getTimeRange());
  }

  public TimeRangeTracker getSnapshotTimeRangeTracker() {
    return this.mSnapshotTimeRangeTracker;
  }

  /**
   * MemStoreScanner implements the KeyValueScanner.
   * It lets the caller scan the contents of a memstore -- both current
   * map and snapshot.
   * This behaves as if it were a real scanner but does not maintain position.
   */
  protected class MemStoreScanner implements KeyValueScanner {
    // Next row information for either kvset or snapshot
    private KeyValue mKvsetNextRow = null;
    private KeyValue mSnapshotNextRow = null;

    // iterator based scanning.
    private Iterator<KeyValue> mKvsetIt;
    private Iterator<KeyValue> mSnapshotIt;

    /*
    Some notes...

    So memstorescanner is fixed at creation time. this includes pointers/iterators into
    existing kvset/snapshot.  during a snapshot creation, the kvset is null, and the
    snapshot is moved.  since kvset is null there is no point on reseeking on both,
      we can save us the trouble. During the snapshot->hfile transition, the memstore
      scanner is re-created by StoreScanner#updateReaders().  StoreScanner should
      potentially do something smarter by adjusting the existing memstore scanner.

      But there is a greater problem here, that being once a scanner has progressed
      during a snapshot scenario, we currently iterate past the kvset then 'finish' up.
      if a scan lasts a little while, there is a chance for new entries in kvset to
      become available but we will never see them.  This needs to be handled at the
      StoreScanner level with coordination with MemStoreScanner.

    */

    MemStoreScanner() {
      super();
    }

    protected KeyValue getNext(Iterator<KeyValue> it) {
      KeyValue ret = null;
      long readPoint = ReadWriteConsistencyControl.getThreadReadPoint();

      while (ret == null && it.hasNext()) {
        KeyValue v = it.next();
        if (v.getMemstoreTS() <= readPoint) {
          // keep it.
          ret = v;
        }
      }
      return ret;
    }

    @Override
    public synchronized boolean seek(KeyValue key) {
      if (key == null) {
        close();
        return false;
      }

      // kvset and snapshot will never be empty.
      // if tailSet cant find anything, SS is empty (not null).
      SortedSet<KeyValue> kvTail = mKvset.tailSet(key);
      SortedSet<KeyValue> snapshotTail = mSnapshot.tailSet(key);

      mKvsetIt = kvTail.iterator();
      mSnapshotIt = snapshotTail.iterator();

      mKvsetNextRow = getNext(mKvsetIt);
      mSnapshotNextRow = getNext(mSnapshotIt);

      KeyValue lowest = getLowest();

      // has data := (lowest != null)
      return lowest != null;
    }

    @Override
    public boolean reseek(KeyValue key) {
      while (mKvsetNextRow != null &&
          mComparator.compare(mKvsetNextRow, key) < 0) {
        mKvsetNextRow = getNext(mKvsetIt);
      }

      while (mSnapshotNextRow != null &&
          mComparator.compare(mSnapshotNextRow, key) < 0) {
        mSnapshotNextRow = getNext(mSnapshotIt);
      }
      
      return (mKvsetNextRow != null || mSnapshotNextRow != null);
    }

    @Override
    public synchronized KeyValue peek() {
      return getLowest();
    }

    @Override
    public synchronized KeyValue next() {
      KeyValue theNext = getLowest();
      if (theNext == null) 
          return null;

      // Advance one of the iterators
      if (theNext == mKvsetNextRow) {
        mKvsetNextRow = getNext(mKvsetIt);
      } else {
        mSnapshotNextRow = getNext(mSnapshotIt);
      }

      return theNext;
    }

    protected KeyValue getLowest() {
      return getLower(mKvsetNextRow, mSnapshotNextRow);
    }

    /**
     * Returns the lower of the two key values, or null if they are both null.
     * This uses comparator.compare() to compare the KeyValue using the memstore
     * comparator.
     */
    protected KeyValue getLower(KeyValue first, KeyValue second) {
      if (first == null && second == null) 
        return null;
      
      if (first != null && second != null) {
        int compare = mComparator.compare(first, second);
        return (compare <= 0 ? first : second);
      }
      
      return (first != null ? first : second);
    }

    @Override
    public synchronized void close() {
      this.mKvsetNextRow = null;
      this.mSnapshotNextRow = null;

      this.mKvsetIt = null;
      this.mSnapshotIt = null;
    }
  }

  public final static long FIXED_OVERHEAD = ClassSize.align(
      ClassSize.OBJECT + (9 * ClassSize.REFERENCE));

  public final static long DEEP_OVERHEAD = ClassSize.align(FIXED_OVERHEAD +
      ClassSize.REENTRANT_LOCK + ClassSize.ATOMIC_LONG +
      ClassSize.COPYONWRITE_ARRAYSET + ClassSize.COPYONWRITE_ARRAYLIST +
      (2 * ClassSize.CONCURRENT_SKIPLISTMAP));

  /**
   * Calculate how the MemStore size has changed.  Includes overhead of the
   * backing Map.
   * @param kv
   * @param notpresent True if the kv was NOT present in the set.
   * @return Size
   */
  protected long heapSizeChange(final KeyValue kv, final boolean notpresent) {
    return notpresent ?
        ClassSize.align(ClassSize.CONCURRENT_SKIPLISTMAP_ENTRY + kv.heapSize()):
        0;
  }

  /**
   * Get the entire heap usage for this MemStore not including keys in the
   * snapshot.
   */
  @Override
  public long heapSize() {
    return mSize.get();
  }

  /**
   * Get the heap usage of KVs in this MemStore.
   */
  public long keySize() {
    return heapSize() - DEEP_OVERHEAD;
  }

}
