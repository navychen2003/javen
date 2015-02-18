package org.javenstudio.hornet.index.segment;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IFieldVisitor;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.common.indexdb.util.BytesRef;

/** 
 * Base class for implementing {@link CompositeReader}s based on an array
 * of sub-readers. The implementing class has to add code for
 * correctly refcounting and closing the sub-readers.
 * 
 * <p>User code will most likely use {@link MultiReader} to build a
 * composite reader on a set of sub-readers (like several
 * {@link DirectoryReader}s).
 * 
 * <p> For efficiency, in this API documents are often referred to via
 * <i>document numbers</i>, non-negative integers which each name a unique
 * document in the index.  These document numbers are ephemeral -- they may change
 * as documents are added to and deleted from an index.  Clients should thus not
 * rely on a given document having the same number between sessions.
 * 
 * <p><a name="thread-safety"></a><p><b>NOTE</b>: {@link
 * IndexReader} instances are completely thread
 * safe, meaning multiple threads can call any of its methods,
 * concurrently.  If your application requires external
 * synchronization, you should <b>not</b> synchronize on the
 * <code>IndexReader</code> instance; use your own
 * (non-Lucene) objects instead.
 * @see MultiReader
 */
abstract class BaseCompositeReader<R extends IndexReader> extends CompositeIndexReader {
	
	private final R[] mSubReaders;
	private final int[] mStarts; 	// 1st docno for each reader
	private final int mMaxDoc;
	private final int mNumDocs;
	private final boolean mHasDeletions;

	/** 
	 * List view solely for {@link #getSequentialSubReaders()},
	 * for effectiveness the array is used internally. 
	 */
	private final List<R> mSubReadersList;

	/**
	 * Constructs a {@code BaseCompositeReader} on the given subReaders.
	 * @param subReaders the wrapped sub-readers. This array is returned by
	 * {@link #getSequentialSubReaders} and used to resolve the correct
	 * subreader for docID-based methods. <b>Please note:</b> This array is <b>not</b>
	 * cloned and not protected for modification, the subclass is responsible 
	 * to do this.
	 */
	protected BaseCompositeReader(R[] subReaders) throws IOException {
		mSubReaders = subReaders;
		mSubReadersList = Collections.unmodifiableList(Arrays.asList(subReaders));
		mStarts = new int[subReaders.length + 1];    // build starts array
		
		int maxDoc = 0, numDocs = 0;
		boolean hasDeletions = false;
		for (int i = 0; i < subReaders.length; i++) {
			mStarts[i] = maxDoc;
			final IndexReader r = subReaders[i];
			maxDoc += r.getMaxDoc(); 	// compute maxDocs
			if (maxDoc < 0) { 			// overflow
				throw new IllegalArgumentException("Too many documents, " + 
						"composite IndexReaders cannot exceed " + Integer.MAX_VALUE);
			}
			numDocs += r.getNumDocs();    	// compute numDocs
			if (r.hasDeletions()) 
				hasDeletions = true;
			r.registerParentReader(this);
		}
		
		mStarts[subReaders.length] = maxDoc;
		mMaxDoc = maxDoc;
		mNumDocs = numDocs;
		mHasDeletions = hasDeletions;
	}

	@Override
	public final IFields getTermVectors(int docID) throws IOException {
		ensureOpen();
		final int i = readerIndex(docID);		// find subreader num
		return mSubReaders[i].getTermVectors(docID - mStarts[i]);	// dispatch to subreader
	}

	@Override
	public final int getNumDocs() {
		// Don't call ensureOpen() here (it could affect performance)
		return mNumDocs;
	}

	@Override
	public final int getMaxDoc() {
		// Don't call ensureOpen() here (it could affect performance)
		return mMaxDoc;
	}

	@Override
	public final void document(int docID, IFieldVisitor visitor) 
			throws CorruptIndexException, IOException {
		ensureOpen();
		final int i = readerIndex(docID);                  		// find subreader num
		mSubReaders[i].document(docID - mStarts[i], visitor); 	// dispatch to subreader
	}

	@Override
	public final boolean hasDeletions() {
		// Don't call ensureOpen() here (it could affect performance)
		return mHasDeletions;
	}

	@Override
	public final int getDocFreq(String field, BytesRef t) throws IOException {
		ensureOpen();
		int total = 0;          // sum freqs in subreaders
		for (int i = 0; i < mSubReaders.length; i++) {
			total += mSubReaders[i].getDocFreq(field, t);
		}
		return total;
	}

	@Override
	public final long getTotalTermFreq(ITerm term) throws IOException {
		ensureOpen();
		long total = 0;        // sum freqs in subreaders
		for (int i = 0; i < mSubReaders.length; i++) {
			long sub = mSubReaders[i].getTotalTermFreq(term);
			if (sub == -1) 
				return -1;
			
			total += sub;
		}
		return total;
	}
	
	/** Helper method for subclasses to get the corresponding reader for a doc ID */
	protected final int readerIndex(int docID) {
		if (docID < 0 || docID >= mMaxDoc) {
			throw new IllegalArgumentException("docID must be >= 0 and < maxDoc=" + 
					mMaxDoc + " (got docID=" + docID + ")");
		}
		return ReaderUtil.subIndex(docID, mStarts);
	}
  
	/** Helper method for subclasses to get the docBase of the given sub-reader index. */
	protected final int readerBase(int readerIndex) {
		if (readerIndex < 0 || readerIndex >= mSubReaders.length) {
			throw new IllegalArgumentException("readerIndex must be >= 0 " + 
					"and < getSequentialSubReaders().size()");
		}
		return mStarts[readerIndex];
	}
  
	@Override
	public final List<? extends R> getSequentialSubReaders() {
		return mSubReadersList;
	}
	
}
