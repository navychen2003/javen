package org.javenstudio.hornet.codec;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.codec.IPostingsConsumer;
import org.javenstudio.common.indexdb.codec.ITermsConsumer;
import org.javenstudio.common.indexdb.index.MergeState;
import org.javenstudio.common.indexdb.index.term.MergeTermState;
import org.javenstudio.common.indexdb.index.term.PerTermState;
import org.javenstudio.common.indexdb.search.FixedBitSet;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.index.term.MappingMultiDocsAndPositionsEnum;
import org.javenstudio.hornet.index.term.MappingMultiDocsEnum;
import org.javenstudio.hornet.index.term.MultiDocsAndPositionsEnum;
import org.javenstudio.hornet.index.term.MultiDocsEnum;
import org.javenstudio.hornet.search.OpenFixedBitSet;

/**
 * Abstract API that consumes terms for an individual field.
 * <p>
 * The lifecycle is:
 * <ol>
 *   <li>TermsConsumer is returned for each field 
 *       by {@link FieldsConsumer#addField(FieldInfo)}.
 *   <li>TermsConsumer returns a {@link PostingsConsumer} for
 *       each term in {@link #startTerm(BytesRef)}.
 *   <li>When the producer (e.g. IndexWriter)
 *       is done adding documents for the term, it calls 
 *       {@link #finishTerm(BytesRef, TermStats)}, passing in
 *       the accumulated term statistics.
 *   <li>Producer calls {@link #finish(long, long, int)} with
 *       the accumulated collection statistics when it is finished
 *       adding terms to the field.
 * </ol>
 */
public abstract class TermsConsumer implements ITermsConsumer {

	private MappingMultiDocsEnum mDocsEnum = null;
	private MappingMultiDocsEnum mDocsAndFreqsEnum = null;
	private MappingMultiDocsAndPositionsEnum mPostingsEnum = null;
	
	/** 
	 * Starts a new term in this field; this may be called
	 *  with no corresponding call to finish if the term had
	 *  no docs. 
	 */
	public abstract IPostingsConsumer startTerm(BytesRef text) throws IOException;

	/** Finishes the current term; numDocs must be > 0. */
	public abstract void finishTerm(BytesRef text, ITermState stats) throws IOException;

	/** Called when we are done adding terms to this field */
	public abstract void finish(long sumTotalTermFreq, long sumDocFreq, int docCount) 
			throws IOException;

	/** 
	 * Return the BytesRef Comparator used to sort terms
	 *  before feeding to this API. 
	 */
	public abstract Comparator<BytesRef> getComparator() throws IOException;

