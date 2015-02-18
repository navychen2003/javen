package org.javenstudio.common.indexdb;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.Bits;

public interface IAtomicReader extends IIndexReader {

	/**
	 * Returns {@link Fields} for this reader.
	 * This method may return null if the reader has no
	 * postings.
	 */
	public IFields getFields() throws IOException;
	
	/**
	 * Get the {@link FieldInfos} describing all fields in
	 * this reader.
	 * 
	 */
	public IFieldInfos getFieldInfos();
	
	/** 
	 * Returns the {@link Bits} representing live (not
	 *  deleted) docs.  A set bit indicates the doc ID has not
	 *  been deleted.  If this method returns null it means
	 *  there are no deleted documents (all documents are
	 *  live).
	 *
	 *  The returned instance has been safely published for
	 *  use by multiple threads without additional
	 *  synchronization.
	 */
	public Bits getLiveDocs();
	
	/** This may return null if the field does not exist.*/
	public ITerms getTerms(String field) throws IOException;
	
}
