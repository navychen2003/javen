package org.javenstudio.common.indexdb.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IIndexOutput;

public interface ITermsFormat {

	public static interface Writer extends IFieldsConsumer {
		public IIndexOutput createTermsOutput() throws IOException;
		public IIndexOutput createTermsIndexOutput() throws IOException;
	}
	
	public IIndexContext getContext();
	public IIndexFormat getIndexFormat();
	
	public String getTermsFileName(String segment);
	public String getTermsIndexFileName(String segment);
	
	//public ITermsFormat.Writer createWriter(IDirectory dir, ISegmentWriteState state) 
	//		throws IOException;
	
}
