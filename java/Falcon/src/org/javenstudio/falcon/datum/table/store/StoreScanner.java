package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;

/**
 * Scanner scans both the memstore and the DBStore. Coalesce KeyValue stream
 * into List<KeyValue> for a single row.
 */
class StoreScanner implements KeyValueScanner, InternalScanner, ChangedReadersObserver {
  //private static final Logger LOG = Logger.getLogger(StoreScanner.class);
  
  private Store mStore;
  private ScanQueryMatcher mMatcher;
  private KeyValueHeap mHeap;
  private boolean mCacheBlocks;

  // Used to indicate that the scanner has closed (see HBASE-1107)
  // Doesnt need to be volatile because it's always accessed via synchronized methods
  private boolean mClosing = false;
  private final boolean mIsGet;

  // if heap == null and lastTop != null, you need to reseek given the key below
  private KeyValue mLastTop = null;

  /**
   * Opens a scanner across memstore, snapshot, and all StoreFiles.
   *
   * @param store who we scan
   * @param scan the spec
   * @param columns which columns we are scanning
   * @throws IOException
   */
  StoreScanner(Store store, Scan scan, final NavigableSet<byte[]> columns) 
	  throws IOException {
    this.mStore = store;
    this.mCacheBlocks = scan.getCacheBlocks();
    this.mMatcher = new ScanQueryMatcher(scan, store.getFamily().getName(),
        columns, store.mTtl, store.mComparator.getRawComparator(),
        store.versionsToReturn(scan.getMaxVersions()));

    this.mIsGet = scan.isGetScan();
    // pass columns = try to filter out unnecessary ScanFiles
    List<KeyValueScanner> scanners = getScanners(scan, columns);

    // Seek all scanners to the initial key
    for (KeyValueScanner scanner : scanners) {
      scanner.seek(mMatcher.getStartKey());
    }

    // Combine all seeked scanners with a heap
    mHeap = new KeyValueHeap(scanners, store.mComparator);

    this.mStore.addChangedReaderObserver(this);
  }

  /**
   * Used for major compactions.<p>
   *
   * Opens a scanner across specified StoreFiles.
   * @param store who we scan
   * @param scan the spec
   * @param scanners ancilliary scanners
   */
  StoreScanner(Store store, Scan scan, List<? extends KeyValueScanner> scanners)
      throws IOException {
    this.mStore = store;
    this.mCacheBlocks = false;
    this.mIsGet = false;
    this.mMatcher = new ScanQueryMatcher(scan, store.getFamily().getName(),
        null, store.mTtl, store.mComparator.getRawComparator(),
        store.versionsToReturn(scan.getMaxVersions()));

    // Seek all scanners to the initial key
    for (KeyValueScanner scanner : scanners) {
      scanner.seek(mMatcher.getStartKey());
    }

    // Combine all seeked scanners with a heap
    mHeap = new KeyValueHeap(scanners, store.mComparator);
  }

  // Constructor for testing.
  StoreScanner(final Scan scan, final byte [] colFamily, final long ttl,
      final KeyValue.KVComparator comparator,
      final NavigableSet<byte[]> columns,
      final List<KeyValueScanner> scanners)
      throws IOException {
    this.mStore = null;
    this.mIsGet = false;
    this.mCacheBlocks = scan.getCacheBlocks();
    this.mMatcher = new ScanQueryMatcher(scan, colFamily, columns, ttl,
        comparator.getRawComparator(), scan.getMaxVersions());

    // Seek all scanners to the initial key
    for (KeyValueScanner scanner : scanners) {
      scanner.seek(mMatcher.getStartKey());
    }
    mHeap = new KeyValueHeap(scanners, comparator);
  }

  /**
   * @return List of scanners ordered properly.
   */
  private List<KeyValueScanner> getScanners() throws IOException {
    // First the store file scanners
    // TODO this used to get the store files in descending order,
    // but now we get them in ascending order, which I think is
    // actually more correct, since memstore get put at the end.
    List<StoreFileScanner> sfScanners = StoreFileScanner
      .getScannersForStoreFiles(mStore.getStorefiles(), mCacheBlocks, mIsGet);
    List<KeyValueScanner> scanners =
      new ArrayList<KeyValueScanner>(sfScanners.size()+1);
    scanners.addAll(sfScanners);
    // Then the memstore scanners
    scanners.addAll(this.mStore.mMemstore.getScanners());
    return scanners;
  }

  /**
   * @return List of scanners to seek, possibly filtered by StoreFile.
   */
  private List<KeyValueScanner> getScanners(Scan scan,
      final NavigableSet<byte[]> columns) throws IOException {
    // First the store file scanners
    List<StoreFileScanner> sfScanners = StoreFileScanner
      .getScannersForStoreFiles(mStore.getStorefiles(), mCacheBlocks, mIsGet);
    List<KeyValueScanner> scanners =
      new ArrayList<KeyValueScanner>(sfScanners.size()+1);

    // include only those scan files which pass all filters
    for (StoreFileScanner sfs : sfScanners) {
      if (sfs.shouldSeek(scan, columns)) 
        scanners.add(sfs);
    }

    // Then the memstore scanners
    if (this.mStore.mMemstore.shouldSeek(scan)) 
      scanners.addAll(this.mStore.mMemstore.getScanners());
    
    return scanners;
  }

  @Override
  public synchronized KeyValue peek() {
    if (this.mHeap == null) {
      return this.mLastTop;
    }
    return this.mHeap.peek();
  }

