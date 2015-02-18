package org.javenstudio.common.entitydb.type;

import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IIdentityGenerator;

public final class LongIdentity implements IIdentity {

	public static class Generator implements IIdentityGenerator<LongIdentity> { 
		private final AtomicInteger mIdGenerator = new AtomicInteger(1);
		
		@Override
		public LongIdentity newIdentity(Object value) { 
			if (value != null) { 
				if (value instanceof Number) 
					return new LongIdentity(((Number)value).longValue());
				try { 
					long num = Long.valueOf(value.toString()).longValue();
					return new LongIdentity(num);
				} catch (Exception e) { 
					return new LongIdentity(0);
				}
			}
			return new LongIdentity(mIdGenerator.getAndIncrement());
		}
	}
	
	private final long mValue; 
	
	public LongIdentity() {
		this(0); 
	}
	
	public LongIdentity(long val) {
		mValue = val; 
	}
	
	public long longValue() { 
		return mValue; 
	}
	
	@Override
	public IIdentity clone() {
		return new LongIdentity(mValue); 
	}
	
	@Override
    public boolean equals(Object o) {
        return (o instanceof LongIdentity) && ((LongIdentity) o).mValue == mValue;
    }
	
	@Override
	public int compareTo(Object object) {
		return compareTo((LongIdentity)object); 
	}
	
	public int compareTo(LongIdentity object) {
		if (object == null) return 1; 
		long thisValue = this.mValue;
        long thatValue = object.mValue;
        return thisValue < thatValue ? -1 : (thisValue == thatValue ? 0 : 1);
	}
	
	@Override
	public String toSQL() {
		return toString(); 
	}
	
	@Override
	public String toString() {
		return Long.toString(mValue);
	}
	
}
