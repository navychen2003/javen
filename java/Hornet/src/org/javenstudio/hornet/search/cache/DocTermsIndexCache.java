package org.javenstudio.hornet.search.cache;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IIntsWriter;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.store.PagedBytes;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.store.packed.GrowableWriter;
import org.javenstudio.hornet.store.packed.PackedInts;

final class DocTermsIndexCache extends Cache {
    public DocTermsIndexCache(FieldCacheImpl wrapper) {
    	super(wrapper);
    }

    @Override
    protected Object createValue(IAtomicReader reader, Entry entryKey, 
    		boolean setDocsWithField /* ignored */) throws IOException {
    	ITerms terms = reader.getTerms(entryKey.mField);
    	final float acceptableOverheadRatio = ((Float) entryKey.mCustom).floatValue();
    	final PagedBytes bytes = new PagedBytes(15);

    	int startBytesBPV;
    	int startTermsBPV;
    	int startNumUniqueTerms;
    	int maxDoc = reader.getMaxDoc();
    	
    	final int termCountHardLimit;
    	if (maxDoc == Integer.MAX_VALUE) 
    		termCountHardLimit = Integer.MAX_VALUE;
    	else 
    		termCountHardLimit = maxDoc+1;

    	if (terms != null) {
    		// Try for coarse estimate for number of bits; this
    		// should be an underestimate most of the time, which
    		// is fine -- GrowableWriter will reallocate as needed
    		long numUniqueTerms = 0;
    		try {
    			numUniqueTerms = terms.size();
    		} catch (UnsupportedOperationException uoe) {
    			numUniqueTerms = -1;
    		}
    		
    		if (numUniqueTerms != -1) {
    			if (numUniqueTerms > termCountHardLimit) {
    				// app is misusing the API (there is more than
    				// one term per doc); in this case we make best
    				// effort to load what we can (see LUCENE-2142)
    				numUniqueTerms = termCountHardLimit;
    			}

    			startBytesBPV = PackedInts.bitsRequired(numUniqueTerms*4);
    			startTermsBPV = PackedInts.bitsRequired(numUniqueTerms);

    			startNumUniqueTerms = (int) numUniqueTerms;
    			
    		} else {
    			startBytesBPV = 1;
    			startTermsBPV = 1;
    			startNumUniqueTerms = 1;
    		}
    	} else {
    		startBytesBPV = 1;
    		startTermsBPV = 1;
    		startNumUniqueTerms = 1;
    	}

    	IIntsWriter termOrdToBytesOffset = new GrowableWriter(
    			startBytesBPV, 1+startNumUniqueTerms, acceptableOverheadRatio);
    	final IIntsWriter docToTermOrd = new GrowableWriter(
    			startTermsBPV, maxDoc, acceptableOverheadRatio);

    	// 0 is reserved for "unset"
    	bytes.copyUsingLengthPrefix(new BytesRef());
    	int termOrd = 1;

    	if (terms != null) {
    		final ITermsEnum termsEnum = terms.iterator(null);
    		IDocsEnum docs = null;

    		while (true) {
    			final BytesRef term = termsEnum.next();
    			if (term == null) 
    				break;
    			if (termOrd >= termCountHardLimit) 
    				break;

    			if (termOrd == termOrdToBytesOffset.size()) {
    				// NOTE: this code only runs if the incoming
    				// reader impl doesn't implement
    				// size (which should be uncommon)
    				termOrdToBytesOffset = termOrdToBytesOffset.resize(ArrayUtil.oversize(1+termOrd, 1));
    			}
    			
    			termOrdToBytesOffset.set(termOrd, bytes.copyUsingLengthPrefix(term));
    			docs = termsEnum.getDocs(null, docs, 0);
    			
    			while (true) {
    				final int docID = docs.nextDoc();
    				if (docID == DocIdSetIterator.NO_MORE_DOCS) 
    					break;
    				
    				docToTermOrd.set(docID, termOrd);
    			}
    			termOrd ++;
    		}

    		if (termOrdToBytesOffset.size() > termOrd) 
    			termOrdToBytesOffset = termOrdToBytesOffset.resize(termOrd);
    	}

    	// maybe an int-only impl?
    	return reader.getContext().createDocTermsIndex(bytes.freeze(true), 
    			termOrdToBytesOffset.getMutable(), docToTermOrd.getMutable(), termOrd);
    }
    
}
