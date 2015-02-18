package org.javenstudio.hornet.search.cache;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.search.FixedBitSet;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.search.OpenFixedBitSet;

final class FloatCache extends Cache {
    public FloatCache(FieldCacheImpl wrapper) {
    	super(wrapper);
    }

    @Override
    protected Object createValue(IAtomicReader reader, Entry entryKey, 
    		boolean setDocsWithField) throws IOException {
    	String field = entryKey.mField;
    	ISortField.FloatParser parser = (ISortField.FloatParser) entryKey.mCustom;
    	if (parser == null) {
    		try {
    			return mWrapper.getFloats(reader, field, 
    					FieldCache.DEFAULT_FLOAT_PARSER, setDocsWithField);
    		} catch (NumberFormatException ne) {
    			return mWrapper.getFloats(reader, field, 
    					FieldCache.NUMERIC_UTILS_FLOAT_PARSER, setDocsWithField);
    		}
    	}
    	
    	final int maxDoc = reader.getMaxDoc();
    	float[] retArray = null;

    	ITerms terms = reader.getTerms(field);
    	FixedBitSet docsWithField = null;
    	
    	if (terms != null) {
    		if (setDocsWithField) {
    			final int termsDocCount = terms.getDocCount();
    			assert termsDocCount <= maxDoc;
    			if (termsDocCount == maxDoc) {
    				// Fast case: all docs have this field:
    				mWrapper.setDocsWithField(reader, field, new Bits.MatchAllBits(maxDoc));
    				setDocsWithField = false;
    			}
    		}
    		
    		final ITermsEnum termsEnum = terms.iterator(null);
    		IDocsEnum docs = null;
    		
    		try {
    			while (true) {
    				final BytesRef term = termsEnum.next();
    				if (term == null) 
    					break;
    				
    				final float termval = parser.parseFloat(term);
    				if (retArray == null) {
    					// late init so numeric fields don't double allocate
    					retArray = new float[maxDoc];
    				}
            
    				docs = termsEnum.getDocs(null, docs, 0);
    				while (true) {
    					final int docID = docs.nextDoc();
    					if (docID == DocIdSetIterator.NO_MORE_DOCS) 
    						break;
    					
    					retArray[docID] = termval;
    					if (setDocsWithField) {
    						if (docsWithField == null) {
    							// Lazy init
    							docsWithField = new OpenFixedBitSet(maxDoc);
    						}
    						docsWithField.set(docID);
    					}
    				}
    			}
    		} catch (FieldCache.StopFillCacheException stop) {
    		}
    	}

    	if (retArray == null) {
    		// no values
    		retArray = new float[maxDoc];
    	}
    	
    	if (setDocsWithField) 
    		mWrapper.setDocsWithField(reader, field, docsWithField);
    	
    	return retArray;
    }
    
}
