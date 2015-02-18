package org.javenstudio.hornet.codec.vectors;

import java.io.IOException;

import org.javenstudio.common.indexdb.index.term.DocsEnum;
import org.javenstudio.common.indexdb.util.Bits;

// NOTE: sort of a silly class, since you can get the
// freq() already by TermsEnum.totalTermFreq
final class TermVectorsDocsEnum extends DocsEnum {
	
    private boolean mDidNext;
    private int mDoc = -1;
    private int mFreq;
    private Bits mLiveDocs;

    @Override
    public int getFreq() throws IOException {
    	return mFreq;
    }

    @Override
    public int getDocID() {
    	return mDoc;
    }

    @Override
    public int nextDoc() {
    	if (!mDidNext && (mLiveDocs == null || mLiveDocs.get(0))) {
    		mDidNext = true;
    		return (mDoc = 0);
    	} else {
    		return (mDoc = NO_MORE_DOCS);
    	}
    }

    @Override
    public int advance(int target) {
    	if (!mDidNext && target == 0) {
    		return nextDoc();
    	} else {
    		return (mDoc = NO_MORE_DOCS);
    	}
    }

    public void reset(Bits liveDocs, int freq) {
    	mLiveDocs = liveDocs;
    	mFreq = freq;
    	mDoc = -1;
    	mDidNext = false;
    }
    
}
