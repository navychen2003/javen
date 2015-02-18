package org.javenstudio.hornet.codec.stored;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.hornet.codec.TermsFormat;
import org.javenstudio.hornet.codec.TermsWriter;

public final class StoredTermsFormat extends TermsFormat {

	public StoredTermsFormat(IIndexFormat format) { 
		super(format);
	}
	
	@Override
	public TermsWriter createWriter(IDirectory dir, ISegmentWriteState state) 
			throws IOException { 
		return null;
	}
	
}
