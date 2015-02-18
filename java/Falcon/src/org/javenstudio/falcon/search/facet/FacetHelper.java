package org.javenstudio.falcon.search.facet;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.DefaultThreadFactory;
import org.javenstudio.falcon.util.LongPriorityQueue;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.hornet.search.query.TermRangeQuery;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.hits.DocIterator;
import org.javenstudio.falcon.search.hits.DocSet;
import org.javenstudio.falcon.search.params.FacetParams;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

public class FacetHelper {

	static final Executor sDirectExecutor = new Executor() {
			public void execute(Runnable r) {
				r.run();
			}
		};

	static final Executor sFacetExecutor = new ThreadPoolExecutor(
			0, Integer.MAX_VALUE, 10, TimeUnit.SECONDS, // terminate idle threads after 10 sec
			new SynchronousQueue<Runnable>(),  // directly hand off tasks
			new DefaultThreadFactory("facetExecutor")
		);
	
	/**
	 * Returns a count of the documents in the set which do not have any 
	 * terms for for the specified field.
	 *
	 * @see FacetParams#FACET_MISSING
	 */
	public static int getFieldMissingCount(Searcher searcher, 
			DocSet docs, String fieldName) throws ErrorException {
		DocSet hasVal = searcher.getDocSet(
				new TermRangeQuery(fieldName, null, null, false, false));
		
		return docs.andNotSize(hasVal);
	}

