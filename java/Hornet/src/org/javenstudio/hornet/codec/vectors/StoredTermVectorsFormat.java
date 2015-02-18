package org.javenstudio.hornet.codec.vectors;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.TermVectorsFormat;
import org.javenstudio.hornet.codec.TermVectorsReader;
import org.javenstudio.hornet.codec.TermVectorsWriter;

public class StoredTermVectorsFormat extends TermVectorsFormat {

	private static final String CODEC_NAME_FIELDS = "Lucene40TermVectorsFields";
	private static final String CODEC_NAME_DOCS = "Lucene40TermVectorsDocs";
	private static final String CODEC_NAME_INDEX = "Lucene40TermVectorsIndex";
	
	static final byte STORE_POSITIONS_WITH_TERMVECTOR = 0x1;
	static final byte STORE_OFFSET_WITH_TERMVECTOR = 0x2;
	static final byte STORE_PAYLOAD_WITH_TERMVECTOR = 0x4;

	static final int VERSION_NO_PAYLOADS = 0;
	static final int VERSION_PAYLOADS = 1;
	static final int VERSION_START = VERSION_NO_PAYLOADS;
	static final int VERSION_CURRENT = VERSION_PAYLOADS;
  
	static final long HEADER_LENGTH_FIELDS = CodecUtil.headerLength(CODEC_NAME_FIELDS);
	static final long HEADER_LENGTH_DOCS = CodecUtil.headerLength(CODEC_NAME_DOCS);
	static final long HEADER_LENGTH_INDEX = CodecUtil.headerLength(CODEC_NAME_INDEX);
	
  
	public StoredTermVectorsFormat(IIndexFormat format) { 
		super(format);
	}
	
	public String getFieldsCodecName() { return CODEC_NAME_FIELDS; }
	public String getDocumentsCodecName() { return CODEC_NAME_DOCS; }
	public String getIndexCodecName() { return CODEC_NAME_INDEX; }
	
	@Override
	public TermVectorsReader createReader(IDirectory dir, ISegmentInfo si, 
			IFieldInfos fieldInfos) throws IOException { 
		return new StoredTermVectorsReader(this, dir, si, fieldInfos);
	}
	
	@Override
	public TermVectorsWriter createWriter(IDirectory dir, String segment) 
			throws IOException { 
		return new StoredTermVectorsWriter(this, dir, segment);
	}
  
}
