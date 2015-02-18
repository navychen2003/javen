package org.javenstudio.falcon.datum.table.store;

import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * State and utility processing {@link DBRegion#getClosestRowBefore(byte[], byte[])}.
 * Like {@link ScanDeleteTracker} and {@link ScanDeleteTracker} but does not
 * implement the {@link DeleteTracker} interface since state spans rows (There
 * is no update nor reset method).
 */
class GetClosestRowBeforeTracker {

  // Deletes keyed by row.  Comparator compares on row portion of KeyValue only.
  private final NavigableMap<KeyValue, NavigableSet<KeyValue>> mDeletes;
	
  private final KeyValue mTargetkey;
  // Any cell w/ a ts older than this is expired.
  private final long mOldestts;
  private KeyValue mCandidate = null;
  private final KeyValue.KVComparator mKvcomparator;
  // Flag for whether we're doing getclosest on a metaregion.
  private final boolean mMetaregion;
  // Offset and length into targetkey demarking table name (if in a metaregion).
  private final int mRowoffset;
  private final int mTablenamePlusDelimiterLength;

  /**
   * @param c
   * @param kv Presume first on row: i.e. empty column, maximum timestamp and
   * a type of Type.Maximum
   * @param ttl Time to live in ms for this Store
   * @param metaregion True if this is .META. or -ROOT- region.
   */
  GetClosestRowBeforeTracker(final KeyValue.KVComparator c, final KeyValue kv,
      final long ttl, final boolean metaregion) {
    super();
    this.mMetaregion = metaregion;
    this.mTargetkey = kv;
    // If we are in a metaregion, then our table name is the prefix on the
    // targetkey.
    this.mRowoffset = kv.getRowOffset();
    int l = -1;
    if (metaregion) {
      l = KeyValue.getDelimiter(kv.getBuffer(), mRowoffset, kv.getRowLength(),
        DBRegionInfo.DELIMITER) - this.mRowoffset;
    }
    this.mTablenamePlusDelimiterLength = metaregion? l + 1: -1;
    this.mOldestts = System.currentTimeMillis() - ttl;
    this.mKvcomparator = c;
    KeyValue.RowComparator rc = new KeyValue.RowComparator(this.mKvcomparator);
    this.mDeletes = new TreeMap<KeyValue, NavigableSet<KeyValue>>(rc);
  }

  /**
   * @param kv
   * @return True if this <code>kv</code> is expired.
   */
  protected boolean isExpired(final KeyValue kv) {
    return Store.isExpired(kv, this.mOldestts);
  }

  /**
   * Add the specified KeyValue to the list of deletes.
   * @param kv
   */
  private void addDelete(final KeyValue kv) {
    NavigableSet<KeyValue> rowdeletes = this.mDeletes.get(kv);
    if (rowdeletes == null) {
      rowdeletes = new TreeSet<KeyValue>(this.mKvcomparator);
      this.mDeletes.put(kv, rowdeletes);
    }
    rowdeletes.add(kv);
  }

  /**
   * @param kv Adds candidate if nearer the target than previous candidate.
   * @return True if updated candidate.
   */
  private boolean addCandidate(final KeyValue kv) {
    if (!isDeleted(kv) && isBetterCandidate(kv)) {
      this.mCandidate = kv;
      return true;
    }
    return false;
  }

  protected boolean isBetterCandidate(final KeyValue contender) {
    return this.mCandidate == null ||
      (this.mKvcomparator.compareRows(this.mCandidate, contender) < 0 &&
        this.mKvcomparator.compareRows(contender, this.mTargetkey) <= 0);
  }

  /**
   * Check if specified KeyValue buffer has been deleted by a previously
   * seen delete.
   * @param kv
   * @return true is the specified KeyValue is deleted, false if not
   */
  private boolean isDeleted(final KeyValue kv) {
    if (this.mDeletes.isEmpty()) return false;
    NavigableSet<KeyValue> rowdeletes = this.mDeletes.get(kv);
    if (rowdeletes == null || rowdeletes.isEmpty()) return false;
    return isDeleted(kv, rowdeletes);
  }

  /**
   * Check if the specified KeyValue buffer has been deleted by a previously
   * seen delete.
   * @param kv
   * @param ds
   * @return True is the specified KeyValue is deleted, false if not
   */
  public boolean isDeleted(final KeyValue kv, final NavigableSet<KeyValue> ds) {
    if (mDeletes == null || mDeletes.isEmpty()) return false;
    for (KeyValue d : ds) {
      long kvts = kv.getTimestamp();
      long dts = d.getTimestamp();
      if (d.isDeleteFamily()) {
        if (kvts <= dts) return true;
        continue;
      }
      // Check column
      int ret = Bytes.compareTo(kv.getBuffer(), kv.getQualifierOffset(),
          kv.getQualifierLength(),
        d.getBuffer(), d.getQualifierOffset(), d.getQualifierLength());
      if (ret <= -1) {
        // This delete is for an earlier column.
        continue;
      } else if (ret >= 1) {
        // Beyond this kv.
        break;
      }
      // Check Timestamp
      if (kvts > dts) return false;

      // Check Type
      switch (KeyValue.Type.codeToType(d.getType())) {
        case Delete: return kvts == dts;
        case DeleteColumn: return true;
        default: continue;
      }
    }
    return false;
  }

  /**
   * Handle keys whose values hold deletes.
   * Add to the set of deletes and then if the candidate keys contain any that
   * might match, then check for a match and remove it.  Implies candidates
   * is made with a Comparator that ignores key type.
   * @param kv
   * @return True if we removed <code>k</code> from <code>candidates</code>.
   */
  protected boolean handleDeletes(final KeyValue kv) {
    addDelete(kv);
    boolean deleted = false;
    if (!hasCandidate()) return deleted;
    if (isDeleted(this.mCandidate)) {
      this.mCandidate = null;
      deleted = true;
    }
    return deleted;
  }

  /**
   * Do right thing with passed key, add to deletes or add to candidates.
   * @param kv
   * @return True if we added a candidate
   */
  protected boolean handle(final KeyValue kv) {
    if (kv.isDelete()) {
      handleDeletes(kv);
      return false;
    }
    return addCandidate(kv);
  }

  /**
   * @return True if has candidate
   */
  public boolean hasCandidate() {
    return this.mCandidate != null;
  }

  /**
   * @return Best candidate or null.
   */
  public KeyValue getCandidate() {
    return this.mCandidate;
  }

  public KeyValue getTargetKey() {
    return this.mTargetkey;
  }

  /**
   * @param kv Current kv
   * @param First on row kv.
   * @param state
   * @return True if we went too far, past the target key.
   */
  protected boolean isTooFar(final KeyValue kv, final KeyValue firstOnRow) {
    return this.mKvcomparator.compareRows(kv, firstOnRow) > 0;
  }

  protected boolean isTargetTable(final KeyValue kv) {
    if (!mMetaregion) return true;
    // Compare start of keys row.  Compare including delimiter.  Saves having
    // to calculate where tablename ends in the candidate kv.
    return Bytes.compareTo(this.mTargetkey.getBuffer(), this.mRowoffset,
        this.mTablenamePlusDelimiterLength,
        kv.getBuffer(), kv.getRowOffset(), this.mTablenamePlusDelimiterLength) == 0;
  }
}
