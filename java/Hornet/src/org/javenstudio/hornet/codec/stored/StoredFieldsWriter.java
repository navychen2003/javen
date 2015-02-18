package org.javenstudio.hornet.codec.stored;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.codec.IFieldsFormat;
import org.javenstudio.common.indexdb.store.DirectoryHelper;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.common.util.Logger;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.FieldsWriter;

final class StoredFieldsWriter extends FieldsWriter {
	private static final Logger LOG = Logger.getLogger(StoredFieldsWriter.class);

	/** 
	 * Maximum number of contiguous documents to bulk-copy
	 * when merging stored fields 
	 */
	static final int MAX_RAW_MERGE_DOCS = 4192;
	
	// NOTE: bit 0 is free here!  You can steal it!
	static final int FIELD_IS_BINARY = 1 << 1;

	// the old bit 1 << 2 was compressed, is now left out

	private static final int _NUMERIC_BIT_SHIFT = 3;
	static final int FIELD_IS_NUMERIC_MASK = 0x07 << _NUMERIC_BIT_SHIFT;

	static final int FIELD_IS_NUMERIC_INT = 1 << _NUMERIC_BIT_SHIFT;
	static final int FIELD_IS_NUMERIC_LONG = 2 << _NUMERIC_BIT_SHIFT;
	static final int FIELD_IS_NUMERIC_FLOAT = 3 << _NUMERIC_BIT_SHIFT;
	static final int FIELD_IS_NUMERIC_DOUBLE = 4 << _NUMERIC_BIT_SHIFT;

	// the next possible bits are: 1 << 6; 1 << 7
	// currently unused: static final int FIELD_IS_NUMERIC_SHORT = 5 << _NUMERIC_BIT_SHIFT;
	// currently unused: static final int FIELD_IS_NUMERIC_BYTE = 6 << _NUMERIC_BIT_SHIFT;
	
	private final StoredFieldsFormat mFormat;
	private final IDirectory mDirectory;
	private final String mSegment;
	
	private IIndexOutput mFieldsOutput;
	private IIndexOutput mIndexOutput;
	
	public StoredFieldsWriter(StoredFieldsFormat format, IDirectory dir, String segment) 
			throws IOException { 
		mFormat = format;
		mDirectory = dir;
		mSegment = segment;
		
		boolean success = false;
		try { 
			mFieldsOutput = mDirectory.createOutput(
					mFormat.getContext(), mFormat.getFieldsFileName(mSegment));
			mIndexOutput = mDirectory.createOutput(
					mFormat.getContext(), mFormat.getFieldsIndexFileName(mSegment));
			
			CodecUtil.writeHeader(mFieldsOutput, mFormat.getDataCodecName(), 
					StoredFieldsFormat.VERSION_CURRENT);
			CodecUtil.writeHeader(mIndexOutput, mFormat.getIndexCodecName(), 
					StoredFieldsFormat.VERSION_CURRENT);
			
			assert StoredFieldsFormat.HEADER_LENGTH_DAT == mFieldsOutput.getFilePointer();
			assert StoredFieldsFormat.HEADER_LENGTH_IDX == mIndexOutput.getFilePointer();
			
			success = true;
		} finally {
			if (!success) abort();
		}
	}
	
	/**
	 * Writes the contents of buffer into the fields stream
	 * and adds a new entry for this document into the index
	 * stream.  This assumes the buffer was already written
	 * in the correct fields format.
	 */
	@Override
	public void startDocument(int numStoredFields) throws IOException {
		mIndexOutput.writeLong(mFieldsOutput.getFilePointer());
		mFieldsOutput.writeVInt(numStoredFields);
	}

	@Override
	public void close() throws IOException {
		try {
			IOUtils.close(mFieldsOutput, mIndexOutput);
		} finally {
			mFieldsOutput = mIndexOutput = null;
		}
	}

	@Override
	public void abort() {
		try {
			close();
		} catch (IOException ignored) {
		}
		
		DirectoryHelper.deleteFilesIgnoringExceptions(
				mDirectory,
				mFormat.getFieldsFileName(mSegment), 
				mFormat.getFieldsIndexFileName(mSegment));
	}

