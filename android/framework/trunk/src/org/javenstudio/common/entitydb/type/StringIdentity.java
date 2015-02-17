package org.javenstudio.common.entitydb.type;

import org.javenstudio.common.entitydb.IIdentity;

public final class StringIdentity implements IIdentity {

	private final String mValue;
	
	public StringIdentity() { 
		this(null);
	}
	
	public StringIdentity(String value) { 
		mValue = value != null ? value : "";
	}
	
	public String stringValue() { 
		return mValue; 
	}
	
	@Override
	public IIdentity clone() {
		return new StringIdentity(mValue);
	}
	
	@Override
    public boolean equals(Object o) {
        return (o instanceof StringIdentity) && ((StringIdentity) o).mValue.equals(mValue);
    }
	
	@Override
	public int compareTo(Object object) {
		return compareTo((StringIdentity)object); 
	}
	
	public int compareTo(StringIdentity object) {
		if (object == null) return 1; 
        return this.mValue.compareTo(object.mValue);
	}
	
	@Override
	public String toSQL() {
		return "'" + mValue + "'"; 
	}
	
	@Override
	public String toString() {
		return mValue;
	}
	
}
