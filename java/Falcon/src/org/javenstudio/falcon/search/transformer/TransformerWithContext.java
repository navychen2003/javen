package org.javenstudio.falcon.search.transformer;

/**
 *
 * @since 4.0
 */
public abstract class TransformerWithContext extends DocTransformer {
	
	protected TransformContext mContext = null;

	@Override
	public void setContext(TransformContext context) {
		mContext = context;
	}
	
}
