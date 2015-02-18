package org.javenstudio.falcon.search.facet;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.PriorityQueue;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.hits.DocSet;
import org.javenstudio.falcon.search.params.FacetParams;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

public class SingleValuedFaceting {

	// input params
	protected Searcher mSearcher;
	protected DocSet mDocs;
	protected String mFieldName;
	protected IFilter mBaseSet;
	
	protected boolean mMissing;
	protected String mSort;
	protected String mPrefix;
	
	protected int mOffset;
	protected int mLimit;
	protected int mMinCount;
	protected int mNumThreads;
	
	public SingleValuedFaceting(Searcher searcher, DocSet docs, 
			String fieldName, int offset, int limit, int mincount, boolean missing, 
			String sort, String prefix) {
		mSearcher = searcher;
		mDocs = docs;
		mFieldName = fieldName;
		mOffset = offset;
		mLimit = limit;
		mMinCount = mincount;
		mMissing = missing;
		mSort = sort;
		mPrefix = prefix;
	}

	public void setNumThreads(int threads) {
		mNumThreads = threads;
	}

	protected NamedList<Integer> getFacetCounts(Executor executor) throws ErrorException {
		try { 
			return doGetFacetCounts(executor);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, ex);
		}
	}
	
	protected NamedList<Integer> doGetFacetCounts(Executor executor) 
			throws IOException, ErrorException {
		CompletionService<SegmentFacet> completionService = 
				new ExecutorCompletionService<SegmentFacet>(executor);

		// reuse the translation logic to go from top level set to per-segment set
		mBaseSet = mDocs.getTopFilter();

		final List<IAtomicReaderRef> leaves = 
				mSearcher.getTopReaderContext().getLeaves();
		
		// The list of pending tasks that aren't immediately submitted
		// TODO: Is there a completion service, or a delegating executor that can
		// limit the number of concurrent tasks submitted to a bigger executor?
		LinkedList<Callable<SegmentFacet>> pending = 
				new LinkedList<Callable<SegmentFacet>>();

		int threads = mNumThreads <= 0 ? Integer.MAX_VALUE : mNumThreads;

		for (final IAtomicReaderRef leave : leaves) {
			final SegmentFacet segFacet = new SegmentFacet(this, leave);

			Callable<SegmentFacet> task = new Callable<SegmentFacet>() {
					public SegmentFacet call() throws Exception {
						segFacet.countTerms();
						return segFacet;
					}
				};

			// TODO: if limiting threads, submit by largest segment first?
			if (--threads >= 0) 
				completionService.submit(task);
			else 
				pending.add(task);
		}

		// now merge the per-segment results
		PriorityQueue<SegmentFacet> queue = new PriorityQueue<SegmentFacet>(leaves.size()) {
				@Override
				protected boolean lessThan(SegmentFacet a, SegmentFacet b) {
					return a.mTempBR.compareTo(b.mTempBR) < 0;
				}
			};

		boolean hasMissingCount = false;
		int missingCount = 0;
		
		for (int i=0, c=leaves.size(); i < c; i++) {
			SegmentFacet seg = null;
			try {
				Future<SegmentFacet> future = completionService.take();        
				seg = future.get();
				if (!pending.isEmpty()) 
					completionService.submit(pending.removeFirst());
				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
				
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException) {
					throw (RuntimeException)cause;
				} else {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Error in per-segment faceting on field: " + mFieldName, cause);
				}
			}

			if (seg.mStartTermIndex < seg.mEndTermIndex) {
				if (seg.mStartTermIndex == 0) {
					hasMissingCount = true;
					missingCount += seg.mCounts[0];
					seg.mPos = 1;
					
				} else {
					seg.mPos = seg.mStartTermIndex;
				}
				
				if (seg.mPos < seg.mEndTermIndex) {
					seg.mTermsEnum = seg.mTermsIndex.getTermsEnum();          
					seg.mTermsEnum.seekExact(seg.mPos);
					seg.mTempBR = seg.mTermsEnum.getTerm();
					
					queue.add(seg);
				}
			}
		}

		FacetCollector collector;
		
		if (mSort.equals(FacetParams.FACET_SORT_COUNT) || 
			mSort.equals(FacetParams.FACET_SORT_COUNT_LEGACY)) {
			collector = new CountSortedCollector(mOffset, mLimit, mMinCount);
			
		} else {
			collector = new IndexSortedCollector(mOffset, mLimit, mMinCount);
		}

		BytesRef val = new BytesRef();
		
		while (queue.size() > 0) {
			SegmentFacet seg = queue.top();

			// make a shallow copy
			val.mBytes = seg.mTempBR.mBytes;
			val.mOffset = seg.mTempBR.mOffset;
			val.mLength = seg.mTempBR.mLength;

			int count = 0;
			do {
				count += seg.mCounts[seg.mPos - seg.mStartTermIndex];

				// TODO: OPTIMIZATION...
				// if mincount>0 then seg.pos++ can skip ahead to the next non-zero entry.
				seg.mPos ++;
				
				if (seg.mPos >= seg.mEndTermIndex) {
					queue.pop();
					seg = queue.top();
					
				}  else {
					seg.mTempBR = seg.mTermsEnum.next();
					seg = queue.updateTop();
				}
			} while (seg != null && val.compareTo(seg.mTempBR) == 0);

			boolean stop = collector.collect(val, count);
			if (stop) break;
		}

		NamedList<Integer> res = collector.getFacetCounts();

		// convert labels to readable form    
		SchemaFieldType ft = mSearcher.getSchema().getFieldType(mFieldName);
		int sz = res.size();
		
		for (int i=0; i < sz; i++) {
			res.setName(i, ft.indexedToReadable(res.getName(i)));
		}

		if (mMissing) {
			if (!hasMissingCount) 
				missingCount = FacetHelper.getFieldMissingCount(mSearcher, mDocs, mFieldName);
			
			res.add(null, missingCount);
		}

		return res;
	}

}
