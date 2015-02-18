package org.javenstudio.hornet.codec.block;

import java.io.IOException;
import java.util.TreeMap;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldsEnum;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.FieldsProducer;
import org.javenstudio.hornet.codec.TermPostingsReader;
import org.javenstudio.hornet.codec.SegmentReadState;
import org.javenstudio.hornet.codec.TermsFormat;
import org.javenstudio.hornet.store.fst.ByteSequenceOutputs;
import org.javenstudio.hornet.store.fst.Outputs;

/** 
 * A block-based terms index and dictionary that assigns
 *  terms to variable length blocks according to how they
 *  share prefixes.  The terms index is a prefix trie
 *  whose leaves are term blocks.  The advantage of this
 *  approach is that seekExact is often able to
 *  determine a term cannot exist without doing any IO, and
 *  intersection with Automata is very fast.  Note that this
 *  terms dictionary has it's own fixed terms index (ie, it
 *  does not support a pluggable terms index
 *  implementation).
 *
 *  <p><b>NOTE</b>: this terms dictionary does not support
 *  index divisor when opening an IndexReader.  Instead, you
 *  can change the min/maxItemsPerBlock during indexing.</p>
 *
 *  <p>The data structure used by this implementation is very
 *  similar to a burst trie
 *  (http://citeseer.ist.psu.edu/viewdoc/summary?doi=10.1.1.18.3499),
 *  but with added logic to break up too-large blocks of all
 *  terms sharing a given prefix into smaller ones.</p>
 *
 *  <p>Use {@link org.apache.lucene.index.CheckIndex} with the <code>-verbose</code>
 *  option to see summary statistics on the blocks in the
 *  dictionary.
 *
 *  See {@link BlockTreeTermsWriter}.
 */
public class BlockTreeTermsReader extends FieldsProducer {
	private static final boolean LOAD_TERMS_INDEX = true;

	private final TreeMap<String, BlockFieldReader> mFields = 
			new TreeMap<String, BlockFieldReader>();
	
	private final Outputs<BytesRef> mFstOutputs = ByteSequenceOutputs.getSingleton();
	private final BytesRef NO_OUTPUT = mFstOutputs.getNoOutput();
	
	// Open input to the main terms dict file (_X.tib)
	private final IIndexInput mInput;

	// Reads the terms dict entries, to gather state to
	// produce DocsEnum on demand
	private final TermPostingsReader mPostingsReader;

	// keeps the dirStart offset
	private long mDirOffset;
	private long mIndexDirOffset;

	private final TermsFormat mTermsFormat;
	private final IDirectory mDirectory;
	private final String mSegment;
  
	final String getSegmentName() { return mSegment; }
	final TreeMap<String, BlockFieldReader> getFields() { return mFields; }
	final IIndexInput getInput() { return mInput; }
	final TermPostingsReader getPostingsReader() { return mPostingsReader; }
	final Outputs<BytesRef> getOutputs() { return mFstOutputs; }
	final BytesRef getNoOutput() { return NO_OUTPUT; }
	
