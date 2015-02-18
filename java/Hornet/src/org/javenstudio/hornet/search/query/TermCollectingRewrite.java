package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IIndexReaderRef;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITermContext;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;

public abstract class TermCollectingRewrite<Q extends IQuery> 
		extends RewriteMethod {
  
	/** Return a suitable top-level Query for holding all expanded terms. */
	protected abstract Q getTopLevelQuery() throws IOException;
  
	/** Add a MultiTermQuery term to the top-level query */
	protected final void addClause(Q topLevel, ITerm term, int docCount, float boost) throws IOException {
		addClause(topLevel, term, docCount, boost, null);
	}
  
	protected abstract void addClause(Q topLevel, ITerm term, int docCount, 
			float boost, ITermContext states) throws IOException;

	protected final void collectTerms(IIndexReader reader, MultiTermQuery query, 
			TermCollector collector) throws IOException {
		IIndexReaderRef topReaderContext = reader.getReaderContext();
		Comparator<BytesRef> lastTermComp = null;
		
		for (IAtomicReaderRef context : topReaderContext.getLeaves()) {
			final IFields fields = context.getReader().getFields();
			if (fields == null) 
				continue; // reader has no fields

			final ITerms terms = fields.getTerms(query.getFieldName());
			if (terms == null) 
				continue; // field does not exist

			final ITermsEnum termsEnum = getTermsEnum(query, terms); //, collector.attributes);
			assert termsEnum != null;

			if (termsEnum == TermsEnum.EMPTY)
				continue;
      
			// Check comparator compatibility:
			final Comparator<BytesRef> newTermComp = termsEnum.getComparator();
			if (lastTermComp != null && newTermComp != null && newTermComp != lastTermComp) {
				throw new RuntimeException("term comparator should not change between segments: " 
						+ lastTermComp + " != " + newTermComp);
			}
			lastTermComp = newTermComp;
			
			collector.setReaderContext(topReaderContext, context);
			collector.setNextEnum(termsEnum);
			
			BytesRef bytes;
			while ((bytes = termsEnum.next()) != null) {
				if (!collector.collect(bytes))
					return; // interrupt whole term collection, so also don't iterate other subReaders
			}
		}
	}
  
}
