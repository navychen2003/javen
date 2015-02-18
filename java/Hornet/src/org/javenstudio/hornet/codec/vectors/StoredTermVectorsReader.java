package org.javenstudio.hornet.codec.vectors;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.TermVectorsReader;

/**
 * Lucene 4.0 Term Vectors reader.
 * <p>
 * It reads .tvd, .tvf, and .tvx files.
 * 
 * @see Lucene40TermVectorsFormat
 */
final class StoredTermVectorsReader extends TermVectorsReader {

	private final StoredTermVectorsFormat mFormat;
	private final IDirectory mDirectory;
	private final String mSegment;
	
	private IFieldInfos mFieldInfos;
	private IIndexInput mTvxIn;
	private IIndexInput mTvdIn;
	private IIndexInput mTvfIn;
	
	private int mNumTotalDocs;
	private int mSize;
  
	@Override
	public TermVectorsReader clone() {
	    IIndexInput cloneTvx = null;
	    IIndexInput cloneTvd = null;
	    IIndexInput cloneTvf = null;

	    // These are null when a TermVectorsReader was created
	    // on a segment that did not have term vectors saved
	    if (mTvxIn != null && mTvdIn != null && mTvfIn != null) {
	    	cloneTvx = (IIndexInput)mTvxIn.clone();
	    	cloneTvd = (IIndexInput)mTvdIn.clone();
	    	cloneTvf = (IIndexInput)mTvfIn.clone();
	    }
	    
	    return new StoredTermVectorsReader(mFormat, mDirectory, mSegment, mFieldInfos, 
	    		cloneTvx, cloneTvd, cloneTvf, mSize, mNumTotalDocs);
	}
	
	/** Used by clone. */
	private StoredTermVectorsReader(StoredTermVectorsFormat format, 
			IDirectory dir, String segment, IFieldInfos fieldInfos, 
			IIndexInput tvx, IIndexInput tvd, IIndexInput tvf, 
			int size, int numTotalDocs) {
		mFormat = format;
		mDirectory = dir;
		mSegment = segment;
		mFieldInfos = fieldInfos;
		mTvxIn = tvx;
    	mTvdIn = tvd;
    	mTvfIn = tvf;
    	mSize = size;
    	mNumTotalDocs = numTotalDocs;
	}
    
	/** Sole constructor. */
	public StoredTermVectorsReader(StoredTermVectorsFormat format, 
			IDirectory dir, ISegmentInfo si, IFieldInfos fieldInfos) 
			throws IOException {
		mFormat = format;
		mDirectory = dir;
		mSegment = si.getName();
		
		final int size = si.getDocCount();
    
		boolean success = false;
		try {
			mTvxIn = mDirectory.openInput(
					mFormat.getContext(), mFormat.getVectorsIndexFileName(mSegment));
			
			final int tvxVersion = CodecUtil.checkHeader(mTvxIn, mFormat.getIndexCodecName(), 
					StoredTermVectorsFormat.VERSION_START, StoredTermVectorsFormat.VERSION_CURRENT);
			
			mTvdIn = mDirectory.openInput(
					mFormat.getContext(), mFormat.getVectorsDocumentsFileName(mSegment));
			
			final int tvdVersion = CodecUtil.checkHeader(mTvdIn, mFormat.getDocumentsCodecName(), 
					StoredTermVectorsFormat.VERSION_START, StoredTermVectorsFormat.VERSION_CURRENT);
			
			mTvfIn = mDirectory.openInput(
					mFormat.getContext(), mFormat.getVectorsFieldsFileName(mSegment));
			
			final int tvfVersion = CodecUtil.checkHeader(mTvfIn, mFormat.getFieldsCodecName(), 
					StoredTermVectorsFormat.VERSION_START, StoredTermVectorsFormat.VERSION_CURRENT);
			
			assert StoredTermVectorsFormat.HEADER_LENGTH_INDEX == mTvxIn.getFilePointer();
			assert StoredTermVectorsFormat.HEADER_LENGTH_DOCS == mTvdIn.getFilePointer();
			assert StoredTermVectorsFormat.HEADER_LENGTH_FIELDS == mTvfIn.getFilePointer();
			assert tvxVersion == tvdVersion;
			assert tvxVersion == tvfVersion;

			mNumTotalDocs = (int) (mTvxIn.length()-StoredTermVectorsFormat.HEADER_LENGTH_INDEX >> 4);
			mSize = mNumTotalDocs;
			assert size == 0 || mNumTotalDocs == size;

			mFieldInfos = fieldInfos;
			
			success = true;
		} finally {
			// With lock-less commits, it's entirely possible (and
			// fine) to hit a FileNotFound exception above. In
			// this case, we want to explicitly close any subset
			// of things that were opened so that we don't have to
			// wait for a GC to do so.
			if (!success) {
				try {
					close();
				} catch (Throwable t) {} // ensure we throw our original exception
			}
		}
	}

