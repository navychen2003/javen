package org.javenstudio.hornet.index;

import java.io.IOException;
import java.util.HashSet;

import org.javenstudio.common.indexdb.Constants;
import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.index.BufferedDeletes;
import org.javenstudio.common.indexdb.index.DeleteQueue;
import org.javenstudio.common.indexdb.index.DeleteSlice;
import org.javenstudio.common.indexdb.index.DocState;
import org.javenstudio.common.indexdb.index.DocumentWriter;
import org.javenstudio.common.indexdb.index.FlushedSegment;
import org.javenstudio.common.indexdb.index.FrozenDeletes;
import org.javenstudio.common.indexdb.index.IndexingChain;
import org.javenstudio.common.indexdb.index.consumer.DocConsumer;
import org.javenstudio.common.indexdb.index.field.FieldInfosBuilder;
import org.javenstudio.common.indexdb.index.segment.SegmentCommitInfo;
import org.javenstudio.common.indexdb.index.segment.SegmentInfo;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.store.TrackingDirectoryWrapper;
import org.javenstudio.common.indexdb.util.Counter;
import org.javenstudio.common.indexdb.util.BytePool;
import org.javenstudio.common.indexdb.util.IntPool;
import org.javenstudio.common.indexdb.util.StringHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.hornet.codec.IndexFormat;
import org.javenstudio.hornet.codec.SegmentWriteState;

final class DocumentsWriterPerThread extends DocumentWriter {
	private static final Logger LOG = Logger.getLogger(DocumentsWriterPerThread.class);
	
	private final DocumentsWriter mWriter;
	private final TrackingDirectoryWrapper mDirectory;
	private final DocState mDocState;
	private final DocConsumer mConsumer;
	
	private final Counter mBytesUsed;
	private final BytePool.Allocator mByteAllocator;
	private final IntPool.Allocator mIntAllocator;
	
	private SegmentWriteState mFlushState;
	private DeleteQueue mDeleteQueue;
	private DeleteSlice mDeleteSlice;
	//Deletes for our still-in-RAM (to be flushed next) segment
	private BufferedDeletes mPendingDeletes; 
	
	private FieldInfosBuilder mFieldInfos;
	// Current segment we are working on
	private SegmentInfo mSegmentInfo = null;
	// True if an abort is pending
	private boolean mAborting = false;
	// True if the last exception throws by #updateDocument was aborting
	private boolean mHasAborted = false;
	private int mNumDocsInRAM = 0;
	private int mFlushedDocCount = 0;
	
	public DocumentsWriterPerThread(DocumentsWriter docWriter, 
			FieldInfosBuilder fieldInfos, IndexingChain indexChain) { 
		mWriter = docWriter;
		mDirectory = new TrackingDirectoryWrapper(docWriter.getIndexWriter().getDirectory());
		mFieldInfos = fieldInfos;
		mDocState = new DocState();
		mBytesUsed = Counter.newCounter();
		mPendingDeletes = new BufferedDeletes();
		mByteAllocator = new BytePool.DirectTrackingAllocator(mBytesUsed);
		mIntAllocator = new IntPool.Allocator(mBytesUsed);
		mConsumer = indexChain.createChain(this);
		initialize();
	}
	
	@Override
	public void initialize() {
		mDeleteQueue = mWriter.getDeleteQueue();
	    assert mNumDocsInRAM == 0 : "num docs " + mNumDocsInRAM;
	    mPendingDeletes.clear();
	    mDeleteSlice = null;
	}
	
	public final DocumentsWriter getSegmentWriter() { return mWriter; }
	public final IDirectory getDirectory() { return mDirectory; }
	public final Counter getBytesUsedCounter() { return mBytesUsed; }
	public final BytePool.Allocator getByteAllocator() { return mByteAllocator; }
	public final IntPool.Allocator getIntAllocator() { return mIntAllocator; }
	public final DocState getDocState() { return mDocState; }
	public final FieldInfosBuilder getFieldInfosBuilder() { return mFieldInfos; }
	public final DeleteQueue getDeleteQueue() { return mDeleteQueue; }
	public final DeleteSlice getDeleteSlice() { return mDeleteSlice; }
	
	public final int getFlushedDocCount() { return mFlushedDocCount; }
	
	public final IIndexContext getContext() { 
		return mWriter.getIndexWriter().getContext(); 
	}
	
	public final long getBytesUsed() {
	    return mBytesUsed.get() + mPendingDeletes.getBytesUsed().get();
	}
	
	public boolean checkAndResetHasAborted() {
	    final boolean retval = mHasAborted;
	    mHasAborted = false;
	    return retval;
	}
	
	public void setAborting() {
		mAborting = true;
	}
	
