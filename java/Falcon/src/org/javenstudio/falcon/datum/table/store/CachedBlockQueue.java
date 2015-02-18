package org.javenstudio.falcon.datum.table.store;

import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * A memory-bound queue that will grow until an element brings
 * total size >= maxSize.  From then on, only entries that are sorted larger
 * than the smallest current entry will be inserted/replaced.
 *
 * <p>Use this when you want to find the largest elements (according to their
 * ordering, not their heap size) that consume as close to the specified
 * maxSize as possible.  Default behavior is to grow just above rather than
 * just below specified max.
 *
 * <p>Object used in this queue must implement {@link HeapSize} as well as
 * {@link Comparable}.
 */
public class CachedBlockQueue implements HeapSize {

  private PriorityQueue<CachedBlock> mQueue;
  private long mHeapSize;
  private long mMaxSize;

  /**
   * @param maxSize the target size of elements in the queue
   * @param blockSize expected average size of blocks
   */
  public CachedBlockQueue(long maxSize, long blockSize) {
    int initialSize = (int)(maxSize / blockSize);
    if (initialSize == 0) initialSize++;
    this.mQueue = new PriorityQueue<CachedBlock>(initialSize);
    this.mHeapSize = 0;
    this.mMaxSize = maxSize;
  }

  /**
   * Attempt to add the specified cached block to this queue.
   *
   * <p>If the queue is smaller than the max size, or if the specified element
   * is ordered before the smallest element in the queue, the element will be
   * added to the queue.  Otherwise, there is no side effect of this call.
   * @param cb block to try to add to the queue
   */
  public void add(CachedBlock cb) {
    if (mHeapSize < mMaxSize) {
      mQueue.add(cb);
      mHeapSize += cb.heapSize();
      
    } else {
      CachedBlock head = mQueue.peek();
      if (cb.compareTo(head) > 0) {
        mHeapSize += cb.heapSize();
        mHeapSize -= head.heapSize();
        
        if (mHeapSize > mMaxSize) {
          mQueue.poll();
        } else {
          mHeapSize += head.heapSize();
        }
        
        mQueue.add(cb);
      }
    }
  }

  /**
   * @return a sorted List of all elements in this queue, in descending order
   */
  public LinkedList<CachedBlock> get() {
    LinkedList<CachedBlock> blocks = new LinkedList<CachedBlock>();
    while (!mQueue.isEmpty()) {
      blocks.addFirst(mQueue.poll());
    }
    return blocks;
  }

  /**
   * Total size of all elements in this queue.
   * @return size of all elements currently in queue, in bytes
   */
  @Override
  public long heapSize() {
    return mHeapSize;
  }
}
