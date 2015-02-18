package org.javenstudio.raptor.bigdb.regionserver;

import org.javenstudio.raptor.bigdb.KeyValue;
import org.javenstudio.raptor.bigdb.KeyValue.KVComparator;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Implements a heap merge across any number of KeyValueScanners.
 * <p>
 * Implements KeyValueScanner itself.
 * <p>
 * This class is used at the Region level to merge across Stores
 * and at the Store level to merge across the memstore and StoreFiles.
 * <p>
 * In the Region case, we also need InternalScanner.next(List), so this class
 * also implements InternalScanner.  WARNING: As is, if you try to use this
 * as an InternalScanner at the Store level, you will get runtime exceptions.
 */
public class KeyValueHeap implements KeyValueScanner, InternalScanner {
  private PriorityQueue<KeyValueScanner> heap = null;
  private KeyValueScanner current = null;
  private KVScannerComparator comparator;

  /**
   * Constructor.  This KeyValueHeap will handle closing of passed in
   * KeyValueScanners.
   * @param scanners
   * @param comparator
   */
  public KeyValueHeap(List<? extends KeyValueScanner> scanners,
      KVComparator comparator) {
    this.comparator = new KVScannerComparator(comparator);
    if (!scanners.isEmpty()) {
      this.heap = new PriorityQueue<KeyValueScanner>(scanners.size(),
          this.comparator);
      for (KeyValueScanner scanner : scanners) {
        if (scanner.peek() != null) {
          this.heap.add(scanner);
        } else {
          scanner.close();
        }
      }
      this.current = heap.poll();
    }
  }

  public KeyValue peek() {
    if (this.current == null) {
      return null;
    }
    return this.current.peek();
  }

  public KeyValue next()  throws IOException {
    if(this.current == null) {
      return null;
    }
    KeyValue kvReturn = this.current.next();
    KeyValue kvNext = this.current.peek();
    if (kvNext == null) {
      this.current.close();
      this.current = this.heap.poll();
    } else {
      KeyValueScanner topScanner = this.heap.peek();
      if (topScanner == null ||
          this.comparator.compare(kvNext, topScanner.peek()) > 0) {
        this.heap.add(this.current);
        this.current = this.heap.poll();
      }
    }
    return kvReturn;
  }

  /**
   * Gets the next row of keys from the top-most scanner.
   * <p>
   * This method takes care of updating the heap.
   * <p>
   * This can ONLY be called when you are using Scanners that implement
   * InternalScanner as well as KeyValueScanner (a {@link StoreScanner}).
   * @param result
   * @param limit
   * @return true if there are more keys, false if all scanners are done
   */
  public boolean next(List<KeyValue> result, int limit) throws IOException {
    if (this.current == null) {
      return false;
    }
    InternalScanner currentAsInternal = (InternalScanner)this.current;
    boolean mayContainsMoreRows = currentAsInternal.next(result, limit);
    KeyValue pee = this.current.peek();
    /*
     * By definition, any InternalScanner must return false only when it has no
     * further rows to be fetched. So, we can close a scanner if it returns
     * false. All existing implementations seem to be fine with this. It is much
     * more efficient to close scanners which are not needed than keep them in
     * the heap. This is also required for certain optimizations.
     */
    if (pee == null || !mayContainsMoreRows) {
      this.current.close();
    } else {
      this.heap.add(this.current);
    }
    this.current = this.heap.poll();
    return (this.current != null);
  }

  /**
   * Gets the next row of keys from the top-most scanner.
   * <p>
   * This method takes care of updating the heap.
   * <p>
   * This can ONLY be called when you are using Scanners that implement
   * InternalScanner as well as KeyValueScanner (a {@link StoreScanner}).
   * @param result
   * @return true if there are more keys, false if all scanners are done
   */
  public boolean next(List<KeyValue> result) throws IOException {
    return next(result, -1);
  }

  private static class KVScannerComparator implements Comparator<KeyValueScanner> {
    private KVComparator kvComparator;
    /**
     * Constructor
     * @param kvComparator
     */
    public KVScannerComparator(KVComparator kvComparator) {
      this.kvComparator = kvComparator;
    }
    public int compare(KeyValueScanner left, KeyValueScanner right) {
      return compare(left.peek(), right.peek());
    }
    /**
     * Compares two KeyValue
     * @param left
     * @param right
     * @return less than 0 if left is smaller, 0 if equal etc..
     */
    public int compare(KeyValue left, KeyValue right) {
      return this.kvComparator.compare(left, right);
    }
    /**
     * @return KVComparator
     */
    public KVComparator getComparator() {
      return this.kvComparator;
    }
  }

  public void close() {
    if (this.current != null) {
      this.current.close();
    }
    if (this.heap != null) {
      KeyValueScanner scanner;
      while ((scanner = this.heap.poll()) != null) {
        scanner.close();
      }
    }
  }

  /**
   * Seeks all scanners at or below the specified seek key.  If we earlied-out
   * of a row, we may end up skipping values that were never reached yet.
   * Rather than iterating down, we want to give the opportunity to re-seek.
   * <p>
   * As individual scanners may run past their ends, those scanners are
   * automatically closed and removed from the heap.
   * @param seekKey KeyValue to seek at or after
   * @return true if KeyValues exist at or after specified key, false if not
   * @throws IOException
   */
  public boolean seek(KeyValue seekKey) throws IOException {
    if (this.current == null) {
      return false;
    }
    this.heap.add(this.current);
    this.current = null;

    KeyValueScanner scanner;
    while((scanner = this.heap.poll()) != null) {
      KeyValue topKey = scanner.peek();
      if(comparator.getComparator().compare(seekKey, topKey) <= 0) { // Correct?
        // Top KeyValue is at-or-after Seek KeyValue
        this.current = scanner;
        return true;
      }
      if(!scanner.seek(seekKey)) {
        scanner.close();
      } else {
        this.heap.add(scanner);
      }
    }
    // Heap is returning empty, scanner is done
    return false;
  }

  public boolean reseek(KeyValue seekKey) throws IOException {
    //This function is very identical to the seek(KeyValue) function except that
    //scanner.seek(seekKey) is changed to scanner.reseek(seekKey)
    if (this.current == null) {
      return false;
    }
    this.heap.add(this.current);
    this.current = null;

    KeyValueScanner scanner;
    while ((scanner = this.heap.poll()) != null) {
      KeyValue topKey = scanner.peek();
      if (comparator.getComparator().compare(seekKey, topKey) <= 0) {
        // Top KeyValue is at-or-after Seek KeyValue
        this.current = scanner;
        return true;
      }
      if (!scanner.reseek(seekKey)) {
        scanner.close();
      } else {
        this.heap.add(scanner);
      }
    }
    // Heap is returning empty, scanner is done
    return false;
  }

  /**
   * @return the current Heap
   */
  public PriorityQueue<KeyValueScanner> getHeap() {
    return this.heap;
  }
}

