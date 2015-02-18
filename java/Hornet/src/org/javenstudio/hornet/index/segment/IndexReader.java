package org.javenstudio.hornet.index.segment;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.indexdb.AlreadyClosedException;
import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IFieldVisitor;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IIndexReaderClosedListener;
import org.javenstudio.common.indexdb.IIndexReaderRef;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.index.field.StoredFieldVisitor;
import org.javenstudio.common.indexdb.util.BytesRef;

/** 
 * IndexReader is an abstract class, providing an interface for accessing an
 * index.  Search of an index is done entirely through this abstract interface,
 * so that any subclass which implements it is searchable.
 *
 * <p>There are two different types of IndexReaders:
 * <ul>
 * <li>{@link AtomicReader}: These indexes do not consist of several sub-readers,
 * they are atomic. They support retrieval of stored fields, doc values, terms,
 * and postings.
 * <li>{@link CompositeReader}: Instances (like {@link DirectoryReader})
 * of this reader can only
 * be used to get stored fields from the underlying AtomicReaders,
 * but it is not possible to directly retrieve postings. To do that, get
 * the sub-readers via {@link CompositeReader#getSequentialSubReaders}.
 * Alternatively, you can mimic an {@link AtomicReader} (with a serious slowdown),
 * by wrapping composite readers with {@link SlowCompositeReaderWrapper}.
 * </ul>
 *
 * <p>IndexReader instances for indexes on disk are usually constructed
 * with a call to one of the static <code>DirectoryReader.open()</code> methods,
 * e.g. {@link DirectoryReader#open(Directory)}. {@link DirectoryReader} implements
 * the {@link CompositeReader} interface, it is not possible to directly get postings.
 *
 * <p> For efficiency, in this API documents are often referred to via
 * <i>document numbers</i>, non-negative integers which each name a unique
 * document in the index.  These document numbers are ephemeral -- they may change
 * as documents are added to and deleted from an index.  Clients should thus not
 * rely on a given document having the same number between sessions.
 *
 * <p>
 * <a name="thread-safety"></a><p><b>NOTE</b>: {@link
 * IndexReader} instances are completely thread
 * safe, meaning multiple threads can call any of its methods,
 * concurrently.  If your application requires external
 * synchronization, you should <b>not</b> synchronize on the
 * <code>IndexReader</code> instance; use your own
 * (non-Lucene) objects instead.
 */
public abstract class IndexReader implements IIndexReader, Closeable {

	private final Set<IIndexReaderClosedListener> mReaderClosedListeners = 
			Collections.synchronizedSet(new LinkedHashSet<IIndexReaderClosedListener>());

	private final Set<IndexReader> mParentReaders = 
			Collections.synchronizedSet(Collections.newSetFromMap(
					new WeakHashMap<IndexReader,Boolean>()));
	
	private final AtomicInteger mRefCount = new AtomicInteger(1);
	private boolean mClosed = false;
	private boolean mClosedByChild = false;
	
	protected IndexReader() {
		if (!(this instanceof CompositeIndexReader || this instanceof AtomicIndexReader)) {
			throw new Error("IndexReader should never be directly extended, " + 
					"subclass AtomicReader or CompositeIndexReader instead.");
		}
	}
  
	/** 
	 * Expert: adds a {@link ReaderClosedListener}.  The
	 * provided listener will be invoked when this reader is closed.
	 */
	@Override
	public final void addClosedListener(IIndexReaderClosedListener listener) {
		ensureOpen();
		mReaderClosedListeners.add(listener);
	}

	/** 
	 * Expert: remove a previously added {@link ReaderClosedListener}.
	 */
	@Override
	public final void removeClosedListener(IIndexReaderClosedListener listener) {
		ensureOpen();
		mReaderClosedListeners.remove(listener);
	}
  
	/** 
	 * Expert: This method is called by {@code IndexReader}s which wrap other readers
	 * (e.g. {@link CompositeReader} or {@link FilterAtomicReader}) to register the parent
	 * at the child (this reader) on construction of the parent. When this reader is closed,
	 * it will mark all registered parents as closed, too. The references to parent readers
	 * are weak only, so they can be GCed once they are no longer in use.
	 */
	public final void registerParentReader(IndexReader reader) {
		ensureOpen();
		mParentReaders.add(reader);
	}

	private void notifyReaderClosedListeners() {
		synchronized (mReaderClosedListeners) {
			for (IIndexReaderClosedListener listener : mReaderClosedListeners) {
				listener.onClose(this);
			}
		}
	}

