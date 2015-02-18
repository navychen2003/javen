package org.javenstudio.hornet.codec.postings;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.index.term.DocsEnum;
import org.javenstudio.common.indexdb.store.ByteArrayDataInput;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.TermPostingsReader;
import org.javenstudio.hornet.codec.SegmentReadState;
import org.javenstudio.hornet.codec.block.BlockTermState;

/** 
 * Concrete class that reads the 4.0 frq/prox
 * postings format. 
 *  
 * @see StoredPostingsFormat
 */
final class StoredTermPostingsReader extends TermPostingsReader {

	static final int BUFFERSIZE = 64;
	
	protected final StoredTermPostingsFormat mFormat;
	protected final IDirectory mDirectory;
	protected final String mSegment;
	
	protected final IIndexInput mFreqIn;
	protected final IIndexInput mProxIn;

	protected int mSkipInterval;
	protected int mMaxSkipLevels;
	protected int mSkipMinimum;

	@Override
	public IDirectory getDirectory() { 
		return mDirectory;
	}
	
	public StoredTermPostingsReader(StoredTermPostingsFormat format, IDirectory dir, 
			SegmentReadState state) throws IOException {
		mFormat = format;
		mDirectory = dir;
		mSegment = state.getSegmentInfo().getName();
		
		IIndexInput freqIn = null;
		IIndexInput proxIn = null;
		
		boolean success = false;
		try {
			freqIn = mDirectory.openInput(mFormat.getContext(), 
					mFormat.getPostingsFreqFileName(mSegment));
			
			CodecUtil.checkHeader(freqIn, mFormat.getFreqCodecName(), 
					StoredTermPostingsFormat.VERSION_START, StoredTermPostingsFormat.VERSION_START);
			
			// TODO: hasProx should (somehow!) become codec private,
			// but it's tricky because 1) FIS.hasProx is global (it
			// could be all fields that have prox are written by a
			// different codec), 2) the field may have had prox in
			// the past but all docs w/ that field were deleted.
			// Really we'd need to init prxOut lazily on write, and
			// then somewhere record that we actually wrote it so we
			// know whether to open on read:
			if (state.getFieldInfos().hasProx()) {
				proxIn = mDirectory.openInput(mFormat.getContext(), 
						mFormat.getPostingsProxFileName(mSegment));
				
				CodecUtil.checkHeader(proxIn, mFormat.getProxCodecName(), 
						StoredTermPostingsFormat.VERSION_START, StoredTermPostingsFormat.VERSION_START);
				
			} else 
				proxIn = null;
			
			mFreqIn = freqIn;
			mProxIn = proxIn;
			
			success = true;
		} finally {
			if (!success) 
				IOUtils.closeWhileHandlingException(freqIn, proxIn);
		}
	}

	@Override
	public void init(IIndexInput termsIn) throws IOException {
		// Make sure we are talking to the matching past writer
		CodecUtil.checkHeader(termsIn, mFormat.getTermsCodecName(), 
				StoredTermPostingsFormat.VERSION_START, StoredTermPostingsFormat.VERSION_START);

		mSkipInterval = termsIn.readInt();
		mMaxSkipLevels = termsIn.readInt();
		mSkipMinimum = termsIn.readInt();
	}

	@Override
	public BlockTermState newTermState() {
		return new StandardTermState();
	}

	@Override
	public void close() throws IOException {
		try {
			if (mFreqIn != null) 
				mFreqIn.close();
		} finally {
			if (mProxIn != null) 
				mProxIn.close();
		}
	}

	/** 
	 * Reads but does not decode the byte[] blob holding
     * metadata for the current terms block 
     */
	@Override
	public void readTermsBlock(IIndexInput termsIn, IFieldInfo fieldInfo, 
			ITermState _termState) throws IOException {
		final StandardTermState termState = (StandardTermState) _termState;
		final int len = termsIn.readVInt();

		if (termState.mBytes == null) {
			termState.mBytes = new byte[ArrayUtil.oversize(len, 1)];
			termState.mBytesReader = new ByteArrayDataInput();
		} else if (termState.mBytes.length < len) {
			termState.mBytes = new byte[ArrayUtil.oversize(len, 1)];
		}

		termsIn.readBytes(termState.mBytes, 0, len);
		termState.mBytesReader.reset(termState.mBytes, 0, len);
	}

