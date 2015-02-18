package org.javenstudio.lightning.core.datum;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.drew.imaging.ImageMetadataReader;

import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.audio.AudioParser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.parser.mp4.MP4Parser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.video.FLVParser;
import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.MimeType;
import org.javenstudio.common.util.MimeTypes;
import org.javenstudio.falcon.datum.Metadata;
import org.javenstudio.falcon.datum.MetadataLoader;
import org.javenstudio.falcon.util.IOUtils;
import org.javenstudio.lightning.util.SimpleExtractor;
import org.javenstudio.raptor.util.StringUtils;

public class DatumMetadataLoader extends MetadataLoader {
	private static final Logger LOG = Logger.getLogger(DatumMetadataLoader.class);

	public DatumMetadataLoader() {}
	
	@Override
	public void loadMetadatas(InputFile file, Collector collector) throws IOException { 
		if (file == null || collector == null) return;
		
		MimeType mimeType = MimeTypes.getExtensionType(file.getExtension());
		if (mimeType != null) { 
			final String type = mimeType.getType();
			final String ext = file.getExtension();
			
			if (type.startsWith("image/")) { 
				loadImageMetadatas(file, collector);
				
			} else if (type.startsWith("audio/")) { 
				if (loadAudioMetadatas(file, collector))
					return;
				
				if (type.endsWith("/mp3") || "mp3".equalsIgnoreCase(ext)) { 
					loadMetadatas(file, new Mp3Parser(), collector);
				} else { 
					loadMetadatas(file, new AudioParser(), collector);
				}
				
			} else if (type.startsWith("video/")) { 
				if (type.endsWith("/flv") || "flv".equalsIgnoreCase(ext)) { 
					loadMetadatas(file, new FLVParser(), collector);
				} else { 
					loadMetadatas(file, new MP4Parser(), collector);
				}
				
			} else if ("xls".equalsIgnoreCase(ext) || "doc".equalsIgnoreCase(ext) || "ppt".equalsIgnoreCase(ext)) { 
				loadMetadatas(file, new OfficeParser(), collector);
				
			} else if ("pdf".equalsIgnoreCase(ext)) { 
				loadMetadatas(file, new PDFParser(), collector);
			}
		}
	}
	
	static void loadMetadatas(InputFile file, Parser parser, 
			Collector collector) throws IOException { 
		if (file == null || parser == null || collector == null) 
			return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadMetadatas: file=" + file.getName() + " parser=" + parser);
		
		//ArrayList<Metadata> list = new ArrayList<Metadata>();
		InputStream stream = null;
		
		try {
			stream = file.openFile();
			
			ContentHandler handler = new DefaultHandler();
			org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();
        	ParseContext parseCtx = new ParseContext();
        	parser.parse(stream, handler, metadata, parseCtx);
        	
        	String[] metadataNames = metadata.names();
        	if (metadataNames != null) { 
        		MetadataImpl meta = new MetadataImpl();
        		
        		for (String name : metadataNames) { 
        			String value = metadata.get(name);
        			meta.addTag(StringUtils.trim(name), value);
        		}
        		
        		collector.addMetadata(meta);
        	}
        	
		} catch (Throwable e) {
			if (e instanceof IOException) 
				throw (IOException)e;
			else
				throw new IOException(e.toString(), e);
			
		} finally { 
			IOUtils.closeQuietly(stream);
		}
		
		//return list.toArray(new Metadata[list.size()]);
	}
	
	static boolean loadAudioMetadatas(InputFile file, final Collector collector) 
			throws IOException { 
		if (file == null || collector == null) 
			return false;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadAudioMetadatas: file=" + file.getFile());
		
		//final ArrayList<Metadata> list = new ArrayList<Metadata>();
		
		try {
			return SimpleExtractor.loadTags(file.getFile(), file.getExtension(), 
				new SimpleExtractor.Collector() {
					@Override
					public void addTag(String name, String value) {
						name = StringUtils.trim(name);
						value = StringUtils.trim(value);
						
						if (name == null || name.length() == 0)
							return;
						
						MetadataImpl meta = new MetadataImpl();
						meta.addTag(name, value);
						
						collector.addMetadata(meta);
					}
					
					@Override
					public void addPic(String mimeType, byte[] data) {
						collector.addAttachment("cover", mimeType, data);
					}
				});
			
			//if (result == false)
			//	return null;
		} catch (Throwable e) {
			if (e instanceof IOException) 
				throw (IOException)e;
			else
				throw new IOException(e.toString(), e);
			
		}
		
		//return list.toArray(new Metadata[list.size()]);
	}
	
	static void loadImageMetadatas(InputFile file, Collector collector) 
			throws IOException { 
		if (file == null || collector == null) 
			return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadImageMetadatas: file=" + file.getName());
		
		//ArrayList<Metadata> list = new ArrayList<Metadata>();
		InputStream stream = null;
		
		try {
			stream = file.openFile();
			
			com.drew.metadata.Metadata metadata = 
					ImageMetadataReader.readMetadata(
							new BufferedInputStream(stream), false);
			
			if (metadata != null) { 
				for (com.drew.metadata.Directory directory : metadata.getDirectories()) { 
					MetadataImpl meta = new MetadataImpl(StringUtils.trim(directory.getName()));
					
					for (com.drew.metadata.Tag tag : directory.getTags()) { 
						meta.addTag(StringUtils.trim(tag.getTagName()), tag.getDescription());
					}
					
					collector.addMetadata(meta);
				}
			}
			
		} catch (Throwable e) {
			if (e instanceof IOException) 
				throw (IOException)e;
			else
				throw new IOException(e.toString(), e);
			
		} finally { 
			IOUtils.closeQuietly(stream);
		}
		
		//return list.toArray(new Metadata[list.size()]);
	}
	
	static class MetadataImpl implements Metadata {
		private final String mName;
		private final List<TagImpl> mTags;
		
		public MetadataImpl() { 
			this(null);
		}
		public MetadataImpl(String name) { 
			mName = name == null ? "" : name;
			mTags = new ArrayList<TagImpl>();
		}
		
		@Override
		public String getName() {
			return mName;
		}

		@Override
		public synchronized int getTagCount() {
			return mTags.size();
		}

		@Override
		public synchronized TagImpl getTagAt(int index) {
			return index >= 0 && index < mTags.size() ? mTags.get(index) : null;
		}
		
		public synchronized void addTag(String name, String value) { 
			if (name != null && name.length() > 0 && value != null)
				mTags.add(new TagImpl(name, value));
		}
	}
	
	static class TagImpl implements Metadata.Tag {
		private final String mName;
		private final String mValue;
		
		public TagImpl(String name, String value) { 
			mName = name;
			mValue = value;
		}
		
		@Override
		public String getName() {
			return mName;
		}

		@Override
		public String getValue() {
			return mValue;
		}
	}
	
}
