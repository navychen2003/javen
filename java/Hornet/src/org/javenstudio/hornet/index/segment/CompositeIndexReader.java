package org.javenstudio.hornet.index.segment;

import java.util.List;

import org.javenstudio.common.indexdb.IIndexReaderRef;

/** 
 * Instances of this reader type can only
 * be used to get stored fields from the underlying AtomicReaders,
 * but it is not possible to directly retrieve postings. To do that, get
 * the sub-readers via {@link #getSequentialSubReaders}.
 * Alternatively, you can mimic an {@link AtomicReader} (with a serious slowdown),
 * by wrapping composite readers with {@link SlowCompositeReaderWrapper}.
 *
 * <p>IndexReader instances for indexes on disk are usually constructed
 * with a call to one of the static <code>DirectoryReader.open()</code> methods,
 * e.g. {@link DirectoryReader#open(Directory)}. {@link DirectoryReader} implements
 * the {@code CompositeReader} interface, it is not possible to directly get postings.
 * <p> Concrete subclasses of IndexReader are usually constructed with a call to
 * one of the static <code>open()</code> methods, e.g. {@link
 * DirectoryReader#open(Directory)}.
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
public abstract class CompositeIndexReader extends IndexReader {

	private volatile CompositeIndexReaderRef mReaderContext = null; // lazy init
	
	protected CompositeIndexReader() { 
		super();
	}
  
	@Override
	public final IIndexReaderRef getReaderContext() {
		ensureOpen();
		// lazy init without thread safety for perf reasons: 
		// Building the readerContext twice does not hurt!
		if (mReaderContext == null) {
			assert getSequentialSubReaders() != null;
			mReaderContext = CompositeIndexReaderRef.create(this);
		}
		return mReaderContext;
	}
	
	/** 
	 * Expert: returns the sequential sub readers that this
	 *  reader is logically composed of. It contrast to previous
	 *  Lucene versions may not return null.
	 *  If this method returns an empty array, that means this
	 *  reader is a null reader (for example a MultiReader
	 *  that has no sub readers).
	 */
	public abstract List<? extends IndexReader> getSequentialSubReaders();
  
	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder();
		buffer.append(getClass().getSimpleName());
		buffer.append('(');
		final List<? extends IndexReader> subReaders = getSequentialSubReaders();
		assert subReaders != null;
		if (!subReaders.isEmpty()) {
			buffer.append(subReaders.get(0));
			for (int i = 1, c = subReaders.size(); i < c; ++i) {
				buffer.append(" ").append(subReaders.get(i));
			}
		}
		buffer.append(')');
		return buffer.toString();
	}
	
}