	@Override
	public void nextTerm(IFieldInfo fieldInfo, ITermState _termState)
			throws IOException {
		final StandardTermState termState = (StandardTermState) _termState;
		final boolean isFirstTerm = termState.getTermBlockOrd() == 0;

		if (isFirstTerm) {
			termState.mFreqOffset = termState.mBytesReader.readVLong();
		} else {
			termState.mFreqOffset += termState.mBytesReader.readVLong();
		}
		assert termState.mFreqOffset < mFreqIn.length();

		if (termState.getDocFreq() >= mSkipMinimum) {
			termState.mSkipOffset = termState.mBytesReader.readVInt();
			assert termState.mFreqOffset + termState.mSkipOffset < mFreqIn.length();
		}

		if (fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0) {
			if (isFirstTerm) 
				termState.mProxOffset = termState.mBytesReader.readVLong();
			else 
				termState.mProxOffset += termState.mBytesReader.readVLong();
		}
	}
    
	@Override
	public IDocsEnum getDocs(IFieldInfo fieldInfo, ITermState termState, Bits liveDocs, 
			IDocsEnum reuse, int flags) throws IOException {
		if (canReuse(reuse, liveDocs)) 
			return ((SegmentDocsEnumBase) reuse).reset(fieldInfo, (StandardTermState)termState);
		
		return newDocsEnum(liveDocs, fieldInfo, (StandardTermState)termState);
	}
  
	private boolean canReuse(IDocsEnum reuse, Bits liveDocs) {
		if (reuse != null && (reuse instanceof SegmentDocsEnumBase)) {
			SegmentDocsEnumBase docsEnum = (SegmentDocsEnumBase) reuse;
			// If you are using ParellelReader, and pass in a
			// reused DocsEnum, it could have come from another
			// reader also using standard codec
			if (docsEnum.mStartFreqIn == mFreqIn) {
				// we only reuse if the the actual the incoming enum has the same liveDocs 
				// as the given liveDocs
				return liveDocs == docsEnum.mLiveDocs;
			}
		}
		return false;
	}
  
	private DocsEnum newDocsEnum(Bits liveDocs, IFieldInfo fieldInfo, 
			StandardTermState termState) throws IOException {
		if (liveDocs == null) 
			return new AllDocsSegmentDocsEnum(this, mFreqIn).reset(fieldInfo, termState);
		else 
			return new LiveDocsSegmentDocsEnum(this, mFreqIn, liveDocs).reset(fieldInfo, termState);
	}

	@Override
	public IDocsAndPositionsEnum getDocsAndPositions(IFieldInfo fieldInfo, 
			ITermState termState, Bits liveDocs, IDocsAndPositionsEnum reuse, int flags)
			throws IOException {
		boolean hasOffsets = fieldInfo.getIndexOptions().compareTo(
				IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;

		// TODO: refactor
		if (fieldInfo.hasPayloads() || hasOffsets) {
			SegmentFullPositionsEnum docsEnum;
			if (reuse == null || !(reuse instanceof SegmentFullPositionsEnum)) {
				docsEnum = new SegmentFullPositionsEnum(this, mFreqIn, mProxIn);
				
			} else {
				docsEnum = (SegmentFullPositionsEnum) reuse;
				if (docsEnum.mStartFreqIn != mFreqIn) {
					// If you are using ParellelReader, and pass in a
					// reused DocsEnum, it could have come from another
					// reader also using standard codec
					docsEnum = new SegmentFullPositionsEnum(this, mFreqIn, mProxIn);
				}
			}
			
			return docsEnum.reset(fieldInfo, (StandardTermState) termState, liveDocs);
			
		} else {
			SegmentDocsAndPositionsEnum docsEnum;
			if (reuse == null || !(reuse instanceof SegmentDocsAndPositionsEnum)) {
				docsEnum = new SegmentDocsAndPositionsEnum(this, mFreqIn, mProxIn);
				
			} else {
				docsEnum = (SegmentDocsAndPositionsEnum) reuse;
				if (docsEnum.mStartFreqIn != mFreqIn) {
					// If you are using ParellelReader, and pass in a
					// reused DocsEnum, it could have come from another
					// reader also using standard codec
					docsEnum = new SegmentDocsAndPositionsEnum(this, mFreqIn, mProxIn);
				}
			}
			
			return docsEnum.reset(fieldInfo, (StandardTermState) termState, liveDocs);
		}
	}

}
