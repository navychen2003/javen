package org.javenstudio.hornet.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.AlreadyClosedException;
import org.javenstudio.common.indexdb.Constants;
import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.LockObtainFailedException;
import org.javenstudio.common.indexdb.ThreadInterruptedException;
import org.javenstudio.common.indexdb.index.CheckAbort;
import org.javenstudio.common.indexdb.index.DeletesStream;
import org.javenstudio.common.indexdb.index.FlushInfo;
import org.javenstudio.common.indexdb.index.FlushedSegment;
import org.javenstudio.common.indexdb.index.FrozenDeletes;
import org.javenstudio.common.indexdb.index.IndexCommit;
import org.javenstudio.common.indexdb.index.IndexParams;
import org.javenstudio.common.indexdb.index.IndexWriter;
import org.javenstudio.common.indexdb.index.MergeInfo;
import org.javenstudio.common.indexdb.index.MergePolicy;
import org.javenstudio.common.indexdb.index.MergeState;
import org.javenstudio.common.indexdb.index.ReadersAndLiveDocs;
import org.javenstudio.common.indexdb.index.SegmentMerger;
import org.javenstudio.common.indexdb.index.field.FieldNumbers;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.common.indexdb.index.segment.SegmentCommitInfo;
import org.javenstudio.common.indexdb.index.segment.SegmentInfo;
import org.javenstudio.common.indexdb.index.segment.SegmentInfos;
import org.javenstudio.common.indexdb.store.TrackingDirectoryWrapper;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.common.util.Logger;
import org.javenstudio.hornet.codec.SegmentInfosFormat;
import org.javenstudio.hornet.index.segment.DirectoryReader;
import org.javenstudio.hornet.index.segment.SegmentReader;
import org.javenstudio.hornet.index.segment.StandardDirectoryReader;

public class AdvancedIndexWriter extends IndexWriter {
	private static final Logger LOG = Logger.getLogger(AdvancedIndexWriter.class);
	
	protected FieldNumbers mGlobalFieldNumberMap;
	protected DocumentsWriter mDocWriter;
	
	/**
	 * Constructs a new IndexWriter per the settings given in <code>conf</code>.
	 * Note that the passed in {@link IndexWriterConfig} is
	 * privately cloned; if you need to make subsequent "live"
	 * changes to the configuration use {@link #getConfig}.
	 * <p>
	 * 
	 * @param d
	 *          the index directory. The index is either created or appended
	 *          according <code>conf.getOpenMode()</code>.
	 * @param conf
	 *          the configuration settings according to which IndexWriter should
	 *          be initialized.
	 * @throws CorruptIndexException
	 *           if the index is corrupt
	 * @throws LockObtainFailedException
	 *           if another writer has this index open (<code>write.lock</code>
	 *           could not be obtained)
	 * @throws IOException
	 *           if the directory cannot be read/written to, or if it does not
	 *           exist and <code>conf.getOpenMode()</code> is
	 *           <code>OpenMode.APPEND</code> or if there is any other low-level
	 *           IO error
	 */
	public AdvancedIndexWriter(IDirectory dir, IndexParams params) throws IOException { 
		super(dir, params);
	}
	
	@Override
	protected void onInitSegmentWriter() throws IOException { 
		// start with previous field numbers, but new FieldInfos
		mGlobalFieldNumberMap = getFieldNumberMap();
		mDocWriter = new DocumentsWriter(this, mGlobalFieldNumberMap);
	}
	
	public final DocumentsWriter getSegmentWriter() { return mDocWriter; }
	public final FieldNumbers getGlobalFieldNumbers() { return mGlobalFieldNumberMap; }
	
