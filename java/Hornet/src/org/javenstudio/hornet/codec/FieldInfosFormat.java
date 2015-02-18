package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.codec.IFieldInfosFormat;
import org.javenstudio.common.indexdb.codec.IIndexFormat;

/**
 * Encodes/decodes {@link FieldInfos}
 * 
 */
public abstract class FieldInfosFormat implements IFieldInfosFormat {

	/** Extension of field infos */
	public static final String FIELD_INFOS_EXTENSION = "fnm";
	
	private final IIndexFormat mFormat;
	
	protected FieldInfosFormat(IIndexFormat format) { 
		mFormat = format;
	}
	
	@Override
	public final IIndexContext getContext() { 
		return mFormat.getContext(); 
	}
	
	@Override
	public final IIndexFormat getIndexFormat() { 
		return mFormat; 
	}
	
	@Override
	public String getFieldInfosFileName(String segment) { 
		return getContext().getSegmentFileName(segment, FIELD_INFOS_EXTENSION);
	}
	
	public abstract String getCodecName();
	
	/** 
	 * Returns a {@link FieldInfosReader} to read field infos
	 *  from the index 
	 */
	public abstract IFieldInfosFormat.Reader createReader(
			IDirectory dir, String segment) throws IOException;

	/** 
	 * Returns a {@link FieldInfosWriter} to write field infos
	 *  to the index 
	 */
	public abstract IFieldInfosFormat.Writer createWriter(
			IDirectory dir, String segment) throws IOException;
	
}
