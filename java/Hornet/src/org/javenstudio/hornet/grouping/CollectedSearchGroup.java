package org.javenstudio.hornet.grouping;

/** 
 * Expert: representation of a group in {@link AbstractFirstPassGroupingCollector},
 * tracking the top doc and {@link FieldComparator} slot.
 */
public class CollectedSearchGroup<T> extends SearchGroup<T> {
	
	private int mTopDoc;
	private int mComparatorSlot;
	
	public void setTopDoc(int doc) { mTopDoc = doc; }
	public int getTopDoc() { return mTopDoc; }
	
	public void setComparatorSlot(int slot) { mComparatorSlot = slot; }
	public int getComparatorSlot() { return mComparatorSlot; }
	
}
