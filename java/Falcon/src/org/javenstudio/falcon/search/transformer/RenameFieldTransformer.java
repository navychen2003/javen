package org.javenstudio.falcon.search.transformer;

import org.javenstudio.falcon.util.ResultItem;

/**
 * Return a field with a name that is different that what is indexed
 *
 * @since 4.0
 */
public class RenameFieldTransformer extends DocTransformer {
	
	protected final String mFrom;
	protected final String mTo;
	protected final boolean mCopy;

	public RenameFieldTransformer(String from, String to, boolean copy) {
		mFrom = from;
		mTo = to;
		mCopy = copy;
	}

	@Override
	public String getName() {
		return "Rename[" + mFrom+  ">>" + mTo + "]";
	}

	@Override
	public void transform(ResultItem doc, int docid) {
		Object v = (mCopy) ? doc.get(mFrom) : doc.remove(mFrom);
		if (v != null) 
			doc.setField(mTo, v);
	}
	
}
