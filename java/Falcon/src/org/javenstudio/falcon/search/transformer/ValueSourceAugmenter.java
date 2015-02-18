package org.javenstudio.falcon.search.transformer;

import java.io.IOException;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.util.ResultItem;

/**
 * Add values from a ValueSource (function query etc)
 *
 * NOT really sure how or if this could work...
 *
 * @since 4.0
 */
public class ValueSourceAugmenter extends DocTransformer {
	
	private final String mName;
	private final QueryBuilder mBuilder;
	private final ValueSource mValueSource;

	private ValueSourceContext mContext;
	
	private List<IAtomicReaderRef> mReaderContexts;
	private FunctionValues[] mDocValuesArr;
	
	public ValueSourceAugmenter(String name, 
			QueryBuilder qparser, ValueSource valueSource) {
		mName = name;
		mBuilder = qparser;
		mValueSource = valueSource;
	}

	@Override
	public String getName() {
		return "function(" + mName + ")";
	}

	@Override
	public void setContext(TransformContext context) throws ErrorException {
		Searcher searcher = mBuilder.getRequest().getSearcher();
		IIndexReader reader = searcher.getIndexReader();
		
		mReaderContexts = reader.getReaderContext().getLeaves();
		mDocValuesArr = new FunctionValues[mReaderContexts.size()];

		mContext = searcher.createValueSourceContext();
		searcher.createValueSourceWeight(mContext, mValueSource);
	}

	@Override
	public void transform(ResultItem doc, int docid) throws ErrorException {
		// This is only good for random-access functions
		try {
			// TODO: calculate this stuff just once across diff functions
			int idx = ReaderUtil.subIndex(docid, mReaderContexts);
			IAtomicReaderRef rcontext = mReaderContexts.get(idx);
			FunctionValues values = mDocValuesArr[idx];
			if (values == null) {
				mDocValuesArr[idx] = values = 
						mValueSource.getValues(mContext, rcontext);
			}

			int localId = docid - rcontext.getDocBase();
			Object val = values.objectVal(localId);
			if (val != null) 
				doc.setField(mName, val);
			
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"exception at docid " + docid + " for valuesource " + mValueSource, e);
		}
	}
	
}
