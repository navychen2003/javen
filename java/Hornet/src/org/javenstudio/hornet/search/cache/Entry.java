package org.javenstudio.hornet.search.cache;

/** Expert: Every composite-key in the internal cache is of this type. */
final class Entry {
	
    final String mField;        // which Fieldable
    final Object mCustom;       // which custom comparator or parser

    /** Creates one of these objects for a custom comparator/parser. */
    Entry (String field, Object custom) {
    	mField = field;
    	mCustom = custom;
    }

    /** Two of these are equal iff they reference the same field and type. */
    @Override
    public boolean equals (Object o) {
    	if (o instanceof Entry) {
    		Entry other = (Entry) o;
    		if (other.mField.equals(mField)) {
    			if (other.mCustom == null) {
    				if (mCustom == null) return true;
    			} else if (other.mCustom.equals(mCustom)) {
    				return true;
    			}
    		}
    	}
    	return false;
	}

	/** Composes a hashcode based on the field and type. */
	@Override
	public int hashCode() {
		return mField.hashCode() ^ (mCustom==null ? 0 : mCustom.hashCode());
	}
    
}
