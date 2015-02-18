package org.javenstudio.common.indexdb.index;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexWriter;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.index.field.FieldNumbers;
import org.javenstudio.common.indexdb.util.Bits;

/**
 * Holds all the configuration that is used to create an {@link IndexWriter}.
 * Once {@link IndexWriter} has been created with this object, changes to this
 * object will not affect the {@link IndexWriter} instance. For that, use
 * {@link IndexParams} that is returned from {@link IndexWriter#getConfig()}.
 * 
 * <p>
 * All setter methods return {@link IndexParams} to allow chaining
 * settings conveniently, for example:
 * 
 */
public abstract class IndexParams {

	/** Default value is 32. Change using {@link #setTermIndexInterval(int)}. */
	// TODO: this should be private to the codec, not settable here
	public static final int DEFAULT_TERM_INDEX_INTERVAL = 32; 

	/** Denotes a flush trigger is disabled. */
	public final static int DISABLE_AUTO_FLUSH = -1;

	/** Disabled by default (because IndexWriter flushes by RAM usage by default). */
	public final static int DEFAULT_MAX_BUFFERED_DELETE_TERMS = DISABLE_AUTO_FLUSH;

	/** Disabled by default (because IndexWriter flushes by RAM usage by default). */
	public final static int DEFAULT_MAX_BUFFERED_DOCS = DISABLE_AUTO_FLUSH;

	/**
	 * Default value is 16 MB (which means flush when buffered docs consume
	 * approximately 16 MB RAM).
	 */
	public final static double DEFAULT_RAM_BUFFER_SIZE_MB = 16.0;
	
	/**
	 * Default value for the write lock timeout (1,000 ms).
	 *
	 * @see #setDefaultWriteLockTimeout(long)
	 */
	public static long WRITE_LOCK_TIMEOUT = 1000;

	/** Default setting for {@link #setReaderPooling}. */
	public final static boolean DEFAULT_READER_POOLING = false;

	/** Default value is 1945. Change using {@link #setRAMPerThreadHardLimitMB(int)} */
	public static final int DEFAULT_RAM_PER_THREAD_HARD_LIMIT_MB = 1945;
  
	/** 
	 * The maximum number of simultaneous threads that may be
	 *  indexing documents at once in IndexWriter; if more
	 *  than this many threads arrive they will wait for
	 *  others to finish. Default value is 8. 
	 */
	public final static int DEFAULT_MAX_THREAD_STATES = 8;
	
	protected IIndexContext mContext;
	protected IAnalyzer mAnalyzer;
	protected IIndexWriter.OpenMode mOpenMode;
	
	protected MergePolicy mMergePolicy;
	protected MergeScheduler mMergeScheduler;
	protected IndexDeletionPolicy mDeletionPolicy;
	protected IndexReaderWarmer mMergedSegmentWarmer;
	protected IndexingChain mIndexingChain;
	
	// TODO: this should be private to the codec, not settable here
	protected volatile int mTermIndexInterval = DEFAULT_TERM_INDEX_INTERVAL; 
	protected volatile int mMaxBufferedDocs = DEFAULT_MAX_BUFFERED_DOCS;
	protected volatile double mRamBufferSizeMB = DEFAULT_RAM_BUFFER_SIZE_MB;
	protected volatile int mMaxBufferedDeleteTerms = DEFAULT_MAX_BUFFERED_DELETE_TERMS;
	protected volatile int mPerThreadHardLimitMB = DEFAULT_RAM_PER_THREAD_HARD_LIMIT_MB;
	
	protected boolean mReaderPooling;
	
	public IndexParams(IAnalyzer analyzer) { 
		this(analyzer, (IIndexContext)null);
	}
	
	public IndexParams(IAnalyzer analyzer, IIndexContext context) { 
		mAnalyzer = analyzer;
		mContext = context;
		
		initParams();
		
		if (mContext == null) 
			throw new NullPointerException("IContext implements is null");
		
		if (mAnalyzer == null) 
			throw new NullPointerException("IAnalyzer implements is null");
		
		if (mOpenMode == null) 
			throw new NullPointerException("Index OpenMode is null");
		
		if (mMergePolicy == null) 
			throw new NullPointerException("MergePolicy is null");
		
		if (mMergeScheduler == null) 
			throw new NullPointerException("MergeScheduler is null");
		
		if (mDeletionPolicy == null) 
			throw new NullPointerException("DeletionPolicy is null");
		
		if (mIndexingChain == null) 
			throw new NullPointerException("IndexingChain is null");
	}
	
	protected void initParams() { 
		mOpenMode = IIndexWriter.OpenMode.CREATE_OR_APPEND;
		mMergeScheduler = new MergeScheduler.SerialMergeScheduler();
		mDeletionPolicy = new IndexDeletionPolicy.KeepOnlyLastCommitDeletionPolicy();
		mIndexingChain = IndexingChain.getIndexingChain();
		mMergePolicy = null;
		mMergedSegmentWarmer = null;
	}
	
	public final IIndexContext getContext() { 
		if (mContext == null) throw new NullPointerException();
		return mContext;
	}
	
	/** Returns the default analyzer to use for indexing documents. */
	public final IAnalyzer getAnalyzer() { 
		if (mAnalyzer == null) throw new NullPointerException();
		return mAnalyzer;
	}
	
