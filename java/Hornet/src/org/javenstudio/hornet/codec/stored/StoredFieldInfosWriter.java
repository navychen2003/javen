package org.javenstudio.hornet.codec.stored;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.FieldInfosWriter;

/**
 * Lucene 4.0 FieldInfos writer.
 */
final class StoredFieldInfosWriter extends FieldInfosWriter {
  
	static final byte IS_INDEXED = 0x1;
	static final byte STORE_TERMVECTOR = 0x2;
	static final byte STORE_OFFSETS_IN_POSTINGS = 0x4;
	static final byte OMIT_NORMS = 0x10;
	static final byte STORE_PAYLOADS = 0x20;
	static final byte OMIT_TERM_FREQ_AND_POSITIONS = 0x40;
	static final byte OMIT_POSITIONS = -128;
  
	private final StoredFieldInfosFormat mFormat;
	private final IDirectory mDirectory;
	private final String mSegment;
	
	public StoredFieldInfosWriter(StoredFieldInfosFormat format, 
			IDirectory dir, String segment) { 
		mFormat = format;
		mDirectory = dir;
		mSegment = segment;
	}
	
	@Override
	public void writeFieldInfos(IFieldInfos infos) throws IOException {
		final String fileName = mFormat.getFieldInfosFileName(mSegment);
		IIndexOutput output = mDirectory.createOutput(mFormat.getContext(), fileName);
		
		try {
			CodecUtil.writeHeader(output, mFormat.getCodecName(), 
					StoredFieldInfosFormat.FORMAT_CURRENT);
			output.writeVInt(infos.size());
			
			for (IFieldInfo fi : infos) {
				IndexOptions indexOptions = fi.getIndexOptions();
				byte bits = 0x0;
				
				if (fi.hasVectors()) bits |= STORE_TERMVECTOR;
				if (fi.isOmitsNorms()) bits |= OMIT_NORMS;
				if (fi.hasPayloads()) bits |= STORE_PAYLOADS;
				if (fi.isIndexed()) {
					bits |= IS_INDEXED;
					assert indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0 || !fi.hasPayloads();
					if (indexOptions == IndexOptions.DOCS_ONLY) {
						bits |= OMIT_TERM_FREQ_AND_POSITIONS;
					} else if (indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) {
						bits |= STORE_OFFSETS_IN_POSTINGS;
					} else if (indexOptions == IndexOptions.DOCS_AND_FREQS) {
						bits |= OMIT_POSITIONS;
					}
				}
				
				output.writeString(fi.getName());
				output.writeVInt(fi.getNumber());
				output.writeByte(bits);

				// pack the DV types in one byte
				final byte dv = 0; //docValuesByte(fi.getDocValuesType());
				final byte nrm = 0; //docValuesByte(fi.getNormType());
				//assert (dv & (~0xF)) == 0 && (nrm & (~0x0F)) == 0;
				
				byte val = (byte) (0xff & ((nrm << 4) | dv));
				output.writeByte(val);
				
				output.writeStringStringMap(fi.getAttributes());
			}
		} finally {
			output.close();
		}
  	}

}
