package org.javenstudio.hornet.codec.stored;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.index.field.FieldInfo;
import org.javenstudio.common.indexdb.index.field.FieldInfos;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.FieldInfosReader;

/**
 * Lucene 4.0 FieldInfos reader.
 */
final class StoredFieldInfosReader extends FieldInfosReader {

	private final StoredFieldInfosFormat mFormat;
	private final IDirectory mDirectory;
	private final String mSegment;
	
	public StoredFieldInfosReader(StoredFieldInfosFormat format, 
			IDirectory dir, String segment) { 
		mFormat = format;
		mDirectory = dir;
		mSegment = segment;
	}
	
	@SuppressWarnings("unused")
	@Override
	public IFieldInfos readFieldInfos() throws IOException { 
	    final String fileName = mFormat.getFieldInfosFileName(mSegment);
	    final IIndexInput input = mDirectory.openInput(mFormat.getContext(), fileName);
	    
	    try {
	    	CodecUtil.checkHeader(input, mFormat.getCodecName(), 
	    			StoredFieldInfosFormat.FORMAT_START, 
	    			StoredFieldInfosFormat.FORMAT_CURRENT);

	    	final int size = input.readVInt(); //read in the size
	    	FieldInfo infos[] = new FieldInfo[size];

	    	for (int i = 0; i < size; i++) {
	    		final String name = input.readString();
	    		final int fieldNumber = input.readVInt();
	    		final byte bits = input.readByte();
	    	  
	    		boolean isIndexed = (bits & StoredFieldInfosWriter.IS_INDEXED) != 0;
	    		boolean storeTermVector = (bits & StoredFieldInfosWriter.STORE_TERMVECTOR) != 0;
	    		boolean omitNorms = (bits & StoredFieldInfosWriter.OMIT_NORMS) != 0;
	    		boolean storePayloads = (bits & StoredFieldInfosWriter.STORE_PAYLOADS) != 0;
	        
	    		final IndexOptions indexOptions;
	    		if (!isIndexed) {
	    			indexOptions = null;
	    		} else if ((bits & StoredFieldInfosWriter.OMIT_TERM_FREQ_AND_POSITIONS) != 0) {
	    			indexOptions = IndexOptions.DOCS_ONLY;
	    		} else if ((bits & StoredFieldInfosWriter.OMIT_POSITIONS) != 0) {
	    			indexOptions = IndexOptions.DOCS_AND_FREQS;
	    		} else if ((bits & StoredFieldInfosWriter.STORE_OFFSETS_IN_POSTINGS) != 0) {
	    			indexOptions = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS;
	    		} else {
	    			indexOptions = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
	    		}

	    		// LUCENE-3027: past indices were able to write
	    		// storePayloads=true when omitTFAP is also true,
	    		// which is invalid.  We correct that, here:
	    		if (isIndexed && indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) < 0) 
	    			storePayloads = false;
	        
	    		// DV Types are packed in one byte
	    		byte val = input.readByte();
	    		//final DocValues.Type docValuesType = getDocValuesType((byte) (val & 0x0F));
	    		//final DocValues.Type normsType = getDocValuesType((byte) ((val >>> 4) & 0x0F));
	        
	    		final Map<String,String> attributes = input.readStringStringMap();
	        
	    		infos[i] = new FieldInfo(name, isIndexed, fieldNumber, storeTermVector, 
	    				omitNorms, storePayloads, indexOptions, Collections.unmodifiableMap(attributes));
	    	}

	    	if (input.getFilePointer() != input.length()) {
	    		throw new CorruptIndexException("did not read all bytes from file \"" + fileName + 
	    				"\": read " + input.getFilePointer() + " vs size " + input.length() + 
	    				" (resource: " + input + ")");
	    	}
	      
	    	return new FieldInfos(infos);
	    } finally {
	    	input.close();
	    }
	}
	
}
