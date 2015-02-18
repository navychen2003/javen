package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.util.Set;

import org.javenstudio.common.indexdb.util.BytesRef;

public interface IIndexReader {

	/** Returns the context of IndexInput for this index reader */
	public IIndexContext getContext();
	
	/** Returns the directory this index resides in. */
	public IDirectory getDirectory();
	
  	/** Returns true if any documents have been deleted */
  	public boolean hasDeletions();
	
  	/** Returns the number of deleted documents. */
  	public int getNumDeletedDocs();
  	
  	/**
  	 * Expert: Returns a the root {@link IndexReaderContext} for this
  	 * {@link IndexReader}'s sub-reader tree. Iff this reader is composed of sub
  	 * readers ,ie. this reader being a composite reader, this method returns a
  	 * {@link CompositeReaderContext} holding the reader's direct children as well as a
  	 * view of the reader tree's atomic leaf contexts. All sub-
  	 * {@link IndexReaderContext} instances referenced from this readers top-level
  	 * context are private to this reader and are not shared with another context
  	 * tree. For example, IndexSearcher uses this API to drive searching by one
  	 * atomic leaf reader at a time. If this reader is not composed of child
  	 * readers, this method returns an {@link AtomicReaderContext}.
  	 * <p>
  	 * Note: Any of the sub-{@link CompositeReaderContext} instances reference from this
  	 * top-level context holds a <code>null</code> {@link CompositeReaderContext#leaves()}
  	 * reference. Only the top-level context maintains the convenience leaf-view
  	 * for performance reasons.
  	 * 
  	 */
  	public IIndexReaderRef getReaderContext();
	
	/** 
	 * Returns one greater than the largest possible document number.
	 * This may be used to, e.g., determine how big to allocate an array which
	 * will have an element for every document number in an index.
	 */
	public int getMaxDoc();
  	
	/** Returns the number of documents in this index. */
	public int getNumDocs();
	
  	/** 
  	 * Expert: visits the fields of a stored document, for
  	 *  custom processing/loading of each field.  If you
  	 *  simply want to load all fields, use {@link
  	 *  #document(int)}.  If you want to load a subset, use
  	 *  {@link StoredFieldVisitor}. 
  	 */
  	public void document(int docID, IFieldVisitor visitor) 
  			throws CorruptIndexException, IOException;
	
  	/**
  	 * Returns the stored fields of the <code>n</code><sup>th</sup>
  	 * <code>Document</code> in this index.  This is just
  	 * sugar for using {@link StoredFieldVisitor}.
  	 * <p>
  	 * <b>NOTE:</b> for performance reasons, this method does not check if the
  	 * requested document is deleted, and therefore asking for a deleted document
  	 * may yield unspecified results. Usually this is not required, however you
  	 * can test if the doc is deleted by checking the {@link
  	 * Bits} returned from {@link MultiFields#getLiveDocs}.
  	 *
  	 * <b>NOTE:</b> only the content of a field is returned,
  	 * if that field was stored during indexing.  Metadata
  	 * like boost, omitNorm, IndexOptions, tokenized, etc.,
  	 * are not preserved.
  	 * 
  	 * @throws CorruptIndexException if the index is corrupt
  	 * @throws IOException if there is a low-level IO error
  	 */
  	public IDocument getDocument(int docID) 
  			throws CorruptIndexException, IOException;
	
  	/**
  	 * Like {@link #document(int)} but only loads the specified
  	 * fields.  Note that this is simply sugar for {@link
  	 * StoredFieldVisitor#DocumentStoredFieldVisitor(Set)}.
  	 */
  	public IDocument getDocument(int docID, Set<String> fieldsToLoad) 
  			throws CorruptIndexException, IOException;
  	
  	/** 
  	 * Returns the number of documents containing the 
  	 * <code>term</code>.  This method returns 0 if the term or
  	 * field does not exists.  This method does not take into
  	 * account deleted documents that have not yet been merged
  	 * away. 
  	 */
  	public int getDocFreq(ITerm term) throws IOException;
  	
  	/** 
  	 * Returns the number of documents containing the
  	 * <code>term</code>.  This method returns 0 if the term or
  	 * field does not exists.  This method does not take into
  	 * account deleted documents that have not yet been merged
  	 * away. 
  	 */
  	public int getDocFreq(String field, BytesRef term) throws IOException;
  	
    /** 
     * Returns the number of documents containing the term
     * <code>term</code>.  This method returns 0 if the term or
     * field does not exists, or -1 if the Codec does not support
     * the measure.  This method does not take into account deleted 
     * documents that have not yet been merged away.
     * @see TermsEnum#totalTermFreq() 
     */
    public long getTotalTermFreq(ITerm term) throws IOException;
  	
	/** 
	 * Retrieve term vectors for this document, or null if
	 *  term vectors were not indexed.  The returned Fields
	 *  instance acts like a single-document inverted index
	 *  (the docID will be 0). 
	 */
	public IFields getTermVectors(int docID) throws IOException;
  	
	/** 
	 * Retrieve term vector for this document and field, or
	 *  null if term vectors were not indexed.  The returned
	 *  Fields instance acts like a single-document inverted
	 *  index (the docID will be 0). 
	 */
	public ITerms getTermVector(int docID, String field) throws IOException;
	
  	/** 
  	 * Expert: Returns a key for this IndexReader, so FieldCache/CachingWrapperFilter 
  	 * can find it again.
  	 * This key must not have equals()/hashCode() methods, 
  	 * so &quot;equals&quot; means &quot;identical&quot;. 
  	 */
  	public Object getCacheKey();
  	
  	/**
  	 * Closes files associated with this index.
  	 * Also saves any new deletions to disk.
  	 * No other methods should be called after this has been called.
  	 * @throws IOException if there is a low-level IO error
  	 */
  	public void close() throws IOException;
  	
	/** 
	 * Expert: adds a {@link ReaderClosedListener}.  The
	 * provided listener will be invoked when this reader is closed.
	 */
	public void addClosedListener(IIndexReaderClosedListener listener);
  	
	/** 
	 * Expert: remove a previously added {@link ReaderClosedListener}.
	 */
	public void removeClosedListener(IIndexReaderClosedListener listener);
	
	/**
	 * Expert: increments the refCount of this IndexReader
	 * instance.  RefCounts are used to determine when a
	 * reader can be closed safely, i.e. as soon as there are
	 * no more references.  Be sure to always call a
	 * corresponding {@link #decRef}, in a finally clause;
	 * otherwise the reader may never be closed.  Note that
	 * {@link #close} simply calls decRef(), which means that
	 * the IndexReader will not really be closed until {@link
	 * #decRef} has been called for all outstanding
	 * references.
	 *
	 * @see #decreaseRef
	 * @see #tryIncreaseRef
	 */
	public void increaseRef();
	
	/**
	 * Expert: decreases the refCount of this IndexReader
	 * instance.  If the refCount drops to 0, then this
	 * reader is closed.  If an exception is hit, the refCount
	 * is unchanged.
	 *
	 * @throws IOException in case an IOException occurs in  doClose()
	 *
	 * @see #incRef
	 */
	public void decreaseRef() throws IOException;
	
	public boolean isClosed();
	
}
