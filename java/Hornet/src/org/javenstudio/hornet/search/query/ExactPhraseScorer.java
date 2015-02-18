package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IExactSimilarityScorer;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.Weight;

public final class ExactPhraseScorer extends Scorer {
  
	private final static int CHUNK = 4096;

	private final static class ChunkState {
		private final IDocsAndPositionsEnum mPosEnum;
		private final int mOffset;
		private final boolean mUseAdvance;
		private int mPosUpto;
		private int mPosLimit;
		private int mPos;
		private int mLastPos;

		public ChunkState(IDocsAndPositionsEnum posEnum, 
				int offset, boolean useAdvance) {
			mPosEnum = posEnum;
			mOffset = offset;
			mUseAdvance = useAdvance;
		}
	}
	
	private final int[] mCounts = new int[CHUNK];
	private final int[] mGens = new int[CHUNK];

	private final ChunkState[] mChunkStates;
	private final IExactSimilarityScorer mDocScorer;
	private final int mEndMinus1;
  
	private boolean mNoDocs;
	private int mGen;
	private int mDocID = -1;
	private int mFreq;

	public ExactPhraseScorer(Weight weight, PostingsAndFreq[] postings,
			IExactSimilarityScorer docScorer) throws IOException {
		super(weight);
		
		mDocScorer = docScorer;
		mChunkStates = new ChunkState[postings.length];
		mEndMinus1 = postings.length-1;

		for (int i=0; i < postings.length; i++) {
			// Coarse optimization: advance(target) is fairly
			// costly, so, if the relative freq of the 2nd
			// rarest term is not that much (> 1/5th) rarer than
			// the first term, then we just use .nextDoc() when
			// ANDing.  This buys ~15% gain for phrases where
			// freq of rarest 2 terms is close:
			final boolean useAdvance = postings[i].getDocFreq() > 5*postings[0].getDocFreq();
			
			mChunkStates[i] = new ChunkState(postings[i].getPostings(), 
					-postings[i].getPosition(), useAdvance);
			
			if (i > 0 && postings[i].getPostings().nextDoc() == IDocIdSetIterator.NO_MORE_DOCS) {
				mNoDocs = true;
				return;
			}
		}
	}

	@Override
	public int nextDoc() throws IOException {
		while (true) {
			// first (rarest) term
			final int doc = mChunkStates[0].mPosEnum.nextDoc();
			if (doc == IDocIdSetIterator.NO_MORE_DOCS) {
				mDocID = doc;
				return doc;
			}

			// not-first terms
			int i = 1;
			
			while (i < mChunkStates.length) {
				final ChunkState cs = mChunkStates[i];
				
				int doc2 = cs.mPosEnum.getDocID();
				if (cs.mUseAdvance) {
					if (doc2 < doc) 
						doc2 = cs.mPosEnum.advance(doc);
					
				} else {
					int iter = 0;
					while (doc2 < doc) {
						// safety net -- fallback to .advance if we've
						// done too many .nextDocs
						if (++iter == 50) {
							doc2 = cs.mPosEnum.advance(doc);
							break;
							
						} else {
							doc2 = cs.mPosEnum.nextDoc();
						}
					}
				}
				
				if (doc2 > doc) 
					break;
				
				i++;
			}

			if (i == mChunkStates.length) {
				// this doc has all the terms -- now test whether
				// phrase occurs
				mDocID = doc;
				mFreq = phraseFreq();
				
				if (mFreq != 0) 
					return mDocID;
			}
		}
	}

	@Override
	public int advance(int target) throws IOException {
		// first term
		int doc = mChunkStates[0].mPosEnum.advance(target);
		if (doc == IDocIdSetIterator.NO_MORE_DOCS) {
			mDocID = IDocIdSetIterator.NO_MORE_DOCS;
			return doc;
		}

		while (true) {
			// not-first terms
			int i = 1;
			
			while (i < mChunkStates.length) {
				int doc2 = mChunkStates[i].mPosEnum.getDocID();
				if (doc2 < doc) 
					doc2 = mChunkStates[i].mPosEnum.advance(doc);
				
				if (doc2 > doc) 
					break;
				
				i++;
			}

			if (i == mChunkStates.length) {
				// this doc has all the terms -- now test whether
				// phrase occurs
				mDocID = doc;
				mFreq = phraseFreq();
				if (mFreq != 0) 
					return mDocID;
			}

			doc = mChunkStates[0].mPosEnum.nextDoc();
			if (doc == IDocIdSetIterator.NO_MORE_DOCS) {
				mDocID = doc;
				return doc;
			}
		}
	}

