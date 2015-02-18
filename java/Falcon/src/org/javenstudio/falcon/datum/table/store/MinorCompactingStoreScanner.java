package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;
import java.util.List;

/**
 * A scanner that does a minor compaction at the same time.  Doesn't need to
 * implement ChangedReadersObserver, since it doesn't scan memstore, only store files
 * and optionally the memstore-snapshot.
 */
public class MinorCompactingStoreScanner implements KeyValueScanner, InternalScanner {

  private KeyValueHeap mHeap;
  private KeyValue.KVComparator mComparator;

  MinorCompactingStoreScanner(Store store, List<? extends KeyValueScanner> scanners)
      throws IOException {
    mComparator = store.mComparator;
    
    KeyValue firstKv = KeyValue.createFirstOnRow(DBConstants.EMPTY_START_ROW);
    for (KeyValueScanner scanner : scanners) {
      scanner.seek(firstKv);
    }
    
    mHeap = new KeyValueHeap(scanners, store.mComparator);
  }

  MinorCompactingStoreScanner(String cfName, KeyValue.KVComparator comparator,
                              List<? extends KeyValueScanner> scanners)
      throws IOException {
    this.mComparator = comparator;

    KeyValue firstKv = KeyValue.createFirstOnRow(DBConstants.EMPTY_START_ROW);
    for (KeyValueScanner scanner : scanners) {
      scanner.seek(firstKv);
    }

    mHeap = new KeyValueHeap(scanners, comparator);
  }

  @Override
  public KeyValue peek() {
    return mHeap.peek();
  }

  @Override
  public KeyValue next() throws IOException {
    return mHeap.next();
  }

  @Override
  public boolean seek(KeyValue key) {
    // cant seek.
    throw new UnsupportedOperationException("Can't seek a MinorCompactingStoreScanner");
  }

  @Override
  public boolean reseek(KeyValue key) {
    return seek(key);
  }

  /**
   * High performance merge scan.
   * @param writer
   * @return True if more.
   * @throws IOException
   */
  public boolean next(StoreFile.Writer writer) throws IOException {
    KeyValue row = mHeap.peek();
    if (row == null) {
      close();
      return false;
    }
    
    KeyValue kv;
    while ((kv = mHeap.peek()) != null) {
      // check to see if this is a different row
      if (mComparator.compareRows(row, kv) != 0) {
        // reached next row
        return true;
      }
      writer.append(mHeap.next());
    }
    
    close();
    return false;
  }

  @Override
  public boolean next(List<KeyValue> results) throws IOException {
    KeyValue row = mHeap.peek();
    if (row == null) {
      close();
      return false;
    }
    
    KeyValue kv;
    while ((kv = mHeap.peek()) != null) {
      // check to see if this is a different row
      if (mComparator.compareRows(row, kv) != 0) {
        // reached next row
        return true;
      }
      results.add(mHeap.next());
    }
    
    close();
    return false;
  }

  @Override
  public boolean next(List<KeyValue> results, int limit) throws IOException {
    // should not use limits with minor compacting store scanner
    return next(results);
  }

  @Override
  public void close() {
    mHeap.close();
  }
}
