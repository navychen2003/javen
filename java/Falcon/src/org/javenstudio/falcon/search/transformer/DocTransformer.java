package org.javenstudio.falcon.search.transformer;

import java.io.IOException;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ResultItem;

/**
 * A DocTransformer can add, remove or alter a Document before 
 * it is written out to the Response.  For instance, 
 * there are implementations that can put explanations inline 
 * with a document, add constant values and mark items as being 
 * artificially boosted (see {@link QueryElevationComponent})
 *
 * <p/>
 * New instance for each request
 *
 * @see TransformerFactory
 */
public abstract class DocTransformer {
	
	/**
	 *
	 * @return The name of the transformer
	 */
	public abstract String getName();

	/**
	 * This is called before transform and sets
	 * @param context The {@link TransformContext} stores information about 
	 * the current state of things that may be
	 * useful for doing transformations.
	 */
	public void setContext(TransformContext context) throws ErrorException { 
		// do nothing
	}

	/**
	 * This is where implementations do the actual work
	 *
	 * @param doc The document to alter
	 * @param docid The internal doc id
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract void transform(ResultItem doc, int docid) throws ErrorException;

	@Override
	public String toString() {
		return getName();
	}
	
}
