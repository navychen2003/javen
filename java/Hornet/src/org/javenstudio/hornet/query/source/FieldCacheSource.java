package org.javenstudio.hornet.query.source;

import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.search.cache.FieldCache;

/**
 * A base class for ValueSource implementations that retrieve values for
 * a single field from the {@link org.apache.lucene.search.FieldCache}.
 *
 */
public abstract class FieldCacheSource extends ValueSource {
	
	protected final FieldCache mCache = FieldCache.DEFAULT;
	protected final String mField;

	public FieldCacheSource(String field) {
		mField = field;
	}

	public FieldCache getFieldCache() { return mCache; }
	public String getField() { return mField; }

	@Override
	public String getDescription() {
		return mField;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FieldCacheSource)) 
			return false;
		
		FieldCacheSource other = (FieldCacheSource)o;
		
		return this.mField.equals(other.mField) && 
				this.mCache == other.mCache;
	}

	@Override
	public int hashCode() {
		return mCache.hashCode() + mField.hashCode();
	}

}
