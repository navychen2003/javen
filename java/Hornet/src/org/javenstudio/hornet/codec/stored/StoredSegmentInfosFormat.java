package org.javenstudio.hornet.codec.stored;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.ISegmentInfos;
import org.javenstudio.common.indexdb.ThreadInterruptedException;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.index.IndexFileNames;
import org.javenstudio.common.indexdb.index.segment.SegmentCommitInfo;
import org.javenstudio.common.indexdb.index.segment.SegmentInfo;
import org.javenstudio.common.indexdb.index.segment.SegmentInfos;
import org.javenstudio.common.indexdb.store.ChecksumIndexInput;
import org.javenstudio.common.indexdb.store.ChecksumIndexOutput;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.common.util.Logger;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.SegmentInfoReader;
import org.javenstudio.hornet.codec.SegmentInfosFormat;

final class StoredSegmentInfosFormat extends SegmentInfosFormat {
	private static final Logger LOG = Logger.getLogger(StoredSegmentInfosFormat.class);

	private static final String CODEC_NAME = "Lucene40";
	
	public StoredSegmentInfosFormat(IIndexFormat format) { 
		super(format);
	}
	
	@Override
	public String getCodecName() { return CODEC_NAME; }
	
	@Override
	public ISegmentInfos newSegmentInfos(IDirectory dir, ISegmentInfos infos) { 
		StoredSegmentInfos storedInfos = new StoredSegmentInfos(dir);
		if (infos != null) 
			storedInfos.copyFrom((SegmentInfos)infos);
		
		return storedInfos;
	}
	
	/**
	 * Read a particular segmentFileName.  Note that this may
	 * throw an IOException if a commit is in process.
	 *
	 * @param directory -- directory containing the segments file
	 * @param segmentFileName -- segment file to load
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	@Override
	protected void doReadSegmentInfos(IDirectory dir, ISegmentInfos infos, String segmentFileName) 
			throws CorruptIndexException, IOException { 
		doRead(dir, (SegmentInfos)infos, segmentFileName);
	}
	
	private StoredSegmentInfos castSegmentInfos(IDirectory dir, ISegmentInfos infos) { 
		if (infos == null) 
			throw new NullPointerException();
		
		if (!(infos instanceof StoredSegmentInfos)) {
			throw new IllegalArgumentException("ISegmentInfos(" + infos.getClass().getName() 
					+ ") is not StoredSegmentInfos");
		}
		
		StoredSegmentInfos storedInfos = (StoredSegmentInfos)infos;
		
		if (infos.getDirectory() != dir) {
			throw new IllegalArgumentException("ISegmentInfos directory: " + infos.getDirectory() 
					+ " not equals: " + dir);
		}
		
		return storedInfos;
	}
	
	/** 
	 * Call this to start a commit.  This writes the new
	 *  segments file, but writes an invalid checksum at the
	 *  end, so that it is not visible to readers.  Once this
	 *  is called you must call {@link #finishCommit} to complete
	 *  the commit or {@link #rollbackCommit} to abort it.
	 *  <p>
	 *  Note: {@link #changed()} should be called prior to this
	 *  method if changes have been made to this {@link SegmentInfos} instance
	 *  </p>  
	 */
	@Override
	public final void prepareCommit(IDirectory dir, ISegmentInfos infos) throws IOException {
		final StoredSegmentInfos storedInfos = castSegmentInfos(dir, infos);
		
		if (storedInfos.getPendingOutput() != null) 
			throw new IllegalStateException("prepareCommit was already called");
		
		doWrite(dir, storedInfos);
	}
	
	@Override
	public final void rollbackCommit(IDirectory directory, ISegmentInfos infos) throws IOException {
		final StoredSegmentInfos storedInfos = castSegmentInfos(directory, infos);
		
		ChecksumIndexOutput output = storedInfos.getPendingOutput();
		if (output != null) {
			try {
				output.close();
			} catch (Throwable t) {
				// Suppress so we keep throwing the original exception
				// in our caller
			}

			// Must carefully compute fileName from "generation"
			// since lastGeneration isn't incremented:
			try {
				final String segmentFileName = getSegmentInfosFileName(storedInfos.getGeneration());
				directory.deleteFile(segmentFileName);
			} catch (Throwable t) {
				// Suppress so we keep throwing the original exception
				// in our caller
			}
			
			storedInfos.setPendingOutput(null);
		}
	}

	@Override
	public final void finishCommit(IDirectory directory, ISegmentInfos infos) throws IOException {
		final IIndexContext context = getContext();
		final StoredSegmentInfos storedInfos = castSegmentInfos(directory, infos);
		
		ChecksumIndexOutput output = storedInfos.getPendingOutput();
		if (output == null) 
			throw new IllegalStateException("prepareCommit was not called");
    
		boolean success = false;
		try {
			output.finishCommit();
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(output);
				rollbackCommit(directory, infos);
			} else {
				output.close();
				storedInfos.setPendingOutput(null);
			}
		}

		// NOTE: if we crash here, we have left a segments_N
		// file in the directory in a possibly corrupt state (if
		// some bytes made it to stable storage and others
		// didn't).  But, the segments_N file includes checksum
		// at the end, which should catch this case.  So when a
		// reader tries to read it, it will throw a
		// CorruptIndexException, which should cause the retry
		// logic in SegmentInfos to kick in and load the last
		// good (previous) segments_N-1 file.
		final String fileName = getSegmentInfosFileName(storedInfos.getGeneration());
		
		success = false;
		try {
			directory.sync(Collections.singleton(fileName));
			success = true;
		} finally {
			if (!success) {
				try {
					directory.deleteFile(fileName);
				} catch (Throwable t) {
					// Suppress so we keep throwing the original exception
				}
			}
		}