	/** Default merge impl */
	@Override
	public ITermState merge(IMergeState mergeState, ITermsEnum termsEnum) throws IOException {
		assert termsEnum != null;
	    
		long sumTotalTerms = 0;
	    long sumTotalTermFreq = 0;
	    long sumDocFreq = 0;
	    long sumDFsinceLastAbortCheck = 0;
	    
	    FixedBitSet visitedDocs = new OpenFixedBitSet(mergeState.getSegmentInfo().getDocCount());
	    IndexOptions indexOptions = mergeState.getFieldInfo().getIndexOptions();
	    
	    if (indexOptions == IndexOptions.DOCS_ONLY) {
	    	if (mDocsEnum == null) 
	    		mDocsEnum = new MappingMultiDocsEnum();
	    	mDocsEnum.setMergeState((MergeState)mergeState);

	    	BytesRef term = null;
	    	MultiDocsEnum docsEnumIn = null;

	    	while ((term = termsEnum.next()) != null) {
	    		// We can pass null for liveDocs, because the
	    		// mapping enum will skip the non-live docs:
	    		docsEnumIn = (MultiDocsEnum) termsEnum.getDocs(null, docsEnumIn, 0);
	    		if (docsEnumIn != null) {
	    			mDocsEnum.reset(docsEnumIn);
	    			
	    			final PostingsConsumer postingsConsumer = (PostingsConsumer)startTerm(term);
	    			final PerTermState stats = (PerTermState)postingsConsumer.merge(
	    					mergeState, mDocsEnum, visitedDocs);
	    			
	    			if (stats.getDocFreq() > 0) {
	    				finishTerm(term, stats);
	    				
	    				sumTotalTerms ++;
	    				sumTotalTermFreq += stats.getDocFreq();
	    				sumDFsinceLastAbortCheck += stats.getDocFreq();
	    				sumDocFreq += stats.getDocFreq();
	    				
	    				if (sumDFsinceLastAbortCheck > 60000) {
	    					mergeState.checkAbort(sumDFsinceLastAbortCheck/5.0);
	    					sumDFsinceLastAbortCheck = 0;
	    				}
	    			}
	    		}
	    	}
	    	
	    } else if (indexOptions == IndexOptions.DOCS_AND_FREQS) {
	    	if (mDocsAndFreqsEnum == null) 
	    		mDocsAndFreqsEnum = new MappingMultiDocsEnum();
	    	mDocsAndFreqsEnum.setMergeState((MergeState)mergeState);

	    	BytesRef term = null;
	    	MultiDocsEnum docsAndFreqsEnumIn = null;

	    	while ((term = termsEnum.next()) != null) {
	    		// We can pass null for liveDocs, because the
	    		// mapping enum will skip the non-live docs:
	    		docsAndFreqsEnumIn = (MultiDocsEnum) termsEnum.getDocs(null, docsAndFreqsEnumIn);
	    		assert docsAndFreqsEnumIn != null;
	    		mDocsAndFreqsEnum.reset(docsAndFreqsEnumIn);
	    		
	    		final IPostingsConsumer postingsConsumer = startTerm(term);
	    		final PerTermState stats = (PerTermState)postingsConsumer.merge(
	    				mergeState, mDocsAndFreqsEnum, visitedDocs);
	    		
	    		if (stats.getDocFreq() > 0) {
	    			finishTerm(term, stats);
	    			
	    			sumTotalTerms ++;
	    			sumTotalTermFreq += stats.getTotalTermFreq();
	    			sumDFsinceLastAbortCheck += stats.getDocFreq();
	    			sumDocFreq += stats.getDocFreq();
	    			
	    			if (sumDFsinceLastAbortCheck > 60000) {
	    				mergeState.checkAbort(sumDFsinceLastAbortCheck/5.0);
	    				sumDFsinceLastAbortCheck = 0;
	    			}
	    		}
	    	}
	    	
	    } else if (indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) {
	    	if (mPostingsEnum == null) 
	    		mPostingsEnum = new MappingMultiDocsAndPositionsEnum();
	    	mPostingsEnum.setMergeState((MergeState)mergeState);
	    	
	    	BytesRef term = null;
	    	MultiDocsAndPositionsEnum postingsEnumIn = null;
	    	
	    	while ((term = termsEnum.next()) != null) {
	    		// We can pass null for liveDocs, because the
	    		// mapping enum will skip the non-live docs:
	    		postingsEnumIn = (MultiDocsAndPositionsEnum) termsEnum.getDocsAndPositions(
	    				null, postingsEnumIn, IDocsAndPositionsEnum.FLAG_PAYLOADS);
	    		assert postingsEnumIn != null;
	    		mPostingsEnum.reset(postingsEnumIn);
	    		
	    		// set PayloadProcessor
	    		if (mergeState.getPayloadProcessorProvider() != null) {
	    			for (int i = 0; i < mergeState.getReaderCount(); i++) {
	    				if (mergeState.getPayloadProcessorReaderAt(i) != null) {
	    					mergeState.setCurrentPayloadProcessorAt(i, 
	    							mergeState.getPayloadProcessorReaderAt(i).getProcessor(
	    									mergeState.getFieldInfo().getName(), term));
	    				}
	    			}
	    		}
	    		
	    		final IPostingsConsumer postingsConsumer = startTerm(term);
	    		final PerTermState stats = (PerTermState)postingsConsumer.merge(
	    				mergeState, mPostingsEnum, visitedDocs);
	    		
	    		if (stats.getDocFreq() > 0) {
	    			finishTerm(term, stats);
	    			
	    			sumTotalTerms ++;
	    			sumTotalTermFreq += stats.getTotalTermFreq();
	    			sumDFsinceLastAbortCheck += stats.getDocFreq();
	    			sumDocFreq += stats.getDocFreq();
	    			
	    			if (sumDFsinceLastAbortCheck > 60000) {
	    				mergeState.checkAbort(sumDFsinceLastAbortCheck/5.0);
	    				sumDFsinceLastAbortCheck = 0;
	    			}
	    		}
	    	}
	    	
	    } else {
	    	assert indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS;
	    	if (mPostingsEnum == null) 
	    		mPostingsEnum = new MappingMultiDocsAndPositionsEnum();
	    	mPostingsEnum.setMergeState((MergeState)mergeState);
	    	
	    	BytesRef term = null;
	    	MultiDocsAndPositionsEnum postingsEnumIn = null;
	    	
	    	while ((term = termsEnum.next()) != null) {
	    		// We can pass null for liveDocs, because the
	    		// mapping enum will skip the non-live docs:
	    		postingsEnumIn = (MultiDocsAndPositionsEnum) termsEnum.getDocsAndPositions(
	    				null, postingsEnumIn);
	    		assert postingsEnumIn != null;
	    		mPostingsEnum.reset(postingsEnumIn);
	    		
	    		// set PayloadProcessor
	    		if (mergeState.getPayloadProcessorProvider() != null) {
	    			for (int i = 0; i < mergeState.getReaderCount(); i++) {
	    				if (mergeState.getPayloadProcessorReaderAt(i) != null) {
	    					mergeState.setCurrentPayloadProcessorAt(i, 
	    							mergeState.getPayloadProcessorReaderAt(i).getProcessor(
	    									mergeState.getFieldInfo().getName(), term));
	    				}
	    			}
	    		}
	    		
	    		final IPostingsConsumer postingsConsumer = startTerm(term);
	    		final PerTermState stats = (PerTermState)postingsConsumer.merge(
	    				mergeState, mPostingsEnum, visitedDocs);
	    		
	    		if (stats.getDocFreq() > 0) {
	    			finishTerm(term, stats);
	    			
	    			sumTotalTerms ++;
	    			sumTotalTermFreq += stats.getTotalTermFreq();
	    			sumDFsinceLastAbortCheck += stats.getDocFreq();
	    			sumDocFreq += stats.getDocFreq();
	    			
	    			if (sumDFsinceLastAbortCheck > 60000) {
	    				mergeState.checkAbort(sumDFsinceLastAbortCheck/5.0);
	    				sumDFsinceLastAbortCheck = 0;
	    			}
	    		}
	    	}
	    }
	    
	    finish((indexOptions == IndexOptions.DOCS_ONLY ? -1 : sumTotalTermFreq), 
	    		sumDocFreq, visitedDocs.cardinality());
	    
	    return new MergeTermState(sumTotalTerms, sumDocFreq, sumTotalTermFreq);
	}
	
}