	/**
	 * Returns the number of delete terms in this {@link DocumentsWriterPerThread}
	 */
	public int numDeleteTerms() {
		// public for FlushPolicy
		return mPendingDeletes.getNumTermDeletes().get();
	}
	
	/**
	 * Returns the number of RAM resident documents in this {@link DocumentsWriterPerThread}
	 */
	public int getNumDocsInRAM() {
		// public for FlushPolicy
		return mNumDocsInRAM;
	}
	
	public synchronized final DocConsumer getConsumer() { 
		return mConsumer;
	}
	
	/** Get current segment info we are writing. */
	public final SegmentInfo getSegmentInfo() { 
		return mSegmentInfo;
	}
	
	private void initSegmentInfo() {
		if (mSegmentInfo == null) {
		    String segment = mWriter.getIndexWriter().newSegmentName();
		    
		    mSegmentInfo = new SegmentInfo(
		    		mWriter.getIndexWriter().getDirectory(), 
		    		Constants.INDEXDB_MAIN_VERSION, segment, -1, false, null, null);
		    
		    assert mNumDocsInRAM == 0;
		    
		    if (LOG.isDebugEnabled()) {
		    	LOG.debug(Thread.currentThread().getName() + " init seg=" + segment 
		    			+ " delQueue=" + mDeleteQueue);  
		    }
		}
	}
	
	@Override
	public int updateDocuments(Iterable<? extends IDocument> docs, IAnalyzer analyzer, 
			ITerm delTerm) throws IOException { 
	    assert mDeleteQueue != null;
	    initSegmentInfo();
	    
	    mDocState.setAnalyzer(analyzer);
	    //if (LOG.isDebugEnabled()) {
	    //	LOG.debug(Thread.currentThread().getName() 
	    //			+ " update delTerm=" + delTerm + " docID=" + mDocState.getDocID() 
	    //			+ " seg=" + mSegmentInfo.getName());
	    //}
	    
	    int docCount = 0;
	    try {
	    	for (IDocument doc : docs) {
	    		mDocState.setDocument(mNumDocsInRAM, doc);
	    		docCount++;

	    		boolean success = false;
	    		try {
	    			mConsumer.processDocument(mFieldInfos);
	    			success = true;
	    		} finally {
	    			if (!success) {
	    				// An exc is being thrown...

	    				if (!mAborting) {
	    					// One of the documents hit a non-aborting
	    					// exception (eg something happened during
	    					// analysis).  We now go and mark any docs
	    					// from this batch that we had already indexed
	    					// as deleted:
	    					int docID = mDocState.getDocID();
	    					final int endDocID = docID - docCount;
	    					while (docID > endDocID) {
	    						deleteDocID(docID);
	    						docID--;
	    					}

	    					// Incr here because finishDocument will not
	    					// be called (because an exc is being thrown):
	    					mNumDocsInRAM++;
	    				} else {
	    					abort();
	    				}
	    			}
	    		}
	    		
	    		success = false;
	    		try {
	    			mConsumer.finishDocument();
	    			success = true;
	    		} finally {
	    			if (!success) 
	    				abort();
	    		}

	    		finishDocument(null);
	    	}

	    	// Apply delTerm only after all indexing has
	    	// succeeded, but apply it only to docs prior to when
	    	// this batch started:
	    	if (delTerm != null) {
	    		mDeleteQueue.add(delTerm, mDeleteSlice);
	    		
	    		assert mDeleteSlice.isTailItem(delTerm) : "expected the delete term as the tail item";
	    		mDeleteSlice.apply(mPendingDeletes, mNumDocsInRAM-docCount);
	    	}

	    } finally {
	    	mDocState.clear();
	    }

	    return docCount;
	}
	
	@Override
	public void updateDocument(IDocument doc, IAnalyzer analyzer, 
			ITerm delTerm) throws IOException { 
	    assert mDeleteQueue != null;
	    initSegmentInfo();
	    
	    mDocState.setAnalyzer(analyzer);
	    mDocState.setDocument(mNumDocsInRAM, doc);
	    
	    //if (LOG.isDebugEnabled()) {
	    //	LOG.debug(Thread.currentThread().getName() 
	    //			+ " update delTerm=" + delTerm + " docID=" + mDocState.getDocID() 
	    //			+ " seg=" + mSegmentInfo.getName());
	    //}
	    
	    boolean success = false;
	    try {
	    	try {
	    		mConsumer.processDocument(mFieldInfos);
	    	} finally {
	    		mDocState.clear();
	    	}
	    	success = true;
	    } finally {
	    	if (!success) {
	    		if (!mAborting) {
	    			// mark document as deleted
	    			deleteDocID(mDocState.getDocID());
	    			mNumDocsInRAM ++;
	    		} else {
	    			abort();
	    		}
	    	}
	    }
	    
	    success = false;
	    try {
	    	mConsumer.finishDocument();
	    	success = true;
	    } finally {
	    	if (!success) 
	    		abort();
	    }
	    
	    finishDocument(delTerm);
	}
	