	/**
	 * Expert: returns a readonly reader, covering all
	 * committed as well as un-committed changes to the index.
	 * This provides "near real-time" searching, in that
	 * changes made during an IndexWriter session can be
	 * quickly made available for searching without closing
	 * the writer nor calling {@link #commit}.
	 *
	 * <p>Note that this is functionally equivalent to calling
	 * {#flush} and then opening a new reader.  But the turnaround time of this
	 * method should be faster since it avoids the potentially
	 * costly {@link #commit}.</p>
	 *
	 * <p>You must close the {@link IndexReader} returned by
	 * this method once you are done using it.</p>
	 *
	 * <p>It's <i>near</i> real-time because there is no hard
	 * guarantee on how quickly you can get a new reader after
	 * making changes with IndexWriter.  You'll have to
	 * experiment in your situation to determine if it's
	 * fast enough.  As this is a new and experimental
	 * feature, please report back on your findings so we can
	 * learn, improve and iterate.</p>
	 *
	 * <p>The resulting reader supports {@link
	 * DirectoryReader#openIfChanged}, but that call will simply forward
	 * back to this method (though this may change in the
	 * future).</p>
	 *
	 * <p>The very first time this method is called, this
	 * writer instance will make every effort to pool the
	 * readers that it opens for doing merges, applying
	 * deletes, etc.  This means additional resources (RAM,
	 * file descriptors, CPU time) will be consumed.</p>
	 *
	 * <p>For lower latency on reopening a reader, you should
	 * call {@link IndexWriterConfig#setMergedSegmentWarmer} to
	 * pre-warm a newly merged segment before it's committed
	 * to the index.  This is important for minimizing
	 * index-to-search delay after a large merge.  </p>
	 *
	 * <p>If an addIndexes* call is running in another thread,
	 * then this reader will only search those segments from
	 * the foreign index that have been successfully copied
	 * over, so far</p>.
	 *
	 * <p><b>NOTE</b>: Once the writer is closed, any
	 * outstanding readers may continue to be used.  However,
	 * if you attempt to reopen any of those readers, you'll
	 * hit an {@link AlreadyClosedException}.</p>
	 *
	 * @return IndexReader that covers entire index plus all
	 * changes made so far by this IndexWriter instance
	 *
	 * @throws IOException
	 */
	@Override
	public DirectoryReader getReader(boolean applyAllDeletes) throws IOException {
		ensureOpen();

		final long tStart = System.currentTimeMillis();
		if (LOG.isDebugEnabled())
			LOG.debug("flush at getReader");

		// Do this up front before flushing so that the readers
		// obtained during this flush are pooled, the first time
		// this method is called:
		mPoolReaders = true;
		
		final DirectoryReader r;
		doBeforeFlush();
		boolean anySegmentFlushed = false;
		
		/**
		 * for releasing a NRT reader we must ensure that 
		 * DW doesn't add any segments or deletes until we are
		 * done with creating the NRT DirectoryReader. 
		 * We release the two stage full flush after we are done opening the
		 * directory reader!
		 */
		synchronized (mFullFlushLock) {
			boolean success = false;
			try {
				anySegmentFlushed = mDocWriter.flushAllWriters();
				if (!anySegmentFlushed) {
					// prevent double increment since docWriter#doFlush increments the flushcount
					// if we flushed anything.
					mFlushCount.incrementAndGet();
				}
				
				success = true;
				
				// Prevent segmentInfos from changing while opening the
				// reader; in theory we could do similar retry logic,
				// just like we do when loading segments_N
				synchronized (this) {
					maybeApplyDeletes(applyAllDeletes);
					r = StandardDirectoryReader.open(this, mSegmentInfos, applyAllDeletes);
					
					if (LOG.isDebugEnabled())
						LOG.debug("return reader version=" + r.getVersion() + " reader=" + r);
				}
				
			} catch (OutOfMemoryError oom) {
				handleOOM(oom, "getReader");
				// never reached but javac disagrees:
				return null;
				
			} finally {
				if (!success) {
					if (LOG.isDebugEnabled()) 
						LOG.debug("hit exception during NRT reader");
				}
				
				// Done: finish the full flush!
				mDocWriter.finishFullFlush(success);
				doAfterFlush();
			}
		}
		
		if (anySegmentFlushed) 
			getMergeControl().maybeMerge();
    
		if (LOG.isDebugEnabled())
			LOG.debug("getReader took " + (System.currentTimeMillis() - tStart) + " msec");
    
		return r;
	}
	
