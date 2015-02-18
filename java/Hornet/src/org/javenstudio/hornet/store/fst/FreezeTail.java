package org.javenstudio.hornet.store.fst;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.IntsRef;

/** 
 * Expert: this is invoked by Builder whenever a suffix
 *  is serialized. 
 */
public abstract class FreezeTail<T> {

	public abstract void freeze(final UnCompiledNode<T>[] frontier, 
			int prefixLenPlus1, IntsRef prevInput) throws IOException;
	
}
