package org.javenstudio.falcon.search.hits;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.search.DocIdSet;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.search.Filter;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.search.BitsFilteredDocIdSet;
import org.javenstudio.hornet.search.OpenBitSet;
import org.javenstudio.hornet.search.OpenBitSetIterator;

/**
 * <code>BitDocSet</code> represents an unordered set of Document Ids
 * using a BitSet.  A set bit represents inclusion in the set for that document.
 *
 * @since 0.9
 */
public class BitDocSet extends DocSetBase {
	
	private final OpenBitSet mBits;
	private int mSize; // number of docs in the set (cached for perf)

	public BitDocSet() {
		mBits = new OpenBitSet();
	}

	/** 
	 * Construct a BitDocSet.
	 * The capacity of the OpenBitSet should be at least maxDoc() 
	 */
	public BitDocSet(OpenBitSet bits) {
		mBits = bits;
		mSize = -1;
	}

	/** 
	 * Construct a BitDocSet, and provides the number of set bits.
	 * The capacity of the OpenBitSet should be at least maxDoc()
	 */
	public BitDocSet(OpenBitSet bits, int size) {
		mBits = bits;
		mSize = size;
	}

  /*** DocIterator using nextSetBit()
  public DocIterator iterator() {
    return new DocIterator() {
      int pos=bits.nextSetBit(0);
      public boolean hasNext() {
        return pos>=0;
      }

      public Integer next() {
        return nextDoc();
      }

      public void remove() {
        bits.clear(pos);
      }

      public int nextDoc() {
        int old=pos;
        pos=bits.nextSetBit(old+1);
        return old;
      }

      public float score() {
        return 0.0f;
      }
    };
  }
  ***/

	public DocIterator iterator() {
		return new DocIterator() {
			private final OpenBitSetIterator mIter = new OpenBitSetIterator(mBits);
			private int mPos = mIter.nextDoc();
			
			@Override
			public boolean hasNext() {
				return mPos != DocIdSetIterator.NO_MORE_DOCS;
			}

			@Override
			public Integer next() {
				return nextDoc();
			}

			@Override
			public void remove() {
				mBits.clear(mPos);
			}

			@Override
			public int nextDoc() {
				int old = mPos;
				mPos = mIter.nextDoc();
				return old;
			}

			@Override
			public float score() {
				return 0.0f;
			}
		};
	}

	/**
	 * @return the <b>internal</b> OpenBitSet that should <b>not</b> be modified.
	 */
	@Override
	public OpenBitSet getBits() {
		return mBits;
	}

	@Override
	public void add(int doc) {
		mBits.set(doc);
		mSize = -1; // invalidate size
	}

	@Override
	public void addUnique(int doc) {
		mBits.set(doc);
		mSize = -1;  // invalidate size
	}

	@Override
	public int size() {
		if (mSize != -1) 
			return mSize;
		
		return mSize = (int)mBits.cardinality();
	}

	/**
	 * The number of set bits - size - is cached. 
	 * If the bitset is changed externally,
	 * this method should be used to invalidate the previously cached size.
	 */
	public void invalidateSize() {
		mSize = -1;
	}

	/** 
	 * Returns true of the doc exists in the set.
	 *  Should only be called when doc < OpenBitSet.size()
	 */
	@Override
	public boolean exists(int doc) {
		return mBits.fastGet(doc);
	}

	@Override
	public int intersectionSize(DocSet other) {
		if (other instanceof BitDocSet) {
			return (int)OpenBitSet.intersectionCount(
					this.mBits, ((BitDocSet)other).mBits);
			
		} else {
			// they had better not call us back!
			return other.intersectionSize(this);
		}
	}

	@Override
	public boolean intersects(DocSet other) {
		if (other instanceof BitDocSet) {
			return mBits.intersects(((BitDocSet)other).mBits);
			
		} else {
			// they had better not call us back!
			return other.intersects(this);
		}
	}

