package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITermContext;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.DocsAndPositionsEnum;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.PriorityQueue;

/**
 * Takes the logical union of multiple DocsEnum iterators.
 */
// TODO: if ever we allow subclassing of the *PhraseScorer
public class UnionDocsAndPositionsEnum extends DocsAndPositionsEnum {

	private static final class DocsQueue extends PriorityQueue<IDocsAndPositionsEnum> {
		DocsQueue(List<IDocsAndPositionsEnum> docsEnums) throws IOException {
			super(docsEnums.size());

			Iterator<IDocsAndPositionsEnum> i = docsEnums.iterator();
			while (i.hasNext()) {
				IDocsAndPositionsEnum postings = i.next();
				if (postings.nextDoc() != IDocIdSetIterator.NO_MORE_DOCS) 
					add(postings);
			}
		}

		@Override
		public final boolean lessThan(IDocsAndPositionsEnum a, IDocsAndPositionsEnum b) {
			return a.getDocID() < b.getDocID();
		}
	}

	private static final class IntQueue {
		
		private int mArraySize = 16;
		private int[] mArray = new int[mArraySize];
		private int mIndex = 0;
		private int mLastIndex = 0;
		
    
		final void add(int i) {
			if (mLastIndex == mArraySize)
				growArray();

			mArray[mLastIndex++] = i;
		}

		final int next() {
			return mArray[mIndex++];
		}

		final void sort() {
			Arrays.sort(mArray, mIndex, mLastIndex);
		}

		final void clear() {
			mIndex = 0;
			mLastIndex = 0;
		}

		final int size() {
			return (mLastIndex - mIndex);
		}

		private void growArray() {
			int[] newArray = new int[mArraySize * 2];
			System.arraycopy(mArray, 0, newArray, 0, mArraySize);
			mArray = newArray;
			mArraySize *= 2;
		}
	}

	private DocsQueue mQueue;
	private IntQueue mPosList;
	private int mDoc;
	private int mFreq;

	public UnionDocsAndPositionsEnum(Bits liveDocs, IAtomicReaderRef context, 
			ITerm[] terms, Map<ITerm,ITermContext> termContexts, ITermsEnum termsEnum) throws IOException {
		List<IDocsAndPositionsEnum> docsEnums = new LinkedList<IDocsAndPositionsEnum>();
		
		for (int i = 0; i < terms.length; i++) {
			final ITerm term = terms[i];
			ITermState termState = termContexts.get(term).get(context.getOrd());
			if (termState == null) {
				// Term doesn't exist in reader
				continue;
			}
			
			termsEnum.seekExact(term.getBytes(), termState);
			IDocsAndPositionsEnum postings = termsEnum.getDocsAndPositions(liveDocs, null, 0);
			if (postings == null) {
				// term does exist, but has no positions
				throw new IllegalStateException("field \"" + term.getField() 
						+ "\" was indexed without position data; cannot run PhraseQuery (term=" 
						+ term.getText() + ")");
			}
			
			docsEnums.add(postings);
		}

		mQueue = new DocsQueue(docsEnums);
		mPosList = new IntQueue();
	}

	@Override
	public final int nextDoc() throws IOException {
		if (mQueue.size() == 0) 
			return NO_MORE_DOCS;

		// TODO: move this init into positions(): if the search
		// doesn't need the positions for this doc then don't
		// waste CPU merging them:
		mPosList.clear();
		mDoc = mQueue.top().getDocID();

		// merge sort all positions together
		IDocsAndPositionsEnum postings;
		do {
			postings = mQueue.top();

			final int freq = postings.getFreq();
			for (int i = 0; i < freq; i++) {
				mPosList.add(postings.nextPosition());
			}

			if (postings.nextDoc() != NO_MORE_DOCS) 
				mQueue.updateTop();
			else 
				mQueue.pop();
			
		} while (mQueue.size() > 0 && mQueue.top().getDocID() == mDoc);

		mPosList.sort();
		mFreq = mPosList.size();

		return mDoc;
	}

	@Override
	public int nextPosition() {
		return mPosList.next();
	}

	@Override
	public int startOffset() {
		return -1;
	}

	@Override
	public int endOffset() {
		return -1;
	}

	@Override
	public BytesRef getPayload() {
		return null;
	}

	@Override
	public final int advance(int target) throws IOException {
		while (mQueue.top() != null && target > mQueue.top().getDocID()) {
			IDocsAndPositionsEnum postings = mQueue.pop();
			if (postings.advance(target) != NO_MORE_DOCS) 
				mQueue.add(postings);
		}
		return nextDoc();
	}

	@Override
	public final int getFreq() {
		return mFreq;
	}

	@Override
	public final int getDocID() {
		return mDoc;
	}
	
}
