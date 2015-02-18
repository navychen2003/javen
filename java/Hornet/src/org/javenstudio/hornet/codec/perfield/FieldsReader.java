package org.javenstudio.hornet.codec.perfield;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.index.field.FieldsEnum;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.hornet.codec.FieldsProducer;
import org.javenstudio.hornet.codec.PostingsFormat;
import org.javenstudio.hornet.codec.SegmentReadState;

final class FieldsReader extends FieldsProducer {

	private final Map<String,FieldsProducer> mFields = new TreeMap<String,FieldsProducer>();
	private final Map<String,FieldsProducer> mFormats = new HashMap<String,FieldsProducer>();
	
	private final PerFieldPostingsFormat mConsumer;
	private final IDirectory mDirectory;

	public FieldsReader(PerFieldPostingsFormat consumer, 
			IDirectory dir, SegmentReadState readState) throws IOException {
		mConsumer = consumer;
		mDirectory = dir;
		
		// Read _X.per and init each format:
		boolean success = false;
		try {
			// Read field name -> format name
			for (IFieldInfo fi : readState.getFieldInfos()) {
				if (fi.isIndexed()) {
					final String fieldName = fi.getName();
					final String formatName = fi.getAttribute(PerFieldPostingsFormat.PER_FIELD_FORMAT_KEY);
					
					if (formatName != null) {
						// null formatName means the field is in fieldInfos, but has no postings!
						final String suffix = fi.getAttribute(PerFieldPostingsFormat.PER_FIELD_SUFFIX_KEY);
						assert suffix != null;
						
						PostingsFormat format = mConsumer.getPostingsFormatForName(formatName); 
						String segmentSuffix = mConsumer.getSuffix(formatName, suffix);
						
						if (!mFormats.containsKey(segmentSuffix)) {
							mFormats.put(segmentSuffix, (FieldsProducer)format.getFieldsProducer(mDirectory, 
									new SegmentReadState((SegmentReadState)readState, segmentSuffix)));
						}
						
						mFields.put(fieldName, mFormats.get(segmentSuffix));
					}
				}
			}
			success = true;
		} finally {
			if (!success) 
				IOUtils.closeWhileHandlingException(mFormats.values());
		}
	}

	private final class FieldsIterator extends FieldsEnum {
		private final Iterator<String> mIt;
		private String mCurrent;

		public FieldsIterator() {
			mIt = mFields.keySet().iterator();
		}

		@Override
		public String next() {
			if (mIt.hasNext()) 
				mCurrent = mIt.next();
			else 
				mCurrent = null;

			return mCurrent;
		}

		@Override
		public ITerms getTerms() throws IOException {
			return mFields.get(mCurrent).getTerms(mCurrent);
		}
	}

	@Override
	public FieldsEnum iterator() {
		return new FieldsIterator();
	}

	@Override
	public ITerms getTerms(String field) throws IOException {
		FieldsProducer fieldsProducer = mFields.get(field);
		return fieldsProducer == null ? null : fieldsProducer.getTerms(field);
	}

	@Override
	public int size() {
		return mFields.size();
	}

	@Override
	public void close() throws IOException {
		IOUtils.close(mFormats.values());
	}
	
}
