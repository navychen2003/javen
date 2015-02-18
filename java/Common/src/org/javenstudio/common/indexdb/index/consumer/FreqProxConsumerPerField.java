package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFixedBitSet;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.codec.IFieldsConsumer;
import org.javenstudio.common.indexdb.codec.ILiveDocsFormat;
import org.javenstudio.common.indexdb.codec.IPostingsConsumer;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.codec.ITermsConsumer;
import org.javenstudio.common.indexdb.index.IndexContext;
import org.javenstudio.common.indexdb.index.IndexWriter;
import org.javenstudio.common.indexdb.index.term.PerTermState;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.store.ByteSliceReader;
import org.javenstudio.common.indexdb.util.BytesRef;

//TODO: break into separate freq and prox writers as
//codecs; make separate container (tii/tis/skip/*) that can
//be configured as any number of files 1..N
final class FreqProxConsumerPerField extends TermsHashConsumerPerField 
		implements Comparable<FreqProxConsumerPerField> {

	private boolean mHasFreq = false;
	private boolean mHasProx = false;
	private boolean mHasOffsets = false;
	private boolean mHasPayloads = false;
	
	private BytesRef mPayload = null;
	
	public FreqProxConsumerPerField(
			TermsHashPerField termsHashPerField, FreqProxConsumer parent, 
			IFieldInfo fieldInfo) { 
		super(termsHashPerField, parent, fieldInfo);
		setIndexOptions(fieldInfo.getIndexOptions());
	}
	
	@Override
	protected ParallelPostingsArray createPostingsArray(int size) {
		return new FreqProxPostingsArray(this, size, mHasFreq, mHasProx, mHasOffsets);
	}
	
	@Override
	public int compareTo(FreqProxConsumerPerField other) {
	    return getFieldInfo().getName().compareTo(other.getFieldInfo().getName());
	}
	
	@Override
	public int getStreamCount() { 
		return !mHasProx ? 1 : 2;
	}
	
	@Override
	public boolean start(IField[] fields, int count) {
		for (int i=0; i < count; i++) {
			if (fields[i].getFieldType().isIndexed()) 
				return true;
		}
		return false;
	}
	
	@Override
	public void finish() throws IOException { 
		if (mHasPayloads) 
			getFieldInfo().setStorePayloads();
	}
	
	// Called after flush
	final void reset() {
		// Record, up front, whether our in-RAM format will be
		// with or without term freqs:
		setIndexOptions(getFieldInfo().getIndexOptions());
	}

	private void setIndexOptions(IndexOptions indexOptions) {
		if (indexOptions == null) {
			// field could later be updated with indexed=true, so set everything on
			mHasFreq = mHasProx = mHasOffsets = true;
		} else {
			mHasFreq = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
			mHasProx = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
			mHasOffsets = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
		}
	}
	
	final void setHasPayloads(boolean hasPayloads) { mHasPayloads = hasPayloads; }
	
	@Override
	public void newTerm(int termID, IToken token) throws IOException { 
	    // First time we're seeing this term since the last flush
	    final FreqProxPostingsArray postings = (FreqProxPostingsArray) 
	    		getTermsHashPerField().getPostingsArray();
	    
	    postings.addNewTerm(termID, token);
	}
	
	@Override
	public void addTerm(int termID, IToken token) throws IOException { 
		final FreqProxPostingsArray postings = (FreqProxPostingsArray) 
				getTermsHashPerField().getPostingsArray();
		
		postings.addTerm(termID, token);
	}
	
	final ILiveDocsFormat getLiveDocsFormat() { 
		IndexWriter writer = getTermsHashConsumer().getDocumentWriter().getSegmentWriter().getIndexWriter();
		return writer.getIndexFormat().getLiveDocsFormat();
	}
	
	/** 
	 * Walk through all unique text tokens (Posting
	 * instances) found in this field and serialize them
	 * into a single RAM segment. 
	 */
	final void flush(String fieldName, IFieldsConsumer consumer, final ISegmentWriteState state)
			throws CorruptIndexException, IOException {
		if (!getFieldInfo().isIndexed()) {
			return; // nothing to flush, don't bother the codec with the unindexed field
		}
      
		final ITermsConsumer termsConsumer = consumer.addField(getFieldInfo());
		final Comparator<BytesRef> termComp = termsConsumer.getComparator();

		// CONFUSING: this.indexOptions holds the index options
		// that were current when we first saw this field.  But
		// it's possible this has changed, eg when other
		// documents are indexed that cause a "downgrade" of the
		// IndexOptions.  So we must decode the in-RAM buffer
		// according to this.indexOptions, but then write the
		// new segment to the directory according to
		// currentFieldIndexOptions:
		final IndexOptions currentFieldIndexOptions = getFieldInfo().getIndexOptions();
		assert currentFieldIndexOptions != null;

		final boolean writeTermFreq = currentFieldIndexOptions.compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
		final boolean writePositions = currentFieldIndexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
		final boolean writeOffsets = currentFieldIndexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;

		final boolean readTermFreq = mHasFreq;
		final boolean readPositions = mHasProx;
		final boolean readOffsets = mHasOffsets;

		// Make sure FieldInfo.update is working correctly!:
		assert !writeTermFreq || readTermFreq;
		assert !writePositions || readPositions;
		assert !writeOffsets || readOffsets;
		assert !writeOffsets || writePositions;

		final Map<ITerm,Integer> segDeletes;
		//if (state.segDeletes != null && state.segDeletes.terms.size() > 0) 
		//	segDeletes = state.segDeletes.terms;
		//else 
			segDeletes = null;

		final int[] termIDs = getTermsHashPerField().sortPostings(termComp);
		final int numTerms = getTermsHashPerField().getBytesHash().size();
		final FreqProxPostingsArray postings = (FreqProxPostingsArray) getTermsHashPerField().getPostingsArray();
		final ByteSliceReader freq = new ByteSliceReader();
		final ByteSliceReader prox = new ByteSliceReader();
		final BytesRef text = new BytesRef();

		IFixedBitSet visitedDocs = IndexContext.getInstance().newFixedBitSet(
				state.getSegmentInfo().getDocCount());
		
		long sumTotalTermFreq = 0;
		long sumDocFreq = 0;

		for (int i = 0; i < numTerms; i++) {
			final int termID = termIDs[i];
			
			// Get BytesRef
			final int textStart = postings.getTextStartAt(termID);
			getTermsHashPerField().getBytePool().setBytesRef(text, textStart);

			getTermsHashPerField().initReader(freq, termID, 0);
			if (readPositions || readOffsets) 
				getTermsHashPerField().initReader(prox, termID, 1);
			

			// TODO: really TermsHashPerField should take over most
			// of this loop, including merge sort of terms from
			// multiple threads and interacting with the
			// TermsConsumer, only calling out to us (passing us the
			// DocsConsumer) to handle delivery of docs/positions
			final IPostingsConsumer postingsConsumer = termsConsumer.startTerm(text);

			int delDocLimit = 0;
			if (segDeletes != null) {
				final Integer docIDUpto = segDeletes.get(new Term(fieldName, text));
				if (docIDUpto != null) 
					delDocLimit = docIDUpto;
			}

			// Now termStates has numToMerge FieldMergeStates
			// which all share the same term.  Now we must
			// interleave the docID streams.
			int docFreq = 0;
			long totTF = 0;
			int docID = 0;

			while (true) {
				final int termFreq;
				
				if (freq.eof()) {
					if (postings.getLastDocCodeAt(termID) != -1) {
						// Return last doc
						docID = postings.getLastDocIDAt(termID);
						
						if (readTermFreq) 
							termFreq = postings.getTermFreqAt(termID);
						else 
							termFreq = -1;
						
						postings.setLastDocCodeAt(termID, -1);
						
					} else 
						break; // EOF
					
				} else {
					final int code = freq.readVInt();
					if (!readTermFreq) {
						docID += code;
						termFreq = -1;
						
					} else {
						docID += code >>> 1;
						if ((code & 1) != 0) 
							termFreq = 1;
						else 
							termFreq = freq.readVInt();
					}

					assert docID != postings.getLastDocIDAt(termID);
				}

				docFreq ++;
				assert docID < state.getSegmentInfo().getDocCount(): "doc=" + docID 
					+ " maxDoc=" + state.getSegmentInfo().getDocCount();

				// NOTE: we could check here if the docID was
				// deleted, and skip it.  However, this is somewhat
				// dangerous because it can yield non-deterministic
				// behavior since we may see the docID before we see
				// the term that caused it to be deleted.  This
				// would mean some (but not all) of its postings may
				// make it into the index, which'd alter the docFreq
				// for those terms.  We could fix this by doing two
				// passes, ie first sweep marks all del docs, and
				// 2nd sweep does the real flush, but I suspect
				// that'd add too much time to flush.
				visitedDocs.set(docID);
				postingsConsumer.startDoc(docID, writeTermFreq ? termFreq : -1);
				
				if (docID < delDocLimit) {
					// Mark it deleted.  TODO: we could also skip
					// writing its postings; this would be
					// deterministic (just for this Term's docs).
            
					// TODO: can we do this reach-around in a cleaner way????
					if (state.getLiveDocs() == null) {
						state.setLiveDocs(getLiveDocsFormat().newLiveDocs(
								state.getSegmentInfo().getDocCount()));
					}
					
					if (state.getLiveDocs().get(docID)) {
						state.increaseDelCountOnFlush(1);
						state.getLiveDocs().clear(docID);
					}
				}

				totTF += termFreq;
          
				// Carefully copy over the prox + payload info,
				// changing the format to match Indexdb's segment
				// format.

				if (readPositions || readOffsets) {
					// we did record positions (& maybe payload) and/or offsets
					int position = 0;
					int offset = 0;
					
					for (int j=0; j < termFreq; j++) {
						final BytesRef thisPayload;

						if (readPositions) {
							final int code = prox.readVInt();
							position += code >>> 1;

	                		if ((code & 1) != 0) {
	                			// This position has a payload
	                			final int payloadLength = prox.readVInt();
	
		                		if (mPayload == null) {
		                			mPayload = new BytesRef();
		                			mPayload.mBytes = new byte[payloadLength];
		                			
		                		} else if (mPayload.mBytes.length < payloadLength) {
		                			mPayload.grow(payloadLength);
		                		}
		
		                		prox.readBytes(mPayload.mBytes, 0, payloadLength);
		                		mPayload.mLength = payloadLength;
		                		
		                		thisPayload = mPayload;
		
		                	} else {
		                		thisPayload = null;
		                	}

	                		if (readOffsets) {
	                			final int startOffset = offset + prox.readVInt();
	                			final int endOffset = startOffset + prox.readVInt();
	                			
	                			if (writePositions) {
	                				if (writeOffsets) {
	                					assert startOffset >=0 && endOffset >= startOffset : 
	                						"startOffset=" + startOffset + ",endOffset=" + endOffset 
	                						+ ",offset=" + offset;
	                					
	                					postingsConsumer.addPosition(position, thisPayload, startOffset, endOffset);
	                				} else {
	                					postingsConsumer.addPosition(position, thisPayload, -1, -1);
	                				}
	                			}
	                			
	                			offset = startOffset;
	                			
	                		} else if (writePositions) {
	                			postingsConsumer.addPosition(position, thisPayload, -1, -1);
	                		}
						}
					}
				}
				
				postingsConsumer.finishDoc();
			}
			
			termsConsumer.finishTerm(text, new PerTermState(docFreq, writeTermFreq ? totTF : -1));
			
			sumTotalTermFreq += totTF;
			sumDocFreq += docFreq;
		}

		termsConsumer.finish(writeTermFreq ? sumTotalTermFreq : -1, 
				sumDocFreq, visitedDocs.cardinality());
	}
	
}
