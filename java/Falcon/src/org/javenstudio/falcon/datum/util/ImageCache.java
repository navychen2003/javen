package org.javenstudio.falcon.datum.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.MimeTypes;
import org.javenstudio.falcon.datum.IImageData;

public class ImageCache {
	private static final Logger LOG = Logger.getLogger(ImageCache.class);

	public static interface ImageData { 
		public String getKey();
		public String getName();
		public String getExtension();
		
		public long getContentLength();
		public long getModifiedTime();
		
		public void putCache(String key, byte[] data);
		public BytesBufferPool.BytesBuffer getCache(String key);
		
		public InputStream openImage(int size) throws IOException;
	}
	
	private final ImageData mData;
	private final String mFormat;
	
	private Map<String, ImageBuffer> mCaches = null;
	
	private int mWidth = -1;
	private int mHeight = -1;
	
	public ImageCache(ImageData data) { 
		this(data, -1, -1);
	}
	
	public ImageCache(ImageData data, int width, int height) { 
		if (data == null) throw new NullPointerException();
		mData = data;
		mWidth = width;
		mHeight = height;
		
		MimeTypes.FileTypeInfo info = MimeTypes.getFileTypeByExtension(data.getExtension());
		if (info == null) 
			info = MimeTypes.getFileTypeByFilename(data.getName());
		
		String format = null;
		if (info != null) format = info.getExtensionName();
		if (format == null || format.length() == 0 || format.equalsIgnoreCase("jpeg"))
			format = "jpg";
		
		mFormat = format.toLowerCase();
	}
	
	public String getFormat() { return mFormat; }
	public ImageData getData() { return mData; }
	
	public synchronized int getWidth() { 
		try { initImage(); } catch (Throwable e) {}
		return mWidth; 
	}
	
	public synchronized int getHeight() { 
		try { initImage(); } catch (Throwable e) {}
		return mHeight; 
	}
	
	public void close() {}
	
	private synchronized void initImage() throws IOException { 
		if (mWidth != -1 && mHeight != -1) 
			return;
		
		String cacheKey = getData().getKey();
		mWidth = mHeight = 0;
		
		long contentLength = getData().getContentLength();
		long lastModified = getData().getModifiedTime();
		
		BytesBufferPool.BytesBuffer cacheBuffer = getData().getCache(cacheKey);
		if (cacheBuffer != null) { 
			ByteArrayInputStream bais = new ByteArrayInputStream(
					cacheBuffer.data, cacheBuffer.offset, cacheBuffer.length);
			DataInputStream in = new DataInputStream(bais);
			
			long contentLen = in.readLong();
			long lastModi = in.readLong();
			
			int width = in.readInt();
			int height = in.readInt();
			
			try { in.close(); }
			catch (Throwable ignore) {}
			
			if (contentLen == contentLength && lastModified == lastModi) { 
				if (LOG.isDebugEnabled()) {
					LOG.debug("initImage: loaded from cache, key=" + getData().getKey() 
							+ ", size=" + width + "x" + height);
				}
				
				mWidth = width;
				mHeight = height;
				
				return;
			}
		}
		
		InputStream input = getData().openImage(0);
		try { 
			if (input != null) {
				int[] sizes = ImageUtils.readImageSize(input);
				if (sizes != null && sizes.length >= 2) { 
					mWidth = sizes[0];
					mHeight = sizes[1];
				}
			}
		} catch (IOException e) { 
			if (LOG.isWarnEnabled()) {
				LOG.warn("initImage: key=" + cacheKey 
						+ " error: " + e, e);
			}
			//throw e;
		} finally { 
			if (input != null) input.close();
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
		
		out.writeLong(contentLength);
		out.writeLong(lastModified);
		
		out.writeInt(mWidth);
		out.writeInt(mHeight);
		
		out.flush();
		out.close();
		
		byte[] buffer = baos.toByteArray();
		getData().putCache(cacheKey, buffer);
		
	}
	
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
	
	public synchronized IImageData.Bitmap getBitmap(
			IImageData.Param param) throws IOException { 
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
		final String format = getFormat();
		initImage();
		
		//if (mWidth <= 0 || mHeight <= 0) 
		//	return new ByteArrayInputStream(new byte[0]);
		
		final int bitmapSize = sizeWidth > sizeHeight ? sizeWidth : sizeHeight;
		
		if (sizeWidth <= 0 || sizeHeight <= 0 || (mWidth > 0 && mHeight > 0 && sizeWidth >= mWidth && sizeHeight >= mHeight)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getBitmap: return bitmap, key=" + key + " width=" + mWidth 
						+ " height=" + mHeight + " size=" + sizeWidth + "x" + sizeHeight);
			}
			
			return new IImageData.Bitmap() {
					@Override
					public String getMimeType() {
						return "image/" + format;
					}
					@Override
					public int getWidth() {
						return mWidth;
					}
					@Override
					public int getHeight() {
						return mHeight;
					}
					@Override
					public InputStream openBitmap() throws IOException {
						return getData().openImage(bitmapSize);
					}
				};
		}
		
		final String cacheKey = sizeWidth == sizeHeight ? 
				(key + "_" + sizeWidth + cacheSuffix) :
				(key + "_" + sizeWidth + "x" + sizeHeight + cacheSuffix); // + "." + format;
		
		ImageBuffer buffer = getBuffer(cacheKey);
		if (buffer == null) {
			BytesBufferPool.BytesBuffer cacheBuffer = getData().getCache(cacheKey);
			if (cacheBuffer != null) { 
				buffer = ImageBuffer.decode(cacheBuffer.data, 
						cacheBuffer.offset, cacheBuffer.length);
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("getBitmap: return cache, key=" + cacheKey + " width=" + mWidth 
							+ " height=" + mHeight + " size=" + sizeWidth + "x" + sizeHeight);
				}
			} else { 
				ByteArrayOutputStream output = null; //new ByteArrayOutputStream();
				InputStream input = getData().openImage(bitmapSize);
				
				byte[] bufferData = null;
				int[] widthHeight = null;
				String errorText = null;
				
				try { 
					if (input != null) {
						output = new ByteArrayOutputStream();
						widthHeight = ImageUtils.resizeImage(input, output, format, sizeWidth, sizeHeight, fillRect, trimRect);
						bufferData = output.toByteArray();
						
					} else { 
						errorText = "Cannot open bitmap file";
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
							LOG.warn("getBitmap: createImage: " + cacheKey + " error: " + e, e);
						
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
					LOG.debug("getBitmap: init cache, key=" + cacheKey + " size=" + mWidth + "x" + mHeight 
							+ " format=" + format + " dataName=" + getData().getName() 
							+ " outputSize=" + sizeWidth + "x" + sizeHeight + " bufferSize=" + buffer.length);
				}
			}
			
			setBuffer(cacheKey, buffer);
		} else { 
			if (LOG.isDebugEnabled()) {
				LOG.debug("getBitmap: return buffer, key=" + key + " width=" + mWidth 
						+ " height=" + mHeight + " size=" + sizeWidth + "x" + sizeHeight);
			}
		}
		
		if (buffer == null) 
			buffer = new ImageBuffer(null, 0, 0, 0, 0);
		
		final ImageBuffer bitmapBuffer = buffer;
		
		return new IImageData.Bitmap() {
				@Override
				public String getMimeType() {
					return "image/" + format;
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