	@Override
	public float getFreq() { return mFreq; }

	@Override
	public int getDocID() { return mDocID; }

	@Override
	public float getScore() {
		return mDocScorer.score(mDocID, mFreq);
	}

	public boolean hasNoDocs() { return mNoDocs; }
	
	private int phraseFreq() throws IOException {
		mFreq = 0;

		// init chunks
		for (int i=0; i < mChunkStates.length; i++) {
			final ChunkState cs = mChunkStates[i];
			cs.mPosLimit = cs.mPosEnum.getFreq();
			cs.mPos = cs.mOffset + cs.mPosEnum.nextPosition();
			cs.mPosUpto = 1;
			cs.mLastPos = -1;
		}

		int chunkStart = 0;
		int chunkEnd = CHUNK;

		// process chunk by chunk
		boolean end = false;

		// TODO: we could fold in chunkStart into offset and
		// save one subtract per pos incr
		while (!end) {
			mGen ++;

			if (mGen == 0) {
				// wraparound
				Arrays.fill(mGens, 0);
				mGen++;
			}

			// first term
			{
				final ChunkState cs = mChunkStates[0];
				while (cs.mPos < chunkEnd) {
					if (cs.mPos > cs.mLastPos) {
						cs.mLastPos = cs.mPos;
						
						final int posIndex = cs.mPos - chunkStart;
						mCounts[posIndex] = 1;
						
						assert mGens[posIndex] != mGen;
						mGens[posIndex] = mGen;
					}

					if (cs.mPosUpto == cs.mPosLimit) {
						end = true;
						break;
					}
					
					cs.mPosUpto ++;
					cs.mPos = cs.mOffset + cs.mPosEnum.nextPosition();
				}
			}

			// middle terms
			boolean any = true;
			
			for (int t=1; t < mEndMinus1; t++) {
				final ChunkState cs = mChunkStates[t];
				any = false;
				
				while (cs.mPos < chunkEnd) {
					if (cs.mPos > cs.mLastPos) {
						cs.mLastPos = cs.mPos;
						
						final int posIndex = cs.mPos - chunkStart;
						if (posIndex >= 0 && mGens[posIndex] == mGen && mCounts[posIndex] == t) {
							// viable
							mCounts[posIndex] ++;
							any = true;
						}
					}

					if (cs.mPosUpto == cs.mPosLimit) {
						end = true;
						break;
					}
					
					cs.mPosUpto ++;
					cs.mPos = cs.mOffset + cs.mPosEnum.nextPosition();
				}

				if (!any) 
					break;
			}

			if (!any) {
				// petered out for this chunk
				chunkStart += CHUNK;
				chunkEnd += CHUNK;
				continue;
			}

			// last term
			{
				final ChunkState cs = mChunkStates[mEndMinus1];
				while (cs.mPos < chunkEnd) {
					if (cs.mPos > cs.mLastPos) {
						cs.mLastPos = cs.mPos;
						
						final int posIndex = cs.mPos - chunkStart;
						if (posIndex >= 0 && mGens[posIndex] == mGen && 
							mCounts[posIndex] == mEndMinus1) {
							mFreq ++;
						}
					}

					if (cs.mPosUpto == cs.mPosLimit) {
						end = true;
						break;
					}
					
					cs.mPosUpto ++;
					cs.mPos = cs.mOffset + cs.mPosEnum.nextPosition();
				}
			}

			chunkStart += CHUNK;
			chunkEnd += CHUNK;
		}

		return mFreq;
	}
	
	@Override
	public String toString() {
		return "ExactPhraseScorer{" + mWeight + "}";
	}
	
}
