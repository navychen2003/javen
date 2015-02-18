package org.javenstudio.falcon.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Chain several Iterators, so that this iterates
 *  over all of them in sequence.
 */

public class IteratorChain<E> implements Iterator<E> {

  private final List<Iterator<E>> iterators = new ArrayList<Iterator<E>>();
  private Iterator<Iterator<E>> itit;
  private Iterator<E> current;
 
  public void addIterator(Iterator<E> it) {
    if(itit!=null) throw new RuntimeException("all Iterators must be added before calling hasNext()");
    iterators.add(it);
  }
  
  public boolean hasNext() {
    if(itit==null) itit = iterators.iterator();
    return recursiveHasNext();
  }
  
  /** test if current iterator hasNext(), and if not try the next
   *  one in sequence, recursively
   */
  private boolean recursiveHasNext() {
    // return false if we have no more iterators
    if(current==null) {
      if(itit.hasNext()) {
        current=itit.next();
      } else {
        return false;
      }
    }
    
    boolean result = current.hasNext();
    if(!result) {
      current = null;
      result = recursiveHasNext();
    }
    
    return result;
  }

  /** hasNext() must ALWAYS be called before calling this
   *  otherwise it's a bit hard to keep track of what's happening
   */
  public E next() {
    if(current==null) { 
      throw new RuntimeException("For an IteratorChain, hasNext() MUST be called before calling next()");
    }
    return current.next();
  }

  public void remove() {
    // we just need this class 
    // to iterate in readonly mode
    throw new UnsupportedOperationException();
  }
  
}
