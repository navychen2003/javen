package org.javenstudio.falcon.datum.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IImageData;
import org.javenstudio.falcon.datum.MetadataLoader;
import org.javenstudio.falcon.datum.util.BytesBufferPool;
import org.javenstudio.falcon.datum.util.ImageCache;

public abstract class SqRootImage extends SqRootFile 
		implements IImageData, ImageCache.ImageData {
	private static final Logger LOG = Logger.getLogger(SqRootImage.class);
	
	private ImageCache mCache = null;
	
	public SqRootImage(SqRoot root, 
			NameData nameData, String contentType) {
		super(root, nameData, contentType);
	}
	
	private synchronized ImageCache getImageCache() { 
		if (mCache == null) {
			mCache = new ImageCache(this, 
					getNameData().getAttrs().getWidth(), 
					getNameData().getAttrs().getHeight());
		}
		return mCache;
	}
	
	@Override
	public IImageData.Bitmap getBitmap(IImageData.Param param) throws IOException { 
		return getImageCache().getBitmap(param);
	}
	
	@Override
	public String getKey() { 
		return getContentKey();
	}
	
	@Override
	protected boolean hasScreenshotPoster(NameData data) { 
		return false;
	}
	
	@Override
	public InputStream openImage(int size) throws IOException { 
		if (size > 0) { 
			FileData data = getFileData();
			if (data != null) { 
				FileScreenshot[] shots = data.getScreenshots();
				FileScreenshot selected = null;
				
				for (int i=0; shots != null && i < shots.length; i++) { 
					FileScreenshot screen = shots[i];
					if (screen == null || screen.getSize() <= 0) continue;
					if (screen.getSize() >= size) {
						selected = screen;
						break;
					}
					if (selected == null || selected.getSize() < screen.getSize())
						selected = screen;
				}
				
				if (selected != null) { 
					InputStream stream = selected.openImage();
					if (stream != null) { 
						if (LOG.isDebugEnabled()) {
							LOG.debug("openImage: return \"" + getName() + "\" screenshot: " 
									+ selected.getName() + " (" + selected.getSize() 
									+ ", " + selected.getBufferSize() + " bytes) for size: " 
									+ size);
						}
						return stream;
					}
				}
			}
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("openImage: return \"" + getName() 
					+ "\" original for size: " + size);
		}
		
		return openImage();
	}
	
	@Override
	public InputStream openImage() throws IOException {
		return open();
	}
	
	@Override
	public void putCache(String key, byte[] data) {
		getLibrary().putImageData(key, data);
	}
	
	@Override
	public BytesBufferPool.BytesBuffer getCache(String key) { 
		return getLibrary().getImageData(key);
	}
	
	@Override
	public synchronized void close() { 
		ImageCache cache = mCache;
		if (cache != null) cache.close();
		mCache = null;
		super.close();
	}
	
	@Override
	public int getMetaTag(final Map<String,Object> tags) throws ErrorException { 
		if (tags == null) return 0;
		
		int count = super.getMetaTag(tags);
		if (count > 0) return count;
		
		count = 0;
		try {
			if (LOG.isDebugEnabled())
				LOG.debug("getMetaTag: load " + getName() + " metadata");
			
			tags.put("Width", getImageCache().getWidth());
			tags.put("Height", getImageCache().getHeight());
			
			MetadataLoader loader = getLibrary().getManager().getCore()
					.getMetadataLoader();
			
			if (loader != null) {
				MetadataLoader.InputFile file = 
					new MetadataLoader.InputFile() {
						@Override
						public File getFile() { 
							return null;
						}
						@Override
						public String getName() {
							return SqRootImage.this.getName();
						}
						@Override
						public String getExtension() {
							return SqRootImage.this.getExtension();
						}
						@Override
						public long getLength() {
							return SqRootImage.this.getContentLength();
						}
						@Override
						public InputStream openFile() throws IOException {
							return SqRootImage.this.open();
						}
					};
				
				MetadataLoader.TagCollector collector = 
					new MetadataLoader.TagCollector() {
						@Override
						protected void addTag(String tagName, Object tagValue) {
							tags.put(tagName, tagValue);
						}
					};
				
				loader.loadMetadatas(file, collector);
				
				count = collector.getTagCount();
			}
		} catch (IOException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("getMetaTag: " + getName() + " error: " + e);
			
			//throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
		
		return count;
	}
	
}
