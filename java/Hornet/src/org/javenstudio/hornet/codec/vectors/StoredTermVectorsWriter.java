package org.javenstudio.hornet.codec.vectors;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.codec.ITermVectorsFormat;
import org.javenstudio.common.indexdb.store.DirectoryHelper;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.common.indexdb.util.StringHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.TermVectorsWriter;

/**
 * Term Vectors writer. It writes .tvd, .tvf, and .tvx files.
 * 
 * make a new 4.0 TV format that encodes better
 *  - use startOffset (not endOffset) as base for delta on
 *    next startOffset because today for syns or ngrams or
 *    WDF or shingles etc. we are encoding negative vints
 *    (= slow, 5 bytes per)
 *  - if doc has no term vectors, write 0 into the tvx
 *    file; saves a seek to tvd only to read a 0 vint (and
 *    saves a byte in tvd)
 */
final class StoredTermVectorsWriter extends TermVectorsWriter {
	private static final Logger LOG = Logger.getLogger(StoredTermVectorsWriter.class);
	
	/** 
	 * Maximum number of contiguous documents to bulk-copy
     * when merging term vectors 
     */
	private final static int MAX_RAW_MERGE_DOCS = 4192;
	
	private final StoredTermVectorsFormat mFormat;
	private final IDirectory mDirectory;
	private final String mSegment;
	
	private IIndexOutput mTvxOutput = null;
	private IIndexOutput mTvdOutput = null;
	private IIndexOutput mTvfOutput = null;
  
	private long mPointers[] = new long[10]; 	// pointers to the tvf before writing each field 
	private int mFieldCount = 0;        		// number of fields we have written so far for this document
	private int mNumVectorFields = 0;   		// total number of fields we will write for this document
	private String mLastFieldName = null;
	
	private final BytesRef mLastTerm = new BytesRef(10);

	// NOTE: we override addProx, so we don't need to buffer when indexing.
	// we also don't buffer during bulk merges.
	private int mOffsetStartBuffer[] = new int[10];
	private int mOffsetEndBuffer[] = new int[10];
	private int mOffsetIndex = 0;
	private int mOffsetFreq = 0;
	private boolean mStorePositions = false;
	private boolean mStoreOffsets = false;
	
	private int mLastPosition = 0;
	private int mLastOffset = 0;
	
	public StoredTermVectorsWriter(StoredTermVectorsFormat format, 
			IDirectory dir, String segment) throws IOException {
		mFormat = format;
		mDirectory = dir;
		mSegment = segment;
		
		boolean success = false;
		try {
			// Open files for TermVector storage
			mTvxOutput = mDirectory.createOutput(
					mFormat.getContext(), mFormat.getVectorsIndexFileName(mSegment));
			CodecUtil.writeHeader(mTvxOutput, mFormat.getIndexCodecName(), 
					StoredTermVectorsFormat.VERSION_CURRENT);
			
			mTvdOutput = mDirectory.createOutput(
					mFormat.getContext(), mFormat.getVectorsDocumentsFileName(mSegment));
			CodecUtil.writeHeader(mTvdOutput, mFormat.getDocumentsCodecName(), 
					StoredTermVectorsFormat.VERSION_CURRENT);
			
			mTvfOutput = mDirectory.createOutput(
					mFormat.getContext(), mFormat.getVectorsFieldsFileName(mSegment));
			CodecUtil.writeHeader(mTvfOutput, mFormat.getFieldsCodecName(), 
					StoredTermVectorsFormat.VERSION_CURRENT);
			
			assert StoredTermVectorsFormat.HEADER_LENGTH_INDEX == mTvxOutput.getFilePointer();
			assert StoredTermVectorsFormat.HEADER_LENGTH_DOCS == mTvdOutput.getFilePointer();
			assert StoredTermVectorsFormat.HEADER_LENGTH_FIELDS == mTvfOutput.getFilePointer();
			
			success = true;
		} finally {
			if (!success) 
				abort();
		}
	}
 
	@Override
	public void startDocument(int numVectorFields) throws IOException {
		mLastFieldName = null;
		mNumVectorFields = numVectorFields;
		mTvxOutput.writeLong(mTvdOutput.getFilePointer());
		mTvxOutput.writeLong(mTvfOutput.getFilePointer());
		mTvdOutput.writeVInt(numVectorFields);
		mFieldCount = 0;
		mPointers = ArrayUtil.grow(mPointers, numVectorFields);
	}

