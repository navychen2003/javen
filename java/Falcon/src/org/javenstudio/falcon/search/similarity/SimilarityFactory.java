package org.javenstudio.falcon.search.similarity;

import org.javenstudio.common.indexdb.search.Similarity;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.schema.SchemaAware;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

/**
 * A factory interface for configuring a {@link XSimilarity} in the 
 * schema.xml.  
 * 
 * <p>
 * Subclasses of <code>SimilarityFactory</code> which are {@link SchemaAware} 
 * must take responsibility for either consulting the similarities configured 
 * on individual field types, or generating appropriate error/warning messages 
 * if field type specific similarities exist but are being ignored.  The 
 * <code>IndexSchema</code> will provide such error checking if a 
 * non-<code>SchemaAware</code> instance of <code>SimilarityFactory</code> 
 * is used.
 * 
 * @see SchemaFieldType#getSimilarity
 */
public abstract class SimilarityFactory {
	
	protected Params mParams;

	public void init(Params params) throws ErrorException { 
		mParams = params; 
	}
	
	public Params getParams() { return mParams; }

	public abstract Similarity getSimilarity();
	
}