	@Override
	public void writeField(IFieldInfo info, IField field) throws IOException {
	    mFieldsOutput.writeVInt(info.getNumber());
	    
	    final BytesRef bytes;
	    final String string;
	    int bits = 0;
	    
	    // TODO: maybe a field should serialize itself?
	    // this way we don't bake into indexer all these
	    // specific encodings for different fields?  and apps
	    // can customize...

	    Number number = field.getNumericValue();
	    if (number != null) {
	    	if (number instanceof Byte || number instanceof Short || number instanceof Integer) {
	    		bits |= FIELD_IS_NUMERIC_INT;
	    	} else if (number instanceof Long) {
	    		bits |= FIELD_IS_NUMERIC_LONG;
	    	} else if (number instanceof Float) {
	    		bits |= FIELD_IS_NUMERIC_FLOAT;
	    	} else if (number instanceof Double) {
	    		bits |= FIELD_IS_NUMERIC_DOUBLE;
	    	} else {
	    		throw new IllegalArgumentException("cannot store numeric type " + number.getClass());
	    	}
	    	
	    	string = null;
	    	bytes = null;
	    	
	    } else {
	    	bytes = field.getBinaryValue();
	    	if (bytes != null) {
	    		bits |= FIELD_IS_BINARY;
	    		string = null;
	    		
	    	} else {
	    		string = field.getStringValue();
	    		if (string == null) {
	    			throw new IllegalArgumentException("field " + field.getName() + 
	    					" is stored but does not have binaryValue, stringValue nor numericValue");
	    		}
	    	}
	    }

	    mFieldsOutput.writeByte((byte) bits);

	    if (bytes != null) {
	    	mFieldsOutput.writeVInt(bytes.getLength());
	    	mFieldsOutput.writeBytes(bytes.getBytes(), bytes.getOffset(), bytes.getLength());
	    	
	    } else if (string != null) {
	    	mFieldsOutput.writeString(field.getStringValue());
	    	
	    } else {
	    	if (number instanceof Byte || number instanceof Short || number instanceof Integer) {
	    		mFieldsOutput.writeInt(number.intValue());
	    	} else if (number instanceof Long) {
	    		mFieldsOutput.writeLong(number.longValue());
	    	} else if (number instanceof Float) {
	    		mFieldsOutput.writeInt(Float.floatToIntBits(number.floatValue()));
	    	} else if (number instanceof Double) {
	    		mFieldsOutput.writeLong(Double.doubleToLongBits(number.doubleValue()));
	    	} else {
	    		assert false;
	    	}
	    }
	}

