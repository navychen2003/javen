package org.javenstudio.falcon.search.hits;

/**
 * A class to hold "phrase slop" and "boost" parameters 
 * for pf, pf2, pf3 parameters
 */
public class FieldParams {
	
	// make bigrams if 2, trigrams if 3, or all if 0
	private final int mWordGrams; 
	private final int mSlop;
	private final float mBoost;
	private final String mField;
  
	public FieldParams(String field, int wordGrams, int slop, float boost) {
		mWordGrams = wordGrams;
		mSlop      = slop;
		mBoost     = boost;
		mField     = field;
	}
	
	public int getWordGrams() { return mWordGrams; }
	public int getSlop() { return mSlop; }
	public float getBoost() { return mBoost; }
	public String getField() { return mField; }
  
}