	public BlockTreeTermsReader(TermsFormat format, SegmentReadState state, 
			TermPostingsReader postingsReader) throws IOException {
		mTermsFormat = format;
		mDirectory = postingsReader.getDirectory();
		mSegment = state.getSegmentInfo().getName();
		mPostingsReader = postingsReader;

		mInput = mDirectory.openInput(mTermsFormat.getContext(), 
				mTermsFormat.getTermsFileName(mSegment));

		boolean success = false;
		IIndexInput indexIn = null;
		try {
			readHeader(mInput);
			
			/**
			 * @see IndexParams.getTermInfosIndexDivisor() Subsamples which indexed
			 *  terms are loaded into RAM. This has the same effect as {@link
			 *  IndexWriterConfig#setTermIndexInterval} except that setting
			 *  must be done at indexing time while this setting can be
			 *  set per reader.  When set to N, then one in every
			 *  N*termIndexInterval terms in the index is loaded into
			 *  memory.  By setting this to a value > 1 you can reduce
			 *  memory usage, at the expense of higher latency when
			 *  loading a TermInfo.  The default value is 1.  Set this
			 *  to -1 to skip loading the terms index entirely.
			 */
			if (LOAD_TERMS_INDEX) {
				indexIn = mDirectory.openInput(mTermsFormat.getContext(), 
						mTermsFormat.getTermsIndexFileName(mSegment));
				
				readIndexHeader(indexIn);
			}

			// Have PostingsReader init itself
			postingsReader.init(mInput);

			// Read per-field details
			seekDir(mInput, mDirOffset);
			if (indexIn != null) 
				seekDir(indexIn, mIndexDirOffset);

			final int numFields = mInput.readVInt();

			for (int i=0; i < numFields; i++) {
				final int field = mInput.readVInt();
				final long numTerms = mInput.readVLong();
				assert numTerms >= 0;
				
				final int numBytes = mInput.readVInt();
				final BytesRef rootCode = new BytesRef(new byte[numBytes]);
				
				mInput.readBytes(rootCode.mBytes, 0, numBytes);
				rootCode.mLength = numBytes;
				
				final IFieldInfo fieldInfo = state.getFieldInfos().getFieldInfo(field);
				assert fieldInfo != null: "field=" + field;
				
				final long sumTotalTermFreq = fieldInfo.getIndexOptions() == IndexOptions.DOCS_ONLY 
						? -1 : mInput.readVLong();
				
				final long sumDocFreq = mInput.readVLong();
				final int docCount = mInput.readVInt();
				final long indexStartFP = (indexIn != null) ? indexIn.readVLong() : 0;
				
				assert !mFields.containsKey(fieldInfo.getName());
				mFields.put(fieldInfo.getName(), new BlockFieldReader(this, 
						fieldInfo, numTerms, rootCode, sumTotalTermFreq, sumDocFreq, 
						docCount, indexStartFP, indexIn));
			}
			
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(indexIn, this);
			} else if (indexIn != null) {
				indexIn.close();
			}
		}
	}

	protected void readHeader(IIndexInput input) throws IOException {
		CodecUtil.checkHeader(input, 
				BlockTreeTermsWriter.TERMS_CODEC_NAME,
				BlockTreeTermsWriter.TERMS_VERSION_START,
				BlockTreeTermsWriter.TERMS_VERSION_CURRENT);
		mDirOffset = input.readLong();    
	}

	protected void readIndexHeader(IIndexInput input) throws IOException {
		CodecUtil.checkHeader(input, 
				BlockTreeTermsWriter.TERMS_INDEX_CODEC_NAME,
				BlockTreeTermsWriter.TERMS_INDEX_VERSION_START,
				BlockTreeTermsWriter.TERMS_INDEX_VERSION_CURRENT);
		mIndexDirOffset = input.readLong();    
	}
  
	protected void seekDir(IIndexInput input, long dirOffset) throws IOException {
		input.seek(dirOffset);
	}

	@Override
	public void close() throws IOException {
		try {
			IOUtils.close(mInput, mPostingsReader);
		} finally { 
			// Clear so refs to terms index is GCable even if
			// app hangs onto us:
			mFields.clear();
		}
	}

	@Override
	public IFieldsEnum iterator() {
		return new TermFieldsEnum(this);
	}

	@Override
	public ITerms getTerms(String field) throws IOException {
		assert field != null;
		return mFields.get(field);
	}

	@Override
	public int size() {
		return mFields.size();
	}

	// for debugging
	static String bytesToString(BytesRef b) {
		if (b == null) {
			return "null";
		} else {
			try {
				return b.utf8ToString() + " " + b;
			} catch (Throwable t) {
				// If BytesRef isn't actually UTF8, or it's eg a
				// prefix of UTF8 that ends mid-unicode-char, we
				// fallback to hex:
				return b.toString();
			}
		}
	}

    @Override
    public String toString() { 
    	return getClass().getSimpleName() + "{hashCode=" + hashCode() 
    			+ ",segment=" + mSegment + ",input=" + mInput 
    			+ ",size=" + sizeNoThrow() + "}";
    }
	
}