	/** 
	 * Bulk write a contiguous series of documents.  The
	 *  lengths array is the length (in bytes) of each raw
	 *  document.  The stream IndexInput is the
	 *  fieldsStream from which we should bulk-copy all bytes. 
	 */
	private void addRawDocuments(IIndexInput stream, int[] lengths, int numDocs) 
			throws IOException {
		long position = mFieldsOutput.getFilePointer();
		long start = position;
		
		for (int i=0; i < numDocs; i++) {
			mIndexOutput.writeLong(position);
			position += lengths[i];
		}
		
		mFieldsOutput.copyBytes(stream, position-start);
		assert mFieldsOutput.getFilePointer() == position;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("addRawDocuments: writeSeg=" + mSegment + " input=" 
					+ stream + " numDocs=" + numDocs);
			
			//LOG.debug("addRawDocuments: filePointer=" + mFieldsOutput.getFilePointer() 
			//		+ " vs position=" + position + ", start=" + start + ", numDocs=" + numDocs);
		}
	}

	@Override
	public void finish(IFieldInfos fis, int numDocs) throws IOException {
		if (StoredFieldsFormat.HEADER_LENGTH_IDX+((long) numDocs)*8 != mIndexOutput.getFilePointer()) {
			// This is most likely a bug in Sun JRE 1.6.0_04/_05;
			// we detect that the bug has struck, here, and
			// throw an exception to prevent the corruption from
			// entering the index.  See LUCENE-1282 for
			// details.
			throw new RuntimeException("fdx size mismatch: docCount is " + numDocs + 
					" but fdx file size is " + mIndexOutput.getFilePointer() + 
					" file=" + mIndexOutput.toString() + 
					"; now aborting this merge to prevent index corruption");
		}
	}
	
	@Override
	public int merge(IMergeState mergeState) throws IOException {
	    // Used for bulk-reading raw bytes for stored fields
	    int rawDocLengths[] = new int[MAX_RAW_MERGE_DOCS];
	    int docCount = 0;
	    
	    for (int i=0; i < mergeState.getReaderCount(); i++) {
	    	IAtomicReader reader = mergeState.getReaderAt(i);
	    	ISegmentReader matchingSegmentReader = mergeState.getMatchingSegmentReaderAt(i);
	    	
	    	StoredFieldsReader matchingFieldsReader = null;
	    	if (matchingSegmentReader != null) {
	    		final IFieldsFormat.Reader fieldsReader = matchingSegmentReader.getFieldsReader();
	    		// we can only bulk-copy if the matching reader is also a StoredFieldsReader
	    		if (fieldsReader != null && fieldsReader instanceof StoredFieldsReader) 
	    			matchingFieldsReader = (StoredFieldsReader) fieldsReader;
	    	}
	    
	    	Bits liveDocs = reader.getLiveDocs();
	    	if (liveDocs != null) {
	    		docCount += copyFieldsWithDeletions(mergeState,
	    				reader, matchingFieldsReader, rawDocLengths);
	    	} else {
	    		docCount += copyFieldsNoDeletions(mergeState,
	    				reader, matchingFieldsReader, rawDocLengths);
	    	}
	    }
	    
	    finish(mergeState.getFieldInfos(), docCount);
	    
	    return docCount;
	}

	private int copyFieldsWithDeletions(IMergeState mergeState, 
			final IAtomicReader reader, StoredFieldsReader matchingFieldsReader, 
			int rawDocLengths[]) throws IOException {
		final int maxDoc = reader.getMaxDoc();
		final Bits liveDocs = reader.getLiveDocs();
		assert liveDocs != null;
		
		int docCount = 0;
		
		if (matchingFieldsReader != null) {
			// We can bulk-copy because the fieldInfos are "congruent"
			for (int j = 0; j < maxDoc;) {
				if (!liveDocs.get(j)) {
					// skip deleted docs
					++j;
					continue;
				}
				
				// We can optimize this case (doing a bulk byte copy) since the field
				// numbers are identical
				int start = j, numDocs = 0;
				do {
					j++;
					numDocs++;
					if (j >= maxDoc) break;
					if (!liveDocs.get(j)) {
						j++;
						break;
					}
				} while(numDocs < MAX_RAW_MERGE_DOCS);

				IIndexInput stream = matchingFieldsReader.rawDocs(rawDocLengths, start, numDocs);
				addRawDocuments(stream, rawDocLengths, numDocs);
				docCount += numDocs;
				mergeState.checkAbort(300 * numDocs);
			}
			
		} else {
			for (int j = 0; j < maxDoc; j++) {
				if (!liveDocs.get(j)) {
					// skip deleted docs
					continue;
				}
				
				// TODO: this could be more efficient using
				// FieldVisitor instead of loading/writing entire
				// doc; ie we just have to renumber the field number
				// on the fly?
				// NOTE: it's very important to first assign to doc then pass it to
				// fieldsWriter.addDocument; see LUCENE-1282
				IDocument doc = reader.getDocument(j);
				addDocument(doc, mergeState.getFieldInfos());
				docCount++;
				mergeState.checkAbort(300);
			}
		}
		
		return docCount;
	}

	private int copyFieldsNoDeletions(IMergeState mergeState, 
			final IAtomicReader reader, final StoredFieldsReader matchingFieldsReader, 
			int rawDocLengths[]) throws IOException {
		final int maxDoc = reader.getMaxDoc();
		int docCount = 0;
		
		if (matchingFieldsReader != null) {
			// We can bulk-copy because the fieldInfos are "congruent"
			while (docCount < maxDoc) {
				int numDocs = Math.min(MAX_RAW_MERGE_DOCS, maxDoc - docCount);
				IIndexInput stream = matchingFieldsReader.rawDocs(rawDocLengths, docCount, numDocs);
				addRawDocuments(stream, rawDocLengths, numDocs);
				docCount += numDocs;
				mergeState.checkAbort(300 * numDocs);
			}
			
		} else {
			for (; docCount < maxDoc; docCount++) {
				// NOTE: it's very important to first assign to doc then pass it to
				// fieldsWriter.addDocument; see LUCENE-1282
				IDocument doc = reader.getDocument(docCount);
				addDocument(doc, mergeState.getFieldInfos());
				mergeState.checkAbort(300);
			}
		}
		
		return docCount;
	}
	
}