	private void reportCloseToParentReaders() {
		synchronized (mParentReaders) {
			for(IndexReader parent : mParentReaders) {
				parent.mClosedByChild = true;
				// cross memory barrier by a fake write:
				parent.mRefCount.addAndGet(0);
				// recurse:
				parent.reportCloseToParentReaders();
			}
		}
	}

	/** Expert: returns the current refCount for this reader */
	public final int getRefCount() {
		// NOTE: don't ensureOpen, so that callers can see
		// refCount is 0 (reader is closed)
		return mRefCount.get();
	}
  
	@Override
	public boolean isClosed() { 
		return mClosed || mRefCount.get() <= 0;
	}
	
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
	@Override
	public final void increaseRef() {
		ensureOpen();
		mRefCount.incrementAndGet();
	}
  
	/**
	 * Expert: increments the refCount of this IndexReader
	 * instance only if the IndexReader has not been closed yet
	 * and returns <code>true</code> iff the refCount was
	 * successfully incremented, otherwise <code>false</code>.
	 * If this method returns <code>false</code> the reader is either
	 * already closed or is currently been closed. Either way this
	 * reader instance shouldn't be used by an application unless
	 * <code>true</code> is returned.
	 * <p>
	 * RefCounts are used to determine when a
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
	 * @see #increaseRef
	 */
	public final boolean tryIncreaseRef() {
		int count;
		while ((count = mRefCount.get()) > 0) {
			if (mRefCount.compareAndSet(count, count+1)) 
				return true;
		}
		return false;
	}

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
	@Override
	public final void decreaseRef() throws IOException {
		// only check refcount here (don't call ensureOpen()), so we can
		// still close the reader if it was made invalid by a child:
		if (mRefCount.get() <= 0) 
			throw new AlreadyClosedException("this IndexReader is closed");
    
		final int rc = mRefCount.decrementAndGet();
		if (rc == 0) {
			boolean success = false;
			try {
				doClose();
				success = true;
			} finally {
				if (!success) {
					// Put reference back on failure
					mRefCount.incrementAndGet();
				}
			}
			reportCloseToParentReaders();
			notifyReaderClosedListeners();
		} else if (rc < 0) {
			throw new IllegalStateException("too many decRef calls: " + 
					"refCount is " + rc + " after decrement");
		}
	}
  
	/**
	 * @throws AlreadyClosedException if this IndexReader is closed
	 */
	protected final void ensureOpen() throws AlreadyClosedException {
		if (mRefCount.get() <= 0) 
			throw new AlreadyClosedException("this IndexReader is closed");
		
		// the happens before rule on reading the refCount, which must be after the fake write,
		// ensures that we see the value:
		if (mClosedByChild) {
			throw new AlreadyClosedException("this IndexReader cannot " + 
					"be used anymore as one of its child readers was closed");
		}
	}
  
	/** 
	 * {@inheritDoc}
	 * <p>For caching purposes, {@code IndexReader} subclasses are not allowed
	 * to implement equals/hashCode, so methods are declared final.
	 * To lookup instances from caches use {@link #getCoreCacheKey} and 
	 * {@link #getCombinedCoreAndDeletesKey}.
	 */
	@Override
	public final boolean equals(Object obj) {
		return (this == obj);
	}
  
	/** 
	 * {@inheritDoc}
	 * <p>For caching purposes, {@code IndexReader} subclasses are not allowed
	 * to implement equals/hashCode, so methods are declared final.
	 * To lookup instances from caches use {@link #getCoreCacheKey} and 
	 * {@link #getCombinedCoreAndDeletesKey}.
	 */
	@Override
	public final int hashCode() {
		return System.identityHashCode(this);
	}

	/** 
	 * Retrieve term vectors for this document, or null if
	 *  term vectors were not indexed.  The returned Fields
	 *  instance acts like a single-document inverted index
	 *  (the docID will be 0). 
	 */
	public abstract IFields getTermVectors(int docID) throws IOException;

	/** 
	 * Retrieve term vector for this document and field, or
	 *  null if term vectors were not indexed.  The returned
	 *  Fields instance acts like a single-document inverted
	 *  index (the docID will be 0). 
	 */
	@Override
	public final ITerms getTermVector(int docID, String field) throws IOException {
		IFields vectors = getTermVectors(docID);
		if (vectors == null) 
			return null;
		
		return vectors.getTerms(field);
	}

