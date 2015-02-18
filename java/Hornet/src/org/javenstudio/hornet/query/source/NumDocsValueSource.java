package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Returns the value of {@link IndexReader#numDocs()}
 * for every document. This is the number of documents
 * excluding deletions.
 */
public class NumDocsValueSource extends ValueSource {
	
	public String getName() {
		return "numdocs";
	}

	@Override
	public String getDescription() {
		return getName() + "()";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		// Searcher has no numdocs so we must use the reader instead
		return new ConstIntDocValues(
				ReaderUtil.getTopLevel(readerContext).getReader().getNumDocs(), this);
	}

	@Override
	public boolean equals(Object o) {
		return this.getClass() == o.getClass();
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode();
	}
	
}
