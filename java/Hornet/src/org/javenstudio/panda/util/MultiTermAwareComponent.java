package org.javenstudio.panda.util;

import org.javenstudio.panda.analysis.AnalysisFactory;

/** 
 * Add to any analysis factory component to allow returning an
 * analysis component factory for use with partial terms in prefix queries,
 * wildcard queries, range query endpoints, regex queries, etc.
 */
public interface MultiTermAwareComponent {
	
	/** 
	 * Returns an analysis component to handle analysis if multi-term queries.
	 * The returned component must be a TokenizerFactory, TokenFilterFactory or CharFilterFactory.
	 */
	public AnalysisFactory getMultiTermComponent();
	
}