	/** 
	 * Called if we hit an exception at a bad time (when
	 *  updating the index files) and must discard all
	 *  currently buffered docs.  This resets our state,
	 *  discarding any docs added since last flush. 
	 */
	public void abort() {
	    mHasAborted = mAborting = true;
	    try {
	      if (LOG.isDebugEnabled()) 
	    	  LOG.debug("now abort");
	      
	      try {
	        mConsumer.abort();
	      } catch (Throwable t) {
	      }

	      mPendingDeletes.clear();
	      mDeleteSlice = mDeleteQueue.newSlice();
	      // Reset all postings data
	      doAfterFlush();

	    } finally {
	      mAborting = false;
	      
	      if (LOG.isDebugEnabled()) 
	    	  LOG.debug("done abort");
	    }
	}
	
	protected void finishDocument(ITerm delTerm) {
	    /**
	     * here we actually finish the document in two steps 1. push the delete into
	     * the queue and update our slice. 2. increment the DWPT private document
	     * id.
	     * 
	     * the updated slice we get from 1. holds all the deletes that have occurred
	     * since we updated the slice the last time.
	     */
	    if (mDeleteSlice == null) {
	    	mDeleteSlice = mDeleteQueue.newSlice();
	    	if (delTerm != null) {
	    		mDeleteQueue.add((Term)delTerm, mDeleteSlice);
	    		mDeleteSlice.reset();
	    	}
	      
	    } else {
	    	if (delTerm != null) {
	    		mDeleteQueue.add(delTerm, mDeleteSlice);
	    		assert mDeleteSlice.isTailItem(delTerm) : "expected the delete term as the tail item";
	    		mDeleteSlice.apply(mPendingDeletes, mNumDocsInRAM);
	    		
	    	} else if (mDeleteQueue.updateSlice(mDeleteSlice)) {
	    		mDeleteSlice.apply(mPendingDeletes, mNumDocsInRAM);
	    	}
	    }
	    
	    ++ mNumDocsInRAM;
	}

	// Buffer a specific docID for deletion.  Currently only
	// used when we hit a exception when adding a document
	protected void deleteDocID(int docIDUpto) {
	    mPendingDeletes.addDocID(docIDUpto);
	    // NOTE: we do not trigger flush here.  This is
	    // potentially a RAM leak, if you have an app that tries
	    // to add docs but every single doc always hits a
	    // non-aborting exception.  Allowing a flush here gets
	    // very messy because we are only invoked when handling
	    // exceptions so to do this properly, while handling an
	    // exception we'd have to go off and flush new deletes
	    // which is risky (likely would hit some other
	    // confounding exception).
	}
	