	/** 
	 * Expert: attempts to delete by document ID, as long as
	 *  the provided reader is a near-real-time reader (from {@link
	 *  DirectoryReader#open(IndexWriter,boolean)}).  If the
	 *  provided reader is an NRT reader obtained from this
	 *  writer, and its segment has not been merged away, then
	 *  the delete succeeds and this method returns true; else, it
	 *  returns false the caller must then separately delete by
	 *  Term or Query.
	 *
	 *  <b>NOTE</b>: this method can only delete documents
	 *  visible to the currently open NRT reader.  If you need
	 *  to delete documents indexed after opening the NRT
	 *  reader you must use the other deleteDocument methods
	 *  (e.g., {@link #deleteDocuments(Term)}). 
	 */
	public synchronized boolean tryDeleteDocument(IIndexReader readerIn, int docID) 
			throws IOException {
		final IAtomicReader reader;
		if (readerIn instanceof IAtomicReader) {
			// Reader is already atomic: use the incoming docID:
			reader = (IAtomicReader) readerIn;
			
		} else {
			// Composite reader: lookup sub-reader and re-base docID:
			List<IAtomicReaderRef> leaves = readerIn.getReaderContext().getLeaves();
			int subIndex = ReaderUtil.subIndex(docID, leaves);
			
			reader = leaves.get(subIndex).getReader();
			docID -= leaves.get(subIndex).getDocBase();
			
			assert docID >= 0;
			assert docID < reader.getMaxDoc();
		}

		if (!(reader instanceof SegmentReader)) {
			throw new IllegalArgumentException("the reader must be a SegmentReader or" + 
					" composite reader containing only SegmentReaders");
		}
      
		final ISegmentCommitInfo info = ((SegmentReader) reader).getCommitInfo();

		// TODO: this is a slow linear search, but, number of
		// segments should be contained unless something is
		// seriously wrong w/ the index, so it should be a minor
		// cost:

		if (mSegmentInfos.indexOf(info) != -1) {
			ReadersAndLiveDocs rld = mReaderPool.get(info, false);
			if (rld != null) {
				
				synchronized (mDeletesStream) {
					rld.initWritableLiveDocs();
					
					if (rld.delete(docID)) {
						final int fullDelCount = rld.getCommitInfo().getDelCount() + 
								rld.getPendingDeleteCount();
						
						if (fullDelCount == rld.getCommitInfo().getSegmentInfo().getDocCount()) {
							// If a merge has already registered for this
							// segment, we leave it in the readerPool; the
							// merge will skip merging it and will then drop
							// it once it's done:
							if (!getMergeControl().containsMergingSegment(rld.getCommitInfo())) {
								mSegmentInfos.remove(rld.getCommitInfo());
								mReaderPool.drop(rld.getCommitInfo());
								checkpoint();
							}
						}
					}
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Adds all segments from an array of indexes into this index.
	 *
	 * <p>This may be used to parallelize batch indexing. A large document
	 * collection can be broken into sub-collections. Each sub-collection can be
	 * indexed in parallel, on a different thread, process or machine. The
	 * complete index can then be created by merging sub-collection indexes
	 * with this method.
	 *
	 * <p>
	 * <b>NOTE:</b> the index in each {@link Directory} must not be
	 * changed (opened by a writer) while this method is
	 * running.  This method does not acquire a write lock in
	 * each input Directory, so it is up to the caller to
	 * enforce this.
	 *
	 * <p>This method is transactional in how Exceptions are
	 * handled: it does not commit a new segments_N file until
	 * all indexes are added.  This means if an Exception
	 * occurs (for example disk full), then either no indexes
	 * will have been added or they all will have been.
	 *
	 * <p>Note that this requires temporary free space in the
	 * {@link Directory} up to 2X the sum of all input indexes
	 * (including the starting index). If readers/searchers
	 * are open against the starting index, then temporary
	 * free space required will be higher by the size of the
	 * starting index (see {@link #forceMerge(int)} for details).
	 *
	 * <p>
	 * <b>NOTE:</b> this method only copies the segments of the incoming indexes
	 * and does not merge them. Therefore deleted documents are not removed and
	 * the new segments are not merged with the existing ones.
	 *
	 * <p>This requires this index not be among those to be added.
	 *
	 * <p>
	 * <b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer. See <a
	 * href="#OOME">above</a> for details.
	 *
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public void addIndexes(IDirectory... dirs) throws IOException {
		ensureOpen();
		noDupDirs(dirs);

		try {
			if (LOG.isDebugEnabled())
				LOG.debug("flush at addIndexes(Directory...)");

			flush(false, true);

			List<ISegmentCommitInfo> infos = new ArrayList<ISegmentCommitInfo>();
			for (IDirectory dir : dirs) {
				if (LOG.isDebugEnabled())
					LOG.debug("addIndexes: process directory " + dir);
				
				SegmentInfos sis = new SegmentInfos(dir); // read infos from dir
				SegmentInfosFormat.read(sis, getIndexFormat());
				
				final Set<String> dsFilesCopied = new HashSet<String>();
				final Map<String, String> dsNames = new HashMap<String, String>();
				final Set<String> copiedFiles = new HashSet<String>();
				
				for (ISegmentCommitInfo info : sis) {
					assert !infos.contains(info): "dup info dir=" + info.getSegmentInfo().getDirectory() 
						+ " name=" + info.getSegmentInfo().getName();

					String newSegName = newSegmentName();
					String dsName = ""; //Lucene3xSegmentInfoFormat.getDocStoreSegment(info.info);

					if (LOG.isDebugEnabled()) {
						LOG.debug("addIndexes: process segment origName=" + info.getSegmentInfo().getName() 
								+ " newName=" + newSegName + " dsName=" + dsName + " info=" + info);
					}

					infos.add(copySegmentAsIs(info, newSegName, dsNames, dsFilesCopied, copiedFiles));
				}
			}

			synchronized (this) {
				ensureOpen();
				mSegmentInfos.addAll(infos);
				checkpoint();
			}

		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "addIndexes(Directory...)");
		}
	}
	
	/**
	 * Merges the provided indexes into this index.
	 * 
	 * <p>
	 * The provided IndexReaders are not closed.
	 * 
	 * <p>
	 * See {@link #addIndexes} for details on transactional semantics, temporary
	 * free space required in the Directory, and non-CFS segments on an Exception.
	 * 
	 * <p>
	 * <b>NOTE</b>: if this method hits an OutOfMemoryError you should immediately
	 * close the writer. See <a href="#OOME">above</a> for details.
	 * 
	 * <p>
	 * <b>NOTE:</b> this method merges all given {@link IndexReader}s in one
	 * merge. If you intend to merge a large number of readers, it may be better
	 * to call this method multiple times, each time with a small set of readers.
	 * In principle, if you use a merge policy with a {@code mergeFactor} or
	 * {@code maxMergeAtOnce} parameter, you should pass that many readers in one
	 * call. Also, if the given readers are {@link DirectoryReader}s, they can be
	 * opened with {@code termIndexInterval=-1} to save RAM, since during merge
	 * the in-memory structure is not used. See
	 * {@link DirectoryReader#open(Directory, int)}.
	 * 
	 * <p>
	 * <b>NOTE</b>: if you call {@link #close(boolean)} with <tt>false</tt>, which
	 * aborts all running merges, then any thread still running this method might
	 * hit a {@link MergePolicy.MergeAbortedException}.
	 * 
   	 * @throws CorruptIndexException
   	 *           if the index is corrupt
   	 * @throws IOException
   	 *           if there is a low-level IO error
   	 */
	public void addIndexes(IIndexReader... readers) throws IOException {
		ensureOpen();
		int numDocs = 0;

		try {
			if (LOG.isDebugEnabled())
				LOG.debug("flush at addIndexes(IndexReader...)");
			
			flush(false, true);

			String mergedName = newSegmentName();
			for (IIndexReader indexReader : readers) {
				numDocs += indexReader.getNumDocs();
			}
			
			getContext().setMergeInfo(new MergeInfo(numDocs, -1, true, -1));
			
			// TODO: somehow we should fix this merge so it's
			// abortable so that IW.close(false) is able to stop it
			TrackingDirectoryWrapper trackingDir = new TrackingDirectoryWrapper(mDirectory);

			SegmentInfo info = new SegmentInfo(mDirectory, Constants.INDEXDB_MAIN_VERSION, 
					mergedName, -1, false, null, null);

			SegmentMerger merger = new AdvancedMerger(this, 
					info, trackingDir, CheckAbort.NONE, mGlobalFieldNumberMap);

			for (IIndexReader reader : readers) { // add new indexes
				merger.add(reader);
			}

			MergeState mergeState = merger.merge(); // merge 'em
			ISegmentCommitInfo infoPerCommit = new SegmentCommitInfo(
					getIndexFormat(), info, 0, -1L);

			info.setFileNames(new HashSet<String>(trackingDir.getCreatedFiles()));
			trackingDir.getCreatedFiles().clear();
                                         
			setDiagnostics(info, "addIndexes(IndexReader...)");

			boolean useCompoundFile;
			synchronized (this) { // Guard segmentInfos
				if (getMergeControl().isStopMerges()) {
					mDeleter.deleteNewFiles(infoPerCommit.getFileNames());
					return;
				}
				ensureOpen();
				useCompoundFile = getMergeControl().getMergePolicy()
						.useCompoundFile(mSegmentInfos, infoPerCommit);
			}

			// Now create the compound file if needed
			if (useCompoundFile) {
				Collection<String> filesToDelete = infoPerCommit.getFileNames();
				createCompoundFile(getContext(), getDirectory(), CheckAbort.NONE, info);

				// delete new non cfs files directly: they were never
				// registered with IFD
				synchronized (this) {
					mDeleter.deleteNewFiles(filesToDelete);
				}
				info.setUseCompoundFile(true);
			}

			// Have codec write SegmentInfo.  Must do this after
			// creating CFS so that 1) .si isn't slurped into CFS,
			// and 2) .si reflects useCompoundFile=true change
			// above:
			getIndexFormat().getSegmentInfoFormat().createWriter(mDirectory).writeSegmentInfo(
					info, mergeState.getFieldInfos());
			info.addFileNames(trackingDir.getCreatedFiles());

			// Register the new segment
			synchronized (this) {
				if (getMergeControl().isStopMerges()) {
					mDeleter.deleteNewFiles(info.getFileNames());
					return;
				}
				
				ensureOpen();
				mSegmentInfos.add(infoPerCommit);
				checkpoint();
			}
			
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "addIndexes(IndexReader...)");
		} finally { 
			getContext().setMergeInfo(null);
		}
	}
	
	/** Copies the segment files as-is into the IndexWriter's directory. */
	protected ISegmentCommitInfo copySegmentAsIs(ISegmentCommitInfo info, String segName,
			Map<String, String> dsNames, Set<String> dsFilesCopied, Set<String> copiedFiles)
			throws IOException {
		
		throw new UnsupportedOperationException();
	}
	
	protected void noDupDirs(IDirectory... dirs) {
		HashSet<IDirectory> dups = new HashSet<IDirectory>();
		
		for (int i=0; i < dirs.length; i++) {
			if (dups.contains(dirs[i]))
				throw new IllegalArgumentException("Directory " + dirs[i] + " appears more than once");
			
			if (dirs[i] == mDirectory)
				throw new IllegalArgumentException("Cannot add directory to itself");
			
			dups.add(dirs[i]);
		}
	}
	
	@Override
	protected void rollbackInternal() throws IOException {
		boolean success = false;
		if (LOG.isDebugEnabled())
			LOG.debug("rollback");
		
		try {
			synchronized (this) {
				getMergeControl().finishMerges(false);
				getMergeControl().setStopMerges(true);
			}

			if (LOG.isDebugEnabled())
				LOG.debug("rollback: done finish merges");
			
			// Must pre-close these two, in case they increment
			// changeCount so that we can then set it to false
			// before calling closeInternal
			getMergeControl().getMergePolicy().close();
			getMergeControl().getMergeScheduler().close();

			mDeletesStream.clear();
			// mark it as closed first to prevent subsequent indexing actions/flushes 
			mDocWriter.close(); 
			mDocWriter.abort();
			
			synchronized(this) {
				if (mPendingCommit != null) {
					getIndexFormat().getSegmentInfosFormat().rollbackCommit(mDirectory, mPendingCommit);
					mDeleter.decreaseRef(mPendingCommit);
					mPendingCommit = null;
					notifyAll();
				}

				// Don't bother saving any changes in our segmentInfos
				mReaderPool.dropAll(false);

				// Keep the same segmentInfos instance but replace all
				// of its SegmentInfo instances.  This is so the next
				// attempt to commit using this instance of IndexWriter
				// will always write to a new generation ("write
				// once").
				mSegmentInfos.rollbackSegmentInfos(mRollbackSegments);
				if (LOG.isDebugEnabled())
					LOG.debug("rollback: infos=" + toSegmentString(mSegmentInfos));
        
				// Ask deleter to locate unreferenced files & remove
				// them:
				mDeleter.checkpoint(mSegmentInfos, false);
				mDeleter.refresh();
			}

			mLastCommitChangeCount = mChangeCount;

			success = true;
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "rollbackInternal");
			
		} finally {
			synchronized(this) {
				if (!success) {
					mClosing = false;
					notifyAll();
					
					if (LOG.isDebugEnabled())
						LOG.debug("hit exception during rollback");
				}
			}
		}

		closeInternal(false, false);
	}
	
	@Override
	protected void closeInternal(boolean waitForMerges, boolean doFlush) 
			throws CorruptIndexException, IOException {
		boolean interrupted = false;
		try {
			if (mPendingCommit != null) {
				throw new IllegalStateException("cannot close: prepareCommit " + 
						"was already called with no corresponding call to commit");
			}

			if (LOG.isDebugEnabled())
				LOG.debug("now flush at close waitForMerges=" + waitForMerges);
			
			mDocWriter.close();

			try {
				// Only allow a new merge to be triggered if we are
				// going to wait for merges:
				if (doFlush) 
					flush(waitForMerges, true);
				else 
					mDocWriter.abort(); // already closed

			} finally { 
				try {
					// clean up merge scheduler in all cases, although flushing may have failed:
					interrupted = Thread.interrupted();
			          
					if (waitForMerges) {
						try {
							// Give merge scheduler last chance to run, in case
							// any pending merges are waiting:
							getMergeControl().getMergeScheduler().merge(this);
						} catch (ThreadInterruptedException tie) {
							// ignore any interruption, does not matter
							interrupted = true;
							
							if (LOG.isDebugEnabled())
								LOG.debug("interrupted while waiting for final merges");
						}
					}
	
					synchronized(this) {
						for (;;) {
							try {
								getMergeControl().finishMerges(waitForMerges && !interrupted);
								break;
								
							} catch (ThreadInterruptedException tie) {
				                // by setting the interrupted status, the
				                // next call to finishMerges will pass false,
				                // so it will not wait
				                interrupted = true;
				                
				                if (LOG.isDebugEnabled())
				                	LOG.debug("interrupted while waiting for merges to finish");
							}
						}
						
						getMergeControl().setStopMerges(true);
					}
				} finally { 
					// shutdown policy, scheduler and all threads (this call is not interruptible):
					IOUtils.closeWhileHandlingException(
							getMergeControl().getMergePolicy(), 
							getMergeControl().getMergeScheduler());
				}
			}
	
			if (LOG.isDebugEnabled())
				LOG.debug("now call final commit");
			
			if (doFlush) 
				commitInternal(null);
	
			if (LOG.isDebugEnabled())
				LOG.debug("at close: " + toSegmentString());
			
			// used by assert below
			//final DocumentWriter oldWriter = mDocWriter;
			synchronized(this) {
				mReaderPool.dropAll(true);
				mDocWriter = null;
				mDeleter.close();
			}
	
			if (mWriteLock != null) {
				mWriteLock.release();	// release write lock
				mWriteLock = null;
			}
			
			synchronized(this) {
				mClosed = true;
			}
			
			//assert oldWriter.perThreadPool.numDeactivatedThreadStates() == 
			//		 oldWriter.perThreadPool.getMaxThreadStates();
			
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "closeInternal");
			
		} finally {
			synchronized(this) {
				mClosing = false;
				notifyAll();
				
				if (!mClosed) {
					if (LOG.isDebugEnabled())
						LOG.debug("hit exception while closing");
				}
			}
			
			// finally, restore interrupt status:
			if (interrupted) Thread.currentThread().interrupt();
		}
	}
	
	protected final void commitInternal(Map<String,String> commitUserData) 
			throws CorruptIndexException, IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("commit: start");
		
		synchronized (mCommitLock) {
			ensureOpen(false);

			if (LOG.isDebugEnabled())
				LOG.debug("commit: enter lock");
			
			if (mPendingCommit == null) {
				if (LOG.isDebugEnabled())
					LOG.debug("commit: now prepare");
				
				prepareCommit(commitUserData);
				
			} else { 
				if (LOG.isDebugEnabled())
					LOG.debug("commit: already prepared");
			}

			finishCommit();
		}
	}
	
	protected synchronized final void finishCommit() throws CorruptIndexException, IOException {
		if (mPendingCommit != null) {
			try {
				if (LOG.isDebugEnabled())
					LOG.debug("commit: pendingCommit != null");
				
				getIndexFormat().getSegmentInfosFormat().finishCommit(mDirectory, mPendingCommit);
				if (LOG.isDebugEnabled())
					LOG.debug("commit: wrote segments file \"" + mPendingCommit.getSegmentsFileName() + "\"");
				
				mLastCommitChangeCount = mPendingCommitChangeCount;
				
				mSegmentInfos.updateGeneration(mPendingCommit);
				mSegmentInfos.setUserData(mPendingCommit.getUserData());
				
				mRollbackSegments = mPendingCommit.createBackupSegmentInfos();
				mDeleter.checkpoint(mPendingCommit, true);
				
			} finally {
				// Matches the incRef done in prepareCommit:
				mDeleter.decreaseRef(mFilesToCommit);
				mFilesToCommit = null;
				mPendingCommit = null;
				
				notifyAll();
			}
			
		} else { 
			if (LOG.isDebugEnabled())
				LOG.debug("commit: pendingCommit == null; skip");
		}
		
		getContext().setFlushInfo(null);
		getContext().setMergeInfo(null);
		
		if (LOG.isDebugEnabled())
			LOG.debug("commit: done");
	}
	
	/** 
	 * <p>Expert: prepare for commit, specifying
	 *  commitUserData Map (String -> String).  This does the
	 *  first phase of 2-phase commit. This method does all
	 *  steps necessary to commit changes since this writer
	 *  was opened: flushes pending added and deleted docs,
	 *  syncs the index files, writes most of next segments_N
	 *  file.  After calling this you must call either {@link
	 *  #commit()} to finish the commit, or {@link
	 *  #rollback()} to revert the commit and undo all changes
	 *  done since the writer was opened.</p>
	 *
	 *  <p>You can also just call {@link #commit(Map)} directly
	 *  without prepareCommit first in which case that method
	 *  will internally call prepareCommit.
	 *
	 *  <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 *  you should immediately close the writer.  See <a
	 *  href="#OOME">above</a> for details.</p>
	 *
	 *  @param commitUserData Opaque Map (String->String)
	 *  that's recorded into the segments file in the index,
	 *  and retrievable by {@link
	 *  IndexCommit#getUserData}.  Note that when
	 *  IndexWriter commits itself during {@link #close}, the
	 *  commitUserData is unchanged (just carried over from
	 *  the prior commit).  If this is null then the previous
	 *  commitUserData is kept.  Also, the commitUserData will
	 *  only "stick" if there are actually changes in the
	 *  index to commit.
	 */
	public final void prepareCommit(Map<String,String> commitUserData)
			throws CorruptIndexException, IOException {
		ensureOpen(false);

		synchronized (mCommitLock) {
			if (LOG.isDebugEnabled()) { 
				LOG.debug("prepareCommit: flush"); 
				LOG.debug("  index before flush " + toSegmentString());
			}
			
			if (mHitOOM) {
				throw new IllegalStateException(
						"this writer hit an OutOfMemoryError; cannot commit");
			}

			if (mPendingCommit != null) {
				throw new IllegalStateException(
						"prepareCommit was already called with no corresponding call to commit");
			}

			doBeforeFlush();
			//assert testPoint("startDoFlush");
      
			SegmentInfos toCommit = null;
			boolean anySegmentsFlushed = false;

			// This is copied from doFlush, except it's modified to
			// clone & incRef the flushed SegmentInfos inside the
			// sync block:
			try {
				synchronized (mFullFlushLock) {
					boolean flushSuccess = false;
					boolean success = false;
					
					try {
						anySegmentsFlushed = mDocWriter.flushAllWriters();
						if (!anySegmentsFlushed) {
							// prevent double increment since docWriter#doFlush increments the flushcount
							// if we flushed anything.
							mFlushCount.incrementAndGet();
						}
						flushSuccess = true;

						synchronized(this) {
							maybeApplyDeletes(true);

							mReaderPool.commit(mSegmentInfos);

							// Must clone the segmentInfos while we still
							// hold fullFlushLock and while sync'd so that
							// no partial changes (eg a delete w/o
							// corresponding add from an updateDocument) can
							// sneak into the commit point:
							toCommit = (SegmentInfos)getIndexFormat().getSegmentInfosFormat()
									.newSegmentInfos(mDirectory, mSegmentInfos);

							mPendingCommitChangeCount = mChangeCount;

							// This protects the segmentInfos we are now going
							// to commit.  This is important in case, eg, while
							// we are trying to sync all referenced files, a
							// merge completes which would otherwise have
							// removed the files we are now syncing.    
							mFilesToCommit = toCommit.getFileNames(false);
							mDeleter.increaseRef(mFilesToCommit);
						}
						
						success = true;
					} finally {
						if (!success) { 
							if (LOG.isDebugEnabled())
								LOG.debug("hit exception during prepareCommit");
						}
						
						// Done: finish the full flush!
						mDocWriter.finishFullFlush(flushSuccess);
						doAfterFlush();
					}
				}
			} catch (OutOfMemoryError oom) {
				handleOOM(oom, "prepareCommit");
			}
 
			boolean success = false;
			try {
				if (anySegmentsFlushed) 
					getMergeControl().maybeMerge();
				
				success = true;
			} finally {
				if (!success) {
					synchronized (this) {
						mDeleter.decreaseRef(mFilesToCommit);
						mFilesToCommit = null;
					}
				}
			}

			startCommit(toCommit, commitUserData);
		}
	}
	
	/**
	 * Prepares the {@link SegmentInfo} for the new flushed segment and persists
	 * the deleted documents {@link MutableBits}. Use
   	 * {@link #publishFlushedSegment(SegmentCommitInfo, FrozenBufferedDeletes, FrozenBufferedDeletes)} to
   	 * publish the returned {@link SegmentInfo} together with its segment private
   	 * delete packet.
   	 * 
   	 * @see #publishFlushedSegment(SegmentCommitInfo, FrozenBufferedDeletes, FrozenBufferedDeletes)
   	 */
	public ISegmentCommitInfo prepareFlushedSegment(FlushedSegment flushedSegment) throws IOException {
		assert flushedSegment != null;
		
		ISegmentCommitInfo newSegment = flushedSegment.getCommitInfo();
		setDiagnostics(newSegment.getSegmentInfo(), "flush");
    
		getContext().setFlushInfo(new FlushInfo(newSegment.getSegmentInfo().getDocCount(), 
				newSegment.getSegmentInfo().getSizeInBytes()));
		
		boolean success = false;
		try {
			if (getMergeControl().useCompoundFile(newSegment)) {
				// Now build compound file
				Collection<String> oldFiles = createCompoundFile(mContext, mDirectory, 
						CheckAbort.NONE, newSegment.getSegmentInfo());
				
				newSegment.getSegmentInfo().setUseCompoundFile(true);
				synchronized(this) {
					mDeleter.deleteNewFiles(oldFiles);
				}
			}

			// Have codec write SegmentInfo.  Must do this after
			// creating CFS so that 1) .si isn't slurped into CFS,
			// and 2) .si reflects useCompoundFile=true change
			// above:
			getIndexFormat().getSegmentInfoFormat().createWriter(mDirectory).writeSegmentInfo(
					newSegment.getSegmentInfo(), flushedSegment.getFieldInfos());
			
			// TODO: ideally we would freeze newSegment here!!
			// because any changes after writing the .si will be
			// lost... 

			// Must write deleted docs after the CFS so we don't
			// slurp the del file into CFS:
			if (flushedSegment.getLiveDocs() != null) {
				final int delCount = flushedSegment.getDelCount();
				assert delCount > 0;
				
				if (LOG.isDebugEnabled()) { 
					LOG.debug("flush: write " + delCount + " deletes gen=" + 
							flushedSegment.getCommitInfo().getDelGen());
				}

				// TODO: in the NRT case it'd be better to hand
				// this del vector over to the
				// shortly-to-be-opened SegmentReader and let it
				// carry the changes; there's no reason to use
				// filesystem as intermediary here.
				ISegmentCommitInfo info = flushedSegment.getCommitInfo();
				getIndexFormat().getLiveDocsFormat().writeLiveDocs(
						mDirectory, flushedSegment.getLiveDocs(), info, delCount);
				
				newSegment.setDelCount(delCount);
				newSegment.advanceDelGen();
			}

			success = true;
		} finally {
			if (!success) {
				if (LOG.isDebugEnabled()) { 
					LOG.debug("hit exception reating compound file for newly flushed segment " 
							+ newSegment.getSegmentInfo().getName());
				}

				synchronized(this) {
					mDeleter.refresh(newSegment.getSegmentInfo().getName());
				}
			}
			
			getContext().setFlushInfo(null);
		}
		
		return newSegment;
	}
	
	public synchronized void publishFrozenDeletes(FrozenDeletes packet) {
	    assert packet != null && packet.any();
	    synchronized (mDeletesStream) {
	    	mDeletesStream.push(packet);
	    }
	}
	
	/**
	 * Atomically adds the segment private delete packet and publishes the flushed
	 * segments SegmentInfo to the index writer. NOTE: use
	 * {@link #prepareFlushedSegment(FlushedSegment)} to obtain the
	 * {@link SegmentInfo} for the flushed segment.
	 * 
	 * @see #prepareFlushedSegment(FlushedSegment)
	 */
	public synchronized void publishFlushedSegment(ISegmentCommitInfo newSegment,
			FrozenDeletes packet, FrozenDeletes globalPacket) throws IOException {
		// Lock order IW -> BDS
		synchronized (mDeletesStream) {
			if (globalPacket != null && globalPacket.any()) 
				mDeletesStream.push(globalPacket);
			
			// Publishing the segment must be synched on IW -> BDS to make the sure
			// that no merge prunes away the seg. private delete packet
			final long nextGen;
			if (packet != null && packet.any()) {
				nextGen = mDeletesStream.push(packet);
			} else {
				// Since we don't have a delete packet to apply we can get a new
				// generation right away
				nextGen = mDeletesStream.nextGen();
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("publish sets newSegment delGen=" + nextGen + " seg=" 
						+ toSegmentString(newSegment));
			}
			
			newSegment.setBufferedDeletesGen(nextGen);
			mSegmentInfos.add((SegmentCommitInfo)newSegment);
			checkpoint();
		}
	}
	
	public final synchronized void applyAllDeletes() throws IOException {
		mFlushDeletesCount.incrementAndGet();
		
		final DeletesStream.ApplyResult result;
		result = mDeletesStream.applyDeletes(mReaderPool, mSegmentInfos.asList());
		if (result.anyDeletes()) 
			checkpoint();
		
		if (result.getAllDeleted() != null) {
			for (ISegmentCommitInfo info : result.getAllDeleted()) {
				// If a merge has already registered for this
				// segment, we leave it in the readerPool; the
				// merge will skip merging it and will then drop
				// it once it's done:
				if (!getMergeControl().containsMergingSegment(info)) {
					mSegmentInfos.remove(info);
					mReaderPool.drop(info);
				}
			}
			checkpoint();
		}
		
		mDeletesStream.prune(mSegmentInfos);
	}
  
	/** 
	 * Walk through all files referenced by the current
	 *  segmentInfos and ask the Directory to sync each file,
	 *  if it wasn't already.  If that succeeds, then we
	 *  prepare a new segments_N file but do not fully commit
	 *  it. 
	 */
	protected void startCommit(final SegmentInfos toSync, 
			final Map<String,String> commitUserData) throws IOException {
		//assert testPoint("startStartCommit");
		assert mPendingCommit == null;

		if (mHitOOM) {
			throw new IllegalStateException(
					"this writer hit an OutOfMemoryError; cannot commit");
		}

		try {
			if (LOG.isDebugEnabled()) {
				LOG.debug("startCommit index=" + toSegmentString(toLiveInfos(toSync)) + 
						" changeCount=" + mChangeCount);
			}
			
			synchronized(this) {
				assert mLastCommitChangeCount <= mChangeCount;

				if (mPendingCommitChangeCount == mLastCommitChangeCount) {
					mDeleter.decreaseRef(mFilesToCommit);
					mFilesToCommit = null;
					return;
				}

				if (LOG.isDebugEnabled()) { 
					LOG.debug("startCommit index=" + toSegmentString(toLiveInfos(toSync)) 
							+ " changeCount=" + mChangeCount);
				}
				//assert filesExist(toSync);
				
				if (commitUserData != null) 
					toSync.setUserData(commitUserData);
			}

			boolean pendingCommitSet = false;

			try {
				synchronized(this) {
					assert mPendingCommit == null;
					assert mSegmentInfos.getGeneration() == toSync.getGeneration();

					// Exception here means nothing is prepared
					// (this method unwinds everything it did on
					// an exception)
					getIndexFormat().getSegmentInfosFormat().prepareCommit(mDirectory, toSync);

					pendingCommitSet = true;
					mPendingCommit = toSync;
				}

				// This call can take a long time -- 10s of seconds
				// or more.  We do it without sync:
				final Collection<String> filesToSync = toSync.getFileNames(false);
				
				boolean success = false;
				try {
					mDirectory.sync(filesToSync);
					success = true;
				} finally {
					if (!success) {
						pendingCommitSet = false;
						mPendingCommit = null;
						getIndexFormat().getSegmentInfosFormat().rollbackCommit(mDirectory, toSync);
					}
				}

				if (LOG.isDebugEnabled())
					LOG.debug("done all syncs: " + filesToSync);
				
			} finally {
				synchronized(this) {
					// Have our master segmentInfos record the
					// generations we just prepared.  We do this
					// on error or success so we don't
					// double-write a segments_N file.
					mSegmentInfos.updateGeneration(toSync);

					if (!pendingCommitSet) {
						if (LOG.isDebugEnabled())
							LOG.debug("hit exception committing segments file");
						
						// Hit exception
						mDeleter.decreaseRef(mFilesToCommit);
						mFilesToCommit = null;
					}
				}
			}
			
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "startCommit");
		}
	}
	
