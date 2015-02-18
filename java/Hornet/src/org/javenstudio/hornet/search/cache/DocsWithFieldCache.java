package org.javenstudio.hornet.search.cache;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.search.FixedBitSet;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.search.OpenFixedBitSet;

final class DocsWithFieldCache extends Cache {
    public DocsWithFieldCache(FieldCacheImpl wrapper) {
    	super(wrapper);
    }
    
    @Override
	protected Object createValue(IAtomicReader reader, Entry entryKey, 
			boolean setDocsWithField /* ignored */) throws IOException {
    	final String field = entryKey.mField;      
    	FixedBitSet res = null;
    	
    	ITerms terms = reader.getTerms(field);
    	final int maxDoc = reader.getMaxDoc();
    	
    	if (terms != null) {
    		final int termsDocCount = terms.getDocCount();
    		assert termsDocCount <= maxDoc;
    		
    		if (termsDocCount == maxDoc) {
    			// Fast case: all docs have this field:
    			return new Bits.MatchAllBits(maxDoc);
    		}
    		
    		final ITermsEnum termsEnum = terms.iterator(null);
    		IDocsEnum docs = null;
    		
    		while(true) {
    			final BytesRef term = termsEnum.next();
    			if (term == null) 
    				break;
    			
    			if (res == null) {
    				// lazy init
    				res = new OpenFixedBitSet(maxDoc);
    			}

    			docs = termsEnum.getDocs(null, docs, 0);
    			// TODO: use bulk API
    			while (true) {
    				final int docID = docs.nextDoc();
    				if (docID == DocIdSetIterator.NO_MORE_DOCS) 
    					break;
    				
    				res.set(docID);
    			}
    		}
    	}
    	
    	if (res == null) 
    		return new Bits.MatchNoBits(maxDoc);
    	
    	final int numSet = res.cardinality();
    	if (numSet >= maxDoc) {
    		// The cardinality of the BitSet is maxDoc if all documents have a value.
    		assert numSet == maxDoc;
    		return new Bits.MatchAllBits(maxDoc);
    	}
    	
    	return res;
	}
    
}
