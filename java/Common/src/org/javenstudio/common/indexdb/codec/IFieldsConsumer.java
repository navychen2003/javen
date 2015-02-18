package org.javenstudio.common.indexdb.codec;

import java.io.Closeable;
import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.ITermState;

public interface IFieldsConsumer extends Closeable {

	/** Add a new field */
	public ITermsConsumer addField(IFieldInfo field) throws IOException;
  
	/** Called when we are done adding everything. */
	public void close() throws IOException;

	public ITermState merge(IMergeState mergeState, IFields fields) throws IOException;
	
}
