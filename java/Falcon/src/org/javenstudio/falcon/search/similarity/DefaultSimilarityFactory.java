package org.javenstudio.falcon.search.similarity;

import org.javenstudio.common.indexdb.search.Similarity;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.hornet.search.similarity.DefaultSimilarity;

/**
 * Factory for {@link DefaultSimilarity}
 * <p>
 * DefaultSimilarity is default scoring implementation, based
 * upon the Vector Space Model.
 * <p>
 * Optional settings:
 * <ul>
 *   <li>discountOverlaps (bool): Sets
 *       {@link DefaultSimilarity#setDiscountOverlaps(boolean)}</li>
 * </ul>
 * @see TFIDFSimilarity
 */
public class DefaultSimilarityFactory extends SimilarityFactory {

	private boolean mDiscountOverlaps = true;
	
	@Override
	public void init(Params params) throws ErrorException { 
		super.init(params);
		mDiscountOverlaps = params.getBool("discountOverlaps", true);
	}
	
	@Override
	public Similarity getSimilarity() {
		DefaultSimilarity sim = new DefaultSimilarity();
		sim.setDiscountOverlaps(mDiscountOverlaps);
		return sim;
	}

}
