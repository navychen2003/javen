package org.javenstudio.hornet.index.segment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDirectoryReader;
import org.javenstudio.common.indexdb.IIndexCommit;
import org.javenstudio.common.indexdb.IIndexWriter;
import org.javenstudio.common.indexdb.ISegmentInfos;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.index.IndexCommit;
import org.javenstudio.common.indexdb.index.IndexWriter;
import org.javenstudio.common.indexdb.index.ReadersAndLiveDocs;
import org.javenstudio.common.indexdb.index.segment.SegmentCommitInfo;
import org.javenstudio.common.indexdb.index.segment.SegmentInfo;
import org.javenstudio.common.indexdb.index.segment.SegmentInfos;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.hornet.codec.IndexFormat;
import org.javenstudio.hornet.codec.SegmentInfosFormat;

public final class StandardDirectoryReader extends DirectoryReader {

	private final IIndexFormat mFormat;
	private final IIndexWriter mWriter;
	private final ISegmentInfos mSegmentInfos;
	private final boolean mApplyAllDeletes;
  
	/** called only from static open() methods */
	StandardDirectoryReader(IDirectory directory, IIndexFormat format, 
			IAtomicReader[] readers, IIndexWriter writer, ISegmentInfos sis, 
			boolean applyAllDeletes) throws IOException {
		super(directory, readers);
		mFormat = format;
		mWriter = writer;
		mSegmentInfos = sis;
		mApplyAllDeletes = applyAllDeletes;
	}

	@Override
	public final IIndexContext getContext() { 
		return mFormat.getContext();
	}
	
	/** called from DirectoryReader.open(...) methods */
	static DirectoryReader open(final IDirectory directory, 
			final IIndexFormat format, final IIndexCommit commit) 
			throws CorruptIndexException, IOException {
		return (DirectoryReader) new SegmentFinder(format.getContext(), directory) {
				@Override
				protected Object doBody(String segmentFileName) throws CorruptIndexException, IOException {
					SegmentInfos sis = new SegmentInfos(directory);
					SegmentInfosFormat.read(sis, format, segmentFileName);
					
					final SegmentReader[] readers = new SegmentReader[sis.size()];
					for (int i = sis.size()-1; i >= 0; i--) {
						IOException prior = null;
						boolean success = false;
						try {
							readers[i] = new SegmentReader(
									(IndexFormat)format, (SegmentCommitInfo)sis.getCommitInfo(i)); 
							success = true;
						} catch(IOException ex) {
							prior = ex;
						} finally {
							if (!success)
								IOUtils.closeWhileHandlingException(prior, readers);
						}
					}
					
					return new StandardDirectoryReader(directory, format, readers, null, sis, false);
				}
			}.run(commit);
	}

	/** Used by near real-time search */
	public static DirectoryReader open(IndexWriter writer, 
			ISegmentInfos infos, boolean applyAllDeletes) throws IOException {
		// IndexWriter synchronizes externally before calling
		// us, which ensures infos will not change; so there's
		// no need to process segments in reverse order
		final int numSegments = infos.size();

		final List<SegmentReader> readers = new ArrayList<SegmentReader>();
		final IDirectory dir = writer.getDirectory();
		final SegmentInfos segmentInfos = (SegmentInfos)infos.clone();
		
		int infosUpto = 0;
		for (int i=0; i < numSegments; i++) {
			IOException prior = null;
			boolean success = false;
			try {
				final SegmentCommitInfo info = (SegmentCommitInfo)infos.getCommitInfo(i);
				assert info.getSegmentInfo().getDirectory() == dir;
				final ReadersAndLiveDocs rld = writer.getReaderPool().get(info, true);
				try {
					final SegmentReader reader = (SegmentReader)rld.getReadOnlyClone();
					if (reader.getNumDocs() > 0) { // || writer.getKeepFullyDeletedSegments()) {
						// Steal the ref:
						readers.add(reader);
						infosUpto ++;
					} else {
						reader.close();
						segmentInfos.remove(infosUpto);
					}
				} finally {
					writer.getReaderPool().release(rld);
				}
				success = true;
			} catch(IOException ex) {
				prior = ex;
			} finally {
				if (!success) 
					IOUtils.closeWhileHandlingException(prior, readers);
			}
		}
		
		return new StandardDirectoryReader(dir, writer.getIndexFormat(), 
				readers.toArray(new SegmentReader[readers.size()]),
				writer, segmentInfos, applyAllDeletes);
	}