		storedInfos.setLastGeneration(storedInfos.getGeneration());

		try {
			IIndexOutput genOutput = directory.createOutput(context, getSegmentsGenFileName()); 
			try {
				genOutput.writeInt(FORMAT_SEGMENTS_GEN_CURRENT);
				genOutput.writeLong(storedInfos.getGeneration());
				genOutput.writeLong(storedInfos.getGeneration());
			} finally {
				genOutput.close();
				directory.sync(Collections.singleton(getSegmentsGenFileName()));
			}
		} catch (Throwable t) {
			// It's OK if we fail to write this file since it's
			// used only as one of the retry fallbacks.
			try {
				directory.deleteFile(getSegmentsGenFileName());
			} catch (Throwable t2) {
				// Ignore; this file is only used in a retry
				// fallback on init.
			}
			if (t instanceof ThreadInterruptedException) 
				throw (ThreadInterruptedException) t;
		}
	}
	
	private void doWrite(IDirectory directory, StoredSegmentInfos infos) throws IOException {
		final IIndexContext context = getContext();
		
		final String segmentsFileName = infos.getNextSegmentFileName();
		final Set<String> upgradedSIFiles = new HashSet<String>();
    
		// Always advance the generation on write:
		if (infos.getGeneration() == -1) 
			infos.setGeneration(1);
		else
			infos.increaseGeneration(1);
		
		if (LOG.isDebugEnabled())
			LOG.debug("writting SegmentInfos: " + infos);
		
		ChecksumIndexOutput output = null;
		boolean success = false;

		try {
			output = new ChecksumIndexOutput(directory.createOutput(context, segmentsFileName));
			CodecUtil.writeHeader(output, "segments", SegmentInfosFormat.VERSION);
			output.writeLong(infos.getVersion()); 
			output.writeInt(infos.getCounter()); // write counter
			output.writeInt(infos.size()); // write infos
			
			for (ISegmentCommitInfo siPerCommit : infos) {
				ISegmentInfo si = siPerCommit.getSegmentInfo();
				output.writeString(si.getName());
				output.writeString(getCodecName());
				output.writeLong(siPerCommit.getDelGen());
				output.writeInt(siPerCommit.getDelCount());
				
				assert si.getDirectory() == directory;
				assert siPerCommit.getDelCount() <= si.getDocCount();
			}
			
			output.writeStringStringMap(infos.getUserData());
			infos.setPendingOutput(output);
			
			if (LOG.isDebugEnabled())
				LOG.debug("write SegmentInfos done.");
			
			success = true;
		} finally {
			if (!success) {
				// We hit an exception above; try to close the file
				// but suppress any exception:
				IOUtils.closeWhileHandlingException(output);

				for (String fileName : upgradedSIFiles) {
					try {
						directory.deleteFile(fileName);
					} catch (Throwable t) {
						// Suppress so we keep throwing the original exception
					}
				}

				try {
					// Try not to leave a truncated segments_N file in
					// the index:
					directory.deleteFile(segmentsFileName);
				} catch (Throwable t) {
					// Suppress so we keep throwing the original exception
				}
			}
		}
	}
	
	/**
	 * Read a particular segmentFileName.  Note that this may
	 * throw an IOException if a commit is in process.
	 *
	 * @param directory -- directory containing the segments file
	 * @param segmentFileName -- segment file to load
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	@SuppressWarnings("unused")
	private void doRead(IDirectory directory, SegmentInfos infos, String segmentsFileName) 
			throws CorruptIndexException, IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("reading SegmentInfos: " + segmentsFileName);
		
		// Clear any previous segments:
		infos.clear();
		infos.setGeneration(IndexFileNames.generationFromSegmentsFileName(segmentsFileName));
		infos.setLastGeneration(infos.getGeneration());
		
		ChecksumIndexInput input = new ChecksumIndexInput(
				directory.openInput(getContext(), segmentsFileName));
		
		boolean success = false;
		try {
			final int formatMagic = input.readInt();
			if (formatMagic == CodecUtil.CODEC_MAGIC) {
				// 4.0+
				CodecUtil.checkHeaderNoMagic(input, "segments", 
						SegmentInfosFormat.VERSION, SegmentInfosFormat.VERSION);
				
				infos.setVersion(input.readLong());
				infos.setCounter(input.readInt());
				int numSegments = input.readInt();
				
				SegmentInfoReader reader = (SegmentInfoReader)getIndexFormat().getSegmentInfoFormat()
						.createReader(directory);
				
				for (int seg=0; seg < numSegments; seg++) {
					String segName = input.readString();
					String codecName = input.readString();
					
					//if (LOG.isDebugEnabled())
					//	LOG.debug("read SegmentInfo: "+segName+" codecName: "+codecName);
					
					SegmentInfo info = (SegmentInfo)reader.readSegmentInfo(segName);
					
					long delGen = input.readLong();
					int delCount = input.readInt();
					assert delCount <= info.getDocCount();
					
					infos.add(new SegmentCommitInfo(getIndexFormat(), info, delCount, delGen));
				}
				
				infos.setUserData(input.readStringStringMap());
				
			} else {
				throw new CorruptIndexException("unknown segment format");
			}

			final long checksumNow = input.getChecksum();
			final long checksumThen = input.readLong();
			if (checksumNow != checksumThen) {
				throw new CorruptIndexException("checksum mismatch in segments file " + 
						"(resource: " + input + ")");
			}

			if (LOG.isDebugEnabled())
				LOG.debug("read SegmentInfos done: " + infos);
			
			success = true;
		} finally {
			if (!success) {
				// Clear any segment infos we had loaded so we
				// have a clean slate on retry:
				infos.clear();
				IOUtils.closeWhileHandlingException(input);
			} else {
				input.close();
			}
		}
	}
	
}