  @Override
  public KeyValue next() {
    // throw runtime exception perhaps?
    throw new RuntimeException("Never call StoreScanner.next()");
  }

  @Override
  public String toString() {
	return getClass().getSimpleName() + "{store=" + mStore + "}";
  }
  
  @Override
  public synchronized void close() {
    if (this.mClosing) return;
    this.mClosing = true;
    // under test, we dont have a this.store
    if (this.mStore != null)
      this.mStore.deleteChangedReaderObserver(this);
    if (this.mHeap != null)
      this.mHeap.close();
    this.mHeap = null; // CLOSED!
    this.mLastTop = null; // If both are null, we are closed.
  }

  @Override
  public synchronized boolean seek(KeyValue key) throws IOException {
    if (this.mHeap == null) {
      List<KeyValueScanner> scanners = getScanners();
      mHeap = new KeyValueHeap(scanners, mStore.mComparator);
    }
    return this.mHeap.seek(key);
  }

  /**
   * Get the next row of values from this Store.
   * @param outResult
   * @param limit
   * @return true if there are more rows, false if scanner is done
   */
  @Override
  public synchronized boolean next(List<KeyValue> outResult, 
		  int limit) throws IOException {
    checkReseek();

    // if the heap was left null, then the scanners had previously run out anyways, close and
    // return.
    if (this.mHeap == null) {
      close();
      return false;
    }

    KeyValue peeked = this.mHeap.peek();
    if (peeked == null) {
      close();
      return false;
    }

    // only call setRow if the row changes; avoids confusing the query matcher
    // if scanning intra-row
    if ((mMatcher.mRow == null) || !peeked.matchingRow(mMatcher.mRow)) {
      mMatcher.setRow(peeked.getRow());
    }

    KeyValue kv;
    List<KeyValue> results = new ArrayList<KeyValue>();
    LOOP: while((kv = this.mHeap.peek()) != null) {
      ScanQueryMatcher.MatchCode qcode = mMatcher.match(kv);
      
      switch(qcode) {
        case INCLUDE:
          KeyValue next = this.mHeap.next();
          results.add(next);
          if (limit > 0 && (results.size() == limit)) {
            break LOOP;
          }
          continue;

        case DONE:
          // copy jazz
          outResult.addAll(results);
          return true;

        case DONE_SCAN:
          close();
          // copy jazz
          outResult.addAll(results);
          return false;

        case SEEK_NEXT_ROW:
          if (!mMatcher.moreRowsMayExistAfter(kv)) {
            outResult.addAll(results);
            return false;
          }
          mHeap.next();
          break;

        case SEEK_NEXT_COL:
          // TODO hfile needs 'hinted' seeking to prevent it from
          // reseeking from the start of the block on every dang seek.
          // We need that API and expose it the scanner chain.
          mHeap.next();
          break;

        case SKIP:
          this.mHeap.next();
          break;

        case SEEK_NEXT_USING_HINT:
          KeyValue nextKV = mMatcher.getNextKeyHint(kv);
          if (nextKV != null) {
            reseek(nextKV);
          } else {
            mHeap.next();
          }
          break;

        default:
          throw new RuntimeException("UNEXPECTED");
      }
    }

    if (!results.isEmpty()) {
      // copy jazz
      outResult.addAll(results);
      return true;
    }

    // No more keys
    close();
    return false;
  }

  @Override
  public synchronized boolean next(List<KeyValue> outResult) throws IOException {
    return next(outResult, -1);
  }

  // Implementation of ChangedReadersObserver
  public synchronized void updateReaders() throws IOException {
    if (this.mClosing) return;

    // All public synchronized API calls will call 'checkReseek' which will cause
    // the scanner stack to reseek if this.heap==null && this.lastTop != null.
    // But if two calls to updateReaders() happen without a 'next' or 'peek' then we
    // will end up calling this.peek() which would cause a reseek in the middle of a updateReaders
    // which is NOT what we want, not to mention could cause an NPE. So we early out here.
    if (this.mHeap == null) return;

    // this could be null.
    this.mLastTop = this.peek();

    // close scanners to old obsolete Store files
    this.mHeap.close(); // bubble thru and close all scanners.
    this.mHeap = null; // the re-seeks could be slow (access HDFS) free up memory ASAP

    // Let the next() call handle re-creating and seeking
  }

  private void checkReseek() throws IOException {
    if (this.mHeap == null && this.mLastTop != null) {
      resetScannerStack(this.mLastTop);
      this.mLastTop = null; // gone!
    }
    // else dont need to reseek
  }

  private void resetScannerStack(KeyValue lastTopKey) throws IOException {
    if (mHeap != null) {
      throw new RuntimeException("StoreScanner.reseek run on an existing heap!");
    }

    /* When we have the scan object, should we not pass it to getScanners()
     * to get a limited set of scanners? We did so in the constructor and we
     * could have done it now by storing the scan object from the constructor 
     */
    List<KeyValueScanner> scanners = getScanners();

    for (KeyValueScanner scanner : scanners) {
      scanner.seek(lastTopKey);
    }

    // Combine all seeked scanners with a heap
    mHeap = new KeyValueHeap(scanners, mStore.mComparator);

    // Reset the state of the Query Matcher and set to top row
    mMatcher.reset();
    KeyValue kv = mHeap.peek();
    mMatcher.setRow((kv == null ? lastTopKey : kv).getRow());
  }

  @Override
  public synchronized boolean reseek(KeyValue kv) throws IOException {
    //Heap cannot be null, because this is only called from next() which
    //guarantees that heap will never be null before this call.
    return this.mHeap.reseek(kv);
  }
}