	/** This constructor is only used for {@link #doOpenIfChanged(SegmentInfos, IndexWriter)} */
	private static DirectoryReader open(IDirectory directory, IIndexFormat format,
			IIndexWriter writer, ISegmentInfos infos, List<? extends AtomicIndexReader> oldReaders) 
			throws IOException {
		// we put the old SegmentReaders in a map, that allows us
		// to lookup a reader using its segment name
		final Map<String,Integer> segmentReaders = new HashMap<String,Integer>();

		if (oldReaders != null) {
			// create a Map SegmentName->SegmentReader
			for (int i = 0, c = oldReaders.size(); i < c; i++) {
				final SegmentReader sr = (SegmentReader) oldReaders.get(i);
				segmentReaders.put(sr.getSegmentName(), Integer.valueOf(i));
			}
		}
    
		SegmentReader[] newReaders = new SegmentReader[infos.size()];
    
		// remember which readers are shared between the old and the re-opened
		// DirectoryReader - we have to incRef those readers
		boolean[] readerShared = new boolean[infos.size()];
    
		for (int i = infos.size() - 1; i>=0; i--) {
			// find SegmentReader for this segment
			Integer oldReaderIndex = segmentReaders.get(infos.getCommitInfo(i).getSegmentInfo().getName());
			if (oldReaderIndex == null) {
				// this is a new segment, no old SegmentReader can be reused
				newReaders[i] = null;
			} else {
				// there is an old reader for this segment - we'll try to reopen it
				newReaders[i] = (SegmentReader) oldReaders.get(oldReaderIndex.intValue());
			}

			boolean success = false;
			IOException prior = null;
			try {
				final SegmentCommitInfo segPer = (SegmentCommitInfo)infos.getCommitInfo(i);
				final SegmentInfo segInfo = (SegmentInfo)segPer.getSegmentInfo();
				
				final SegmentCommitInfo newPer = (newReaders[i] != null) ? newReaders[i].getCommitInfo() : null;
				final SegmentInfo newInfo = (newPer != null) ? (SegmentInfo)newPer.getSegmentInfo() : null;
				
				SegmentReader newReader;
				
				if (newReaders[i] == null || newInfo == null || 
					segInfo.getUseCompoundFile() != newInfo.getUseCompoundFile()) {
					// this is a new reader; in case we hit an exception we can close it safely
					newReader = new SegmentReader((IndexFormat)format, segPer);
					readerShared[i] = false;
					newReaders[i] = newReader;
					
				} else {
					if (newPer.getDelGen() == segPer.getDelGen()) {
						// No change; this reader will be shared between
						// the old and the new one, so we must incRef
						// it:
						readerShared[i] = true;
						newReaders[i].increaseRef();
						
					} else {
						readerShared[i] = false;
						// Steal the ref returned by SegmentReader ctor:
						assert segInfo.getDirectory() == newInfo.getDirectory();
						assert segPer.hasDeletions();
						newReaders[i] = new SegmentReader((IndexFormat)format, segPer, 
								newReaders[i].getReaders());
					}
				}
				
				success = true;
				
			} catch (IOException ex) {
				prior = ex;
				
			} finally {
				if (!success) {
					for (i++; i < infos.size(); i++) {
						if (newReaders[i] != null) {
							try {
								if (!readerShared[i]) {
									// this is a new subReader that is not used by the old one,
									// we can close it
									newReaders[i].close();
								} else {
									// this subReader is also used by the old reader, so instead
									// closing we must decRef it
									newReaders[i].decreaseRef();
								}
							} catch (IOException ex) {
								if (prior == null) prior = ex;
							}
						}
					}
				}
				// throw the first exception
				if (prior != null) throw prior;
			}
		}
		
		return new StandardDirectoryReader(directory, format, newReaders, writer, infos, false);
	}

	@Override
	protected IDirectoryReader doOpenIfChanged() throws CorruptIndexException, IOException {
		return doOpenIfChanged(null);
	}

	@Override
	protected IDirectoryReader doOpenIfChanged(final IIndexCommit commit) 
			throws CorruptIndexException, IOException {
		ensureOpen();

		// If we were obtained by writer.getReader(), re-ask the
		// writer to get a new reader.
		if (mWriter != null) 
			return doOpenFromWriter(commit);
		else 
			return doOpenNoWriter(commit);
	}

