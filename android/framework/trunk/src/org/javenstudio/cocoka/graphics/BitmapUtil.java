package org.javenstudio.cocoka.graphics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.FloatMath;

import org.javenstudio.cocoka.util.ApiHelper;
import org.javenstudio.cocoka.util.BitmapHelper;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapPool;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.cocoka.worker.job.JobCancelListener;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.common.util.Logger;

public class BitmapUtil {
	private static final Logger LOG = Logger.getLogger(BitmapUtil.class);
	//private static final boolean DEBUG = BaseDrawable.DEBUG;

	public static final int HDPI_THUMB_ICON_WIDTH = 120; 
	public static final int HDPI_THUMB_ICON_HEIGHT = 120; 
	public static final int HDPI_THUMB_STROCKWIDTH = 2; 
	public static final float HDPI_THUMB_ICON_SCALE = 0.96f; 
	public static final float HDPI_PREVIEW_BITMAP_SCALE = 0.85f; 
	
	public static final int HDPI_SMALL_PREVIEW_WIDTH = 128; 
	public static final int HDPI_SMALL_PREVIEW_HEIGHT = 128; 
	
	public static final int THUMB_BACKGROUND_ALPHA = 128; 
	public static final int THUMB_BAACKGROUND = Color.BLACK; //Color.WHITE; 

	
	// Rotates the bitmap by the specified degree.
    // If a new bitmap is created, the original bitmap is recycled.
    public static BitmapRef rotate(BitmapHolder holder, BitmapRef b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees,
                    (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            try {
            	BitmapRef b2 = BitmapRef.createBitmap(holder,
                        b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b2 != null && b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            	Utilities.handleOOM(ex);
            }
        }
        
        return b;
    }
	
    public static BitmapRef rotateBitmap(BitmapHolder holder, BitmapRef source, 
    		int rotation, boolean recycle) {
        if (rotation == 0) return source;
        int w = source.getWidth();
        int h = source.getHeight();
        Matrix m = new Matrix();
        m.postRotate(rotation);
        BitmapRef bitmap = BitmapRef.createBitmap(holder, source, 0, 0, w, h, m, true);
        if (recycle) source.recycle();
        return bitmap;
    }
    
	public static BitmapRef createBitmap(BitmapHolder holder, byte[] data) {
    	if (holder == null || data == null || data.length == 0) 
    		return null; 
    	
    	try {
    		BitmapRef bitmap = BitmapRef.decodeByteArray(holder, data, 0, data.length); 
	    	if (bitmap != null) {
	    		bitmap.setDensity(Utilities.getDensityDpi(holder.getContext())); 
	    		//BitmapRefs.onBitmapCreated(bitmap);
	    	}
	    	
	    	//Bitmap newbitmap = createScaledBitmap(bitmap, context, 120, 150);
	    	//if (newbitmap != bitmap && bitmap != null) 
	    	//	bitmap.recycle(); 
	    	
	    	return bitmap; 
    	} catch (OutOfMemoryError ex) {
    		Utilities.handleOOM(ex);
    		
    		return null; 
    	}
    }
	
	public static BitmapRef createBitmap(BitmapHolder holder, InputStream is) {
    	if (holder == null || is == null) 
    		return null; 
    	
    	try {
    		long length = is.available(); 
    		float scale = (float)length / (512 * 1024); 
    		
    		BitmapFactory.Options opt = new BitmapFactory.Options(); 
    		opt.inPreferredConfig = Bitmap.Config.RGB_565; 
    		opt.inPurgeable = true; 
    		opt.inInputShareable = true; 
    		
    		if (scale > 1.5f) 
    			opt.inSampleSize = (int)(scale + 0.5f); 

    		BitmapRef bitmap = BitmapRef.decodeStream(holder, is, null, opt); 
	    	if (bitmap != null) {
	    		bitmap.setDensity(Utilities.getDensityDpi(holder.getContext())); 
	    		//BitmapRefs.onBitmapCreated(bitmap);
	    	}
	    	
	    	return bitmap; 
    	} catch (IOException ex) {
    		if (LOG.isErrorEnabled())
        		LOG.error("createBitmap error: " + ex.toString(), ex);
    		
    		return null; 
    		
    	} catch (OutOfMemoryError ex) {
    		Utilities.handleOOM(ex);
    		
    		return null; 
    	}
    }
	
	public static BitmapRef createBitmap(BitmapHolder holder, Drawable d) {
		return createBitmap(holder, d, 0, 0); 
	}
	
	public static BitmapRef createBitmap(BitmapHolder holder, 
			Drawable d, int width, int height) {
    	if (holder == null || d == null) 
    		return null; 
    	
    	try {
    		int bitmapWidth = width <= 0 ? d.getIntrinsicWidth() : width; 
    		int bitmapHeight = height <= 0 ? d.getIntrinsicHeight() : height; 
    		
    		final BitmapRef output = BitmapRef.createBitmap(holder,
    				bitmapWidth, bitmapHeight);
    		if (output == null) 
    			return null;
    		
	    	final Canvas canvas = new Canvas(output.get());
    		
	    	final Rect savedRect = d.getBounds(); 
	    	d.setBounds(0, 0, bitmapWidth, bitmapHeight); 
	    	d.draw(canvas); 
	    	d.setBounds(savedRect); 
	    	
	    	//BitmapRefs.onBitmapCreated(output);
	    	
    		return output; 
    	} catch (OutOfMemoryError ex) {
    		Utilities.handleOOM(ex);
    		
    		return null; 
    	}
	}
	
	public static BitmapRef createThumbBitmap(BitmapHolder holder, BitmapRef bitmap) {
		int color = Color.TRANSPARENT; //THUMB_BAACKGROUND; 
		//color = Color.argb(THUMB_BACKGROUND_ALPHA, Color.red(color), Color.green(color), Color.blue(color)); 
		
		return createThumbBitmap(holder, bitmap, color); 
	}
	
	public static BitmapRef createThumbBitmap(BitmapHolder holder, 
			BitmapRef bitmap, int color) {
		if (holder == null || bitmap == null) 
			return null; 
		
		final int thumbWidth = Utilities.getDisplaySize(
				holder.getContext(), HDPI_THUMB_ICON_WIDTH); 
		final int thumbHeight = Utilities.getDisplaySize(
				holder.getContext(), HDPI_THUMB_ICON_HEIGHT); 
		
		final int maxBitmapWidth = (int)(thumbWidth * HDPI_THUMB_ICON_SCALE); 
		final int maxBitmapHeight = (int)(thumbHeight * HDPI_THUMB_ICON_SCALE); 
		
		BitmapRef scaledBitmap = createRatioScaledBitmap(holder, 
				bitmap, maxBitmapWidth, maxBitmapHeight); 
		if (scaledBitmap == null) 
			return bitmap; 
		
		final int bitmapWidth = scaledBitmap.getWidth(); 
		final int bitmapHeight = scaledBitmap.getHeight(); 
		
		if (thumbWidth <= bitmapWidth || thumbHeight <= bitmapHeight)
			return scaledBitmap; 
		
		BitmapRef thumb = scaledBitmap; 
		
		try {
			final int strockWidth = HDPI_THUMB_STROCKWIDTH; 
			
			final float bitmapLeft = (float)(thumbWidth - bitmapWidth) / 2.0f; 
			final float bitmapTop  = (float)(thumbHeight - bitmapHeight) / 2.0f; 
			
			final float left = bitmapLeft - strockWidth; 
			final float right = bitmapLeft + bitmapWidth + strockWidth; 
			final float top = bitmapTop - strockWidth; 
			final float bottom = bitmapTop + bitmapHeight + strockWidth; 
			
			BitmapRef thumbTmp = BitmapRef.createBitmap(holder,
					thumbWidth, thumbHeight, Bitmap.Config.ARGB_4444); 
			if (thumbTmp == null) 
				return thumb;
			
			Canvas canvas = new Canvas(thumbTmp.get());
			Paint paint = new Paint(); 
			canvas.drawColor(color);
			
			paint.setColor(Color.DKGRAY); 
			paint.setStyle(Paint.Style.FILL); 
			//canvas.drawRoundRect(new RectF(left, top, right, bottom), 2, 2, paint); 
			canvas.drawRect(left, top, right, bottom, paint); 
			
			canvas.drawBitmap(scaledBitmap.get(), bitmapLeft, bitmapTop, paint); 
			
			thumb = thumbTmp; 
			thumb.setDensity(bitmap.getDensity()); 
			//BitmapRefs.onBitmapCreated(thumb);
			
			if (scaledBitmap != bitmap && scaledBitmap != null)
				scaledBitmap.recycle(); 
			
		} catch (OutOfMemoryError ex) {
			Utilities.handleOOM(ex);
		}
		
		return thumb; 
	}
	
	public static BitmapRef createSmallBitmap(BitmapHolder holder, BitmapRef bitmap) {
		if (holder == null || bitmap == null) 
			return null; 
		
		final int thumbWidth = Utilities.getDisplaySize(
				holder.getContext(), HDPI_SMALL_PREVIEW_WIDTH); 
		final int thumbHeight = Utilities.getDisplaySize(
				holder.getContext(), HDPI_SMALL_PREVIEW_HEIGHT); 
		
		final int maxBitmapWidth = (int)(thumbWidth); 
		final int maxBitmapHeight = (int)(thumbHeight); 
		
		BitmapRef scaledBitmap = createRatioScaledBitmap(holder, 
				bitmap, maxBitmapWidth, maxBitmapHeight); 
		if (scaledBitmap == null) 
			return bitmap; 
		
		return scaledBitmap; 
	}
	
	public static BitmapRef createPreviewBitmap(BitmapHolder holder, byte[] data) {
		BitmapRef bitmap = createBitmap(holder, data); 
		return createPreviewBitmap(holder, bitmap); 
	}
	
	public static BitmapRef createPreviewBitmap(BitmapHolder holder, InputStream is) {
		BitmapRef bitmap = createBitmap(holder, is); 
		return createPreviewBitmap(holder, bitmap); 
	}
	
	public static BitmapRef createPreviewBitmap(BitmapHolder holder, BitmapRef bitmap) {
		final int screenWidth = Utilities.getScreenWidth(holder.getContext()); 
		final int screenHeight = Utilities.getScreenHeight(holder.getContext()); 
		
		final int maxScaleWidth = (int)((float)screenWidth * HDPI_PREVIEW_BITMAP_SCALE); 
		final int maxScaleHeight = (int)((float)screenHeight * HDPI_PREVIEW_BITMAP_SCALE); 
		
		final int maxWidth = maxScaleWidth < maxScaleHeight ? maxScaleWidth : maxScaleHeight; 
		final int maxHeight = maxWidth; 
		
		return createPreviewBitmap(holder, bitmap, maxWidth, maxHeight);
	}
	
	public static BitmapRef createPreviewBitmap(BitmapHolder holder, 
			BitmapRef bitmap, int maxWidth, int maxHeight) {
		if (holder == null || bitmap == null) 
			return bitmap; 
		
		//Bitmap savedBitmap = bitmap; 
		
		bitmap = createRatioScaledBitmap(holder, bitmap, maxWidth, maxHeight); 
		
		//if (savedBitmap != bitmap && savedBitmap != null) 
        //	savedBitmap.recycle(); 
		
		return bitmap; 
	}
	
	public static BitmapRef createRatioScaledBitmap(BitmapHolder holder, 
			BitmapRef bitmap, int maxWidth, int maxHeight) {
		if (holder == null || bitmap == null) 
			return bitmap; 
		
		//Bitmap savedBitmap = bitmap; 
		
		int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        
        int width = -1, height = -1; 
        
        if (bitmapWidth > maxWidth || bitmapHeight > maxHeight) {
        	float bitmapRate = (float)bitmapWidth / (float)bitmapHeight; 
        	
        	int width1 = maxWidth; 
        	int height1 = (int)(width1 / bitmapRate); 
        		
        	int height2 = maxHeight; 
        	int width2 = (int)(height2 * bitmapRate); 
        	
        	if (width1 <= maxWidth && height1 <= maxHeight) {
        		width = width1; 
        		height = height1; 
        	} else {
        		width = width2; 
        		height = height2; 
        	}
        }
        
        if (width > 0 && height > 0) 
        	bitmap = createScaledBitmap(holder, bitmap, width, height); 
        
		return bitmap; 
	}

	public static BitmapRef createScaledBitmap(BitmapHolder holder, 
			BitmapRef bitmap, int width, int height) {
    	return createScaledBitmap(holder, bitmap, width, height, 0, false); 
    }
	
	public static BitmapRef createScaledBitmap(BitmapHolder holder, 
			BitmapRef bitmap, int width, int height, int density) {
    	return createScaledBitmap(holder, bitmap, width, height, density, false);
    }
    
	public static BitmapRef createScaledBitmap(BitmapHolder holder, 
			BitmapRef bitmap, int width, int height, int density, boolean forceCreate) {
		return createScaledBitmap(holder, bitmap, width, height, density, 
				FITTYPE_NONE, forceCreate); 
	}
	
	public static BitmapRef createZoomOutScaledBitmap(BitmapHolder holder, 
			BitmapRef bitmap, int width, int height) {
		return createScaledBitmap(holder, bitmap, width, height, 0, 
				FITTYPE_ZOOMOUT, false); 
	}
	
	public static BitmapRef createZoomOutScaledBitmap(BitmapHolder holder, 
			BitmapRef bitmap, int width, int height, int density) {
		return createScaledBitmap(holder, bitmap, width, height, density, 
				FITTYPE_ZOOMOUT, false); 
	}
	
	public static BitmapRef createZoomInScaledBitmap(BitmapHolder holder, 
			BitmapRef bitmap, int width, int height) {
		return createScaledBitmap(holder, bitmap, width, height, 0, 
				FITTYPE_ZOOMIN, false); 
	}
	
	public static BitmapRef createZoomInScaledBitmap(BitmapHolder holder, 
			BitmapRef bitmap, int width, int height, int density) {
		return createScaledBitmap(holder, bitmap, width, height, density, 
				FITTYPE_ZOOMIN, false); 
	}
	
	public static int FITTYPE_NONE = 0; 
	public static int FITTYPE_ZOOMOUT = 1; 
	public static int FITTYPE_ZOOMIN = 2; 
	
    public static BitmapRef createScaledBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, int width, int height, int density, int fitType, 
    		boolean forceCreate) {
    	if (bitmap == null || holder == null || width <= 0 || height <= 0) 
    		return forceCreate ? null : bitmap; 
    	
        int actualWidth = width; 
        int actualHeight = height; 
        
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        
        int defaultDensity = bitmap.getDensity(); 
        int targetDensity = density <= 0 ? defaultDensity : density; 
        
        if (forceCreate == false && defaultDensity == targetDensity && 
        	actualWidth == bitmapWidth && actualHeight == bitmapHeight) 
        	return bitmap; 
        
        //float scale = targetDensity / (float)defaultDensity; 
        //if (targetDensity <= 0) 
        //	scale = 1.0f; 
        //if (defaultDensity > 160) // bitmap by 160dpi?
        //	scale *= (float) defaultDensity / 160.0f; 

        if (fitType == FITTYPE_ZOOMOUT) {
        	float actualRate = (float)actualWidth / (float)actualHeight; 
            float bitmapRate = (float)bitmapWidth / (float)bitmapHeight; 
	        if (actualRate > bitmapRate) {
	        	actualWidth = (int)((float)actualHeight * bitmapRate); 
	        } else if (actualRate < bitmapRate) {
	        	actualHeight = (int)((float)actualWidth / bitmapRate); 
	        }
        } else if (fitType == FITTYPE_ZOOMIN) {
        	float actualRate = (float)actualWidth / (float)actualHeight; 
            float bitmapRate = (float)bitmapWidth / (float)bitmapHeight; 
        	if (actualRate < bitmapRate) {
	        	actualWidth = (int)((float)actualHeight * bitmapRate); 
	        } else if (actualRate > bitmapRate) {
	        	actualHeight = (int)((float)actualWidth / bitmapRate); 
	        }
        }
        
        try {
        	BitmapRef endImage = BitmapRef.createScaledBitmap(holder,
        			bitmap, actualWidth, actualHeight, true);
        	if (endImage == null) 
        		return bitmap;
        	
        	if (targetDensity > 0)
        		endImage.setDensity(targetDensity);
        	
        	//BitmapRefs.onBitmapCreated(endImage);
        	
        	return endImage;
        } catch (OutOfMemoryError e) {
        	Utilities.handleOOM(e);
        	
            return bitmap;
        }
    }
	
