package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.codec.ITermVectorsFormat;

public abstract class TermVectorsFormat implements ITermVectorsFormat {

	/** Extension of vectors fields file */
	public static final String VECTORS_FIELDS_EXTENSION = "tvf";

	/** Extension of vectors documents file */
	public static final String VECTORS_DOCUMENTS_EXTENSION = "tvd";

	/** Extension of vectors index file */
	public static final String VECTORS_INDEX_EXTENSION = "tvx";
	
	private final IIndexFormat mFormat;
	
	protected TermVectorsFormat(IIndexFormat format) { 
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
	public String getVectorsIndexFileName(String segment) { 
		return getContext().getSegmentFileName(segment, VECTORS_INDEX_EXTENSION);
	}
	
	@Override
	public String getVectorsDocumentsFileName(String segment) { 
		return getContext().getSegmentFileName(segment, VECTORS_DOCUMENTS_EXTENSION);
	}
	
	@Override
	public String getVectorsFieldsFileName(String segment) { 
		return getContext().getSegmentFileName(segment, VECTORS_FIELDS_EXTENSION);
	}
	
	public abstract String getFieldsCodecName();
	public abstract String getDocumentsCodecName();
	public abstract String getIndexCodecName();
	
	public abstract ITermVectorsFormat.Reader createReader(
			IDirectory dir, ISegmentInfo si, IFieldInfos fieldInfos) throws IOException;
	
	public abstract ITermVectorsFormat.Writer createWriter(
			IDirectory dir, String segment) throws IOException;
	
}
