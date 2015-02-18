package org.javenstudio.hornet.index.term;

import org.javenstudio.common.indexdb.IBytesReader;
import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.index.term.DocTerms;
import org.javenstudio.common.indexdb.util.BytesRef;

public final class DocTermsImpl extends DocTerms {
	
    private final IBytesReader mBytes;
    private final IIntsReader mDocToOffset;

    public DocTermsImpl(IBytesReader bytes, IIntsReader docToOffset) {
    	mBytes = bytes;
    	mDocToOffset = docToOffset;
    }

    @Override
    public int size() {
    	return mDocToOffset.size();
    }

    @Override
    public boolean exists(int docID) {
    	return mDocToOffset.get(docID) == 0;
    }

    @Override
    public BytesRef getTerm(int docID, BytesRef ret) {
    	final int pointer = (int) mDocToOffset.get(docID);
    	return mBytes.fill(ret, pointer);
    }
    
}
