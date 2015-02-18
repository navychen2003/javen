package org.javenstudio.common.indexdb.search;

import org.javenstudio.common.indexdb.IScoreDoc;

/** Holds one hit in {@link TopDocs}. */
public class ScoreDoc implements IScoreDoc {

	/** The score of this document for the query. */
	private float mScore;

	/** A hit document's number.
	 * @see IndexSearcher#doc(int) */
	private int mDoc;

	/** Only set by {@link TopDocs#merge} */
	private int mShardIndex;

	/** Constructs a ScoreDoc. */
	public ScoreDoc(int doc, float score) {
		this(doc, score, -1);
	}

	/** Constructs a ScoreDoc. */
	public ScoreDoc(int doc, float score, int shardIndex) {
		mDoc = doc;
		mScore = score;
		mShardIndex = shardIndex;
	}
  
	public float getScore() { return mScore; }
	public int getDoc() { return mDoc; }
	public int getShardIndex() { return mShardIndex; }
  
	public void setScore(float score) { mScore = score; }
	public void setDoc(int doc) { mDoc = doc; }
	public void setShardIndex(int index) { mShardIndex = index; }
  
	// A convenience method for debugging.
	@Override
	public String toString() {
		return "doc=" + mDoc + " score=" + mScore + " shardIndex=" + mShardIndex;
	}
	
}
