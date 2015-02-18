package org.javenstudio.common.indexdb.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.util.Bits;

/** 
 *  Abstract base class for restricting which documents may
 *  be returned during searching.
 */
public abstract class Filter implements IFilter {
  
	/**
	 * Creates a {@link DocIdSet} enumerating the documents that should be
	 * permitted in search results. <b>NOTE:</b> null can be
	 * returned if no documents are accepted by this Filter.
	 * <p>
	 * Note: This method will be called once per segment in
	 * the index during searching.  The returned {@link DocIdSet}
	 * must refer to document IDs for that segment, not for
	 * the top-level reader.
	 * 
	 * @param context a {@link AtomicReaderContext} instance opened on the index currently
	 *         searched on. Note, it is likely that the provided reader info does not
	 *         represent the whole underlying index i.e. if the index has more than
	 *         one segment the given reader only represents a single segment.
	 *         The provided context is always an atomic context, so you can call 
	 *         {@link AtomicReader#fields()}
	 *         on the context's reader, for example.
	 *
	 * @param acceptDocs
	 *          Bits that represent the allowable docs to match (typically deleted docs
	 *          but possibly filtering other documents)
	 *          
	 * @return a DocIdSet that provides the documents which should be permitted or
	 *         prohibited in search results. <b>NOTE:</b> null can be returned if
	 *         no documents will be accepted by this Filter.
	 */
	public abstract IDocIdSet getDocIdSet(IAtomicReaderRef context, Bits acceptDocs) throws IOException;
	
}
