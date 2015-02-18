package org.javenstudio.hornet.codec.stored;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.FieldsFormat;
import org.javenstudio.hornet.codec.FieldsReader;
import org.javenstudio.hornet.codec.FieldsWriter;

final class StoredFieldsFormat extends FieldsFormat {

	private static final String CODEC_NAME_IDX = "Lucene40StoredFieldsIndex";
	private static final String CODEC_NAME_DAT = "Lucene40StoredFieldsData";
	
	static final int VERSION_START = 0;
	static final int VERSION_CURRENT = VERSION_START;
	
	static final long HEADER_LENGTH_IDX = CodecUtil.headerLength(CODEC_NAME_IDX);
	static final long HEADER_LENGTH_DAT = CodecUtil.headerLength(CODEC_NAME_DAT);
	
	public StoredFieldsFormat(IIndexFormat format) { 
		super(format);
	}
	
	public String getIndexCodecName() { return CODEC_NAME_IDX; }
	public String getDataCodecName() { return CODEC_NAME_DAT; }
	
	@Override
	public FieldsReader createReader(IDirectory dir, String segment, 
			ISegmentInfo si, IFieldInfos fn) throws IOException { 
		return new StoredFieldsReader(this, dir, segment, si, fn);
	}
	
	@Override
	public FieldsWriter createWriter(IDirectory dir, String segment) 
			throws IOException { 
		return new StoredFieldsWriter(this, dir, segment);
	}
	
}
