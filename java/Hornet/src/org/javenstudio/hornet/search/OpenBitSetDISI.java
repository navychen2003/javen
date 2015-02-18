package org.javenstudio.hornet.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.search.DocIdSetIterator;

/** 
 * OpenBitSet with added methods to bulk-update the bits
 *  from a {@link DocIdSetIterator}. 
 */ 
public class OpenBitSetDISI extends OpenBitSet {

	/** 
	 * Construct an OpenBitSetDISI with its bits set
	 * from the doc ids of the given DocIdSetIterator.
	 * Also give a maximum size one larger than the largest doc id for which a
	 * bit may ever be set on this OpenBitSetDISI.
	 */
	public OpenBitSetDISI(DocIdSetIterator disi, int maxSize) throws IOException {
		super(maxSize);
		inPlaceOr(disi);
	}

	/** 
	 * Construct an OpenBitSetDISI with no bits set, and a given maximum size
	 * one larger than the largest doc id for which a bit may ever be set
	 * on this OpenBitSetDISI.
	 */
	public OpenBitSetDISI(int maxSize) {
		super(maxSize);
	}

	/**
	 * Perform an inplace OR with the doc ids from a given DocIdSetIterator,
	 * setting the bit for each such doc id.
	 * These doc ids should be smaller than the maximum size passed to the
	 * constructor.
	 */
	public void inPlaceOr(DocIdSetIterator disi) throws IOException {
		int doc;
		long size = size();
		while ((doc = disi.nextDoc()) < size) {
			fastSet(doc);
		}
	}

	/**
	 * Perform an inplace AND with the doc ids from a given DocIdSetIterator,
	 * leaving only the bits set for which the doc ids are in common.
	 * These doc ids should be smaller than the maximum size passed to the
	 * constructor.
	 */
	public void inPlaceAnd(DocIdSetIterator disi) throws IOException {
		int bitSetDoc = nextSetBit(0);
		int disiDoc;
		while (bitSetDoc != -1 && (disiDoc = disi.advance(bitSetDoc)) != DocIdSetIterator.NO_MORE_DOCS) {
			clear(bitSetDoc, disiDoc);
			bitSetDoc = nextSetBit(disiDoc + 1);
		}
		if (bitSetDoc != -1) 
			clear(bitSetDoc, size());
	}

	/**
	 * Perform an inplace NOT with the doc ids from a given DocIdSetIterator,
	 * clearing all the bits for each such doc id.
	 * These doc ids should be smaller than the maximum size passed to the
	 * constructor.
	 */
	public void inPlaceNot(DocIdSetIterator disi) throws IOException {
		int doc;
		long size = size();
		while ((doc = disi.nextDoc()) < size) {
			fastClear(doc);
		}
	}

	/**
	 * Perform an inplace XOR with the doc ids from a given DocIdSetIterator,
	 * flipping all the bits for each such doc id.
	 * These doc ids should be smaller than the maximum size passed to the
	 * constructor.
	 */
	public void inPlaceXor(DocIdSetIterator disi) throws IOException {
		int doc;
		long size = size();
		while ((doc = disi.nextDoc()) < size) {
			fastFlip(doc);
		}
	}
	
}
