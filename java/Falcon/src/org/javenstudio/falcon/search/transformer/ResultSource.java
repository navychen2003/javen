package org.javenstudio.falcon.search.transformer;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.falcon.util.ResultItem;

/**
 * Abstracts the source for {@link ResultItem} instances.
 * The source of documents is different for a distributed search than local search
 */
public interface ResultSource {

	public ResultItem retrieve(IScoreDoc doc);

}
