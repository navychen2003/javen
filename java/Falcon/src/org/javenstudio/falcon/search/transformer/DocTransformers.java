package org.javenstudio.falcon.search.transformer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ResultItem;

/**
 * Transform a document before it gets sent out
 *
 */
public class DocTransformers extends DocTransformer {
	
	private final List<DocTransformer> mChildren = new ArrayList<DocTransformer>();

	@Override
	public String getName() {
		StringBuilder str = new StringBuilder();
		str.append("Transformers[");
		
		Iterator<DocTransformer> iter = mChildren.iterator();
		while (iter.hasNext()) {
			str.append(iter.next().getName());
			if (iter.hasNext()) 
				str.append(",");
		}
		
		str.append("]");
		return str.toString();
	}

	public void addTransformer(DocTransformer a) {
		mChildren.add( a );
	}

	public int size() {
		return mChildren.size();
	}

	public DocTransformer getTransformer(int idx) {
		return mChildren.get(idx);
	}

	@Override
	public void setContext(TransformContext context) throws ErrorException {
		for (DocTransformer a : mChildren) {
			a.setContext(context);
		}
	}

	@Override
	public void transform(ResultItem doc, int docid) throws ErrorException {
		for (DocTransformer a : mChildren) {
			a.transform(doc, docid);
		}
	}
	
}
