package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface IFieldsBuilder {

	public void startDocument();
	public void finishDocument() throws IOException;
	public void flush() throws IOException;
	public void abort();
	
	public void addField(IFieldInfo fieldInfo, IField field) throws IOException;
	
}
