package org.javenstudio.common.indexdb;

public interface IIntsWriter extends IIntsMutable {

	public abstract IIntsWriter resize(int newSize);
	public abstract IIntsMutable getMutable();
	
}
