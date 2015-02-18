package org.javenstudio.hornet.index;

import java.util.Iterator;
import java.util.Map;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.index.BufferedDeletes;
import org.javenstudio.common.indexdb.index.DeletesStream;
import org.javenstudio.common.indexdb.index.FrozenDeletes;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.JvmUtil;

/** 
 * Holds buffered deletes by term or query, once pushed.
 *  Pushed deletes are write-once, so we shift to more
 *  memory efficient data structure to hold them.  We don't
 *  hold docIDs because these are applied on flush. 
 */
final class FrozenBufferedDeletes extends FrozenDeletes {

	/** Query we often undercount (say 24 bytes), plus int. */
	static final int BYTES_PER_DEL_QUERY = JvmUtil.NUM_BYTES_OBJECT_REF + JvmUtil.NUM_BYTES_INT + 24;

	// Terms, in sorted order:
	private final PrefixCodedTerms mTerms;
	private int mTermCount; // just for debugging

	// Parallel array of deleted query, and the docIDUpto for each
	private final IQuery[] mQueries;
	private final int[] mQueryLimits;
	private final int mBytesUsed;
	private final int mNumTermDeletes;
	
	private long mGen = -1; // assigned by BufferedDeletesStream once pushed
	
	// set to true if this frozen packet represents
	// a segment private deletes. in that case is should
	// only have Queries 
	private final boolean mIsSegmentPrivate;   

	public FrozenBufferedDeletes(IIndexContext context, BufferedDeletes deletes, boolean isSegmentPrivate) {
		mIsSegmentPrivate = isSegmentPrivate;
		assert !isSegmentPrivate || deletes.getTerms().size() == 0 : 
			"segment private package should only have del queries"; 
		
		ITerm termsArray[] = deletes.getTerms().keySet().toArray(new ITerm[deletes.getTerms().size()]);
		mTermCount = termsArray.length;
		ArrayUtil.mergeSort(termsArray);
		PrefixCodedTerms.Builder builder = new PrefixCodedTerms.Builder(context);
		for (ITerm term : termsArray) {
			builder.add(term);
		}
		mTerms = builder.finish();
    
		mQueries = new IQuery[deletes.getQueries().size()];
		mQueryLimits = new int[deletes.getQueries().size()];
		int upto = 0;
		
		for (Map.Entry<IQuery,Integer> ent : deletes.getQueries().entrySet()) {
			mQueries[upto] = ent.getKey();
			mQueryLimits[upto] = ent.getValue();
			upto ++;
		}

		mBytesUsed = (int) mTerms.getSizeInBytes() + mQueries.length * BYTES_PER_DEL_QUERY;
		mNumTermDeletes = deletes.getNumTermDeletes().get();
	}
  
	@Override
	public boolean isSegmentPrivate() { return mIsSegmentPrivate; }
	public int getNumTermDeletes() { return mNumTermDeletes; }
	public int getBytesUsed() { return mBytesUsed; }
	
	@Override
	public int getQueryCount() { 
		return mQueries != null ? mQueries.length : 0;
	}
	
	@Override
	public IQuery getQueryAt(int index) { 
		return mQueries[index];
	}
	
	@Override
	public void setDelGen(long gen) {
		assert mGen == -1;
		mGen = gen;
	}
  
	@Override
	public long getDelGen() {
		assert mGen != -1;
		return mGen;
	}

	@Override
	public Iterable<ITerm> termsIterable() {
		return new Iterable<ITerm>() {
				@Override
				public Iterator<ITerm> iterator() {
					return mTerms.iterator();
				}
			};
	}

	@Override
	public Iterable<DeletesStream.QueryAndLimit> queriesIterable() {
		return new Iterable<DeletesStream.QueryAndLimit>() {
			@Override
			public Iterator<DeletesStream.QueryAndLimit> iterator() {
				return new Iterator<DeletesStream.QueryAndLimit>() {
					private int mUpto;

					@Override
					public boolean hasNext() {
						return mUpto < mQueries.length;
					}

					@Override
					public DeletesStream.QueryAndLimit next() {
						DeletesStream.QueryAndLimit ret = 
								new DeletesStream.QueryAndLimit(mQueries[mUpto], mQueryLimits[mUpto]);
						mUpto ++;
						return ret;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	@Override
	public boolean any() {
		return mTermCount > 0 || mQueries.length > 0;
	}
	
	@Override
	public String toString() {
		StringBuilder sbuf = new StringBuilder();
		
		if (mNumTermDeletes != 0) {
			sbuf.append(" ").append(mNumTermDeletes).append(" deleted terms (unique count=")
				.append(mTermCount).append(")");
		}
		if (mQueries.length != 0) {
			sbuf.append(" ").append(mQueries.length).append(" deleted queries");
		}
		if (mBytesUsed != 0) {
			sbuf.append(" bytesUsed=").append(mBytesUsed);
		}

		return sbuf.toString();
	}
  
}
