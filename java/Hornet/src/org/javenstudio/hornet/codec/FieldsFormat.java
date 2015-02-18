package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.codec.IFieldsFormat;
import org.javenstudio.common.indexdb.codec.IIndexFormat;

/**
 * Controls the format of stored fields
 */
public abstract class FieldsFormat implements IFieldsFormat {

	/** Extension of stored fields file */
	public static final String FIELDS_EXTENSION = "fdt";
	  
	/** Extension of stored fields index file */
	public static final String FIELDS_INDEX_EXTENSION = "fdx";
	
	private final IIndexFormat mFormat;
	
	protected FieldsFormat(IIndexFormat format) { 
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
	public String getFieldsFileName(String segment) { 
		return getContext().getSegmentFileName(segment, FIELDS_EXTENSION);
	}
	
	@Override
	public String getFieldsIndexFileName(String segment) { 
		return getContext().getSegmentFileName(segment, FIELDS_INDEX_EXTENSION);
	}
	
	public abstract String getIndexCodecName();
	public abstract String getDataCodecName();
	
	/** Returns a {@link StoredFieldsReader} to load stored fields. */
	public abstract IFieldsFormat.Reader createReader(IDirectory dir, String segment, 
			ISegmentInfo si, IFieldInfos fn) throws IOException;
	
    /** Returns a {@link StoredFieldsWriter} to write stored fields. */
	public abstract IFieldsFormat.Writer createWriter(IDirectory dir, String segment) 
			throws IOException;
	
}
