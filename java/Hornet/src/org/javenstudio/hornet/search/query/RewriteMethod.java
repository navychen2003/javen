package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;

/** Abstract class that defines how the query is rewritten. */
public abstract class RewriteMethod {

	public abstract IQuery rewrite(IIndexReader reader, MultiTermQuery query) throws IOException;
	
	/**
	 * Returns the {@link MultiTermQuery}s {@link TermsEnum}
	 * @see MultiTermQuery#getTermsEnum(Terms, AttributeSource)
	 */
	protected ITermsEnum getTermsEnum(MultiTermQuery query, ITerms terms) throws IOException {
		return query.getTermsEnum(terms); // allow RewriteMethod subclasses to pull a TermsEnum from the MTQ 
	}
	
}