	/**
	 * Use the Indexdb FieldCache to get counts for each unique field value in <code>docs</code>.
	 * The field must have at most one indexed token per document.
	 */
	public static NamedList<Integer> getFieldCacheCounts(Searcher searcher, 
			DocSet docs, String fieldName, int offset, int limit, int mincount, boolean missing, 
			String sort, String prefix) throws ErrorException {
		//
		// TODO: If the number of terms is high compared to docs.size(), and zeros==false,
		//  we should use an alternate strategy to avoid
		//  1) creating another huge int[] for the counts
		//  2) looping over that huge int[] looking for the rare non-zeros.
		//
		// Yet another variation: if docs.size() is small and termvectors are stored,
		// then use them instead of the FieldCache.
		//

		// TODO: this function is too big and could use some refactoring, but
		// we also need a facet cache, and refactoring of SimpleFacets instead of
		// trying to pass all the various params around.

		SchemaFieldType ft = searcher.getSchema().getFieldType(fieldName);
		NamedList<Integer> res = new NamedList<Integer>();

		IDocTermsIndex si = null; 
		try {
			si = FieldCache.DEFAULT.getTermsIndex(searcher.getAtomicReader(), fieldName);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
    
		final BytesRef prefixRef;
		if (prefix == null) {
			prefixRef = null;
		} else if (prefix.length() == 0) {
			prefix = null;
			prefixRef = null;
		} else {
			prefixRef = new BytesRef(prefix);
		}

		final BytesRef br = new BytesRef();
		int startTermIndex, endTermIndex;
		
		if (prefix != null) {
			startTermIndex = si.binarySearch(prefixRef, br);
			if (startTermIndex < 0) 
				startTermIndex = -startTermIndex-1;
			
			prefixRef.append(UnicodeUtil.BIG_TERM);
			
			endTermIndex = si.binarySearch(prefixRef, br);
			assert endTermIndex < 0;
			
			endTermIndex = -endTermIndex-1;
			
		} else {
			startTermIndex = 0;
			endTermIndex = si.getNumOrd();
		}

		final int nTerms=endTermIndex-startTermIndex;
		final CharsRef charsRef = new CharsRef(10);
		int missingCount = -1; 
		
		if (nTerms > 0 && docs.size() >= mincount) {
			// count collection array only needs to be as big as the number of terms we are
			// going to collect counts for.
			final int[] counts = new int[nTerms];

			DocIterator iter = docs.iterator();
			IIntsReader ordReader = si.getDocToOrd();
			
			final Object arr;
			if (ordReader.hasArray()) 
				arr = ordReader.getArray();
			else 
				arr = null;

			if (arr instanceof int[]) {
				int[] ords = (int[]) arr;
				if (prefix == null) {
					while (iter.hasNext()) {
						counts[ords[iter.nextDoc()]]++;
					}
					
				} else {
					while (iter.hasNext()) {
						int term = ords[iter.nextDoc()];
						int arrIdx = term-startTermIndex;
						
						if (arrIdx >= 0 && arrIdx<nTerms) 
							counts[arrIdx]++;
					}
				}
				
			} else if (arr instanceof short[]) {
				short[] ords = (short[]) arr;
				if (prefix == null) {
					while (iter.hasNext()) {
						counts[ords[iter.nextDoc()] & 0xffff]++;
					}
					
				} else {
					while (iter.hasNext()) {
						int term = ords[iter.nextDoc()] & 0xffff;
						int arrIdx = term-startTermIndex;
						
						if (arrIdx >= 0 && arrIdx < nTerms) 
							counts[arrIdx]++;
					}
				}
				
			} else if (arr instanceof byte[]) {
				byte[] ords = (byte[]) arr;
				if (prefix == null) {
					while (iter.hasNext()) {
						counts[ords[iter.nextDoc()] & 0xff]++;
					}
					
				} else {
					while (iter.hasNext()) {
						int term = ords[iter.nextDoc()] & 0xff;
						int arrIdx = term-startTermIndex;
						
						if (arrIdx >= 0 && arrIdx < nTerms) 
							counts[arrIdx]++;
					}
				}
				
			} else {
				while (iter.hasNext()) {
					int term = si.getOrd(iter.nextDoc());
					int arrIdx = term-startTermIndex;
					
					if (arrIdx >= 0 && arrIdx < nTerms) 
						counts[arrIdx]++;
				}
			}

			if (startTermIndex == 0) 
				missingCount = counts[0];

			// IDEA: we could also maintain a count of "other"... everything that fell outside
			// of the top 'N'
			int off = offset;
			int lim = limit >= 0 ? limit : Integer.MAX_VALUE;

			if (sort.equals(FacetParams.FACET_SORT_COUNT) || 
				sort.equals(FacetParams.FACET_SORT_COUNT_LEGACY)) {
				int maxsize = limit>0 ? offset+limit : Integer.MAX_VALUE-1;
				maxsize = Math.min(maxsize, nTerms);
				
				LongPriorityQueue queue = new LongPriorityQueue(
						Math.min(maxsize,1000), maxsize, Long.MIN_VALUE);

				int min = mincount-1;  // the smallest value in the top 'N' values
				
				for (int i = (startTermIndex==0)?1:0; i < nTerms; i++) {
					int c = counts[i];
					
					if (c > min) {
						// NOTE: we use c>min rather than c>=min as an optimization because we are going in
						// index order, so we already know that the keys are ordered.  This can be very
						// important if a lot of the counts are repeated (like zero counts would be).

						// smaller term numbers sort higher, so subtract the term number instead
						long pair = (((long)c)<<32) + (Integer.MAX_VALUE - i);
						boolean displaced = queue.insert(pair);
						
						if (displaced) 
							min = (int)(queue.top() >>> 32);
					}
				}

				// if we are deep paging, we don't have to order the highest "offset" counts.
				int collectCount = Math.max(0, queue.size() - off);
				assert collectCount <= lim;

				// the start and end indexes of our list "sorted" (starting with the highest value)
				int sortedIdxStart = queue.size() - (collectCount - 1);
				int sortedIdxEnd = queue.size() + 1;
				
				final long[] sorted = queue.sort(collectCount);

				for (int i = sortedIdxStart; i < sortedIdxEnd; i++) {
					long pair = sorted[i];
					int c = (int)(pair >>> 32);
					int tnum = Integer.MAX_VALUE - (int)pair;
					
					ft.indexedToReadable(si.lookup(startTermIndex+tnum, br), charsRef);
					res.add(charsRef.toString(), c);
				}
      
			} else {
				// add results in index order
				int i = (startTermIndex == 0) ? 1 : 0;
				
				if (mincount <= 0) {
					// if mincount<=0, then we won't discard any terms and we know exactly
					// where to start.
					i += off;
					off = 0;
				}

				for (; i < nTerms; i++) {          
					int c = counts[i];
					if (c < mincount || --off >= 0) continue;
					if (--lim < 0) break;
					
					ft.indexedToReadable(si.lookup(startTermIndex+i, br), charsRef);
					res.add(charsRef.toString(), c);
				}
			}
		}

		if (missing) {
			if (missingCount < 0) 
				missingCount = getFieldMissingCount(searcher, docs, fieldName);
			
			res.add(null, missingCount);
		}
    
		return res;
	}

}
