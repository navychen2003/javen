package org.javenstudio.hornet.search.cache;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IIntsWriter;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.store.PagedBytes;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.store.packed.GrowableWriter;
import org.javenstudio.hornet.store.packed.PackedInts;

final class DocTermsCache extends Cache {
    public DocTermsCache(FieldCacheImpl wrapper) {
    	super(wrapper);
    }

    @Override
    protected Object createValue(IAtomicReader reader, Entry entryKey, 
    		boolean setDocsWithField /* ignored */) throws IOException {

    	ITerms terms = reader.getTerms(entryKey.mField);
    	final float acceptableOverheadRatio = ((Float) entryKey.mCustom).floatValue();
    	final int termCountHardLimit = reader.getMaxDoc();

    	// Holds the actual term data, expanded.
    	final PagedBytes bytes = new PagedBytes(15);
    	int startBPV;

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
    			if (numUniqueTerms > termCountHardLimit) 
    				numUniqueTerms = termCountHardLimit;
    			
    			startBPV = PackedInts.bitsRequired(numUniqueTerms*4);
    		} else {
    			startBPV = 1;
    		}
    	} else {
    		startBPV = 1;
    	}

    	final IIntsWriter docToOffset = new GrowableWriter(
    			startBPV, reader.getMaxDoc(), acceptableOverheadRatio);
      
    	// pointer==0 means not set
    	bytes.copyUsingLengthPrefix(new BytesRef());

    	if (terms != null) {
    		int termCount = 0;
    		final ITermsEnum termsEnum = terms.iterator(null);
    		IDocsEnum docs = null;
    		
    		while (true) {
    			if (termCount++ == termCountHardLimit) {
    				// app is misusing the API (there is more than
    				// one term per doc); in this case we make best
    				// effort to load what we can (see LUCENE-2142)
    				break;
    			}

    			final BytesRef term = termsEnum.next();
    			if (term == null) 
    				break;
    			
    			final long pointer = bytes.copyUsingLengthPrefix(term);
    			docs = termsEnum.getDocs(null, docs, 0);
    			
    			while (true) {
    				final int docID = docs.nextDoc();
    				if (docID == IDocIdSetIterator.NO_MORE_DOCS) 
    					break;
    				
    				docToOffset.set(docID, pointer);
    			}
    		}
    	}

    	// maybe an int-only impl?
    	return reader.getContext().createDocTerms(
    			bytes.freeze(true), docToOffset.getMutable());
    }
    
}
