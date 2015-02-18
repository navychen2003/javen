package org.javenstudio.raptor.bigdb.regionserver;

import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.KeyValue;

import java.io.IOException;
import java.util.List;

/**
 * A scanner that does a minor compaction at the same time.  Doesn't need to
 * implement ChangedReadersObserver, since it doesn't scan memstore, only store files
 * and optionally the memstore-snapshot.
 */
public class MinorCompactingStoreScanner implements KeyValueScanner, InternalScanner {
  private KeyValueHeap heap;
  private KeyValue.KVComparator comparator;

  MinorCompactingStoreScanner(Store store, List<? extends KeyValueScanner> scanners)
      throws IOException {
    comparator = store.comparator;
    KeyValue firstKv = KeyValue.createFirstOnRow(DBConstants.EMPTY_START_ROW);
    for (KeyValueScanner scanner : scanners ) {
      scanner.seek(firstKv);
    }
    heap = new KeyValueHeap(scanners, store.comparator);
  }

  MinorCompactingStoreScanner(String cfName, KeyValue.KVComparator comparator,
                              List<? extends KeyValueScanner> scanners)
      throws IOException {
    this.comparator = comparator;

    KeyValue firstKv = KeyValue.createFirstOnRow(DBConstants.EMPTY_START_ROW);
    for (KeyValueScanner scanner : scanners ) {
      scanner.seek(firstKv);
    }

    heap = new KeyValueHeap(scanners, comparator);
  }

  public KeyValue peek() {
    return heap.peek();
  }

  public KeyValue next() throws IOException {
    return heap.next();
  }

  @Override
  public boolean seek(KeyValue key) {
    // cant seek.
    throw new UnsupportedOperationException("Can't seek a MinorCompactingStoreScanner");
  }

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
    KeyValue row = heap.peek();
    if (row == null) {
      close();
      return false;
    }
    KeyValue kv;
    while ((kv = heap.peek()) != null) {
      // check to see if this is a different row
      if (comparator.compareRows(row, kv) != 0) {
        // reached next row
        return true;
      }
      writer.append(heap.next());
    }
    close();
    return false;
  }

  @Override
  public boolean next(List<KeyValue> results) throws IOException {
    KeyValue row = heap.peek();
    if (row == null) {
      close();
      return false;
    }
    KeyValue kv;
    while ((kv = heap.peek()) != null) {
      // check to see if this is a different row
      if (comparator.compareRows(row, kv) != 0) {
        // reached next row
        return true;
      }
      results.add(heap.next());
    }
    close();
    return false;
  }

  @Override
  public boolean next(List<KeyValue> results, int limit) throws IOException {
    // should not use limits with minor compacting store scanner
    return next(results);
  }

  public void close() {
    heap.close();
  }
}

