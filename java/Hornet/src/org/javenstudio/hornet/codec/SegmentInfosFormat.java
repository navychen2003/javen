package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.ISegmentInfos;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.codec.ISegmentInfosFormat;
import org.javenstudio.common.indexdb.index.IndexFileNames;
import org.javenstudio.common.indexdb.index.segment.SegmentInfos;
import org.javenstudio.hornet.index.segment.SegmentFinder;

public abstract class SegmentInfosFormat implements ISegmentInfosFormat {

	/**
	 * The file format version for the segments_N codec header
	 */
	public static final int VERSION = 0;

	/** 
	 * Used for the segments.gen file only!
	 * Whenever you add a new format, make it 1 smaller (negative version logic)! 
	 */
	public static final int FORMAT_SEGMENTS_GEN_CURRENT = -2;
	
	/** 
	 * Advanced configuration of retry logic in loading
	 * segments_N file 
	 */
	private static int sDefaultGenLookaheadCount = 10;
	
	/**
	 * Advanced: set how many times to try incrementing the
	 * gen when loading the segments file.  This only runs if
	 * the primary (listing directory) and secondary (opening
	 * segments.gen file) methods fail to find the segments
	 * file.
	 */
	public static void setDefaultGenLookaheadCount(int count) {
		sDefaultGenLookaheadCount = count;
	}
	
	/**
	 * @see #setDefaultGenLookaheadCount
	 */
	public static int getDefaultGenLookahedCount() {
		return sDefaultGenLookaheadCount;
	}
	
	public static void read(ISegmentInfos infos, IIndexFormat format, 
			String segmentFileName) throws CorruptIndexException, IOException {
		read(infos, format, infos.getDirectory(), segmentFileName);
	}
	
	public static void read(ISegmentInfos infos, IIndexFormat format, IDirectory dir, 
			String segmentFileName) throws CorruptIndexException, IOException {
		((SegmentInfosFormat)format.getSegmentInfosFormat()).doReadSegmentInfos(
				dir, infos, segmentFileName);
	}
	
	public static void read(ISegmentInfos infos, IIndexFormat format) 
			throws CorruptIndexException, IOException {
		SegmentFinder.findAndRead((SegmentInfos)infos, (IndexFormat)format);
	}
	
	public static void read(ISegmentInfos infos, IIndexFormat format, IDirectory dir) 
			throws CorruptIndexException, IOException {
		SegmentFinder.findAndRead((SegmentInfos)infos, (IndexFormat)format, dir);
	}
	
	private final IIndexFormat mFormat;
	
	protected SegmentInfosFormat(IIndexFormat format) { 
		mFormat = format;
	}
	
	@Override
	public final IIndexContext getContext() { 
		return mFormat.getContext(); 
	}
	
	@Override
	public final IIndexFormat getIndexFormat() { 
		return mFormat;
	}
	
	@Override
	public String getSegmentInfosFileName(long generation) { 
		return IndexFileNames.getFileNameFromGeneration(
				IndexFileNames.SEGMENTS, "", generation);
	}
	
	@Override
	public String getSegmentsGenFileName() { 
		return IndexFileNames.SEGMENTS_GEN;
	}
	
	@Override
	public ISegmentInfos readSegmentInfos(IDirectory dir) 
			throws CorruptIndexException, IOException { 
		return readSegmentInfos(dir, null, null);
	}
	
	@Override
	public ISegmentInfos readSegmentInfos(IDirectory dir, String segment) 
			throws CorruptIndexException, IOException { 
		return readSegmentInfos(dir, null, segment);
	}
	
	@Override
	public ISegmentInfos readSegmentInfos(IDirectory dir, ISegmentInfos infos) 
			throws CorruptIndexException, IOException { 
		return readSegmentInfos(dir, infos, null);
	}
	
	@Override
	public ISegmentInfos readSegmentInfos(IDirectory dir, ISegmentInfos infos, String segment) 
			throws CorruptIndexException, IOException { 
		if (infos == null)
			infos = newSegmentInfos(dir);
		
		if (segment == null)
			SegmentInfosFormat.read(infos, getIndexFormat(), dir);
		else 
			SegmentInfosFormat.read(infos, getIndexFormat(), dir, segment);
		
		return infos;
	}
	
	@Override
	public ISegmentInfos newSegmentInfos(IDirectory dir) { 
		return newSegmentInfos(dir, null);
	}
	
	public abstract String getCodecName();
	public abstract ISegmentInfos newSegmentInfos(IDirectory dir, ISegmentInfos infos);
	
	/**
	 * Read a particular segmentFileName.  Note that this may
	 * throw an IOException if a commit is in process.
	 *
	 * @param directory -- directory containing the segments file
	 * @param segmentFileName -- segment file to load
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	protected abstract void doReadSegmentInfos(IDirectory dir, ISegmentInfos infos, 
			String segmentFileName) throws CorruptIndexException, IOException;
	
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
	public abstract void prepareCommit(IDirectory dir, ISegmentInfos infos) 
			throws IOException;
	
	public abstract void rollbackCommit(IDirectory dir, ISegmentInfos infos) 
			throws IOException;
	
	public abstract void finishCommit(IDirectory dir, ISegmentInfos infos) 
			throws IOException;
	
}
