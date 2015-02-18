package org.javenstudio.hornet.index.term;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.segment.ReaderSlice;
import org.javenstudio.common.indexdb.index.term.Terms;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Exposes flex API, merged from flex API of
 * sub-segments.
 *
 */
public final class MultiTerms extends Terms {
	//private static final Logger LOG = Logger.getLogger(MultiTerms.class);
	
	private final Terms[] mSubs;
	private final ReaderSlice[] mSubSlices;
	private final Comparator<BytesRef> mTermComp;

	public MultiTerms(Terms[] subs, ReaderSlice[] subSlices) throws IOException {
		if (subs == null || subSlices == null) throw new NullPointerException();
		mSubs = subs;
		mSubSlices = subSlices;
    
		Comparator<BytesRef> termComp = null;
		for (int i=0; i < subs.length; i++) {
			if (termComp == null) {
				termComp = subs[i].getComparator();
			} else {
				// We cannot merge sub-readers that have
				// different TermComps
				final Comparator<BytesRef> subTermComp = subs[i].getComparator();
				if (subTermComp != null && !subTermComp.equals(termComp)) {
					throw new IllegalStateException(
							"sub-readers have different BytesRef.Comparators; cannot merge");
				}
			}
		}

		mTermComp = termComp;
	}

	@Override
	public ITermsEnum iterator(ITermsEnum reuse) throws IOException {
		final List<MultiTermsEnum.TermsEnumIndex> termsEnums = 
				new ArrayList<MultiTermsEnum.TermsEnumIndex>();
		
		//if (LOG.isDebugEnabled())
		//	LOG.debug("iterator: reuse=" + reuse);
		
		for (int i=0; i < mSubs.length; i++) {
			Terms terms = mSubs[i];
			
			//if (LOG.isDebugEnabled())
			//	LOG.debug("iterator: subs[" + i + "]=" + terms);
			
			final TermsEnum termsEnum = (TermsEnum)terms.iterator(null);
			if (termsEnum != null) 
				termsEnums.add(new MultiTermsEnum.TermsEnumIndex(termsEnum, i));
		}

		if (termsEnums.size() > 0) {
			return new MultiTermsEnum(mSubSlices).reset(
					termsEnums.toArray(new MultiTermsEnum.TermsEnumIndex[0]));
		}
		
		return TermsEnum.EMPTY;
	}

	@Override
	public long size() throws IOException {
		return -1;
	}

	@Override
	public long getSumTotalTermFreq() throws IOException {
		long sum = 0;
		for (Terms terms : mSubs) {
			final long v = terms.getSumTotalTermFreq();
			if (v == -1) 
				return -1;
			
			sum += v;
		}
		return sum;
	}
  
	@Override
	public long getSumDocFreq() throws IOException {
		long sum = 0;
		for (Terms terms : mSubs) {
			final long v = terms.getSumDocFreq();
			if (v == -1) 
				return -1;
			
			sum += v;
		}
		return sum;
	}
  
	@Override
	public int getDocCount() throws IOException {
		int sum = 0;
		for (Terms terms : mSubs) {
			final int v = terms.getDocCount();
			if (v == -1) 
				return -1;
			
			sum += v;
		}
		return sum;
	}

	@Override
	public Comparator<BytesRef> getComparator() {
		return mTermComp;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{subCount=" + mSubs.length 
				+ ",sliceCount=" + mSubSlices.length + "}";
	}
	
}

