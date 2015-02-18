package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IIndexReaderRef;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;

public abstract class TermCollector {
    
	private IAtomicReaderRef mReaderContext;
	private IIndexReaderRef mTopReaderContext;

	public void setReaderContext(IIndexReaderRef topReaderContext, IAtomicReaderRef readerContext) {
		mReaderContext = readerContext;
		mTopReaderContext = topReaderContext;
	}
	
	public final IAtomicReaderRef getReaderContext() { return mReaderContext; }
	public final IIndexReaderRef getTopReaderContext() { return mTopReaderContext; }
	
	/** return false to stop collecting */
	public abstract boolean collect(BytesRef bytes) throws IOException;

	/** the next segment's {@link TermsEnum} that is used to collect terms */
	public abstract void setNextEnum(ITermsEnum termsEnum) throws IOException;
	
}
