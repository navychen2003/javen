package org.javenstudio.hornet.query.source;

import org.javenstudio.hornet.query.ValueSource;

/**
 * <code>ConstNumberSource</code> is the base class for all constant numbers
 */
public abstract class ConstNumberSource extends ValueSource {
	
	public abstract int getInt();
	public abstract long getLong();
	public abstract float getFloat();
	public abstract double getDouble();  
	public abstract Number getNumber();  
	public abstract boolean getBool();
	
}
