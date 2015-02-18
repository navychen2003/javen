package org.javenstudio.falcon.datum.table.store;

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
  //private static final Logger LOG = Logger.getLogger(KeyValueHeap.class);
  
  private PriorityQueue<KeyValueScanner> mHeap = null;
  private KeyValueScanner mCurrent = null;
  private KVScannerComparator mComparator;

  /**
   * Constructor.  This KeyValueHeap will handle closing of passed in
   * KeyValueScanners.
   * @param scanners
   * @param comparator
   */
  public KeyValueHeap(List<? extends KeyValueScanner> scanners,
      KeyValue.KVComparator comparator) {
    this.mComparator = new KVScannerComparator(comparator);
    if (!scanners.isEmpty()) {
      this.mHeap = new PriorityQueue<KeyValueScanner>(scanners.size(),
          this.mComparator);
      
      for (KeyValueScanner scanner : scanners) {
        if (scanner.peek() != null) {
          this.mHeap.add(scanner);
        } else {
          scanner.close();
        }
      }
      this.mCurrent = mHeap.poll();
    }
  }

  @Override
  public KeyValue peek() {
    if (this.mCurrent == null) 
      return null;
    
    return this.mCurrent.peek();
  }

  @Override
  public KeyValue next()  throws IOException {
    if (this.mCurrent == null) 
      return null;
    
    KeyValue kvReturn = this.mCurrent.next();
    KeyValue kvNext = this.mCurrent.peek();
    
    if (kvNext == null) {
      this.mCurrent.close();
      this.mCurrent = this.mHeap.poll();
      
    } else {
      KeyValueScanner topScanner = this.mHeap.peek();
      if (topScanner == null ||
          this.mComparator.compare(kvNext, topScanner.peek()) > 0) {
        this.mHeap.add(this.mCurrent);
        this.mCurrent = this.mHeap.poll();
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
  @Override
  public boolean next(List<KeyValue> result, int limit) throws IOException {
    if (this.mCurrent == null) 
      return false;
    
    InternalScanner currentAsInternal = (InternalScanner)this.mCurrent;
    boolean mayContainsMoreRows = currentAsInternal.next(result, limit);
    KeyValue pee = this.mCurrent.peek();
    
    /*
     * By definition, any InternalScanner must return false only when it has no
     * further rows to be fetched. So, we can close a scanner if it returns
     * false. All existing implementations seem to be fine with this. It is much
     * more efficient to close scanners which are not needed than keep them in
     * the heap. This is also required for certain optimizations.
     */
    if (pee == null || !mayContainsMoreRows) {
      this.mCurrent.close();
    } else {
      this.mHeap.add(this.mCurrent);
    }
    
    this.mCurrent = this.mHeap.poll();
    return (this.mCurrent != null);
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
  @Override
  public boolean next(List<KeyValue> result) throws IOException {
    return next(result, -1);
  }

  private static class KVScannerComparator implements Comparator<KeyValueScanner> {
    private KeyValue.KVComparator mKvComparator;
    
    /**
     * Constructor
     * @param kvComparator
     */
    public KVScannerComparator(KeyValue.KVComparator kvComparator) {
      this.mKvComparator = kvComparator;
    }
    
    @Override
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
      return this.mKvComparator.compare(left, right);
    }
    
    /**
     * @return KVComparator
     */
	public KeyValue.KVComparator getComparator() {
      return this.mKvComparator;
    }
  }

  @Override
  public void close() {
    if (this.mCurrent != null) {
      this.mCurrent.close();
    }
    if (this.mHeap != null) {
      KeyValueScanner scanner;
      while ((scanner = this.mHeap.poll()) != null) {
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
    if (this.mCurrent == null) 
      return false;
    
    this.mHeap.add(this.mCurrent);
    this.mCurrent = null;

    KeyValueScanner scanner;
    while ((scanner = this.mHeap.poll()) != null) {
      KeyValue topKey = scanner.peek();
      if (mComparator.getComparator().compare(seekKey, topKey) <= 0) { // Correct?
        // Top KeyValue is at-or-after Seek KeyValue
        this.mCurrent = scanner;
        return true;
      }
      if(!scanner.seek(seekKey)) {
        scanner.close();
      } else {
        this.mHeap.add(scanner);
      }
    }
    
    // Heap is returning empty, scanner is done
    return false;
  }

  @Override
  public boolean reseek(KeyValue seekKey) throws IOException {
    //This function is very identical to the seek(KeyValue) function except that
    //scanner.seek(seekKey) is changed to scanner.reseek(seekKey)
    if (this.mCurrent == null) 
      return false;
    
    this.mHeap.add(this.mCurrent);
    this.mCurrent = null;

    KeyValueScanner scanner;
    while ((scanner = this.mHeap.poll()) != null) {
      KeyValue topKey = scanner.peek();
      if (mComparator.getComparator().compare(seekKey, topKey) <= 0) {
        // Top KeyValue is at-or-after Seek KeyValue
        this.mCurrent = scanner;
        return true;
      }
      if (!scanner.reseek(seekKey)) {
        scanner.close();
      } else {
        this.mHeap.add(scanner);
      }
    }
    
    // Heap is returning empty, scanner is done
    return false;
  }

  /**
   * @return the current Heap
   */
  public PriorityQueue<KeyValueScanner> getHeap() {
    return this.mHeap;
  }
}
