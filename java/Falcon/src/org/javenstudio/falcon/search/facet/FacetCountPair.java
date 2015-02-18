package org.javenstudio.falcon.search.facet;

/**
 * A simple key=>val pair whose natural order is such that 
 * <b>higher</b> vals come before lower vals.
 * In case of tie vals, then <b>lower</b> keys come before higher keys.
 */
public class FacetCountPair<K extends Comparable<? super K>, V extends Comparable<? super V>>
		implements Comparable<FacetCountPair<K,V>> {

	private final K mKey;
	private final V mVal;
	
	public FacetCountPair(K k, V v) {
		mKey = k; mVal = v;
	}

	public final K getKey() { return mKey; }
	public final V getValue() { return mVal; }
	
	@Override
	public int hashCode() {
		return mKey.hashCode() ^ mVal.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || !(o instanceof FacetCountPair)) 
			return false;
		
		FacetCountPair<?,?> that = (FacetCountPair<?,?>) o;
		return (this.mKey.equals(that.mKey) && this.mVal.equals(that.mVal));
	}
	
	@Override
	public int compareTo(FacetCountPair<K,V> o) {
		int vc = o.mVal.compareTo(mVal);
		return (0 != vc ? vc : mKey.compareTo(o.mKey));
	}
	
}
