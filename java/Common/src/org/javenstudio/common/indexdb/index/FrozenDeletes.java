package org.javenstudio.common.indexdb.index;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;

public abstract class FrozenDeletes {

	public abstract boolean any();
	public abstract boolean isSegmentPrivate();
	
	public abstract int getNumTermDeletes();
	public abstract int getBytesUsed();
	
	public abstract void setDelGen(long gen);
	public abstract long getDelGen();
	
	public abstract Iterable<ITerm> termsIterable();
	public abstract Iterable<DeletesStream.QueryAndLimit> queriesIterable();
	
	public abstract int getQueryCount();
	public abstract IQuery getQueryAt(int index);
	
}
