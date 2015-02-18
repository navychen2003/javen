package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.codec.IFieldsConsumer;
import org.javenstudio.common.indexdb.codec.IFieldsProducer;
import org.javenstudio.common.indexdb.codec.IPostingsFormat;
import org.javenstudio.common.indexdb.codec.ISegmentReadState;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;

/** 
 * Encodes/decodes terms, postings, and proximity data.
 */
public abstract class PostingsFormat implements IPostingsFormat {
	
	public abstract String getName();
	
	/** Writes a new segment */
	public abstract IFieldsConsumer getFieldsConsumer(IDirectory dir, 
			ISegmentWriteState state) throws IOException;
	
	/** 
	 * Reads a segment.  NOTE: by the time this call
	 *  returns, it must hold open any files it will need to
	 *  use; else, those files may be deleted. 
	 */
	public abstract IFieldsProducer getFieldsProducer(IDirectory dir, 
			ISegmentReadState state) throws IOException;
	
}
