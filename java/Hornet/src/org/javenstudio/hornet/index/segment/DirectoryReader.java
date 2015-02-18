package org.javenstudio.hornet.index.segment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDirectoryReader;
import org.javenstudio.common.indexdb.IIndexCommit;
import org.javenstudio.common.indexdb.IIndexWriter;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.index.IndexCommit;
import org.javenstudio.common.indexdb.index.IndexFileNames;
import org.javenstudio.common.indexdb.index.IndexWriter;
import org.javenstudio.common.indexdb.index.segment.SegmentInfos;
import org.javenstudio.hornet.codec.IndexFormat;
import org.javenstudio.hornet.codec.SegmentInfosFormat;

/** 
 * DirectoryReader is an implementation of {@link CompositeReader}
 * that can read indexes in a {@link Directory}. 
 *
 * <p>DirectoryReader instances are usually constructed with a call to
 * one of the static <code>open()</code> methods, e.g. {@link
 * #open(Directory)}.
 *
 * <p> For efficiency, in this API documents are often referred to via
 * <i>document numbers</i>, non-negative integers which each name a unique
 * document in the index.  These document numbers are ephemeral -- they may change
 * as documents are added to and deleted from an index.  Clients should thus not
 * rely on a given document having the same number between sessions.
 *
 * <p>
 * <a name="thread-safety"></a><p><b>NOTE</b>: {@link
 * IndexReader} instances are completely thread
 * safe, meaning multiple threads can call any of its methods,
 * concurrently.  If your application requires external
 * synchronization, you should <b>not</b> synchronize on the
 * <code>IndexReader</code> instance; use your own
 * (non-Lucene) objects instead.
 */
