package org.javenstudio.hornet.index;

import java.io.IOException;

import org.javenstudio.common.indexdb.Constants;
import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IBytesReader;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDataOutput;
import org.javenstudio.common.indexdb.IDocTermOrds;
import org.javenstudio.common.indexdb.IDocTerms;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IIndexReaderRef;
import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITermContext;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.index.IndexContext;
import org.javenstudio.common.indexdb.index.segment.SegmentCommitInfo;
import org.javenstudio.common.indexdb.search.FixedBitSet;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.IndexFormat;
import org.javenstudio.hornet.codec.stored.StoredIndexFormat;
import org.javenstudio.hornet.index.field.MultiFields;
import org.javenstudio.hornet.index.segment.SegmentReader;
import org.javenstudio.hornet.index.term.DocTermOrds;
import org.javenstudio.hornet.index.term.DocTermsImpl;
import org.javenstudio.hornet.index.term.DocTermsIndexImpl;
import org.javenstudio.hornet.index.term.TermContext;
import org.javenstudio.hornet.search.OpenFixedBitSet;

public class AdvancedIndexContext extends IndexContext {

	public static AdvancedIndexContext getOrCreate() { 
		synchronized (sLock) { 
			if (sInstance == null) 
				sInstance = new AdvancedIndexContext();
			
			return (AdvancedIndexContext)sInstance;
		}
	}
	
	protected AdvancedIndexContext() {
		super();
		mIndexFormat = new StoredIndexFormat(this);
	}
	
	@Override
	public FixedBitSet newFixedBitSet(int numBits) { 
		return new OpenFixedBitSet(numBits);
	}
	
	@Override
	public ISegmentReader newSegmentReader(ISegmentCommitInfo si) 
			throws IOException { 
		return new SegmentReader((IndexFormat)getIndexFormat(), (SegmentCommitInfo)si);
	}
	
	@Override
	public ISegmentReader newSegmentReader(ISegmentReader reader, Bits liveDocs, 
			int numDocs) throws IOException { 
		SegmentReader r = (SegmentReader)reader;
		return new SegmentReader(r.getCommitInfo(), r.getReaders(), liveDocs, numDocs);
	}
	
	@Override
	public boolean isMerge() { return false; }
	
	/**
	 * Returns default buffer sizes for the given {@link IIndexContext}
	 */
	@Override
	public int getInputBufferSize() {
		//switch (context.context) {
		//case DEFAULT:
		//case FLUSH:
		//case READ:
		//  return BUFFER_SIZE;
		//case MERGE:
		//  return MERGE_BUFFER_SIZE;
		//default:
    	//  assert false : "unknown IOContext " + context.context;
      		return Constants.INPUT_BUFFER_SIZE;
      	//}
	}
	
	@Override
	public int getOutputBufferSize() {
		return Constants.OUTPUT_BUFFER_SIZE;
	}
	
	@Override
	public ITerms getMultiFieldsTerms(IIndexReader r, String field) 
			throws IOException { 
		return MultiFields.getTerms(r, field);
	}
	
	@Override
	public ITermContext buildTermContext(IIndexReaderRef context, ITerm term, 
			boolean cache) throws IOException { 
		return TermContext.build(context, term, cache);
	}
	
	@Override
	public IDocTermOrds createDocTermOrds(IAtomicReader reader, String field) 
			throws IOException { 
		return new DocTermOrds(reader, field);
	}
	
	@Override
	public IDocTerms createDocTerms(IBytesReader bytes, IIntsReader docToOffset) { 
		return new DocTermsImpl(bytes, docToOffset);
	}
	
	@Override
	public IDocTermsIndex createDocTermsIndex(IBytesReader bytes, 
			IIntsReader termOrdToBytesOffset, IIntsReader docToTermOrd, int numOrd) { 
		return new DocTermsIndexImpl(bytes, termOrdToBytesOffset, docToTermOrd, numOrd);
	}
	
	@Override
	public void writeCodecHeader(IDataOutput out, String codec, int version)
			throws IOException { 
		CodecUtil.writeHeader(out, codec, version);
	}
	
	@Override
	public int checkCodecHeader(IDataInput in, String codec, 
			int minVersion, int maxVersion) throws IOException { 
		return CodecUtil.checkHeader(in, codec, minVersion, maxVersion);
	}
	
	@Override
	public int checkCodecHeaderNoMagic(IDataInput in, String codec, 
			int minVersion, int maxVersion) throws IOException { 
		return CodecUtil.checkHeaderNoMagic(in, codec, minVersion, maxVersion);
	}
	
}
