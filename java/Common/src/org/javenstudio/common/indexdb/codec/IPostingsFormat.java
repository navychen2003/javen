package org.javenstudio.common.indexdb.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;

/** Encodes/decodes terms, postings, and proximity data. */
public interface IPostingsFormat {

	public String getName();
	
	/** Writes a new segment */
	public IFieldsConsumer getFieldsConsumer(IDirectory dir, ISegmentWriteState state) 
			throws IOException;
	
	/** 
	 * Reads a segment.  NOTE: by the time this call
	 *  returns, it must hold open any files it will need to
	 *  use; else, those files may be deleted. 
	 */
	public IFieldsProducer getFieldsProducer(IDirectory dir, ISegmentReadState state) 
			throws IOException;
	
}