public abstract class DirectoryReader extends BaseCompositeReader<AtomicIndexReader> 
		implements IDirectoryReader {

	protected final IDirectory mDirectory;
  
	/** 
	 * Returns a IndexReader reading the index in the given
	 *  Directory
	 * @param directory the index directory
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public static DirectoryReader open(IDirectory directory, IIndexFormat format) 
			throws CorruptIndexException, IOException {
		return StandardDirectoryReader.open(directory, format, null);
	}
  
	/**
	 * Open a near real time IndexReader from the {@link org.apache.lucene.index.IndexWriter}.
	 *
	 * @param writer The IndexWriter to open from
	 * @param applyAllDeletes If true, all buffered deletes will
	 * be applied (made visible) in the returned reader.  If
	 * false, the deletes are not applied but remain buffered
	 * (in IndexWriter) so that they will be applied in the
	 * future.  Applying deletes can be costly, so if your app
	 * can tolerate deleted documents being returned you might
	 * gain some performance by passing false.
	 * @return The new IndexReader
	 * @throws CorruptIndexException
	 * @throws IOException if there is a low-level IO error
	 *
	 * @see #openIfChanged(DirectoryReader,IndexWriter,boolean)
	 */
	public static DirectoryReader open(IIndexWriter writer, boolean applyAllDeletes) 
			throws CorruptIndexException, IOException {
		return (DirectoryReader)writer.getReader(applyAllDeletes);
	}

	/** 
	 * Expert: returns an IndexReader reading the index in the given
	 *  {@link IndexCommit}.
	 * @param commit the commit point to open
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public static DirectoryReader open(final IIndexFormat format, final IIndexCommit commit) 
			throws CorruptIndexException, IOException {
		return StandardDirectoryReader.open(commit.getDirectory(), format, commit);
	}

	/**
	 * If the index has changed since the provided reader was
	 * opened, open and return a new reader; else, return
	 * null.  The new reader, if not null, will be the same
	 * type of reader as the previous one, ie an NRT reader
	 * will open a new NRT reader, a MultiReader will open a
	 * new MultiReader,  etc.
	 *
	 * <p>This method is typically far less costly than opening a
	 * fully new <code>DirectoryReader</code> as it shares
	 * resources (for example sub-readers) with the provided
	 * <code>DirectoryReader</code>, when possible.
	 *
	 * <p>The provided reader is not closed (you are responsible
	 * for doing so); if a new reader is returned you also
	 * must eventually close it.  Be sure to never close a
	 * reader while other threads are still using it; see
	 * {@link SearcherManager} to simplify managing this.
	 *
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 * @return null if there are no changes; else, a new
	 * DirectoryReader instance which you must eventually close
	 */  
	public static DirectoryReader openIfChanged(DirectoryReader oldReader) throws IOException {
		final DirectoryReader newReader = (DirectoryReader)oldReader.doOpenIfChanged();
		assert newReader != oldReader;
		return newReader;
	}

	/**
	 * If the IndexCommit differs from what the
	 * provided reader is searching, open and return a new
	 * reader; else, return null.
	 *
	 * @see #openIfChanged(DirectoryReader)
	 */
	public static DirectoryReader openIfChanged(DirectoryReader oldReader, IIndexCommit commit) 
			throws IOException {
		final DirectoryReader newReader = (DirectoryReader)oldReader.doOpenIfChanged(commit);
		assert newReader != oldReader;
		return newReader;
	}

	/**
	 * Expert: If there changes (committed or not) in the
	 * {@link IndexWriter} versus what the provided reader is
	 * searching, then open and return a new
	 * IndexReader searching both committed and uncommitted
	 * changes from the writer; else, return null (though, the
	 * current implementation never returns null).
	 *
	 * <p>This provides "near real-time" searching, in that
	 * changes made during an {@link IndexWriter} session can be
	 * quickly made available for searching without closing
	 * the writer nor calling {@link IndexWriter#commit}.
	 *
	 * <p>It's <i>near</i> real-time because there is no hard
	 * guarantee on how quickly you can get a new reader after
	 * making changes with IndexWriter.  You'll have to
	 * experiment in your situation to determine if it's
	 * fast enough.  As this is a new and experimental
	 * feature, please report back on your findings so we can
	 * learn, improve and iterate.</p>
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
	 * over, so far.</p>
	 *
	 * <p><b>NOTE</b>: Once the writer is closed, any
	 * outstanding readers may continue to be used.  However,
	 * if you attempt to reopen any of those readers, you'll
	 * hit an {@link org.apache.lucene.store.AlreadyClosedException}.</p>
	 *
	 * @return DirectoryReader that covers entire index plus all
	 * changes made so far by this IndexWriter instance, or
	 * null if there are no new changes
	 *
	 * @param writer The IndexWriter to open from
	 *
	 * @param applyAllDeletes If true, all buffered deletes will
	 * be applied (made visible) in the returned reader.  If
	 * false, the deletes are not applied but remain buffered
	 * (in IndexWriter) so that they will be applied in the
	 * future.  Applying deletes can be costly, so if your app
	 * can tolerate deleted documents being returned you might
	 * gain some performance by passing false.
	 *
	 * @throws IOException
	 */
	public static DirectoryReader openIfChanged(DirectoryReader oldReader, IIndexWriter writer, 
			boolean applyAllDeletes) throws IOException {
		final DirectoryReader newReader = (DirectoryReader)oldReader.doOpenIfChanged(
				writer, applyAllDeletes);
		assert newReader != oldReader;
		return newReader;
	}

	/** 
	 * Returns all commit points that exist in the Directory.
	 *  Normally, because the default is {@link
	 *  KeepOnlyLastCommitDeletionPolicy}, there would be only
	 *  one commit point.  But if you're using a custom {@link
	 *  IndexDeletionPolicy} then there could be many commits.
	 *  Once you have a given commit, you can open a reader on
	 *  it by calling {@link DirectoryReader#open(IndexCommit)}
	 *  There must be at least one commit in
	 *  the Directory, else this method throws {@link
	 *  IndexNotFoundException}.  Note that if a commit is in
	 *  progress while this method is running, that commit
	 *  may or may not be returned.
	 *  
	 *  @return a sorted list of {@link IndexCommit}s, from oldest 
	 *  to latest. 
	 */
	public static List<IndexCommit> listCommits(IndexFormat format, IDirectory dir) 
			throws IOException {
		final String[] files = dir.listAll();
		final List<IndexCommit> commits = new ArrayList<IndexCommit>();

		SegmentInfos latest = new SegmentInfos(dir);
		SegmentInfosFormat.read(latest, format);
		final long currentGen = latest.getGeneration();

		commits.add(new DirectoryReaderCommit(latest));

		for (int i=0; files != null && i < files.length; i++) {
			final String fileName = files[i];

			if (fileName.startsWith(IndexFileNames.SEGMENTS) && 
			   !fileName.equals(IndexFileNames.SEGMENTS_GEN) &&
				IndexFileNames.generationFromSegmentsFileName(fileName) < currentGen) {

				SegmentInfos sis = new SegmentInfos(dir);
				try {
					// IOException allowed to throw there, in case
					// segments_N is corrupt
					SegmentInfosFormat.read(sis, format, fileName);
				} catch (FileNotFoundException fnfe) {
					// LUCENE-948: on NFS (and maybe others), if
					// you have writers switching back and forth
					// between machines, it's very likely that the
					// dir listing will be stale and will claim a
					// file segments_X exists when in fact it
					// doesn't.  So, we catch this and handle it
					// as if the file does not exist
					sis = null;
				}

				if (sis != null)
					commits.add(new DirectoryReaderCommit(sis));
			}
		}

		// Ensure that the commit points are sorted in ascending order.
		Collections.sort(commits);

		return commits;
	}
  
	/**
	 * Returns <code>true</code> if an index exists at the specified directory.
	 * @param  directory the directory to check for an index
	 * @return <code>true</code> if an index exists; <code>false</code> otherwise
	 * @throws IOException if there is a problem with accessing the index
	 */
	public static boolean indexExists(IIndexFormat format, IDirectory directory) throws IOException {
		try {
			SegmentInfosFormat.read(new SegmentInfos(directory), format);
			return true;
		} catch (IOException ioe) {
			return false;
		}
	}

	/**
	 * Expert: Constructs a {@code DirectoryReader} on the given subReaders.
	 * @param segmentReaders the wrapped atomic index segment readers. This array is
	 * returned by {@link #getSequentialSubReaders} and used to resolve the correct
	 * subreader for docID-based methods. <b>Please note:</b> This array is <b>not</b>
	 * cloned and not protected for modification outside of this reader.
	 * Subclasses of {@code DirectoryReader} should take care to not allow
	 * modification of this internal array, e.g. {@link #doOpenIfChanged()}.
	 */
	protected DirectoryReader(IDirectory directory, IAtomicReader[] segmentReaders) 
			throws CorruptIndexException, IOException {
		super((AtomicIndexReader[])segmentReaders);
		mDirectory = directory;
	}
  
	/** Returns the directory this index resides in. */
	@Override
	public final IDirectory getDirectory() {
		// Don't ensureOpen here -- in certain cases, when a
		// cloned/reopened reader needs to commit, it may call
		// this method on the closed original reader
		return mDirectory;
	}

	/** 
	 * Implement this method to support {@link #openIfChanged(DirectoryReader)}.
	 * If this reader does not support reopen, return {@code null}, so
	 * client code is happy. This should be consistent with {@link #isCurrent}
	 * (should always return {@code true}) if reopen is not supported.
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 * @return null if there are no changes; else, a new
	 * DirectoryReader instance.
	 */
	protected abstract IDirectoryReader doOpenIfChanged() 
			throws CorruptIndexException, IOException;

	/** 
	 * Implement this method to support {@link #openIfChanged(DirectoryReader,IndexCommit)}.
	 * If this reader does not support reopen from a specific {@link IndexCommit},
	 * throw {@link UnsupportedOperationException}.
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 * @return null if there are no changes; else, a new DirectoryReader instance.
	 */
	protected abstract IDirectoryReader doOpenIfChanged(final IIndexCommit commit) 
			throws CorruptIndexException, IOException;

	/** 
	 * Implement this method to support {@link #openIfChanged(DirectoryReader,IndexWriter,boolean)}.
	 * If this reader does not support reopen from {@link IndexWriter},
	 * throw {@link UnsupportedOperationException}.
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 * @return null if there are no changes; else, a new DirectoryReader instance.
	 */
	protected abstract IDirectoryReader doOpenIfChanged(IIndexWriter writer, boolean applyAllDeletes) 
			throws CorruptIndexException, IOException;

	/**
	 * Version number when this IndexReader was opened.
	 *
	 * <p>This method
	 * returns the version recorded in the commit that the
	 * reader opened.  This version is advanced every time
	 * a change is made with {@link IndexWriter}.</p>
	 */
	public abstract long getVersion();

	/**
	 * Check whether any new changes have occurred to the
	 * index since this reader was opened.
	 *
	 * <p>If this reader was created by calling {@link #open},  
	 * then this method checks if any further commits 
	 * (see {@link IndexWriter#commit}) have occurred in the 
	 * directory.</p>
	 *
	 * <p>If instead this reader is a near real-time reader
	 * (ie, obtained by a call to {@link
	 * DirectoryReader#open(IndexWriter,boolean)}, or by calling {@link #openIfChanged}
	 * on a near real-time reader), then this method checks if
	 * either a new commit has occurred, or any new
	 * uncommitted changes have taken place via the writer.
	 * Note that even if the writer has only performed
	 * merging, this method will still return false.</p>
	 *
	 * <p>In any event, if this returns false, you should call
	 * {@link #openIfChanged} to get a new reader that sees the
	 * changes.</p>
	 *
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException           if there is a low-level IO error
	 */
	public abstract boolean isCurrent() throws CorruptIndexException, IOException;

	/**
	 * Expert: return the IndexCommit that this reader has opened.
	 */
	public abstract IIndexCommit getIndexCommit() throws CorruptIndexException, IOException;

}
