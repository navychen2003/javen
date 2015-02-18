package org.javenstudio.lightning.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.MetadataLoader;
import org.javenstudio.falcon.datum.data.FileMetaCollector;
import org.javenstudio.falcon.datum.data.FileStream;

public class SimpleFileStream implements FileStream {
	private static final Logger LOG = Logger.getLogger(SimpleFileStream.class);

	static final SimpleDateFormat sFormater  = new SimpleDateFormat("yyyyMMddHHmmss"); 
	
	private final byte[] mContent;
	private final long mContentLength;
	private final String mContentType;
	private final String mName;
	private Map<String,Object> mMetadata = null;
	
	public SimpleFileStream(byte[] buffer, String contentType, 
			String filename) {
		if (buffer == null) throw new NullPointerException();
		
		if (contentType != null && contentType.length() > 0) {
			StringTokenizer st = new StringTokenizer(contentType, " \t\r\n,;");
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token != null && token.length() > 0) {
					contentType = token;
					break;
				}
			}
		}
		if (contentType == null || contentType.length() == 0)
			contentType = "application/*";
		
		if (filename == null || filename.length() == 0) {
			String name = null;
			
			int pos = contentType.indexOf('/');
			if (pos >= 0) name = contentType.substring(pos + 1);
			
			if (name != null && name.length() > 0) {
				name = name.toLowerCase();
				pos = name.lastIndexOf('-');
				if (pos >= 0) name = name.substring(pos + 1);
			}
			
			if (name ==  null || name.length() == 0) 
				name = "dat";
			
			if (name.equalsIgnoreCase("jpeg"))
				name = "jpg";
			
			filename = sFormater.format(new Date()) + "." + name;
		}
		
		mContent = buffer;
		mContentLength = buffer.length;
		mContentType = contentType;
		mName = filename;
	}
	
	@Override
	public String getName() {
		return mName;
	}

	@Override
	public String getContentType() {
		return mContentType;
	}

	@Override
	public long getSize() {
		return mContentLength;
	}

	@Override
	public InputStream getStream() throws IOException {
		return new ByteArrayInputStream(mContent);
	}

	@Override
	public void close() {
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
		
		return result;
	}

	@Override
	public File getFile() {
		return null;
	}

	public synchronized void addMetaTag(String name, Object value) { 
		if (name == null || value == null) return;
		
		if (mMetadata == null) 
			mMetadata = new TreeMap<String,Object>();
		
		mMetadata.put(name, value);
	}
	
	public static SimpleFileStream create(IHttpResult entity, 
			MetadataLoader loader) throws ErrorException {
		if (entity == null) throw new NullPointerException();
		
		byte[] buffer = entity.getContentAsBinary();
		if (buffer == null) buffer = new byte[0];
		
		SimpleFileStream fstream = new SimpleFileStream(buffer, 
				entity.getContentType(), null);
		
		loadMetadata(loader, fstream);
		
		return fstream;
	}
	
	private static void loadMetadata(final MetadataLoader loader, 
			final SimpleFileStream fstream) throws ErrorException { 
		if (loader == null || fstream == null) 
			return;
		
		try { 
			MetadataLoader.InputFile file = 
				new MetadataLoader.InputFile() {
					@Override
					public InputStream openFile() throws IOException {
						return fstream.getStream();
					}
					@Override
					public File getFile() { 
						return fstream.getFile();
					}
					@Override
					public String getName() {
						return fstream.getName();
					}
					@Override
					public long getLength() {
						return fstream.getSize();
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
				};
			
			loader.loadMetadatas(file, collector);
		} catch (IOException e) {
			if (LOG.isWarnEnabled())
				LOG.warn("loadMetadata: " + fstream.getName() + " error: " + e);
			
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
