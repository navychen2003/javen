package org.javenstudio.common.indexdb.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfos;

/** Encodes/decodes {@link IFieldInfos} */
public interface IFieldInfosFormat {

	public static interface Reader {
		public IFieldInfos readFieldInfos() 
				throws IOException;
	}

	public static interface Writer {
		public void writeFieldInfos(IFieldInfos infos) 
				throws IOException;
	}
	
	public IIndexContext getContext();
	public IIndexFormat getIndexFormat();
	
	public String getCodecName();
	public String getFieldInfosFileName(String segment);
	
	public Reader createReader(IDirectory dir, String segment) 
			throws IOException;
	
	public Writer createWriter(IDirectory dir, String segment) 
			throws IOException;
	
}
