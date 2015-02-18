package org.javenstudio.hornet.search.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Collector;

/**
 * A {@link Collector} which allows running a search with several
 * {@link Collector}s. It offers a static {@link #wrap} method which accepts a
 * list of collectors and wraps them with {@link MultiCollector}, while
 * filtering out the <code>null</code> null ones.
 */
public class MultiCollector extends Collector {

	/**
	 * Wraps a list of {@link Collector}s with a {@link MultiCollector}. This
	 * method works as follows:
	 * <ul>
	 * <li>Filters out the <code>null</code> collectors, so they are not used
	 * during search time.
	 * <li>If the input contains 1 real collector (i.e. non-<code>null</code> ),
	 * it is returned.
	 * <li>Otherwise the method returns a {@link MultiCollector} which wraps the
	 * non-<code>null</code> ones.
	 * </ul>
	 * 
	 * @throws IllegalArgumentException
	 *           if either 0 collectors were input, or all collectors are
	 *           <code>null</code>.
	 */
	public static ICollector wrap(ICollector... collectors) {
		// For the user's convenience, we allow null collectors to be passed.
		// However, to improve performance, these null collectors are found
		// and dropped from the array we save for actual collection time.
		int n = 0;
		for (ICollector c : collectors) {
			if (c != null) 
				n++;
		}

		if (n == 0) {
			throw new IllegalArgumentException("At least 1 collector must not be null");
			
		} else if (n == 1) {
			// only 1 Collector - return it.
			ICollector col = null;
			
			for (ICollector c : collectors) {
				if (c != null) {
					col = c;
					break;
				}
			}
			
			return col;
			
		} else if (n == collectors.length) {
			return new MultiCollector(collectors);
			
		} else {
			ICollector[] colls = new Collector[n];
			
			n = 0;
			for (ICollector c : collectors) {
				if (c != null) 
					colls[n++] = c;
			}
			
			return new MultiCollector(colls);
		}
	}
  
	private final ICollector[] mCollectors;

	private MultiCollector(ICollector... collectors) {
		mCollectors = collectors;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		for (ICollector c : mCollectors) {
			if (!c.acceptsDocsOutOfOrder()) 
				return false;
		}
		return true;
	}

	@Override
	public void collect(int doc) throws IOException {
		for (ICollector c : mCollectors) {
			c.collect(doc);
		}
	}

	@Override
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		for (ICollector c : mCollectors) {
			c.setNextReader(context);
		}
	}

	@Override
	public void setScorer(IScorer s) throws IOException {
		for (ICollector c : mCollectors) {
			c.setScorer(s);
		}
	}

}