	@Override
	protected boolean doFlush(boolean applyAllDeletes) 
			throws CorruptIndexException, IOException {
		if (mHitOOM) {
			throw new IllegalStateException(
					"this writer hit an OutOfMemoryError; cannot flush");
		}

		doBeforeFlush();
		
		boolean success = false;
		try {
			if (LOG.isDebugEnabled()) {
				LOG.debug("  start flush: applyAllDeletes=" + applyAllDeletes);
				LOG.debug("  index before flush " + toSegmentString());
			}
			
			final boolean anySegmentFlushed;
      
			synchronized (mFullFlushLock) {
				boolean flushSuccess = false;
				try {
					anySegmentFlushed = mDocWriter.flushAllWriters();
					flushSuccess = true;
				} finally {
					mDocWriter.finishFullFlush(flushSuccess);
				}
			}
			
			synchronized(this) {
				maybeApplyDeletes(applyAllDeletes);
				doAfterFlush();
				if (!anySegmentFlushed) {
					// flushCount is incremented in flushAllThreads
					mFlushCount.incrementAndGet();
				}
				success = true;
				return anySegmentFlushed;
			}
			
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "doFlush");
			// never hit
			return false;
			
		} finally {
			if (!success) {
				if (LOG.isDebugEnabled())
					LOG.debug("hit exception during flush");
			}
		}
	}
	
}