	final String getSegmentName() { return mSegment; }
	final IFieldInfos getFieldInfos() { return mFieldInfos; }
	
	// Used for bulk copy when merging
	final IIndexInput getTvdStream() { return mTvdIn; }
	// Used for bulk copy when merging
	final IIndexInput getTvfStream() { return mTvfIn; }
	// Used for bulk copy when merging
	final IIndexInput getTvxStream() { return mTvxIn; }

	// Not private to avoid synthetic access$NNN methods
	final void seekTvx(final int docNum) throws IOException {
		mTvxIn.seek(docNum * 16L + StoredTermVectorsFormat.HEADER_LENGTH_INDEX);
	}

	/** 
	 * Retrieve the length (in bytes) of the tvd and tvf
	 *  entries for the next numDocs starting with
	 *  startDocID.  This is used for bulk copying when
	 *  merging segments, if the field numbers are
	 *  congruent.  Once this returns, the tvf & tvd streams
	 *  are seeked to the startDocID. 
	 */
	final void rawDocs(int[] tvdLengths, int[] tvfLengths, 
			int startDocID, int numDocs) throws IOException {
		if (mTvxIn == null) {
			Arrays.fill(tvdLengths, 0);
			Arrays.fill(tvfLengths, 0);
			return;
		}

		seekTvx(startDocID);

		long tvdPosition = mTvxIn.readLong();
		mTvdIn.seek(tvdPosition);

		long tvfPosition = mTvxIn.readLong();
		mTvfIn.seek(tvfPosition);

		long lastTvdPosition = tvdPosition;
		long lastTvfPosition = tvfPosition;

		int count = 0;
		while (count < numDocs) {
			final int docID = startDocID + count + 1;
			assert docID <= mNumTotalDocs;
			
			if (docID < mNumTotalDocs)  {
				tvdPosition = mTvxIn.readLong();
				tvfPosition = mTvxIn.readLong();
			} else {
				tvdPosition = mTvdIn.length();
				tvfPosition = mTvfIn.length();
				assert count == numDocs-1;
			}
			
			tvdLengths[count] = (int) (tvdPosition-lastTvdPosition);
			tvfLengths[count] = (int) (tvfPosition-lastTvfPosition);
			count++;
			
			lastTvdPosition = tvdPosition;
			lastTvfPosition = tvfPosition;
		}
	}

	@Override
	public void close() throws IOException {
		IOUtils.close(mTvxIn, mTvdIn, mTvfIn);
	}

	/** @return The number of documents in the reader  */
	final int size() { return mSize; }
	
	@Override
	public IFields getFields(int docID) throws IOException {
	    if (docID < 0 || docID >= mNumTotalDocs) {
	    	throw new IllegalArgumentException("doID=" + docID + " is out of bounds [0.." 
	    			+ (mNumTotalDocs-1) + "]");
	    }
	    
	    if (mTvxIn != null) {
	    	IFields fields = new TermVectorsFields(this, docID);
	    	if (fields.size() > 0) {
	    		// TODO: we can improve writer here, eg write 0 into
	    		// tvx file, so we know on first read from tvx that
	    		// this doc has no TVs
	    		return fields;
	    	}
	    }
	    
	    return null;
	}
	
}