	/** Returns the number of documents in this index. */
	public abstract int getNumDocs();

	/** 
	 * Returns one greater than the largest possible document number.
	 * This may be used to, e.g., determine how big to allocate an array which
	 * will have an element for every document number in an index.
	 */
	public abstract int getMaxDoc();

  	/** Returns the number of deleted documents. */
	@Override
  	public final int getNumDeletedDocs() {
  		return getMaxDoc() - getNumDocs();
  	}

  	/** 
  	 * Expert: visits the fields of a stored document, for
  	 *  custom processing/loading of each field.  If you
  	 *  simply want to load all fields, use {@link
  	 *  #document(int)}.  If you want to load a subset, use
  	 *  {@link StoredFieldVisitor}. 
  	 */
  	public abstract void document(int docID, IFieldVisitor visitor) 
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
  	// TODO: we need a separate StoredField, so that the
  	// Document returned here contains that class not
  	// IndexableField
  	@Override
  	public final IDocument getDocument(int docID) throws CorruptIndexException, IOException {
  		final StoredFieldVisitor visitor = new StoredFieldVisitor();
  		document(docID, visitor);
  		return visitor.getDocument();
  	}

  	/**
  	 * Like {@link #document(int)} but only loads the specified
  	 * fields.  Note that this is simply sugar for {@link
  	 * StoredFieldVisitor#DocumentStoredFieldVisitor(Set)}.
  	 */
  	@Override
  	public final IDocument getDocument(int docID, Set<String> fieldsToLoad) 
  			throws CorruptIndexException, IOException {
  		final StoredFieldVisitor visitor = new StoredFieldVisitor(fieldsToLoad);
  		document(docID, visitor);
  		return visitor.getDocument();
  	}

  	/** Returns true if any documents have been deleted */
  	public abstract boolean hasDeletions();

  	/**
  	 * Closes files associated with this index.
  	 * Also saves any new deletions to disk.
  	 * No other methods should be called after this has been called.
  	 * @throws IOException if there is a low-level IO error
  	 */
  	@Override
  	public final synchronized void close() throws IOException {
  		if (!mClosed) {
  			decreaseRef();
  			mClosed = true;
  		}
  	}
  
  	/** Implements close. */
  	protected abstract void doClose() throws IOException;

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
  	public abstract IIndexReaderRef getReaderContext();

  	/** 
  	 * Expert: Returns a key for this IndexReader, so FieldCache/CachingWrapperFilter 
  	 * can find it again.
  	 * This key must not have equals()/hashCode() methods, 
  	 * so &quot;equals&quot; means &quot;identical&quot;. 
  	 */
  	@Override
  	public Object getCacheKey() {
  		// Don't can ensureOpen since FC calls this (to evict)
  		// on close
  		return this;
  	}

  	/** 
  	 * Expert: Returns a key for this IndexReader that also includes deletions,
  	 * so FieldCache/CachingWrapperFilter can find it again.
  	 * This key must not have equals()/hashCode() methods, so &quot;equals&quot; 
  	 * means &quot;identical&quot;. 
  	 */
  	public Object getCombinedCoreAndDeletesKey() {
  		// Don't can ensureOpen since FC calls this (to evict)
  		// on close
  		return this;
  	}
  
  	/** 
  	 * Returns the number of documents containing the 
  	 * <code>term</code>.  This method returns 0 if the term or
  	 * field does not exists.  This method does not take into
  	 * account deleted documents that have not yet been merged
  	 * away. 
  	 */
  	public final int getDocFreq(ITerm term) throws IOException {
  		return getDocFreq(term.getField(), term.getBytes());
  	}

  	/** 
  	 * Returns the number of documents containing the
  	 * <code>term</code>.  This method returns 0 if the term or
  	 * field does not exists.  This method does not take into
  	 * account deleted documents that have not yet been merged
  	 * away. 
  	 */
  	public abstract int getDocFreq(String field, BytesRef term) throws IOException;
	
    /** 
     * Returns the number of documents containing the term
     * <code>term</code>.  This method returns 0 if the term or
     * field does not exists, or -1 if the Codec does not support
     * the measure.  This method does not take into account deleted 
     * documents that have not yet been merged away.
     * @see TermsEnum#totalTermFreq() 
     */
    public abstract long getTotalTermFreq(ITerm term) throws IOException;
  	
}