	public IndexingChain getIndexingChain() { 
		if (mIndexingChain == null) throw new NullPointerException();
		return mIndexingChain;
	}
	
	public abstract DeleteQueue newDeleteQueue(IndexWriter writer, long generation);
	public abstract DeletesStream newDeletesStream(IndexWriter writer);
	
	public abstract SegmentMerger newSegmentMerger(IndexWriter writer, 
			ISegmentInfo segmentInfo, IDirectory dir, CheckAbort checkAbort, 
			FieldNumbers globalFieldNumbers);
	
	public abstract ISegmentReader newSegmentReader(ISegmentReader reader, 
			ISegmentCommitInfo info, Bits liveDocs, int numDocs) throws IOException;
	
	public MergeControl newMergeControl(IndexWriter writer) { 
		return new MergeControl(writer, this);
	}
	
	public FlushControl newFlushControl(SegmentWriter writer) { 
		return new FlushControl(writer);
	}
	
	public FlushPolicy newFlushPolicy(SegmentWriter writer) { 
		return new FlushByRamOrCountsPolicy(writer);
	}
	
	public long getWriterWaitTimeout() { 
		return 1000;
	}
	
	/**
	 * Returns allowed timeout when acquiring the write lock.
	 * @see IndexParams#setWriteLockTimeout(long)
	 */
	public long getWriteLockTimeout() { 
		return WRITE_LOCK_TIMEOUT; 
	}
	
	/** Returns the {@link OpenMode} set by {@link IIndexWriter#setOpenMode(OpenMode)}. */
	public IIndexWriter.OpenMode getOpenMode() { 
		return mOpenMode;
	}
	
	/**
	 * Returns the {@link IndexCommit} as specified in
	 * {@link IndexWriterConfig#setIndexCommit(IndexCommit)} or the default,
	 * {@code null} which specifies to open the latest index commit point.
	 */
	public IndexCommit getIndexCommit() { 
		return null; 
	}
	
	/**
	 * Returns the {@link IndexDeletionPolicy} specified in
	 * {@link IndexWriterConfig#setIndexDeletionPolicy(IndexDeletionPolicy)} or
	 * the default {@link KeepOnlyLastCommitDeletionPolicy}/
	 */
	public IndexDeletionPolicy getIndexDeletionPolicy() {
		return mDeletionPolicy;
	}
	
	/**
	 * Returns the current MergePolicy in use by this writer.
	 *
	 * @see IndexWriterConfig#setMergePolicy(MergePolicy)
	 */
	public MergePolicy getMergePolicy() { 
		if (mMergePolicy == null) throw new NullPointerException();
		return mMergePolicy;
	}
	
	/**
	 * Returns the {@link MergeScheduler} that was set by
	 * {@link IndexWriterConfig#setMergeScheduler(MergeScheduler)}.
	 */
	public MergeScheduler getMergeScheduler() { 
		return mMergeScheduler;
	}
	
	/**
	 * Returns {@code true} if {@link IndexWriter} should pool readers even if
	 * {@link DirectoryReader#open(IndexWriter, boolean)} has not been called.
	 */
	public boolean getReaderPooling() {
		return mReaderPooling;
	}
	
	/**
	 * Set the merged segment warmer. See {@link IndexReaderWarmer}.
	 * 
	 * <p>
	 * Takes effect on the next merge.
	 */
	public IndexParams setMergedSegmentWarmer(IndexReaderWarmer mergeSegmentWarmer) {
		mMergedSegmentWarmer = mergeSegmentWarmer;
		return this;
	}

	/** Returns the current merged segment warmer. See {@link IndexReaderWarmer}. */
	public IndexReaderWarmer getMergedSegmentWarmer() {
		return mMergedSegmentWarmer;
	}
	
	/**
	 * Returns the interval between indexed terms.
	 *
	 * @see #setTermIndexInterval(int)
	 */
	public int getTermIndexInterval() { 
		// TODO: this should be private to the codec, not settable here
		return mTermIndexInterval;
	}
	
	/**
	 * Returns the number of buffered deleted terms that will trigger a flush of all
	 * buffered deletes if enabled.
	 *
	 * @see #setMaxBufferedDeleteTerms(int)
	 */
	public int getMaxBufferedDeleteTerms() {
		return mMaxBufferedDeleteTerms;
	}
	
	/** Returns the value set by {@link #setRAMBufferSizeMB(double)} if enabled. */
	public double getRAMBufferSizeMB() {
		return mRamBufferSizeMB;
	}
  
	/**
	 * Returns the number of buffered added documents that will trigger a flush if
	 * enabled.
	 *
	 * @see #setMaxBufferedDocs(int)
	 */
	public int getMaxBufferedDocs() {
		return mMaxBufferedDocs;
	}
  
	/**
	 * Returns the max amount of memory each {@link DocumentsWriterPerThread} can
	 * consume until forcefully flushed.
	 * 
	 * @see IndexWriterConfig#setRAMPerThreadHardLimitMB(int)
	 */
	public int getRAMPerThreadHardLimitMB() {
		return mPerThreadHardLimitMB;
	}
	
}
