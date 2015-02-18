package org.javenstudio.hornet.codec.stored;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.codec.ISegmentInfosFormat;
import org.javenstudio.hornet.codec.FieldInfosFormat;
import org.javenstudio.hornet.codec.FieldsFormat;
import org.javenstudio.hornet.codec.IndexFormat;
import org.javenstudio.hornet.codec.LiveDocsFormat;
import org.javenstudio.hornet.codec.PostingsFormat;
import org.javenstudio.hornet.codec.SegmentInfoFormat;
import org.javenstudio.hornet.codec.TermVectorsFormat;
import org.javenstudio.hornet.codec.perfield.PerFieldPostingsFormat;
import org.javenstudio.hornet.codec.postings.StoredPostingsFormat;
import org.javenstudio.hornet.codec.vectors.StoredTermVectorsFormat;

public class StoredIndexFormat extends IndexFormat {

	public StoredIndexFormat(IIndexContext context) { 
		super(context);
	}
	
	@Override
	protected ISegmentInfosFormat createSegmentInfosFormat() { 
		return new StoredSegmentInfosFormat(this);
	}
	
	@Override
	protected SegmentInfoFormat createSegmentInfoFormat() { 
		return new StoredSegmentInfoFormat(this);
	}
	
	@Override
	protected LiveDocsFormat createLiveDocsFormat() { 
		return new StoredLiveDocsFormat(this);
	}
	
	@Override
	protected FieldInfosFormat createFieldInfosFormat() { 
		return new StoredFieldInfosFormat(this);
	}
	
	@Override
	protected FieldsFormat createFieldsFormat() { 
		return new StoredFieldsFormat(this);
	}
	
	@Override
	protected PostingsFormat createPostingsFormat() { 
		final IIndexFormat format = this;
	
		return new PerFieldPostingsFormat(format) { 
				private StoredPostingsFormat mPostingsFormat = null;
				
				private synchronized StoredPostingsFormat getPostingsFormat() { 
					if (mPostingsFormat == null) {
						mPostingsFormat = new StoredPostingsFormat(format);
					}
					return mPostingsFormat;
				}
				
				@Override
				public String getName() { 
					return getPostingsFormat().getTermPostingsFormat().getFormatName(); 
				}
				
				@Override
				public PostingsFormat getPostingsFormatForName(String formatName) { 
					return getPostingsFormat();
				}
				
				@Override
				public PostingsFormat getPostingsFormatForField(String field) {
					return getPostingsFormat();
				}
			};
	}
	
	@Override
	protected TermVectorsFormat createTermVectorsFormat() { 
		return new StoredTermVectorsFormat(this);
	}
	
}