	@Override
	public void startField(IFieldInfo info, int numTerms, boolean positions, boolean offsets) 
			throws IOException {
		assert mLastFieldName == null || info.getName().compareTo(mLastFieldName) > 0 : 
			"fieldName=" + info.getName() + " lastFieldName=" + mLastFieldName;
		
		mLastFieldName = info.getName();
		mStorePositions = positions;
		mStoreOffsets = offsets;
		mLastTerm.setLength(0);
		mPointers[mFieldCount++] = mTvfOutput.getFilePointer();
		mTvdOutput.writeVInt(info.getNumber());
		mTvfOutput.writeVInt(numTerms);
		
		byte bits = 0x0;
		if (positions)
			bits |= StoredTermVectorsFormat.STORE_POSITIONS_WITH_TERMVECTOR;
		if (offsets)
			bits |= StoredTermVectorsFormat.STORE_OFFSET_WITH_TERMVECTOR;
		mTvfOutput.writeByte(bits);
    
		assert mFieldCount <= mNumVectorFields;
		if (mFieldCount == mNumVectorFields) {
			// last field of the document
			// this is crazy because the file format is crazy!
			for (int i = 1; i < mFieldCount; i++) {
				mTvdOutput.writeVLong(mPointers[i] - mPointers[i-1]);
			}
		}
	}
  
	@Override
	public void startTerm(BytesRef term, int freq) throws IOException {
		final int prefix = StringHelper.bytesDifference(mLastTerm, term);
		final int suffix = term.getLength() - prefix;
		
		mTvfOutput.writeVInt(prefix);
		mTvfOutput.writeVInt(suffix);
		mTvfOutput.writeBytes(term.getBytes(), term.getOffset() + prefix, suffix);
		mTvfOutput.writeVInt(freq);
		mLastTerm.copyBytes(term);
		mLastPosition = mLastOffset = 0;
    
		if (mStoreOffsets && mStorePositions) {
			// we might need to buffer if its a non-bulk merge
			mOffsetStartBuffer = ArrayUtil.grow(mOffsetStartBuffer, freq);
			mOffsetEndBuffer = ArrayUtil.grow(mOffsetEndBuffer, freq);
			mOffsetIndex = 0;
			mOffsetFreq = freq;
		}
	}

	@Override
	public void addProx(int numProx, IDataInput positions, IDataInput offsets) 
			throws IOException {
		// TODO: technically we could just copy bytes and not re-encode if we knew the length...
		if (positions != null) {
			for (int i = 0; i < numProx; i++) {
				mTvfOutput.writeVInt(positions.readVInt());
			}
		}
    
		if (offsets != null) {
			for (int i = 0; i < numProx; i++) {
				mTvfOutput.writeVInt(offsets.readVInt());
				mTvfOutput.writeVInt(offsets.readVInt());
			}
		}
	}

	@Override
	public void addPosition(int position, int startOffset, int endOffset) 
			throws IOException {
		if (mStorePositions && mStoreOffsets) {
			// write position delta
			mTvfOutput.writeVInt(position - mLastPosition);
			mLastPosition = position;
      
			// buffer offsets
			mOffsetStartBuffer[mOffsetIndex] = startOffset;
			mOffsetEndBuffer[mOffsetIndex] = endOffset;
			mOffsetIndex ++;
      
			// dump buffer if we are done
			if (mOffsetIndex == mOffsetFreq) {
				for (int i = 0; i < mOffsetIndex; i++) {
					mTvfOutput.writeVInt(mOffsetStartBuffer[i] - mLastOffset);
					mTvfOutput.writeVInt(mOffsetEndBuffer[i] - mOffsetStartBuffer[i]);
					mLastOffset = mOffsetEndBuffer[i];
				}
			}
		} else if (mStorePositions) {
			// write position delta
			mTvfOutput.writeVInt(position - mLastPosition);
			mLastPosition = position;
			
		} else if (mStoreOffsets) {
			// write offset deltas
			mTvfOutput.writeVInt(startOffset - mLastOffset);
			mTvfOutput.writeVInt(endOffset - startOffset);
			mLastOffset = endOffset;
		}
	}

