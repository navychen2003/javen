package org.javenstudio.falcon.datum.util;

import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IImageSource;
import org.javenstudio.falcon.datum.data.FileScreenshot;
import org.javenstudio.falcon.util.ContentStreamBase;

public class ImageStream extends ContentStreamBase implements IImageSource.Param {
	private static final Logger LOG = Logger.getLogger(ImageStream.class);
	
	private static final int SIZE = FileScreenshot.SIZE_HD;
	
	private final IImageSource mData;
	private final IImageSource.Bitmap mBitmap;
	private final int mWidth, mHeight;
	private final boolean mTrim;
	
	public ImageStream(IImageSource data, String type, 
			String param, boolean share) throws ErrorException { 
		if (data == null) throw new NullPointerException();
		mData = data;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("create: data=" + data + " type=" + type 
					+ " param=" + param + " share=" + share);
		}
		
		int width = SIZE, height = SIZE;
		boolean trim = false;
		
		if (param != null && param.length() > 0) { 
			param = param.toLowerCase();
			if (param.endsWith("t")) {
				param = param.substring(0, param.length() -1);
				trim = true;
			}
			
			try { 
				String paramW = param;
				String paramH = param;
				
				int pos = param.indexOf('x');
				if (pos >= 0) {
					paramW = param.substring(0, pos);
					paramH = param.substring(pos+1);
				}
				
				width = Integer.parseInt(paramW);
				height = Integer.parseInt(paramH);
			} catch (Throwable e) { 
			}
		}
		
		if ((width < 0 || height < 0) || (share && (width == 0 || height == 0))) 
			width = height = SIZE;
		
		mWidth = width;
		mHeight = height;
		mTrim = trim;
		
		try {
			mBitmap = mData.getBitmap(this);
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	@Override
    public String getContentType() {
		String mimeType =  mBitmap != null ? mBitmap.getMimeType() : null;
		if (mimeType == null || mimeType.length() == 0) 
			mimeType = "image/jpeg";
		return mimeType;
	}
	
	@Override
    public InputStream getStream() throws IOException {
		return mBitmap != null ? mBitmap.openBitmap() : null;
	}
	
	@Override
	public int getParamWidth() { 
		return mWidth;
	}
	
	@Override
	public int getParamHeight() { 
		return mHeight;
	}
	
	@Override
	public boolean getParamTrim() { 
		return mTrim;
	}
	
}
