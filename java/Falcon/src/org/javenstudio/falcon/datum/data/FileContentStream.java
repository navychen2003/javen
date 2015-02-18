package org.javenstudio.falcon.datum.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.MetadataLoader;
import org.javenstudio.falcon.util.ContentStream;

public class FileContentStream implements FileStream {
	private static final Logger LOG = Logger.getLogger(FileContentStream.class);

	static class Attachment { 
		private final String mName;
		private final String mMimeType;
		private final byte[] mData;
		
		public Attachment(String name, String mimeType, byte[] data) { 
			mName = name;
			mMimeType = mimeType;
			mData = data;
		}
	}
	
	private final ContentStream mStream;
	private Map<String,Object> mMetadata = null;
	private List<Attachment> mAttachment = null;
	
	public FileContentStream(ContentStream stream) { 
		if (stream == null) throw new NullPointerException();
		mStream = stream;
	}
	
	@Override
	public String getName() {
		return mStream.getName();
	}

	@Override
	public String getContentType() {
		return mStream.getContentType();
	}

	@Override
	public long getSize() {
		Long size = mStream.getSize();
		return size != null ? size : -1;
	}

	@Override
	public InputStream getStream() throws IOException {
		return mStream.getStream();
	}

	@Override
	public File getFile() { 
		return mStream.getFile();
	}
	
	@Override
	public void close() {
		mStream.close();
	}

	@Override
	public synchronized boolean loadMetadata(FileMetaCollector collector)
			throws IOException, ErrorException { 
		boolean result = false;
		
		if (mMetadata != null) {
			for (Map.Entry<String, Object> entry : mMetadata.entrySet()) { 
				String name = entry.getKey();
				Object val = entry.getValue();
				
				if (name != null && val != null) 
					collector.addMetaTag(name, val);
			}
			
			result = true;
		}
		
		if (mAttachment != null) { 
			for (Attachment att : mAttachment) { 
				if (att == null) continue;
				
				collector.addScreenshot(att.mName, att.mMimeType, att.mData);
			}
			
			result = true;
		}
		
		return result;
	}
	
	private synchronized void addMetaTag(String name, Object value) { 
		if (name == null || value == null) return;
		
		if (mMetadata == null) 
			mMetadata = new TreeMap<String,Object>();
		
		mMetadata.put(name, value);
	}
	
	private synchronized void addAttachment(String name, String mimeType, byte[] data) { 
		if (mimeType == null || data == null) return;
		
		if (mAttachment == null)
			mAttachment = new ArrayList<Attachment>();
		
		mAttachment.add(new Attachment(name, mimeType, data));
	}
	
	public static FileContentStream[] createStreams(
			Iterable<ContentStream> streams, MetadataLoader loader) 
			throws ErrorException { 
		if (streams == null) return null;
		
		ArrayList<FileContentStream> list = new ArrayList<FileContentStream>();
		
		Iterator<ContentStream> it = streams.iterator();
		while (it.hasNext()) { 
			final ContentStream stream = it.next();
			if (stream == null) continue;
			
			FileContentStream fstream = new FileContentStream(stream);
			loadMetadata(stream, loader, fstream);
			
			list.add(fstream);
		}
		
		return list.toArray(new FileContentStream[list.size()]);
	}
	
	private static void loadMetadata(final ContentStream stream, 
			final MetadataLoader loader, final FileContentStream fstream) 
			throws ErrorException { 
		if (stream == null || loader == null || fstream == null) 
			return;
		
		try { 
			MetadataLoader.InputFile file = 
				new MetadataLoader.InputFile() {
					@Override
					public InputStream openFile() throws IOException {
						return stream.getStream();
					}
					@Override
					public File getFile() { 
						return stream.getFile();
					}
					@Override
					public String getName() {
						return stream.getName();
					}
					@Override
					public long getLength() {
						Long size = stream.getSize();
						return size != null ? size : -1;
					}
					@Override
					public String getExtension() {
						return toExtension(getName());
					}
				};
			
			MetadataLoader.TagCollector collector = 
				new MetadataLoader.TagCollector() {
					@Override
					protected void addTag(String tagName, Object tagValue) {
						fstream.addMetaTag(tagName, tagValue);
					}
					@Override
					public void addAttachment(String name, String mimeType, byte[] data) { 
						fstream.addAttachment(name, mimeType, data);
					}
				};
			
			loader.loadMetadatas(file, collector);
		} catch (IOException e) {
			if (LOG.isWarnEnabled())
				LOG.warn("loadMetadata: " + stream.getName() + " error: " + e);
			
		}
	}
	
	private static String toExtension(String filename) { 
		String extname = "";
		if (filename != null) { 
			int pos = filename.lastIndexOf('.'); 
			if (pos > 0) { 
				extname = filename.substring(pos+1); 
				//name = filename.substring(0, pos);
			}
		}
		
		if (extname == null) extname = "";
		extname = extname.toLowerCase();
		
		return extname;
	}
	
}