	@Override
	protected IDirectoryReader doOpenIfChanged(IIndexWriter writer, boolean applyAllDeletes) 
			throws CorruptIndexException, IOException {
		ensureOpen();
		
		if (writer == mWriter && applyAllDeletes == mApplyAllDeletes) 
			return doOpenFromWriter(null);
		else 
			return writer.getReader(applyAllDeletes);
	}

	private DirectoryReader doOpenFromWriter(IIndexCommit commit) 
			throws CorruptIndexException, IOException {
		if (commit != null) {
			throw new IllegalArgumentException("a reader obtained from " + 
					"IndexWriter.getReader() cannot currently accept a commit");
		}

		if (((IndexWriter)mWriter).isCurrent(mSegmentInfos)) 
			return null;

		DirectoryReader reader = (DirectoryReader)mWriter.getReader(mApplyAllDeletes);

		// If in fact no changes took place, return null:
		if (reader.getVersion() == mSegmentInfos.getVersion()) {
			reader.decreaseRef();
			return null;
		}

		return reader;
	}

	private synchronized DirectoryReader doOpenNoWriter(IIndexCommit commit) 
			throws CorruptIndexException, IOException {
		if (commit == null) {
			if (isCurrent()) return null;
			
		} else {
			if (getDirectory() != commit.getDirectory()) 
				throw new IOException("the specified commit does not match the specified Directory");
			
			if (mSegmentInfos != null && commit.getSegmentsFileName().equals(
					mSegmentInfos.getSegmentsFileName())) {
				return null;
			}
		}

		return (DirectoryReader) new SegmentFinder(mFormat.getContext(), getDirectory()) {
				@Override
				protected Object doBody(String segmentFileName) throws CorruptIndexException, IOException {
					final SegmentInfos infos = new SegmentInfos(getDirectory());
					SegmentInfosFormat.read(infos, mFormat, segmentFileName);
					return doOpenIfChanged(infos, null);
				}
			}.run(commit);
	}

	synchronized DirectoryReader doOpenIfChanged(SegmentInfos infos, IndexWriter writer) 
			throws CorruptIndexException, IOException {
		return StandardDirectoryReader.open(getDirectory(), mFormat, writer, infos, 
				getSequentialSubReaders());
	}

	@Override
	public long getVersion() {
		ensureOpen();
		return mSegmentInfos.getVersion();
	}

	@Override
	public boolean isCurrent() throws CorruptIndexException, IOException {
		ensureOpen();
		if (mWriter == null || mWriter.isClosed()) {
			// Fully read the segments file: this ensures that it's
			// completely written so that if
			// IndexWriter.prepareCommit has been called (but not
			// yet commit), then the reader will still see itself as
			// current:
			SegmentInfos sis = new SegmentInfos(getDirectory());
			SegmentInfosFormat.read(sis, mFormat);

			// we loaded SegmentInfos from the directory
			return sis.getVersion() == mSegmentInfos.getVersion();
		} else {
			return ((IndexWriter)mWriter).isCurrent(mSegmentInfos);
		}
	}

	@Override
	protected synchronized void doClose() throws IOException {
		IOException ioe = null;
		for (final AtomicIndexReader r : getSequentialSubReaders()) {
			// try to close each reader, even if an exception is thrown
			try {
				r.decreaseRef();
			} catch (IOException e) {
				if (ioe == null) ioe = e;
			}
		}

		if (mWriter != null) {
			// Since we just closed, writer may now be able to
			// delete unused files:
			((IndexWriter)mWriter).deletePendingFiles();
		}

		// throw the first exception
		if (ioe != null) throw ioe;
	}

	@Override
	public IndexCommit getIndexCommit() throws IOException {
		ensureOpen();
		return new DirectoryReaderCommit(mSegmentInfos);
	}

	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder();
		buffer.append(getClass().getSimpleName());
		buffer.append('(');
		final String segmentsFile = mSegmentInfos.getSegmentsFileName();
		if (segmentsFile != null) 
			buffer.append(segmentsFile).append(":").append(mSegmentInfos.getVersion());
		if (mWriter != null) 
			buffer.append(":nrt");
		for (final AtomicIndexReader r : getSequentialSubReaders()) {
			buffer.append(' ');
			buffer.append(r);
		}
		buffer.append(')');
		return buffer.toString();
	}
	
}