	@Override
	public void abort() {
		try {
			close();
		} catch (IOException ignored) {
		}
		DirectoryHelper.deleteFilesIgnoringExceptions(
				mDirectory, 
				mFormat.getVectorsIndexFileName(mSegment), 
				mFormat.getVectorsDocumentsFileName(mSegment), 
				mFormat.getVectorsFieldsFileName(mSegment));
	}
  
	/**
	 * Do a bulk copy of numDocs documents from reader to our
	 * streams.  This is used to expedite merging, if the
	 * field numbers are congruent.
	 */
	final void addRawDocuments(StoredTermVectorsReader reader, 
			int[] tvdLengths, int[] tvfLengths, int numDocs) throws IOException {
		long tvdPosition = mTvdOutput.getFilePointer();
		long tvfPosition = mTvfOutput.getFilePointer();
		long tvdStart = tvdPosition;
		long tvfStart = tvfPosition;
		
		for (int i=0; i < numDocs; i++) {
			mTvxOutput.writeLong(tvdPosition);
			tvdPosition += tvdLengths[i];
			mTvxOutput.writeLong(tvfPosition);
			tvfPosition += tvfLengths[i];
		}
		
		mTvdOutput.copyBytes(reader.getTvdStream(), tvdPosition-tvdStart);
		mTvfOutput.copyBytes(reader.getTvfStream(), tvfPosition-tvfStart);
		
		assert mTvdOutput.getFilePointer() == tvdPosition;
		assert mTvfOutput.getFilePointer() == tvfPosition;
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("addRawDocuments: writeSeg=" + mSegment + " readSeg=" 
					+ reader.getSegmentName() + " numDocs=" + numDocs);
			
			//LOG.debug("addRawDocuments: tvdFilePointer=" + mTvdOutput.getFilePointer() 
			//		+ " vs tvdPosition=" + tvdPosition + ", tvdStart=" + tvdStart + ", numDocs=" + numDocs);
			//LOG.debug("addRawDocuments: tvfFilePointer=" + mTvfOutput.getFilePointer() 
			//		+ " vs tvfPosition=" + tvfPosition + ", tvfStart=" + tvfStart + ", numDocs=" + numDocs);
		}
	}

	@Override
	public final int merge(IMergeState mergeState) throws IOException {
	    // Used for bulk-reading raw bytes for term vectors
	    int rawDocLengths[] = new int[MAX_RAW_MERGE_DOCS];
	    int rawDocLengths2[] = new int[MAX_RAW_MERGE_DOCS];
	
	    int numDocs = 0;
	    
	    for (int i=0; i < mergeState.getReaderCount(); i++) {
	    	IAtomicReader reader = mergeState.getReaderAt(i);
	    	ISegmentReader matchingSegmentReader = mergeState.getMatchingSegmentReaderAt(i);
	    	
	    	StoredTermVectorsReader matchingVectorsReader = null;
	    	if (matchingSegmentReader != null) {
	    		ITermVectorsFormat.Reader vectorsReader = matchingSegmentReader.getTermVectorsReader();
	
	    		if (vectorsReader != null && vectorsReader instanceof StoredTermVectorsReader) 
	    			matchingVectorsReader = (StoredTermVectorsReader) vectorsReader;
	    	}
	    	
	    	Bits liveDocs = reader.getLiveDocs();
	    	if (liveDocs != null) {
	    		numDocs += copyVectorsWithDeletions(mergeState, 
	    				matchingVectorsReader, reader, rawDocLengths, rawDocLengths2);
	    	} else {
	    		numDocs += copyVectorsNoDeletions(mergeState, 
	    				matchingVectorsReader, reader, rawDocLengths, rawDocLengths2);
	    	}
	    }
	    
	    finish(mergeState.getFieldInfos(), numDocs);
	    
	    return numDocs;
	}

	@Override
	public void finish(IFieldInfos fis, int numDocs) throws IOException {
		if (StoredTermVectorsFormat.HEADER_LENGTH_INDEX+((long) numDocs)*16 != mTvxOutput.getFilePointer()) {
			// This is most likely a bug in Sun JRE 1.6.0_04/_05;
			// we detect that the bug has struck, here, and
			// throw an exception to prevent the corruption from
			// entering the index.  See LUCENE-1282 for
			// details.
			throw new RuntimeException("tvx size mismatch: mergedDocs is " + numDocs + 
					" but tvx size is " + mTvxOutput.getFilePointer() + " file=" + mTvxOutput.toString() + 
					"; now aborting this merge to prevent index corruption");
		}
	}

	/** Close all streams. */
	@Override
	public void close() throws IOException {
		// make an effort to close all streams we can but remember and re-throw
		// the first exception encountered in this process
		IOUtils.close(mTvxOutput, mTvdOutput, mTvfOutput);
		mTvxOutput = mTvdOutput = mTvfOutput = null;
	}

	@Override
	public Comparator<BytesRef> getComparator() throws IOException {
		return BytesRef.getUTF8SortedAsUnicodeComparator();
	}
	
	private int copyVectorsWithDeletions(IMergeState mergeState,
			final StoredTermVectorsReader matchingVectorsReader, final IAtomicReader reader,
			int rawDocLengths[], int rawDocLengths2[]) throws IOException {
		final int maxDoc = reader.getMaxDoc();
		final Bits liveDocs = reader.getLiveDocs();
		
		int totalNumDocs = 0;
		
		if (matchingVectorsReader != null) {
			// We can bulk-copy because the fieldInfos are "congruent"
			for (int docNum = 0; docNum < maxDoc;) {
				if (!liveDocs.get(docNum)) {
					// skip deleted docs
					++docNum;
					continue;
				}
				
				// We can optimize this case (doing a bulk byte copy) since the field
				// numbers are identical
				int start = docNum, numDocs = 0;
				do {
					docNum++;
					numDocs++;
					if (docNum >= maxDoc) break;
					if (!liveDocs.get(docNum)) {
						docNum++;
						break;
					}
				} while(numDocs < MAX_RAW_MERGE_DOCS);

				matchingVectorsReader.rawDocs(rawDocLengths, rawDocLengths2, start, numDocs);
				addRawDocuments(matchingVectorsReader, rawDocLengths, rawDocLengths2, numDocs);
				totalNumDocs += numDocs;
				
				mergeState.checkAbort(300 * numDocs);
			}
			
		} else {
			for (int docNum = 0; docNum < maxDoc; docNum++) {
				if (!liveDocs.get(docNum)) {
					// skip deleted docs
					continue;
				}

				// NOTE: it's very important to first assign to vectors then pass it to
				// termVectorsWriter.addAllDocVectors; see LUCENE-1282
				IFields vectors = reader.getTermVectors(docNum);
				addAllDocVectors(vectors, mergeState.getFieldInfos());
				totalNumDocs++;
				
				mergeState.checkAbort(300);
			}
		}
		return totalNumDocs;
	}
	
	private int copyVectorsNoDeletions(IMergeState mergeState,
			final StoredTermVectorsReader matchingVectorsReader, final IAtomicReader reader,
			int rawDocLengths[], int rawDocLengths2[]) throws IOException {
		final int maxDoc = reader.getMaxDoc();
		
		if (matchingVectorsReader != null) {
			// We can bulk-copy because the fieldInfos are "congruent"
			int docCount = 0;
			while (docCount < maxDoc) {
				int len = Math.min(MAX_RAW_MERGE_DOCS, maxDoc - docCount);
				
				matchingVectorsReader.rawDocs(rawDocLengths, rawDocLengths2, docCount, len);
				addRawDocuments(matchingVectorsReader, rawDocLengths, rawDocLengths2, len);
				docCount += len;
				
				mergeState.checkAbort(300 * len);
			}
			
		} else {
			for (int docNum = 0; docNum < maxDoc; docNum++) {
				// NOTE: it's very important to first assign to vectors then pass it to
				// termVectorsWriter.addAllDocVectors; see LUCENE-1282
				IFields vectors = reader.getTermVectors(docNum);
				addAllDocVectors(vectors, mergeState.getFieldInfos());
				mergeState.checkAbort(300);
			}
		}
		
		return maxDoc;
	}
	
}
