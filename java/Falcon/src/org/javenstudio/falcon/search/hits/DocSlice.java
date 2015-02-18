package org.javenstudio.falcon.search.hits;

/**
 * <code>DocSlice</code> implements DocList as an array of docids and optional scores.
 *
 * @since 0.9
 */
public class DocSlice extends DocSetBase implements DocList {
	
	private final int mOffset;    	// starting position of the docs (zero based)
	private final int mLen;       	// number of positions used in arrays
	private final int[] mDocs;    	// a slice of documents (docs 0-100 of the query)

	private final float[] mScores;  // optional score list
	private final int mMatches;
	private final float mMaxScore;

	/**
	 * Primary constructor for a DocSlice instance.
	 *
	 * @param offset  starting offset for this range of docs
	 * @param len     length of results
	 * @param docs    array of docids starting at position 0
	 * @param scores  array of scores that corresponds to docs, may be null
	 * @param matches total number of matches for the query
	 */
	public DocSlice(int offset, int len, int[] docs, float[] scores, 
			int matches, float maxScore) {
		mOffset = offset;
		mLen = len;
		mDocs = docs;
		mScores = scores;
		mMatches = matches;
		mMaxScore = maxScore;
	}

	@Override
	public DocList subset(int offset, int len) {
		if (mOffset == offset && mLen == len) 
			return this;

		// if we didn't store enough (and there was more to store)
		// then we can't take a subset.
		int requestedEnd = offset + len;
		if (requestedEnd > mDocs.length && mMatches > mDocs.length) 
			return null;
		
		int realEndDoc = Math.min(requestedEnd, mDocs.length);
		int realLen = Math.max(realEndDoc-offset, 0);
		
		if (mOffset == offset && mLen == realLen) 
			return this;
		
		return new DocSlice(offset, realLen, 
				mDocs, mScores, mMatches, mMaxScore);
	}

	@Override
	public boolean hasScores() {
		return mScores != null;
	}

	@Override
	public float maxScore() {
		return mMaxScore;
	}

	public int offset()  { return mOffset; }
	public int size()    { return mLen; }
	public int matches() { return mMatches; }

	@Override
	public long getMemorySize() {
		return (mDocs.length << 2)
				+ (mScores == null ? 0 : (mScores.length << 2))
				+ 24;
	}

	@Override
	public boolean exists(int doc) {
		int end = mOffset + mLen;
		
		for (int i=mOffset; i < end; i++) {
			if (mDocs[i] == doc) 
				return true;
		}
		
		return false;
	}

	// Hmmm, maybe I could have reused the scorer interface here...
	// except that it carries Similarity baggage...
	public DocIterator iterator() {
		return new DocIterator() {
			int pos = mOffset;
			final int end = mOffset + mLen;
			
			@Override
			public boolean hasNext() {
				return pos < end;
			}

			@Override
			public Integer next() {
				return nextDoc();
			}

			/**
			 * The remove  operation is not supported by this Iterator.
			 */
			public void remove() {
				throw new UnsupportedOperationException(
						"The remove operation is not supported by this Iterator.");
			}

			@Override
			public int nextDoc() {
				return mDocs[pos++];
			}

			@Override
			public float score() {
				return mScores[pos-1];
			}
		};
	}

	@Override
	public DocSet intersection(DocSet other) {
		if (other instanceof SortedIntDocSet || other instanceof HashDocSet) 
			return other.intersection(this);
		
		HashDocSet h = new HashDocSet(mDocs, mOffset, mLen);
		return h.intersection(other);
	}

	@Override
	public int intersectionSize(DocSet other) {
		if (other instanceof SortedIntDocSet || other instanceof HashDocSet) 
			return other.intersectionSize(this);
		
		HashDocSet h = new HashDocSet(mDocs, mOffset, mLen);
		return h.intersectionSize(other);  
	}

	@Override
	public boolean intersects(DocSet other) {
		if (other instanceof SortedIntDocSet || other instanceof HashDocSet) 
			return other.intersects(this);
		
		HashDocSet h = new HashDocSet(mDocs, mOffset, mLen);
		return h.intersects(other);
	}

	@Override
	protected DocSlice clone() {
		try {
			// DocSlice is not currently mutable
			@SuppressWarnings("unused")
			DocSlice slice = (DocSlice) super.clone();
		} catch (CloneNotSupportedException e) {
			// ignore
		}
		
		return null;
	}
	
}
