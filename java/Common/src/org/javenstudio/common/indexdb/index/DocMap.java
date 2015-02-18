package org.javenstudio.common.indexdb.index;

import org.javenstudio.common.indexdb.IIntsMutable;
import org.javenstudio.common.indexdb.util.Bits;

public abstract class DocMap {
    private final Bits mLiveDocs;

    protected DocMap(Bits liveDocs) {
      	mLiveDocs = liveDocs;
    }

    public int get(int docId) {
    	if (mLiveDocs == null || mLiveDocs.get(docId)) 
    		return remap(docId);
    	else 
    		return -1;
    }

    public abstract int remap(int docId);
    public abstract int getMaxDoc();
    public abstract int getNumDeletedDocs();

    public final int getNumDocs() {
    	return getMaxDoc() - getNumDeletedDocs();
    }

    public boolean hasDeletions() {
    	return getNumDeletedDocs() > 0;
    }

    public static DocMap buildDelCountDocmap(IIntsMutable numDeletesSoFar, 
    		int maxDoc, int numDeletes, Bits liveDocs, 
    		float acceptableOverheadRatio) {
    	int del = 0;
    	for (int i = 0; i < maxDoc; ++i) {
    		if (!liveDocs.get(i)) 
    			++ del;
    		
    		numDeletesSoFar.set(i, del);
    	}
    	
    	assert del == numDeletes : "del=" + del + ", numdeletes=" + numDeletes;
    	
    	return new DocMap.DelCountDocMap(liveDocs, numDeletesSoFar);
    }
    
    public static DocMap buildDirectDocMap(IIntsMutable docIds, 
    		int maxDoc, int numDocs, Bits liveDocs, float acceptableOverheadRatio) {
    	int del = 0;
    	for (int i = 0; i < maxDoc; ++i) {
    		if (liveDocs.get(i)) 
    			docIds.set(i, i - del);
    		else 
    			++ del;
    	}
    	
    	assert numDocs + del == maxDoc : "maxDoc=" + maxDoc 
    			+ ", del=" + del + ", numDocs=" + numDocs;
    	
    	return new DocMap.DirectDocMap(liveDocs, docIds, del);
    }
    
    static class DelCountDocMap extends DocMap {
        private final IIntsMutable mNumDeletesSoFar;

        public DelCountDocMap(Bits liveDocs, IIntsMutable numDeletesSoFar) {
        	super(liveDocs);
        	mNumDeletesSoFar = numDeletesSoFar;
        }

        @Override
        public int remap(int docId) {
        	return docId - (int)mNumDeletesSoFar.get(docId);
        }

        @Override
        public int getMaxDoc() {
        	return mNumDeletesSoFar.size();
        }

        @Override
        public int getNumDeletedDocs() {
        	final int maxDoc = getMaxDoc();
        	return (int)mNumDeletesSoFar.get(maxDoc - 1);
        }
    }
    
    static class DirectDocMap extends DocMap {
        private final IIntsMutable mDocIds;
        private final int mNumDeletedDocs;

        public DirectDocMap(Bits liveDocs, IIntsMutable docIds, int numDeletedDocs) {
        	super(liveDocs);
        	mDocIds = docIds;
        	mNumDeletedDocs = numDeletedDocs;
        }

        @Override
        public int remap(int docId) {
        	return (int)mDocIds.get(docId);
        }

        @Override
        public int getMaxDoc() {
        	return mDocIds.size();
        }

        @Override
        public int getNumDeletedDocs() {
        	return mNumDeletedDocs;
        }
    }
    
    public static class NoDelDocMap extends DocMap {
        private final int mMaxDoc;

        public NoDelDocMap(int maxDoc) {
        	super(null);
        	mMaxDoc = maxDoc;
        }

        @Override
        public int remap(int docId) {
        	return docId;
        }

        @Override
        public int getMaxDoc() {
        	return mMaxDoc;
        }

        @Override
        public int getNumDeletedDocs() {
        	return 0;
        }
    }
    
}