    public static BitmapRef createShaderBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, int color) {
    	if (bitmap == null || holder == null) 
    		return bitmap; 
    	
    	int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        
    	int actualWidth = bitmapWidth; 
        int actualHeight = bitmapHeight; 
        
        int startX = 0; 
        int startY = 0; 
        
        actualWidth += startX * 2;
        actualHeight += startY * 2; 
        
        BitmapRef original = createScaledBitmap(holder, 
        		bitmap, actualWidth, actualHeight, 0, true); 
        if (original == null) 
        	return bitmap;
        
        Canvas canvas = new Canvas(original.get());
        //canvas.setBitmap(original.get());
        
        Paint paint = new Paint(); 
        Shader saveShader = paint.getShader(); 
        Xfermode saveMode = paint.getXfermode(); 
        
        LinearGradient shader = new LinearGradient(
        		actualWidth/2, 0, actualHeight/2, actualHeight,
        		Color.argb(192, Color.red(color), Color.green(color), Color.blue(color)),
        		Color.argb(128, Color.red(color), Color.green(color), Color.blue(color)),
        		TileMode.CLAMP); 
        
        paint.setShader(shader); 
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawRect(0, 0, actualWidth, actualHeight, paint);
	    
        paint.setShader(saveShader); 
        paint.setXfermode(saveMode); 
        //canvas.drawBitmap(bitmap, startX, startY, paint); 
    	
        try {
        	BitmapRef endImage = BitmapRef.createScaledBitmap(holder,
        			original, actualWidth, actualHeight, true);
        	if (endImage == null) 
        		return bitmap;
        	
        	endImage.setDensity(bitmap.getDensity());
        	//BitmapRefs.onBitmapCreated(endImage);
        	
        	if (original != bitmap)
        		original.recycle();
        	
        	return endImage;
        } catch (OutOfMemoryError e) {
        	Utilities.handleOOM(e);
        	
            return bitmap;
        }
    }
    
	public static BitmapRef createGrayBitmap(BitmapHolder holder, BitmapRef bitmap) {
        if (holder == null || bitmap == null) 
        	return bitmap; 
        
        try {
	        int w = bitmap.getWidth(), h = bitmap.getHeight(); 
	        int[] pix = new int[w * h]; 
	        bitmap.getPixels(pix, 0, w, 0, 0, w, h); 
	 
	        int alpha = 0xFF<<24; 
	        for (int i = 0; i < h; i++) { 
	            for (int j = 0; j < w; j++) { 
	                int color = pix[w * i + j]; 
	                int red = ((color & 0x00FF0000) >> 16); 
	                int green = ((color & 0x0000FF00) >> 8); 
	                int blue = color & 0x000000FF; 
	                color = (red + green + blue)/3; 
	                color = alpha | (color << 16) | (color << 8) | color; 
	                pix[w * i + j] = color; 
	            } 
	        }
	        
	        BitmapRef result = BitmapRef.createBitmap(holder, 
	        		w, h, Bitmap.Config.RGB_565); 
	        if (result == null) 
	        	return bitmap;
	        
	        result.setPixels(pix, 0, w, 0, 0, w, h); 
	        
	        //BitmapRefs.onBitmapCreated(result);
	        
	        return result; 
        } catch (OutOfMemoryError e) {
        	Utilities.handleOOM(e);
        	
        	return bitmap; 
        }
    }
	
	public static BitmapRef createRoundBitmap(BitmapHolder holder, BitmapRef bitmap) {
		if (holder == null || bitmap == null) return bitmap; 
		
		final int width = bitmap.getWidth(); 
    	final int height = bitmap.getHeight(); 
    	
    	float px = (float)width / 2.0f;
    	float py = (float)height / 2.0f;
    	
    	return createRoundedCornerBitmap(holder, bitmap, 0, px, py); 
    }
	
    public static BitmapRef createRoundedCornerBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, float roundPixels) {
    	return createRoundedCornerBitmap(holder, bitmap, 0, 
    			roundPixels, roundPixels); 
    }
    
    public static BitmapRef createRoundedCornerBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, int frameWidth, float roundPx, float roundPy) {
    	if (holder == null || bitmap == null) 
    		return bitmap; 
    	
    	final int width = bitmap.getWidth(); 
    	final int height = bitmap.getHeight(); 
    	
    	if (frameWidth < 0 || frameWidth >= width / 2 || frameWidth >= height / 2) 
    		frameWidth = 0; 
    	
    	try {
    		BitmapRef output = BitmapRef.createBitmap(holder, width, height);
	    	if (output == null) 
	    		return bitmap;
	    	
	    	final Canvas canvas = new Canvas(output.get());
	    	final int color = 0xFF424242;
	    	
	    	if (roundPx < 0) roundPx = 0;
	    	if (roundPy < 0) roundPy = 0;
	    	
	    	final Paint paint = new Paint();
	    	final Rect rect = new Rect(frameWidth, frameWidth, 
	    			width - frameWidth, height - frameWidth);
	    	final RectF rectF = new RectF(rect);
	    	
	    	paint.setAntiAlias(true);
	    	canvas.drawARGB(0, 0, 0, 0); 
	    	
	    	paint.setColor(color);
	    	canvas.drawRoundRect(rectF, roundPx, roundPy, paint);
	    	
	    	paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	    	canvas.drawBitmap(bitmap.get(), rect, rect, paint);
	    	
	    	output.setDensity(bitmap.getDensity()); 
	    	//BitmapRefs.onBitmapCreated(output);
	    	
	    	return output; 
	    	
    	} catch (OutOfMemoryError ex) {
    		Utilities.handleOOM(ex);
    		
        	return bitmap; 
        }
    }
    
    public static BitmapRef createScaledCropBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, int width, int height) {
    	return createScaledCropBitmap(holder, bitmap, width, height, -1, -1); 
    }
    
    public static BitmapRef createScaledCropBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, int width, int height, int startX, int startY) {
    	return createScaledCropBitmap(holder, bitmap, width, height, startX, startY, 0); 
    }
    
    public static BitmapRef createScaledCropBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, int width, int height, int startX, int startY, int density) {
    	return createScaledCropFrameBitmap(holder, bitmap, width, height, startX, startY, 
    			width, height, density); 
    }
    
    public static BitmapRef createCenterScaledCropFrameBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, int width, int height, int outputWidth, int outputHeight) {
    	return createScaledCropFrameBitmap(holder, bitmap, width, height, -1, -1, 
    			outputWidth, outputHeight, 0); 
    }
    
    public static BitmapRef createScaledCropFrameBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, int width, int height, int startX, int startY, 
    		int outputWidth, int outputHeight) {
    	return createScaledCropFrameBitmap(holder, bitmap, width, height, startX, startY, 
    			outputWidth, outputHeight, 0); 
    }
    
    public static BitmapRef createScaledCropFrameBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, int width, int height, int startX, int startY, 
    		int outputWidth, int outputHeight, int density) {
    	return createScaledCropFrameBitmap(holder, bitmap, width, height, startX, startY, 
    			outputWidth, outputHeight, density, true);
    }
    
    public static BitmapRef createScaledCropFrameBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, int width, int height, int startX, int startY, 
    		int outputWidth, int outputHeight, int density, boolean autoScale) {
    	if (holder == null || bitmap == null) 
    		return bitmap; 
    	
    	final float bitmapWidth = bitmap.getWidth(); 
    	final float bitmapHeight = bitmap.getHeight(); 
    	
    	float inputWidth = width; //Utilities.getDisplaySize(context, width); 
    	float inputHeight = height; //Utilities.getDisplaySize(context, height); 
    	
    	if (inputWidth <= 0) inputWidth = bitmapWidth; 
    	if (inputHeight <= 0) inputHeight = bitmapHeight; 
    	
    	float actualWidth = outputWidth; //Utilities.getDisplaySize(context, outputWidth); 
    	float actualHeight = outputHeight; //Utilities.getDisplaySize(context, outputHeight); 
    	
    	if (actualWidth < inputWidth) actualWidth = inputWidth; 
    	if (actualHeight < inputHeight) actualHeight = inputHeight; 
    	
		float rate1 = bitmapWidth / bitmapHeight; 
		float rate2 = inputWidth / inputHeight; 
		
		if (rate1 >= rate2) {
			if (inputHeight > bitmapHeight || autoScale) {
				inputWidth = inputWidth * bitmapHeight / inputHeight; 
				inputHeight = bitmapHeight; 
			}
		} else {
			if (inputWidth > bitmapWidth || autoScale) {
				inputHeight = inputHeight * bitmapWidth / inputWidth; 
    			inputWidth = bitmapWidth; 
			}
		}
    	
		float wb = bitmapWidth / 2; 
		float hb = bitmapHeight / 2; 
    	float wi = inputWidth / 2; 
		float hi = inputHeight / 2; 
		
		float left = wb - wi; 
		float right = wb + wi; 
		float top = hb - hi; 
		float bottom = hb + hi; 
    	
		if (startX >= 0 && startY >= 0) {
			float endX = bitmapWidth - inputWidth; 
			float endY = bitmapHeight - inputHeight; 
			
			float beginX = startX; 
			float beginY = startY; 
			
			if (beginX > endX) beginX = endX; 
			if (beginY > endY) beginY = endY; 
			
			left = beginX; 
			top = beginY; 
			right = left + inputWidth; 
			bottom = top + inputHeight; 
		}
		
		float leftDst = 0, topDst = 0; 
		float rightDst = actualWidth, bottomDst = actualHeight; 
		
		if (actualWidth > inputWidth) {
			float w = (actualWidth - inputWidth)/ 2; 
			leftDst = w; 
			rightDst = w + inputWidth; 
		}
		
		if (actualHeight > inputHeight) {
			float w = (actualHeight - inputHeight)/ 2; 
			topDst = w; 
			bottomDst = w + inputHeight; 
		}
		
		int defaultDensity = bitmap.getDensity(); 
        //int targetDensity = density <= 0 ? defaultDensity : density; 
		
    	try {
    		BitmapRef output = BitmapRef.createBitmap(holder, 
    				(int)actualWidth, (int)actualHeight);
	    	if (output == null) 
	    		return bitmap;
	    	
	    	final Canvas canvas = new Canvas(output.get());
	    	final Paint paint = new Paint();
	    	final Rect rectSrc = new Rect((int)left, (int)top, (int)right, (int)bottom);
	    	final RectF rectDst = new RectF(leftDst, topDst, rightDst, bottomDst);

	    	canvas.drawBitmap(bitmap.get(), rectSrc, rectDst, paint);
	    	
	    	output.setDensity(defaultDensity); 
	    	//BitmapRefs.onBitmapCreated(output);
	    	
	    	return output; 
	    	
    	} catch (OutOfMemoryError ex) {
    		Utilities.handleOOM(ex);
    		
        	return bitmap; 
        }
    }
    
    protected static BitmapRef drawBubbleText(BitmapHolder holder, 
    		BitmapRef bitmap, String text) {
    	return drawBubbleText(holder, bitmap, text, 0); 
    }
    
    protected static BitmapRef drawBubbleText(BitmapHolder holder, 
    		BitmapRef bitmap, String text, int density) {
    	if (holder == null || bitmap == null) 
    		return bitmap; 
    	
    	final int bitmapWidth = bitmap.getWidth(); 
    	final int bitmapHeight = bitmap.getHeight(); 
    	
		int defaultDensity = bitmap.getDensity(); 
        //int targetDensity = density <= 0 ? defaultDensity : density; 
    	
    	try {
    		final BitmapRef output = BitmapRef.createBitmap(holder, 
    				bitmapWidth, bitmapHeight);
    		if (output == null) 
    			return bitmap;
    		
	    	final Canvas canvas = new Canvas(output.get());
	    	final Paint paint = new Paint();
	    	
	    	canvas.drawBitmap(bitmap.get(), 0, 0, paint); 
	    	
	    	final RectF rect = new RectF(10, 10, 60, 50); 
	    	paint.setColor(Color.RED); 
	    	canvas.drawRoundRect(rect, 20, 20, paint); 
	    	
	    	paint.setColor(Color.YELLOW); 
	    	paint.setTextSize(12); 
	    	canvas.drawText(text, 20, 20, paint); 
	    	
	    	output.setDensity(defaultDensity); 
	    	//BitmapRefs.onBitmapCreated(output);
	    	
	    	return output; 
	    	
    	} catch (OutOfMemoryError ex) {
    		Utilities.handleOOM(ex);
    		
        	return bitmap; 
        }
    }
    
    public static BitmapRef drawFaceBitmaps(BitmapHolder holder, 
    		int width, int height, BitmapRef... bitmaps) {
    	if (holder == null) return null; 
    	
    	final int bitmapWidth = width; 
    	final int bitmapHeight = height; 
    	
    	if (bitmapWidth <= 0 || bitmapHeight <= 0) 
    		return null; 
    	
    	BitmapRef bitmapFrame = null; 
    	BitmapRef bitmap1 = null; 
    	BitmapRef bitmap2 = null; 
    	BitmapRef bitmap3 = null; 
    	BitmapRef bitmap4 = null; 
    	
    	boolean isSingle = true; 
    	
    	if (bitmaps != null && bitmaps.length > 0) {
    		bitmapFrame = bitmaps[0]; 
    		
    		if (bitmaps.length > 1) 
    			bitmap1 = bitmaps[1]; 
    		
    		if (bitmaps.length > 2) {
    			bitmap2 = bitmaps[2]; 
    			isSingle = false; 
    		}
    		
    		if (bitmaps.length > 3) {
    			bitmap3 = bitmaps[3]; 
    			isSingle = false; 
    		}
    		
    		if (bitmaps.length > 4) {
    			bitmap4 = bitmaps[4]; 
    			isSingle = false; 
    		}
    	}
    	
    	//int density = 0; 
		int defaultDensity = bitmapFrame != null ? bitmapFrame.getDensity() : 0; 
        //int targetDensity = density <= 0 ? defaultDensity : density; 
    	
    	try {
    		final BitmapRef output = BitmapRef.createBitmap(holder, 
    				bitmapWidth, bitmapHeight);
    		if (output == null) 
    			return null;
    		
	    	final Canvas canvas = new Canvas(output.get());
	    	final Paint paint = new Paint();
	    	
	    	if (isSingle) {
	    		drawFaceBitmap(canvas, bitmapFrame, paint, 0, 0, height, height); 
	    		drawFaceBitmap(canvas, bitmap1, paint, 0, 0, height, height); 
	    		
	    	} else {
	    		float middle = height / 2; 
	    		
	    		//final RectF rect = new RectF(0, 0, height, height); 
		    	//paint.setColor(Color.BLACK); 
		    	//canvas.drawRoundRect(rect, 4, 4, paint); 
		    	
		    	drawFaceBitmap(canvas, bitmapFrame, paint, 0, 0, height, height); 
	    		
	    		drawFaceBitmap(canvas, bitmap1, paint, 0, 0, middle, middle); 
	    		drawFaceBitmap(canvas, bitmap2, paint, middle, 0, height, middle); 
	    		drawFaceBitmap(canvas, bitmap3, paint, 0, middle, middle, height); 
	    		drawFaceBitmap(canvas, bitmap4, paint, middle, middle, height, height); 
	    	}
	    	
	    	if (defaultDensity > 0) 
	    		output.setDensity(defaultDensity); 
	    	
	    	//BitmapRefs.onBitmapCreated(output);
	    	
	    	return output; 
	    	
    	} catch (OutOfMemoryError ex) {
    		Utilities.handleOOM(ex);
    		
        	return null; 
        }
    }
    
    private static void drawFaceBitmap(Canvas canvas, BitmapRef bitmap, Paint paint, 
    		float startX, float startY, float width, float height) {
    	if (canvas != null && paint != null && bitmap != null) {
    		final Rect rectSrc = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()); 
    		final RectF rectDst = new RectF(startX, startY, width, height); 
    		
    		canvas.drawBitmap(bitmap.get(), rectSrc, rectDst, paint); 
    	}
    }
    
    public static BitmapRef createScaledCropBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, int inWidth, int inHeight, int inFrameWidth) {
    	return createScaledCropBitmap(holder, bitmap, inWidth, inHeight, inFrameWidth, false);
    }
    
    public static BitmapRef createScaledCropBitmap(BitmapHolder holder, 
    		BitmapRef bitmap, int inWidth, int inHeight, int inFrameWidth, boolean zoomOutput) {
		if (holder == null || bitmap == null) 
			return null; 
		
		final int inputWidth = bitmap.getWidth(); 
		final int inputHeight = bitmap.getHeight(); 
		
		final int frameWidth = inFrameWidth * 2; 
		
		int outputWidth = inWidth; 
		int outputHeight = inHeight; 
		
		if (zoomOutput && (outputWidth > inputWidth || outputHeight > inputHeight)) { 
			float outputRate = (float)outputWidth / (float)outputHeight;
			float inputRate = (float)inputWidth / (float)inputHeight;
			
			if (outputRate >= inputRate) { 
				outputWidth = inputWidth;
				outputHeight = (int)((float)outputWidth / outputRate);
				
			} else { 
				outputHeight = inputHeight;
				outputWidth = (int)((float)outputHeight * outputRate);
			}
		}
		
		int cropWidth = outputWidth - frameWidth; 
		int cropHeight = outputHeight - frameWidth; 
		
		BitmapRef savedInput = bitmap; 
		
		if (inputWidth < outputWidth || inputHeight < outputHeight) 
			bitmap = createZoomInScaledBitmap(holder, bitmap, outputWidth, outputHeight); 
		
		BitmapRef output = createCenterScaledCropFrameBitmap(holder, bitmap, 
				cropWidth, cropHeight, outputWidth, outputHeight); 
		
		if (bitmap != null && savedInput != bitmap && output != bitmap) 
			bitmap.recycle(); 
		
		return output; 
	}
    
    private static class DecodeCanceller implements JobCancelListener {
        private final BitmapFactory.Options mOptions;
        public DecodeCanceller(BitmapFactory.Options options) {
            mOptions = options;
        }
        @Override
        public void onCancel() {
            mOptions.requestCancelDecode();
        }
    }

    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
    public static void setOptionsMutable(BitmapFactory.Options options) {
        if (ApiHelper.HAS_OPTIONS_IN_MUTABLE) 
        	options.inMutable = true;
    }

    public static BitmapRef decode(BitmapHolder holder, JobContext jc, 
    		FileDescriptor fd, BitmapFactory.Options options) {
        if (options == null) 
        	options = new BitmapFactory.Options();
        
        jc.setCancelListener(new DecodeCanceller(options));
        setOptionsMutable(options);
        
        return ensureGLCompatibleBitmap(holder, 
        		BitmapRef.decodeFileDescriptor(holder, fd, null, options));
    }

    public static void decodeBounds(BitmapHolder holder, JobContext jc, 
    		FileDescriptor fd, BitmapFactory.Options options) {
        Utils.assertTrue(options != null);
        options.inJustDecodeBounds = true;
        
        jc.setCancelListener(new DecodeCanceller(options));
        BitmapRef.decodeFileDescriptor(holder, fd, null, options);
        
        options.inJustDecodeBounds = false;
    }

    public static BitmapRef decode(BitmapHolder holder, JobContext jc, 
    		byte[] bytes, BitmapFactory.Options options) {
        return decode(holder, jc, bytes, 0, bytes.length, options);
    }

    public static BitmapRef decode(BitmapHolder holder, JobContext jc, 
    		byte[] bytes, int offset, int length, BitmapFactory.Options options) {
        if (options == null) 
        	options = new BitmapFactory.Options();
        
        jc.setCancelListener(new DecodeCanceller(options));
        setOptionsMutable(options);
        
        return ensureGLCompatibleBitmap(holder, 
        		BitmapRef.decodeByteArray(holder, bytes, offset, length, options));
    }

    public static void decodeBounds(BitmapHolder holder, JobContext jc, 
    		byte[] bytes, int offset, int length, BitmapFactory.Options options) {
        Utils.assertTrue(options != null);
        options.inJustDecodeBounds = true;
        
        jc.setCancelListener(new DecodeCanceller(options));
        BitmapRef.decodeByteArray(holder, bytes, offset, length, options);
        
        options.inJustDecodeBounds = false;
    }

    public static BitmapRef decodeThumbnail(BitmapHolder holder, JobContext jc, 
    		File file, BitmapFactory.Options options, int targetSize, int type) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return decodeThumbnail(holder, jc, fis.getFD(), options, targetSize, type);
            
        } catch (Throwable ex) {
        	if (LOG.isWarnEnabled())
            	LOG.warn(ex.toString(), ex);
        	
            return null;
        } finally {
            Utils.closeSilently(fis);
        }
    }

    public static BitmapRef decodeThumbnail(BitmapHolder holder, JobContext jc, 
    		FileDescriptor fd, BitmapFactory.Options options, int targetSize, int type) {
        if (options == null) 
        	options = new BitmapFactory.Options();
        
        jc.setCancelListener(new DecodeCanceller(options));
        options.inJustDecodeBounds = true;
        
        BitmapRef.decodeFileDescriptor(holder, fd, null, options);
        if (jc.isCancelled()) return null;

        int w = options.outWidth;
        int h = options.outHeight;

        if (type == BitmapHelper.TYPE_MICROTHUMBNAIL) {
            // We center-crop the original image as it's micro thumbnail. In this case,
            // we want to make sure the shorter side >= "targetSize".
            float scale = (float) targetSize / Math.min(w, h);
            options.inSampleSize = computeSampleSizeLarger(scale);

            // For an extremely wide image, e.g. 300x30000, we may got OOM when decoding
            // it for TYPE_MICROTHUMBNAIL. So we add a max number of pixels limit here.
            final int MAX_PIXEL_COUNT = 640000; // 400 x 1600
            if ((w / options.inSampleSize) * (h / options.inSampleSize) > MAX_PIXEL_COUNT) {
                options.inSampleSize = computeSampleSize(
                        FloatMath.sqrt((float) MAX_PIXEL_COUNT / (w * h)));
            }
        } else {
            // For screen nail, we only want to keep the longer side >= targetSize.
            float scale = (float) targetSize / Math.max(w, h);
            options.inSampleSize = computeSampleSizeLarger(scale);
        }

        options.inJustDecodeBounds = false;
        setOptionsMutable(options);

        BitmapRef result = BitmapRef.decodeFileDescriptor(holder, fd, null, options);
        if (result == null) return null;

        // We need to resize down if the decoder does not support inSampleSize
        // (For example, GIF images)
        float scale = (float) targetSize / (type == BitmapHelper.TYPE_MICROTHUMBNAIL
                ? Math.min(result.getWidth(), result.getHeight())
                : Math.max(result.getWidth(), result.getHeight()));

        if (scale <= 0.5) 
        	result = resizeBitmapByScale(holder, result, scale, true);
        
        return ensureGLCompatibleBitmap(holder, result);
    }

    /**
     * Decodes the bitmap from the given byte array if the image size is larger than the given
     * requirement.
     *
     * Note: The returned image may be resized down. However, both width and height must be
     * larger than the <code>targetSize</code>.
     */
    public static BitmapRef decodeIfBigEnough(BitmapHolder holder, JobContext jc, 
    		byte[] data, BitmapFactory.Options options, int targetSize) {
        if (options == null) 
        	options = new BitmapFactory.Options();
        
        jc.setCancelListener(new DecodeCanceller(options));
        options.inJustDecodeBounds = true;
        
        BitmapRef.decodeByteArray(holder, data, 0, data.length, options);
        if (jc.isCancelled()) return null;
        
        if (options.outWidth < targetSize || options.outHeight < targetSize) 
            return null;
        
        options.inSampleSize = computeSampleSizeLarger(
                options.outWidth, options.outHeight, targetSize);
        
        options.inJustDecodeBounds = false;
        setOptionsMutable(options);

        return ensureGLCompatibleBitmap(holder, 
                BitmapRef.decodeByteArray(holder, data, 0, data.length, options));
    }

    // TODO: This function should not be called directly from
    // DecodeUtils.requestDecode(...), since we don't have the knowledge
    // if the bitmap will be uploaded to GL.
    public static BitmapRef ensureGLCompatibleBitmap(BitmapHolder holder, BitmapRef bitmap) {
        if (bitmap == null || bitmap.getConfig() != null) 
        	return bitmap;
        
        BitmapRef newBitmap = bitmap.copy(holder, BitmapRef.getBitmapConfig(), false);
        bitmap.recycle();
        
        return newBitmap;
    }

    public static BitmapRegionDecoder createBitmapRegionDecoder(
            JobContext jc, byte[] bytes, int offset, int length,
            boolean shareable) {
        if (offset < 0 || length <= 0 || offset + length > bytes.length) {
            throw new IllegalArgumentException(String.format(
                    "offset = %s, length = %s, bytes = %s", offset, length, bytes.length));
        }

        try {
            return BitmapRegionDecoder.newInstance(
                    bytes, offset, length, shareable);
            
        } catch (Throwable t)  {
        	if (LOG.isWarnEnabled())
            	LOG.warn(t.toString(), t);
        	
            return null;
        }
    }

    public static BitmapRegionDecoder createBitmapRegionDecoder(
            JobContext jc, String filePath, boolean shareable) {
        try {
            return BitmapRegionDecoder.newInstance(filePath, shareable);
            
        } catch (Throwable t)  {
        	if (LOG.isWarnEnabled())
            	LOG.warn(t.toString(), t);
        	
            return null;
        }
    }

    public static BitmapRegionDecoder createBitmapRegionDecoder(
            JobContext jc, FileDescriptor fd, boolean shareable) {
        try {
            return BitmapRegionDecoder.newInstance(fd, shareable);
            
        } catch (Throwable t)  {
        	if (LOG.isWarnEnabled())
            	LOG.warn(t.toString(), t);
        	
            return null;
        }
    }

    public static BitmapRegionDecoder createBitmapRegionDecoder(
            JobContext jc, InputStream is, boolean shareable) {
        try {
            return BitmapRegionDecoder.newInstance(is, shareable);
            
        } catch (Throwable t)  {
            // We often cancel the creating of bitmap region decoder,
            // so just log one line.
        	if (LOG.isWarnEnabled())
            	LOG.warn("requestCreateBitmapRegionDecoder: " + t);
        	
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static BitmapRef decode(BitmapHolder holder, JobContext jc, 
    		byte[] data, int offset, int length, BitmapFactory.Options options, 
    		BitmapPool pool) {
        if (pool == null) 
            return decode(holder, jc, data, offset, length, options);

        if (options == null) 
        	options = new BitmapFactory.Options();
        
        if (options.inSampleSize < 1) 
        	options.inSampleSize = 1;
        
        options.inPreferredConfig = BitmapRef.getBitmapConfig();
        BitmapRef ref = (options.inSampleSize == 1) ? 
        		findCachedBitmap(holder, jc, data, offset, length, options, pool) : null;
        		
        options.inBitmap = ref != null ? ref.get() : null;
        
        try {
        	BitmapRef bitmap = decode(holder, jc, data, offset, length, options);
        	
            if (options.inBitmap != null && options.inBitmap != bitmap.get()) {
                pool.recycle(BitmapRef.newBitmap(holder, options.inBitmap));
                options.inBitmap = null;
            }
            
            return bitmap;
        } catch (IllegalArgumentException e) {
            if (options.inBitmap == null) 
            	throw e;
            
            if (LOG.isWarnEnabled())
            	LOG.warn("decode fail with a given bitmap, try decode to a new bitmap");
            
            pool.recycle(BitmapRef.newBitmap(holder, options.inBitmap));
            options.inBitmap = null;
            
            return decode(holder, jc, data, offset, length, options);
        }
    }

    // This is the same as the method above except the source data comes
    // from a file descriptor instead of a byte array.
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static BitmapRef decode(BitmapHolder holder, JobContext jc, 
    		FileDescriptor fileDescriptor, BitmapFactory.Options options, BitmapPool pool) {
        if (pool == null) 
            return decode(holder, jc, fileDescriptor, options);

        if (options == null) 
        	options = new BitmapFactory.Options();
        
        if (options.inSampleSize < 1) 
        	options.inSampleSize = 1;
        
        options.inPreferredConfig = BitmapRef.getBitmapConfig();
        
        BitmapRef ref = (options.inSampleSize == 1) ? 
        		findCachedBitmap(holder, jc, fileDescriptor, options, pool) : null;
        		
        options.inBitmap = ref != null ? ref.get() : null;
        
        try {
            BitmapRef bitmap = decode(holder, jc, fileDescriptor, options);
            
            if (options.inBitmap != null && options.inBitmap != bitmap.get()) {
                pool.recycle(BitmapRef.newBitmap(holder, options.inBitmap));
                options.inBitmap = null;
            }
            
            return bitmap;
        } catch (IllegalArgumentException e) {
            if (options.inBitmap == null) 
            	throw e;
            
            if (LOG.isWarnEnabled())
            	LOG.warn("decode fail with a given bitmap, try decode to a new bitmap");
            
            pool.recycle(BitmapRef.newBitmap(holder, options.inBitmap));
            options.inBitmap = null;
            
            return decode(holder, jc, fileDescriptor, options);
        }
    }

    private static BitmapRef findCachedBitmap(BitmapHolder holder, JobContext jc,
            byte[] data, int offset, int length, BitmapFactory.Options options, BitmapPool pool) {
        if (pool.isOneSize()) 
        	return pool.getBitmap();
        
        decodeBounds(holder, jc, data, offset, length, options);
        
        return pool.getBitmap(options.outWidth, options.outHeight);
    }

    private static BitmapRef findCachedBitmap(BitmapHolder holder, JobContext jc,
            FileDescriptor fileDescriptor, BitmapFactory.Options options, BitmapPool pool) {
        if (pool.isOneSize()) 
        	return pool.getBitmap();
        
        decodeBounds(holder, jc, fileDescriptor, options);
        
        return pool.getBitmap(options.outWidth, options.outHeight);
    }
    
    public static final int DEFAULT_JPEG_QUALITY = 90;
    public static final int UNCONSTRAINED = -1;
    
    /**
     * Compute the sample size as a function of minSideLength
     * and maxNumOfPixels.
     * minSideLength is used to specify that minimal width or height of a
     * bitmap.
     * maxNumOfPixels is used to specify the maximal size in pixels that is
     * tolerable in terms of memory usage.
     *
     * The function returns a sample size based on the constraints.
     * Both size and minSideLength can be passed in as UNCONSTRAINED,
     * which indicates no care of the corresponding constraint.
     * The functions prefers returning a sample size that
     * generates a smaller bitmap, unless minSideLength = UNCONSTRAINED.
     *
     * Also, the function rounds up the sample size to a power of 2 or multiple
     * of 8 because BitmapFactory only honors sample size this way.
     * For example, BitmapFactory downsamples an image by 2 even though the
     * request is 3. So we round up the sample size to avoid OOM.
     */
    public static int computeSampleSize(int width, int height,
            int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(
                width, height, minSideLength, maxNumOfPixels);

        return initialSize <= 8
                ? Utils.nextPowerOf2(initialSize)
                : (initialSize + 7) / 8 * 8;
    }

    private static int computeInitialSampleSize(int w, int h,
            int minSideLength, int maxNumOfPixels) {
        if (maxNumOfPixels == UNCONSTRAINED && minSideLength == UNCONSTRAINED) 
        	return 1;

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 :
                (int) FloatMath.ceil(FloatMath.sqrt((float) (w * h) / maxNumOfPixels));

        if (minSideLength == UNCONSTRAINED) {
            return lowerBound;
            
        } else {
            int sampleSize = Math.min(w / minSideLength, h / minSideLength);
            return Math.max(sampleSize, lowerBound);
        }
    }

    // This computes a sample size which makes the longer side at least
    // minSideLength long. If that's not possible, return 1.
    public static int computeSampleSizeLarger(int w, int h,
            int minSideLength) {
        int initialSize = Math.max(w / minSideLength, h / minSideLength);
        if (initialSize <= 1) return 1;

        return initialSize <= 8
                ? Utils.prevPowerOf2(initialSize)
                : initialSize / 8 * 8;
    }

    // Find the min x that 1 / x >= scale
    public static int computeSampleSizeLarger(float scale) {
        int initialSize = (int) FloatMath.floor(1f / scale);
        if (initialSize <= 1) return 1;

        return initialSize <= 8
                ? Utils.prevPowerOf2(initialSize)
                : initialSize / 8 * 8;
    }

    // Find the max x that 1 / x <= scale.
    public static int computeSampleSize(float scale) {
        Utils.assertTrue(scale > 0);
        int initialSize = Math.max(1, (int) FloatMath.ceil(1 / scale));
        
        return initialSize <= 8
                ? Utils.nextPowerOf2(initialSize)
                : (initialSize + 7) / 8 * 8;
    }
    
    public static BitmapRef resizeBitmapByScale(BitmapHolder holder, 
    		BitmapRef bitmap, float scale, boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        
        if (width == bitmap.getWidth() && height == bitmap.getHeight()) 
        	return bitmap;
        
        BitmapRef target = BitmapRef.createBitmap(holder, width, height, getConfig(bitmap));
        if (target == null) 
        	return bitmap;
        
        Canvas canvas = new Canvas(target.get());
        canvas.scale(scale, scale);
        
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap.get(), 0, 0, paint);
        
        if (recycle) bitmap.recycle();
        return target;
    }
    
    private static Bitmap.Config getConfig(BitmapRef bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) 
            config = BitmapRef.getBitmapConfig();
        
        return config;
    }
    
    public static BitmapRef resizeDownBySideLength(BitmapHolder holder,
    		BitmapRef bitmap, int maxLength, boolean recycle) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        
        float scale = Math.min((float) maxLength / srcWidth, 
        		(float) maxLength / srcHeight);
        if (scale >= 1.0f) 
        	return bitmap;
        
        return resizeBitmapByScale(holder, bitmap, scale, recycle);
    }

    public static BitmapRef resizeAndCropCenter(BitmapHolder holder, 
    		BitmapRef bitmap, int size, boolean recycle) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        
        if (w == size && h == size) 
        	return bitmap;

        // scale the image so that the shorter side equals to the target;
        // the longer side will be center-cropped.
        float scale = (float) size / Math.min(w,  h);

        BitmapRef target = BitmapRef.createBitmap(holder, size, size, getConfig(bitmap));
        if (target == null) 
        	return bitmap;
        
        int width = Math.round(scale * bitmap.getWidth());
        int height = Math.round(scale * bitmap.getHeight());
        
        Canvas canvas = new Canvas(target.get());
        canvas.translate((size - width) / 2f, (size - height) / 2f);
        canvas.scale(scale, scale);
        
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap.get(), 0, 0, paint);
        
        if (recycle) bitmap.recycle();
        return target;
    }
    
    public static BitmapRef createVideoThumbnail(BitmapHolder holder, String filePath) {
        // MediaMetadataRetriever is available on API Level 8
        // but is hidden until API Level 10
        Class<?> clazz = null;
        Object instance = null;
        
        try {
            clazz = Class.forName("android.media.MediaMetadataRetriever");
            instance = clazz.newInstance();

            Method method = clazz.getMethod("setDataSource", String.class);
            method.invoke(instance, filePath);

            // The method name changes between API Level 9 and 10.
            if (Build.VERSION.SDK_INT <= 9) {
                return BitmapRef.newBitmap(holder, 
                		clazz.getMethod("captureFrame").invoke(instance));
                
            } else {
                byte[] data = (byte[]) clazz.getMethod("getEmbeddedPicture").invoke(instance);
                if (data != null) {
                	BitmapRef bitmap = BitmapRef.decodeByteArray(holder, data, 0, data.length);
                    if (bitmap != null) 
                    	return bitmap;
                }
                
                return BitmapRef.newBitmap(holder, 
                		clazz.getMethod("getFrameAtTime").invoke(instance));
            }
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } catch (Throwable e) {
        	if (LOG.isWarnEnabled())
            	LOG.warn("createVideoThumbnail", e);
        	
        } finally {
            try {
                if (instance != null) 
                    clazz.getMethod("release").invoke(instance);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static byte[] compressToBytes(BitmapRef bitmap) {
        return compressToBytes(bitmap, DEFAULT_JPEG_QUALITY);
    }

    public static byte[] compressToBytes(BitmapRef bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
        bitmap.compress(CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }
    
}
