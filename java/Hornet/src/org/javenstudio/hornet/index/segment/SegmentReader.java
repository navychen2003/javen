package org.javenstudio.hornet.index.segment;

import java.io.IOException;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldVisitor;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.ISegmentClosedListener;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.index.field.FieldInfos;
import org.javenstudio.common.indexdb.index.segment.SegmentCommitInfo;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.codec.FieldsReader;
import org.javenstudio.hornet.codec.IndexFormat;
import org.javenstudio.hornet.codec.TermVectorsReader;

/**
 * IndexReader implementation over a single segment. 
 * <p>
 * Instances pointing to the same segment (but with different deletes, etc)
 * may share the same core data.
 * 
 */
public final class SegmentReader extends AtomicIndexReader implements ISegmentReader {

	private final SegmentCommitInfo mSegmentInfo;
	private final Bits mLiveDocs;
	private final SegReaders mReaders;

	// Normally set to si.docCount - si.delDocCount, unless we
	// were created as an NRT reader from IW, in which case IW
	// tells us the docCount:
	private final int mNumDocs;
	
	/**
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public SegmentReader(IndexFormat format, SegmentCommitInfo si) 
			throws IOException {
		mSegmentInfo = si;
		mReaders = new SegReaders(this, si.getSegmentInfo().getDirectory(), 
				format, si);
		
		boolean success = false;
		try {
			if (si.hasDeletions()) {
				// NOTE: the bitvector is stored using the regular directory, not cfs
				mLiveDocs = format.getLiveDocsFormat().readLiveDocs(
						si.getSegmentInfo().getDirectory(), si); 
			} else {
				assert si.getDelCount() == 0;
				mLiveDocs = null;
			}
			
			mNumDocs = si.getSegmentInfo().getDocCount() - si.getDelCount();
			
			success = true;
		} finally {
			// With lock-less commits, it's entirely possible (and
			// fine) to hit a FileNotFound exception above.  In
			// this case, we want to explicitly close any subset
			// of things that were opened so that we don't have to
			// wait for a GC to do so.
			if (!success) 
				mReaders.decreaseRef();
		}
	}

	// Create new SegmentReader sharing core from a previous
	// SegmentReader and loading new live docs from a new
	// deletes file.  Used by openIfChanged.
	public SegmentReader(IndexFormat format, SegmentCommitInfo si, SegReaders files) 
			throws IOException {
		this(si, files, format.getLiveDocsFormat().readLiveDocs(si.getSegmentInfo().getDirectory(), si),
				si.getSegmentInfo().getDocCount() - si.getDelCount());
	}

	// Create new SegmentReader sharing core from a previous
	// SegmentReader and using the provided in-memory
	// liveDocs.  Used by IndexWriter to provide a new NRT
	// reader:
	public SegmentReader(SegmentCommitInfo si, SegReaders files, Bits liveDocs, int numDocs) 
			throws IOException {
		mSegmentInfo = si;
		mReaders = files;
		files.increaseRef();

		assert liveDocs != null;
		mLiveDocs = liveDocs;
		mNumDocs = numDocs;
	}

	public final SegReaders getReaders() { 
		return mReaders; 
	}
	
	@Override
	public IIndexContext getContext() { 
		return mReaders.getContext();
	}
	
	public IDirectory getDirectory() { 
		return mReaders.getDirectory();
	}
	
	@Override
	public Bits getLiveDocs() {
		ensureOpen();
		return mLiveDocs;
	}

	@Override
	protected void doClose() throws IOException {
		mReaders.decreaseRef();
	}

	@Override
	public boolean hasDeletions() {
		// Don't call ensureOpen() here (it could affect performance)
		return mLiveDocs != null;
	}

	@Override
	public FieldInfos getFieldInfos() {
		ensureOpen();
		return mReaders.getFieldInfos();
	}

	@Override
	public FieldsReader getFieldsReader() {
		ensureOpen();
		return mReaders.getFieldsReader();
	}
  
	@Override
	public void document(int docID, IFieldVisitor visitor) 
			throws CorruptIndexException, IOException {
		if (docID < 0 || docID >= getMaxDoc()) {    
			throw new IllegalArgumentException("docID must be >= 0 and < maxDoc=" + 
					getMaxDoc() + " (got docID=" + docID + ")");
		}
		getFieldsReader().visitDocument(docID, visitor);
	}

	@Override
	public IFields getFields() throws IOException {
		ensureOpen();
		return mReaders.getFieldsProducer();
	}

	@Override
	public int getNumDocs() {
		// Don't call ensureOpen() here (it could affect performance)
		return mNumDocs;
	}

	@Override
	public int getMaxDoc() {
		// Don't call ensureOpen() here (it could affect performance)
		return mSegmentInfo.getSegmentInfo().getDocCount();
	}

	@Override
	public TermVectorsReader getTermVectorsReader() {
		ensureOpen();
		return mReaders.getTermVectorsReader();
	}

	/** 
	 * Return a term frequency vector for the specified document and field. The
	 *  vector returned contains term numbers and frequencies for all terms in
	 *  the specified field of this document, if the field had storeTermVector
	 *  flag set.  If the flag was not set, the method returns null.
	 * @throws IOException
	 */
	@Override
	public IFields getTermVectors(int docID) throws IOException {
		TermVectorsReader termVectorsReader = getTermVectorsReader();
		if (termVectorsReader == null) 
			return null;
		
		return termVectorsReader.getFields(docID);
	}

	@Override
	public String toString() {
		// SegmentInfo.toString takes dir and number of
		// *pending* deletions; so we reverse compute that here:
		return mSegmentInfo.toString(mSegmentInfo.getSegmentInfo().getDirectory(), 
				mSegmentInfo.getSegmentInfo().getDocCount() - mNumDocs - mSegmentInfo.getDelCount());
	}
  
	/**
	 * Return the name of the segment this reader is reading.
	 */
	public String getSegmentName() {
		return mSegmentInfo.getSegmentInfo().getName();
	}
  
	/**
	 * Return the SegmentInfoPerCommit of the segment this reader is reading.
	 */
	public SegmentCommitInfo getCommitInfo() {
		return mSegmentInfo;
	}

	/** Returns the directory this index resides in. */
	public IDirectory directory() {
		// Don't ensureOpen here -- in certain cases, when a
		// cloned/reopened reader needs to commit, it may call
		// this method on the closed original reader
		return mSegmentInfo.getSegmentInfo().getDirectory();
	}

	// This is necessary so that cloned SegmentReaders (which
	// share the underlying postings data) will map to the
	// same entry in the FieldCache.  See LUCENE-1579.
	@Override
	public Object getCacheKey() {
		return mReaders;
	}

	@Override
	public Object getCombinedCoreAndDeletesKey() {
		return this;
	}
  
	/** Expert: adds a CoreClosedListener to this reader's shared core */
	@Override
	public void addSegmentClosedListener(ISegmentClosedListener listener) {
		ensureOpen();
		mReaders.addClosedListener(listener);
	}
  
	/** Expert: removes a CoreClosedListener from this reader's shared core */
	@Override
	public void removeSegmentClosedListener(ISegmentClosedListener listener) {
		ensureOpen();
		mReaders.removeClosedListener(listener);
	}

}
