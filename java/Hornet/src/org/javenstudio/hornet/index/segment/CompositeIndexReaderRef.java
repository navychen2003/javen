package org.javenstudio.hornet.index.segment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IIndexReaderRef;

/**
 * {@link IndexReaderRef} for {@link CompositeIndexReader} instance.
 * 
 */
public class CompositeIndexReaderRef extends IndexReaderRef {
	
	private final List<IIndexReaderRef> mChildren;
	private final List<IAtomicReaderRef> mLeaves;
	private final CompositeIndexReader mReader;
  
	static CompositeIndexReaderRef create(CompositeIndexReader reader) {
		return new Builder(reader).build();
	}

	/**
	 * Creates a {@link CompositeIndexReaderRef} for intermediate readers that aren't
	 * not top-level readers in the current context
	 */
	CompositeIndexReaderRef(CompositeIndexReaderRef parent, CompositeIndexReader reader,
			int ordInParent, int docbaseInParent, List<IIndexReaderRef> children) {
		this(parent, reader, ordInParent, docbaseInParent, children, null);
	}
  
	/**
	 * Creates a {@link CompositeIndexReaderRef} for top-level readers with parent set to <code>null</code>
	 */
	CompositeIndexReaderRef(CompositeIndexReader reader, 
			List<IIndexReaderRef> children, List<IAtomicReaderRef> leaves) {
		this(null, reader, 0, 0, children, leaves);
	}
  
	private CompositeIndexReaderRef(CompositeIndexReaderRef parent, 
			CompositeIndexReader reader, int ordInParent, int docbaseInParent, 
			List<IIndexReaderRef> children, List<IAtomicReaderRef> leaves) {
		super(parent, ordInParent, docbaseInParent);
		
		mChildren = Collections.unmodifiableList(children);
		mLeaves = (leaves == null) ? null : Collections.unmodifiableList(leaves);
		mReader = reader;
	}

	@Override
	public List<IAtomicReaderRef> getLeaves() {
		if (!mIsTopLevel)
			throw new UnsupportedOperationException("This is not a top-level context.");
		
		assert mLeaves != null;
		return mLeaves;
	}
  
	@Override
	public List<IIndexReaderRef> getChildren() {
		return mChildren;
	}
  
	@Override
	public CompositeIndexReader getReader() {
		return mReader;
	}
  
	private static final class Builder {
		private final CompositeIndexReader mReader;
		private final List<IAtomicReaderRef> mLeaves = new ArrayList<IAtomicReaderRef>();
		private int mLeafDocBase = 0;
    
		public Builder(CompositeIndexReader reader) {
			mReader = reader;
		}
    
		public CompositeIndexReaderRef build() {
			return (CompositeIndexReaderRef) build(null, mReader, 0, 0);
		}
    
		private IIndexReaderRef build(CompositeIndexReaderRef parent, 
				IIndexReader reader, int ord, int docBase) {
			if (reader instanceof AtomicIndexReader) {
				final AtomicIndexReader ar = (AtomicIndexReader) reader;
				final IAtomicReaderRef atomic = new AtomicIndexReaderRef(
						parent, ar, ord, docBase, mLeaves.size(), mLeafDocBase);
				
				mLeaves.add(atomic);
				mLeafDocBase += reader.getMaxDoc();
				return atomic;
				
			} else {
				final CompositeIndexReader cr = (CompositeIndexReader) reader;
				final List<? extends IndexReader> sequentialSubReaders = cr.getSequentialSubReaders();
				final List<IIndexReaderRef> children = Arrays.asList(new IIndexReaderRef[sequentialSubReaders.size()]);
				
				final CompositeIndexReaderRef newParent;
				if (parent == null) 
					newParent = new CompositeIndexReaderRef(cr, children, mLeaves);
				else 
					newParent = new CompositeIndexReaderRef(parent, cr, ord, docBase, children);
				
				int newDocBase = 0;
				for (int i = 0, c = sequentialSubReaders.size(); i < c; i++) {
					final IndexReader r = sequentialSubReaders.get(i);
					children.set(i, build(newParent, r, i, newDocBase));
					newDocBase += r.getMaxDoc();
				}
				
				assert newDocBase == cr.getMaxDoc();
				return newParent;
			}
		}
	}
	
}
