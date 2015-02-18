package org.javenstudio.hornet.codec.perfield;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.codec.ISegmentReadState;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.hornet.codec.FieldsConsumer;
import org.javenstudio.hornet.codec.FieldsProducer;
import org.javenstudio.hornet.codec.PostingsFormat;
import org.javenstudio.hornet.codec.SegmentReadState;
import org.javenstudio.hornet.codec.SegmentWriteState;

/**
 * Enables per field format support.
 * <p>
 * Note, when extending this class, the name ({@link #getName}) is 
 * written into the index. In order for the field to be read, the
 * name must resolve to your implementation via {@link #forName(String)}.
 * This method uses Java's 
 * {@link ServiceLoader Service Provider Interface} to resolve format names.
 * <p>
 * Files written by each posting format have an additional suffix containing the 
 * format name. For example, in a per-field configuration instead of <tt>_1.prx</tt> 
 * filenames would look like <tt>_1_XXXX_0.prx</tt>.
 * @see ServiceLoader
 * 
 */
public abstract class PerFieldPostingsFormat extends PostingsFormat {
	//private static final String PER_FIELD_NAME = "PerField40";
  
	public static final String PER_FIELD_FORMAT_KEY = 
			PerFieldPostingsFormat.class.getSimpleName() + ".format";
	public static final String PER_FIELD_SUFFIX_KEY = 
			PerFieldPostingsFormat.class.getSimpleName() + ".suffix";

	private final IIndexFormat mFormat;
  
	public PerFieldPostingsFormat(IIndexFormat format) {
		mFormat = format;
	}

	public final IIndexContext getContext() { return mFormat.getContext(); }
	public final IIndexFormat getIndexFormat() { return mFormat; }
  
	@Override
	public FieldsConsumer getFieldsConsumer(IDirectory dir, ISegmentWriteState state)
			throws IOException {
		return new FieldsWriter(this, dir, (SegmentWriteState)state);
	}
  
	@Override
	public FieldsProducer getFieldsProducer(IDirectory dir, ISegmentReadState state)
			throws IOException {
		return new FieldsReader(this, dir, (SegmentReadState)state);
	}

	// NOTE: only called during writing; for reading we read
	// all we need from the index (ie we save the field ->
	// format mapping)
	public abstract PostingsFormat getPostingsFormatForField(String field);
	public abstract PostingsFormat getPostingsFormatForName(String formatName);
  
	final String getSuffix(String formatName, String suffix) {
		return formatName + "_" + suffix;
	}

	final String getFullSegmentSuffix(String fieldName, 
			String outerSegmentSuffix, String segmentSuffix) {
		if (outerSegmentSuffix.length() == 0) {
			return segmentSuffix;
			
		} else {
			// TODO: support embedding; I think it should work but
			// we need a test confirm to confirm
			// return outerSegmentSuffix + "_" + segmentSuffix;
			throw new IllegalStateException("cannot embed PerFieldPostingsFormat inside itself " + 
					"(field \"" + fieldName + "\" returned PerFieldPostingsFormat)");
		}
	}

}
