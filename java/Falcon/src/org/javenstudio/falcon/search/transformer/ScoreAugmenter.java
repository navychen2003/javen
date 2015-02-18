package org.javenstudio.falcon.search.transformer;

import org.javenstudio.falcon.util.ResultItem;

/**
 * Simple Augmenter that adds the score
 *
 * @since 4.0
 */
public class ScoreAugmenter extends TransformerWithContext {
	
	private final String mName;

	public ScoreAugmenter(String display) {
		mName = display;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public void transform(ResultItem doc, int docid) {
		if (mContext != null && mContext.wantsScores()) {
			if (mContext.getDocIterator() != null) 
				doc.setField(mName, mContext.getDocIterator().score());
		}
	}
	
}