	@Override
	public int unionSize(DocSet other) {
		if (other instanceof BitDocSet) {
			// if we don't know our current size, this is faster than
			// size + other.size - intersection_size
			return (int)OpenBitSet.unionCount(
					this.mBits, ((BitDocSet)other).mBits);
			
		} else {
			// they had better not call us back!
			return other.unionSize(this);
		}
	}

	@Override
	public int andNotSize(DocSet other) {
		if (other instanceof BitDocSet) {
			// if we don't know our current size, this is faster than
			// size - intersection_size
			return (int)OpenBitSet.andNotCount(
					this.mBits, ((BitDocSet)other).mBits);
			
		} else {
			return super.andNotSize(other);
		}
	}

	@Override
	public void setBitsOn(OpenBitSet target) {
		target.union(mBits);
	}

	@Override
	public DocSet andNot(DocSet other) {
		OpenBitSet newbits = (OpenBitSet)(mBits.clone());
		if (other instanceof BitDocSet) {
			newbits.andNot(((BitDocSet)other).mBits);
			
		} else {
			DocIterator iter = other.iterator();
			while (iter.hasNext()) { 
				newbits.clear(iter.nextDoc()); 
			}
		}
		
		return new BitDocSet(newbits);
	}

	@Override
	public DocSet union(DocSet other) {
		OpenBitSet newbits = (OpenBitSet)(mBits.clone());
		if (other instanceof BitDocSet) {
			newbits.union(((BitDocSet)other).mBits);
			
		} else {
			DocIterator iter = other.iterator();
			while (iter.hasNext()) { 
				newbits.set(iter.nextDoc()); 
			}
		}
		
		return new BitDocSet(newbits);
	}

	@Override
	public long getMemorySize() {
		return (mBits.getBits().length() << 3) + 16;
	}

	@Override
	protected BitDocSet clone() {
		return new BitDocSet((OpenBitSet)mBits.clone(), mSize);
	}

	@Override
	public Filter getTopFilter() {
		final OpenBitSet bs = mBits;
		// TODO: if cardinality isn't cached, do a quick measure of sparseness
		// and return null from bits() if too sparse.

		return new Filter() {
			@Override
			public DocIdSet getDocIdSet(final IAtomicReaderRef context, final Bits acceptDocs) {
				IAtomicReader reader = context.getReader();
				// all DocSets that are used as filters only include live docs
				final Bits acceptDocs2 = (acceptDocs == null) ? null : 
					(reader.getLiveDocs() == acceptDocs ? null : acceptDocs);

				if (context.isTopLevel()) 
					return BitsFilteredDocIdSet.wrap(bs, acceptDocs);
				
				final int base = context.getDocBase();
				final int maxDoc = reader.getMaxDoc();
				final int max = base + maxDoc; // one past the max doc in this segment.

				return BitsFilteredDocIdSet.wrap(new DocIdSet() {
					@Override
					public DocIdSetIterator iterator() {
						return new DocIdSetIterator() {
							int mPos = base-1;
							int mAdjustedDoc = -1;

							@Override
							public int getDocID() {
								return mAdjustedDoc;
							}

							@Override
							public int nextDoc() {
								mPos = bs.nextSetBit(mPos + 1);
								return mAdjustedDoc = (mPos >= 0 && mPos < max) ? mPos-base : NO_MORE_DOCS;
							}

							@Override
							public int advance(int target) {
								if (target == NO_MORE_DOCS) 
									return mAdjustedDoc = NO_MORE_DOCS;
								
								mPos = bs.nextSetBit(target + base);
								return mAdjustedDoc = (mPos >= 0 && mPos < max) ? mPos-base : NO_MORE_DOCS;
							}
						};
					}

					@Override
					public boolean isCacheable() {
						return true;
					}

					@Override
					public Bits getBits() {
						return new Bits() {
							@Override
							public boolean get(int index) {
								return bs.fastGet(index + base);
							}

							@Override
							public int length() {
								return maxDoc;
							}
						};
					}

				}, acceptDocs2);
			}
		};
	}
	
}
