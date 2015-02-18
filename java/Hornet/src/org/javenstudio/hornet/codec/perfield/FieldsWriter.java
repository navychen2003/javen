package org.javenstudio.hornet.codec.perfield;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.codec.ITermsConsumer;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.hornet.codec.FieldsConsumer;
import org.javenstudio.hornet.codec.PostingsFormat;
import org.javenstudio.hornet.codec.SegmentWriteState;

final class FieldsWriter extends FieldsConsumer {
	
	private final Map<PostingsFormat,FieldsConsumerAndSuffix> mFormats = 
			new HashMap<PostingsFormat,FieldsConsumerAndSuffix>();
	
	private final Map<String,Integer> mSuffixes = new HashMap<String,Integer>();
	private final SegmentWriteState mSegmentWriteState;
	private final PerFieldPostingsFormat mConsumer;
	private final IDirectory mDirectory;

	public FieldsWriter(PerFieldPostingsFormat consumer, 
			IDirectory dir, SegmentWriteState state) {
		mConsumer = consumer;
		mSegmentWriteState = state;
		mDirectory = dir;
	}

	@Override
	public ITermsConsumer addField(IFieldInfo field) throws IOException {
		final PostingsFormat format = mConsumer.getPostingsFormatForField(field.getName());
		if (format == null) {
			throw new IllegalStateException("invalid null PostingsFormat for " + 
					"field=\"" + field.getName() + "\"");
		}
		
		final String formatName = format.getName();
		String previousValue = field.putAttribute(PerFieldPostingsFormat.PER_FIELD_FORMAT_KEY, formatName);
		assert previousValue == null;
  
		FieldsConsumerAndSuffix consumer = mFormats.get(format);
		Integer suffix;
		
		if (consumer == null) {
			// First time we are seeing this format; create a new instance
			// bump the suffix
			suffix = mSuffixes.get(formatName);
			if (suffix == null) 
				suffix = 0;
			else 
				suffix = suffix + 1;
			
			mSuffixes.put(formatName, suffix);
    
			final String segmentSuffix = mConsumer.getFullSegmentSuffix(field.getName(),
					mSegmentWriteState.getSegmentSuffix(),
					mConsumer.getSuffix(formatName, Integer.toString(suffix)));
			
			consumer = new FieldsConsumerAndSuffix();
			consumer.mConsumer = (FieldsConsumer)format.getFieldsConsumer(mDirectory, 
					new SegmentWriteState((SegmentWriteState)mSegmentWriteState, segmentSuffix));
			consumer.mSuffix = suffix;
			
			mFormats.put(format, consumer);
			
		} else {
			// we've already seen this format, so just grab its suffix
			assert mSuffixes.containsKey(formatName);
			suffix = consumer.mSuffix;
		}
  
		previousValue = field.putAttribute(PerFieldPostingsFormat.PER_FIELD_SUFFIX_KEY, 
				Integer.toString(suffix));
		assert previousValue == null;

		// TODO: we should only provide the "slice" of FIS
		// that this PF actually sees ... then stuff like
		// .hasProx could work correctly?
		// NOTE: .hasProx is already broken in the same way for the non-perfield case,
		// if there is a fieldinfo with prox that has no postings, you get a 0 byte file.
		return consumer.mConsumer.addField(field);
	}

	@Override
	public void close() throws IOException {
		// Close all subs
		IOUtils.close(mFormats.values());
	}
	
}