	/** Flush all pending docs to a new segment */
	public FlushedSegment flush() throws IOException {
		final long startBytesUsed = mWriter.getFlushControl().getNetBytes();
	    assert mNumDocsInRAM > 0;
	    assert mDeleteSlice == null : "all deletes must be applied in prepareFlush";
	    
	    mSegmentInfo.setDocCount(mNumDocsInRAM);
	    mFlushState = new SegmentWriteState(mSegmentInfo, mFieldInfos.finish(), mPendingDeletes);
	    
	    // Apply delete-by-docID now (delete-byDocID only
	    // happens when an exception is hit processing that
	    // doc, eg if analyzer has some problem w/ the text):
	    if (mPendingDeletes.getDocIDs().size() > 0) {
	    	mFlushState.setLiveDocs(mWriter.getIndexWriter().getIndexFormat()
	    			.getLiveDocsFormat().newLiveDocs(mNumDocsInRAM));
	    	for (int delDocID : mPendingDeletes.getDocIDs()) {
	    		mFlushState.getLiveDocs().clear(delDocID);
	    	}
	    	mFlushState.setDelCountOnFlush(mPendingDeletes.getDocIDs().size());
	    	mPendingDeletes.getBytesUsed().addAndGet(
	    			-mPendingDeletes.getDocIDs().size() * BufferedDeletes.BYTES_PER_DEL_DOCID);
	    	mPendingDeletes.getDocIDs().clear();
	    }

	    if (mAborting) {
	    	if (LOG.isDebugEnabled()) 
	    		LOG.debug("flush: skip because aborting is set");
	    	
	    	return null;
	    }

	    if (LOG.isDebugEnabled()) {
    		LOG.debug("flush postings as segment " + mFlushState.getSegmentInfo().getName() 
    				+ " numDocs=" + mNumDocsInRAM);
	    }

	    boolean success = false;
	    try {
	    	mConsumer.flush(mFlushState);
	    	mPendingDeletes.getTerms().clear();
	    	mSegmentInfo.setFileNames(new HashSet<String>(mDirectory.getCreatedFiles()));

	    	final SegmentCommitInfo commitInfo = new SegmentCommitInfo(
	    			(IndexFormat)mWriter.getIndexWriter().getIndexFormat(), mSegmentInfo, 0, -1L);
	    	
	    	if (LOG.isDebugEnabled()) {
	    		LOG.debug("new segment has " + (mFlushState.getLiveDocs() == null ? 
	    				0 : (mFlushState.getSegmentInfo().getDocCount() - mFlushState.getDelCountOnFlush())) 
	    				+ " deleted docs");
	    		LOG.debug("new segment has " +
	    				(mFlushState.getFieldInfos().hasVectors() ? "vectors" : "no vectors") + "; " +
	    				(mFlushState.getFieldInfos().hasNorms() ? "norms" : "no norms") + "; " + 
	    				(mFlushState.getFieldInfos().hasProx() ? "prox" : "no prox") + "; " + 
	    				(mFlushState.getFieldInfos().hasFreq() ? "freqs" : "no freqs"));
	    		LOG.debug("flushed files: " + commitInfo.getFileNames());
	    	}

	    	mFlushedDocCount += mFlushState.getSegmentInfo().getDocCount();

	    	final BufferedDeletes segmentDeletes;
	    	if (mPendingDeletes.getQueries().isEmpty()) {
	    		mPendingDeletes.clear();
	    		segmentDeletes = null;
	    	} else {
	    		segmentDeletes = mPendingDeletes;
	    		mPendingDeletes = new BufferedDeletes();
	    	}

	    	if (LOG.isDebugEnabled()) {
	    		final long newSegmentSize = mSegmentInfo.getSizeInBytes();
	    		final long docsPerMB = (long)(1024.f * 1024.f * (float)mFlushedDocCount / (float)newSegmentSize);
	    		LOG.debug("flushed: segment=" + mSegmentInfo.getName() + 
	    				" ramUsed=" + StringHelper.toHumanReadableUnits(startBytesUsed) +
	    				" newFlushedSize(includes docstores)=" + StringHelper.toHumanReadableUnits(newSegmentSize) +
	    				" flushedDocs=" + mFlushedDocCount + " docs/MB=" + docsPerMB);
	    	}
	    	assert mSegmentInfo != null;

	    	FlushedSegment fs = new FlushedSegment(commitInfo, 
	    			mFlushState.getFieldInfos(), segmentDeletes, 
	    			mFlushState.getLiveDocs(), mFlushState.getDelCountOnFlush());
	    	
	    	doAfterFlush();
	    	
	    	success = true;
	    	return fs;
	    	
	    } finally {
	    	if (!success) {
	    		if (mSegmentInfo != null) {
	    			synchronized (mWriter.getIndexWriter()) {
	    				mWriter.getIndexWriter().getDeleter().refresh(mSegmentInfo.getName());
	    			}
	    		}
	    		abort();
	    	}
	    }
	}
	
	/**
	 * Prepares this DWPT for flushing. This method will freeze and return the
	 * {@link AdvancedDeleteQueue}s global buffer and apply all pending
	 * deletes to this DWPT.
	 */
	public FrozenDeletes prepareFlush() {
	    assert mNumDocsInRAM > 0;
	    final FrozenBufferedDeletes globalDeletes = (FrozenBufferedDeletes)
	    		mDeleteQueue.freezeGlobalBuffer(mDeleteSlice);
	    
	    /** 
	     * deleteSlice can possibly be null if we have hit non-aborting exceptions during indexing and never succeeded 
	     * adding a document. 
	     */
	    if (mDeleteSlice != null) {
	    	// apply all deletes before we flush and release the delete slice
	    	mDeleteSlice.apply(mPendingDeletes, mNumDocsInRAM);
	    	assert mDeleteSlice.isEmpty();
	    	mDeleteSlice = null;
	    }
	    
	    return globalDeletes;
	}
	
	/** Reset after a flush */
	protected void doAfterFlush() {
	    mSegmentInfo = null;
	    mConsumer.doAfterFlush();
	    mDirectory.getCreatedFiles().clear();
	    mFieldInfos = new FieldInfosBuilder(mFieldInfos.getGlobalFieldNumbers());
	    mWriter.subtractFlushedNumDocs(mNumDocsInRAM);
	    mNumDocsInRAM = 0;
	}
	
}
