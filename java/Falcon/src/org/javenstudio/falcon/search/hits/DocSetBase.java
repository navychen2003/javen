package org.javenstudio.falcon.search.hits;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.search.DocIdSet;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.search.Filter;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.search.BitsFilteredDocIdSet;
import org.javenstudio.hornet.search.OpenBitSet;

/** A base class that may be usefull for implementing DocSets */
public abstract class DocSetBase implements DocSet {

	// Not implemented efficiently... for testing purposes only
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DocSet)) 
			return false;
		
		DocSet other = (DocSet)obj;
		if (this.size() != other.size()) 
			return false;

		if (this instanceof DocList && other instanceof DocList) {
			// compare ordering
			DocIterator i1 = this.iterator();
			DocIterator i2 = other.iterator();
			
			while (i1.hasNext() && i2.hasNext()) {
				if (i1.nextDoc() != i2.nextDoc()) 
					return false;
			}
			
			return true;
			// don't compare matches
		}

		// if (this.size() != other.size()) return false;
		
		return this.getBits().equals(other.getBits());
	}

	/**
	 * @throws ErrorException Base implementation does not allow modifications
	 */
	@Override
	public void add(int doc) throws ErrorException {
		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
				"Unsupported Operation");
	}

	/**
	 * @throws ErrorException Base implementation does not allow modifications
	 */
	@Override
	public void addUnique(int doc) throws ErrorException {
		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
				"Unsupported Operation");
	}

	/**
	 * Inefficient base implementation.
	 *
	 * @see BitDocSet#getBits
	 */
	@Override
	public OpenBitSet getBits() {
		OpenBitSet bits = new OpenBitSet();
		for (DocIterator iter = iterator(); iter.hasNext();) {
			bits.set(iter.nextDoc());
		}
		return bits;
	}

	@Override
	public DocSet intersection(DocSet other) {
		// intersection is overloaded in the smaller DocSets to be more
		// efficient, so dispatch off of it instead.
		if (!(other instanceof BitDocSet)) 
			return other.intersection(this);

		// Default... handle with bitsets.
		OpenBitSet newbits = (OpenBitSet)(this.getBits().clone());
		newbits.and(other.getBits());
		
		return new BitDocSet(newbits);
	}

	@Override
	public boolean intersects(DocSet other) {
		// intersection is overloaded in the smaller DocSets to be more
		// efficient, so dispatch off of it instead.
		if (!(other instanceof BitDocSet)) 
			return other.intersects(this);
		
		// less efficient way: get the intersection size
		return intersectionSize(other) > 0;
	}

	@Override
	public DocSet union(DocSet other) {
		OpenBitSet newbits = (OpenBitSet)(this.getBits().clone());
		newbits.or(other.getBits());
		
		return new BitDocSet(newbits);
	}

	@Override
	public int intersectionSize(DocSet other) {
		// intersection is overloaded in the smaller DocSets to be more
		// efficient, so dispatch off of it instead.
		if (!(other instanceof BitDocSet)) 
			return other.intersectionSize(this);
		
		// less efficient way: do the intersection then get it's size
		return intersection(other).size();
	}

	@Override
	public int unionSize(DocSet other) {
		return this.size() + other.size() - this.intersectionSize(other);
	}

	@Override
	public DocSet andNot(DocSet other) {
		OpenBitSet newbits = (OpenBitSet)(this.getBits().clone());
		newbits.andNot(other.getBits());
		
		return new BitDocSet(newbits);
	}

	@Override
	public int andNotSize(DocSet other) {
		return this.size() - this.intersectionSize(other);
	}

	@Override
	public Filter getTopFilter() {
		final OpenBitSet bs = getBits();

		return new Filter() {
			@Override
			public DocIdSet getDocIdSet(final IAtomicReaderRef context, Bits acceptDocs) {
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
							int pos = base-1;
							int adjustedDoc = -1;

							@Override
							public int getDocID() {
								return adjustedDoc;
							}

							@Override
							public int nextDoc() {
								pos = bs.nextSetBit(pos+1);
								return adjustedDoc = (pos>=0 && pos<max) ? pos-base : NO_MORE_DOCS;
							}

							@Override
							public int advance(int target) {
								if (target == NO_MORE_DOCS) 
									return adjustedDoc=NO_MORE_DOCS;
								
								pos = bs.nextSetBit(target+base);
								return adjustedDoc = (pos>=0 && pos<max) ? pos-base : NO_MORE_DOCS;
							}
						};
					}

					@Override
					public boolean isCacheable() {
						return true;
					}

					@Override
					public Bits getBits() {
						// sparse filters should not use random access
						return null;
					}

				}, acceptDocs2);
			}
		};
	}

	@Override
	public void setBitsOn(OpenBitSet target) {
		DocIterator iter = iterator();
		while (iter.hasNext()) {
			target.fastSet(iter.nextDoc());
		}
	}

}
