package org.javenstudio.falcon.datum;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.raptor.util.StringUtils;

public abstract class MetadataLoader {
	//private static final Logger LOG = Logger.getLogger(MetadataLoader.class);

	public static interface InputFile { 
		public String getName();
		public String getExtension();
		public long getLength();
		public File getFile();
		public InputStream openFile() throws IOException;
	}
	
	public static interface Collector { 
		public void addMetadata(Metadata metadata);
		public void addAttachment(String name, String mimeType, byte[] data);
	}
	
	public static abstract class TagCollector implements Collector { 
		private int mCount = 0;
		
		public TagCollector() {}
		
		public int getTagCount() { return mCount; }
		
		@Override
		public final void addMetadata(Metadata metadata) { 
			if (metadata == null) return;
			
			for (int j=0; j < metadata.getTagCount(); j++) { 
				Metadata.Tag tag = metadata.getTagAt(j);
				if (tag == null) continue;
				
				String name = StringUtils.trim(tag.getName());
				Object value = tag.getValue();
				
				if (name != null && name.length() > 0) { 
					String tagName = StringUtils.trim(metadata.getName() + " " + name);
					
					if (value instanceof CharSequence) 
						value = StringUtils.trim(value.toString());
					
					if (tagName != null && value != null) {
						addTag(tagName, value);
						mCount ++;
					}
				}
			}
		}
		
		@Override
		public void addAttachment(String name, String mimeType, byte[] data) { 
		}
		
		protected abstract void addTag(String tagName, Object tagValue);
	}
	
	public MetadataLoader() {}
	
	public abstract void loadMetadatas(InputFile file, 
			Collector collector) throws IOException;
	
}
