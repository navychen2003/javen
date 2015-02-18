package org.javenstudio.hornet.grouping;

/** How the GroupDocs score (if any) should be merged. */
public enum ScoreMergeMode {

	/** Set score to Float.NaN */
	None,
	
	/** Sum score across all shards for this group. */
	Total,
	
	/** Avg score across all shards for this group. */
	Avg,
	
}
