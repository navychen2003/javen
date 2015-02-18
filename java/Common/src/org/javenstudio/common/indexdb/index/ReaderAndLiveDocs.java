package org.javenstudio.common.indexdb.index;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.util.Bits;

public class ReaderAndLiveDocs {
	
    private final IAtomicReader mReader;
    private final Bits mLiveDocs;
    private final int mNumDeletedDocs;

    public ReaderAndLiveDocs(IAtomicReader reader, Bits liveDocs, int numDeletedDocs) {
    	mReader = reader;
    	mLiveDocs = liveDocs;
    	mNumDeletedDocs = numDeletedDocs;
    }
    
    public final IAtomicReader getReader() { return mReader; }
    public final Bits getLiveDocs() { return mLiveDocs; }
    public final int getNumDeletedDocs() { return mNumDeletedDocs; }
    
}
