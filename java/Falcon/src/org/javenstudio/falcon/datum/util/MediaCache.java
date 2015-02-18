package org.javenstudio.falcon.datum.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.datum.IImageData;
import org.javenstudio.falcon.datum.IImageSource;
import org.javenstudio.falcon.datum.IScreenshot;

public class MediaCache {
	private static final Logger LOG = Logger.getLogger(MediaCache.class);

	public static interface MediaData { 
		public String getKey();
		public IScreenshot[] getScreenshots() throws IOException;
		
		public void putCache(String key, byte[] data);
		public BytesBufferPool.BytesBuffer getCache(String key);
	}
	
	private final MediaData mData;
	private Map<String, ImageBuffer> mCaches = null;
	
	public MediaCache(MediaData data) { 
		if (data == null) throw new NullPointerException();
		mData = data;
	}
	
	public MediaData getData() { return mData; }
	public void close() {}
	
	private synchronized ImageBuffer getBuffer(String key) { 
		if (mCaches != null) 
			return mCaches.get(key);
		else
			return null;
	}
	
	private synchronized void setBuffer(String key, ImageBuffer buffer) { 
		if (mCaches == null)
			mCaches = new HashMap<String, ImageBuffer>();
			
		mCaches.put(key, buffer);
	}
	
	public synchronized IImageSource.Bitmap getBitmap(
			IImageSource.Param param) throws IOException { 
		String cacheSuffix = "";
		int sizeWidth = 192, sizeHeight = 192;
		boolean fillRect = false;
		boolean trimRect = false;
		
		if (param != null) { 
			sizeWidth = param.getParamWidth();
			sizeHeight = param.getParamHeight();
			if (param.getParamTrim()) { 
				fillRect = true;
				trimRect = true;
				cacheSuffix = "t";
			}
		}
		
		sizeWidth = ImageUtils.normalizeSize(sizeWidth);
		sizeHeight = ImageUtils.normalizeSize(sizeHeight);
		
		final String key = getData().getKey();
		final String cacheKey = sizeWidth == sizeHeight ? 
				(key + "_s_" + sizeWidth + cacheSuffix) :
				(key + "_s_" + sizeWidth + "x" + sizeHeight + cacheSuffix); // + "." + format;
		
		String format = "jpg";
		ImageBuffer buffer = getBuffer(cacheKey);
		
		if (buffer == null) {
			BytesBufferPool.BytesBuffer cacheBuffer = getData().getCache(cacheKey);
			if (cacheBuffer != null) { 
				buffer = ImageBuffer.decode(cacheBuffer.data, 
						cacheBuffer.offset, cacheBuffer.length);
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("getBitmap: return cache, key=" + cacheKey 
							+ " size=" + sizeWidth + "x" + sizeHeight);
				}
			} else { 
				ByteArrayOutputStream output = null; //new ByteArrayOutputStream();
				InputStream input = null; //getData().openBitmap(size);
				
				byte[] bufferData = null;
				int[] widthHeight = null;
				String errorText = null;
				
				try { 
					final IScreenshot[] shots = getData().getScreenshots();
					final IScreenshot shot = (shots != null && shots.length > 0) ? shots[0] : null;
					if (shot != null) { 
						String mimeType = shot.getImageType();
						if (mimeType != null && mimeType.startsWith("image/")) { 
							format = mimeType.substring(6);
							if (format == null || format.equalsIgnoreCase("jpeg"))
								format = "jpg";
						}
						
						if (trimRect == false && (sizeWidth == 0 || sizeHeight == 0 || (shot.getImageWidth() <= sizeWidth && shot.getImageHeight() <= sizeHeight))) { 
							byte[] data = shot.getImageBuffer();
							if (data != null && data.length > 0) { 
								bufferData = new byte[data.length];
								System.arraycopy(data, 0, bufferData, 0, data.length);
								widthHeight = new int[]{shot.getImageWidth(),shot.getImageHeight()};
							}
						} else {
							input = shot.openImage();
						}
					}
					
					if (bufferData == null) {
						if (input != null) {
							output = new ByteArrayOutputStream();
							widthHeight = ImageUtils.resizeImage(input, output, format, sizeWidth, sizeHeight, fillRect, trimRect);
							bufferData = output.toByteArray();
							
						} else { 
							errorText = "No screenshot found";
						}
					}
				} catch (IOException e) { 
					if (LOG.isWarnEnabled())
						LOG.warn("getBitmap: resizeImage: " + cacheKey + " error: " + e, e);
					
					if (e instanceof javax.imageio.IIOException)
						errorText = e.getMessage();
					
					//throw e;
				} finally { 
					if (input != null) input.close();
				}
				
				if (bufferData == null || bufferData.length == 0) {
					String title = "Image Error";
					
					if (LOG.isDebugEnabled())
						LOG.debug("getBitmap: create error image: " + errorText);
					
					try { 
						output = new ByteArrayOutputStream();
						widthHeight = ImageUtils.createImage(output, format, sizeWidth, sizeHeight, title, errorText);
						bufferData = output.toByteArray();
						
					} catch (IOException e) { 
						if (LOG.isWarnEnabled())
							LOG.warn("openImage: createImage: " + cacheKey + " error: " + e, e);
						
						//throw e;
					}
				}
				
				if (bufferData == null)
					bufferData = new byte[0];
				
				if (widthHeight == null)
					widthHeight = new int[]{sizeWidth, sizeHeight};
				
				buffer = new ImageBuffer(bufferData, 0, bufferData.length, 
						widthHeight[0], widthHeight[1]);
				
				if (bufferData.length > 0) 
					getData().putCache(cacheKey, ImageBuffer.encode(buffer));
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("openImage: init cache, key=" + cacheKey + " format=" + format 
							+ " outputSize=" + sizeWidth + "x" + sizeHeight 
							+ " bufferSize=" + buffer.length);
				}
			}
			
			setBuffer(cacheKey, buffer);
		} else { 
			if (LOG.isDebugEnabled()) {
				LOG.debug("openImage: return buffer, key=" + key 
						+ " size=" + sizeWidth + "x" + sizeHeight);
			}
		}
		
		if (buffer == null) 
			buffer = new ImageBuffer(null, 0, 0, 0, 0);
		
		final String bitmapFormat = format;
		final ImageBuffer bitmapBuffer = buffer;
		
		return new IImageData.Bitmap() {
				@Override
				public String getMimeType() {
					return "image/" + bitmapFormat;
				}
				@Override
				public int getWidth() {
					return bitmapBuffer.width;
				}
				@Override
				public int getHeight() {
					return bitmapBuffer.height;
				}
				@Override
				public InputStream openBitmap() throws IOException {
					return new ByteArrayInputStream(bitmapBuffer.data, 
							bitmapBuffer.offset, bitmapBuffer.length);
				}
			};
	}
	
}
