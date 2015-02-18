package org.javenstudio.cocoka.graphics;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;

import org.javenstudio.cocoka.util.Utilities;

//@SuppressWarnings({"unused"})
public class BitmapUtil {

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
    public static Bitmap rotate(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees,
                    (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(
                        b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            }
        }
        return b;
    }
	
	public static Bitmap createBitmap(Context context, byte[] data) {
    	if (context == null || data == null || data.length == 0) 
    		return null; 
    	
    	try {
	    	Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length); 
	    	if (bitmap != null) 
	    		bitmap.setDensity(Utilities.getDensityDpi(context)); 
	    	
	    	//Bitmap newbitmap = createScaledBitmap(bitmap, context, 120, 150);
	    	//if (newbitmap != bitmap && bitmap != null) 
	    	//	bitmap.recycle(); 
	    	
	    	return bitmap; 
    	} catch (OutOfMemoryError e) {
    		return null; 
    	}
    }
	
	public static Bitmap createBitmap(Context context, InputStream is) {
    	if (context == null || is == null) 
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

	    	Bitmap bitmap = BitmapFactory.decodeStream(is, null, opt); 
	    	if (bitmap != null) 
	    		bitmap.setDensity(Utilities.getDensityDpi(context)); 
	    	
	    	return bitmap; 
    	} catch (IOException e) {
    		return null; 
    	} catch (OutOfMemoryError e) {
    		return null; 
    	}
    }
	
	public static Bitmap createBitmap(Context context, Drawable d) {
		return createBitmap(context, d, 0, 0); 
	}
	
	public static Bitmap createBitmap(Context context, Drawable d, int width, int height) {
    	if (context == null || d == null) 
    		return null; 
    	
    	try {
    		int bitmapWidth = width <= 0 ? d.getIntrinsicWidth() : width; 
    		int bitmapHeight = height <= 0 ? d.getIntrinsicHeight() : height; 
    		
    		final Bitmap output = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
	    	final Canvas canvas = new Canvas(output);
    		
	    	final Rect savedRect = d.getBounds(); 
	    	d.setBounds(0, 0, bitmapWidth, bitmapHeight); 
	    	d.draw(canvas); 
	    	d.setBounds(savedRect); 
	    	
    		return output; 
    	} catch (OutOfMemoryError e) {
    		return null; 
    	}
	}
	
	public static Bitmap createThumbBitmap(Context context, Bitmap bitmap) {
		int color = Color.TRANSPARENT; //THUMB_BAACKGROUND; 
		//color = Color.argb(THUMB_BACKGROUND_ALPHA, Color.red(color), Color.green(color), Color.blue(color)); 
		
		return createThumbBitmap(context, bitmap, color); 
	}
	
	public static Bitmap createThumbBitmap(Context context, Bitmap bitmap, int color) {
		if (context == null || bitmap == null) 
			return null; 
		
		final int thumbWidth = Utilities.getDisplaySize(context, HDPI_THUMB_ICON_WIDTH); 
		final int thumbHeight = Utilities.getDisplaySize(context, HDPI_THUMB_ICON_HEIGHT); 
		
		final int maxBitmapWidth = (int)(thumbWidth * HDPI_THUMB_ICON_SCALE); 
		final int maxBitmapHeight = (int)(thumbHeight * HDPI_THUMB_ICON_SCALE); 
		
		Bitmap scaledBitmap = createRatioScaledBitmap(context, bitmap, maxBitmapWidth, maxBitmapHeight); 
		if (scaledBitmap == null) 
			return null; 
		
		final int bitmapWidth = scaledBitmap.getWidth(); 
		final int bitmapHeight = scaledBitmap.getHeight(); 
		
		if (thumbWidth <= bitmapWidth || thumbHeight <= bitmapHeight)
			return scaledBitmap; 
		
		Bitmap thumb = scaledBitmap; 
		
		try {
			final int strockWidth = HDPI_THUMB_STROCKWIDTH; 
			
			final float bitmapLeft = (float)(thumbWidth - bitmapWidth) / 2.0f; 
			final float bitmapTop  = (float)(thumbHeight - bitmapHeight) / 2.0f; 
			
			final float left = bitmapLeft - strockWidth; 
			final float right = bitmapLeft + bitmapWidth + strockWidth; 
			final float top = bitmapTop - strockWidth; 
			final float bottom = bitmapTop + bitmapHeight + strockWidth; 
			
			Bitmap thumbTmp = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_4444); 
			Canvas canvas = new Canvas(thumbTmp);
	        
			Paint paint = new Paint(); 
			canvas.drawColor(color);
			
			paint.setColor(Color.DKGRAY); 
			paint.setStyle(Paint.Style.FILL); 
			//canvas.drawRoundRect(new RectF(left, top, right, bottom), 2, 2, paint); 
			canvas.drawRect(left, top, right, bottom, paint); 
			
			canvas.drawBitmap(scaledBitmap, bitmapLeft, bitmapTop, paint); 
			
			thumb = thumbTmp; 
			thumb.setDensity(bitmap.getDensity()); 
			
			if (scaledBitmap != bitmap && scaledBitmap != null)
				scaledBitmap.recycle(); 
			
		} catch (OutOfMemoryError e) {
			// ignore
		}
		
		return thumb; 
	}
	
	public static Bitmap createSmallBitmap(Context context, Bitmap bitmap) {
		if (context == null || bitmap == null) 
			return null; 
		
		final int thumbWidth = Utilities.getDisplaySize(context, HDPI_SMALL_PREVIEW_WIDTH); 
		final int thumbHeight = Utilities.getDisplaySize(context, HDPI_SMALL_PREVIEW_HEIGHT); 
		
		final int maxBitmapWidth = (int)(thumbWidth); 
		final int maxBitmapHeight = (int)(thumbHeight); 
		
		Bitmap scaledBitmap = createRatioScaledBitmap(context, bitmap, maxBitmapWidth, maxBitmapHeight); 
		if (scaledBitmap == null) 
			return bitmap; 
		
		return scaledBitmap; 
	}
	
	public static Bitmap createPreviewBitmap(Context context, byte[] data) {
		Bitmap bitmap = createBitmap(context, data); 
		return createPreviewBitmap(context, bitmap); 
	}
	
	public static Bitmap createPreviewBitmap(Context context, InputStream is) {
		Bitmap bitmap = createBitmap(context, is); 
		return createPreviewBitmap(context, bitmap); 
	}
	
	public static Bitmap createPreviewBitmap(Context context, Bitmap bitmap) {
		final int screenWidth = Utilities.getScreenWidth(context); 
		final int screenHeight = Utilities.getScreenHeight(context); 
		
		final int maxScaleWidth = (int)((float)screenWidth * HDPI_PREVIEW_BITMAP_SCALE); 
		final int maxScaleHeight = (int)((float)screenHeight * HDPI_PREVIEW_BITMAP_SCALE); 
		
		final int maxWidth = maxScaleWidth < maxScaleHeight ? maxScaleWidth : maxScaleHeight; 
		final int maxHeight = maxWidth; 
		
		return createPreviewBitmap(context, bitmap, maxWidth, maxHeight);
	}
	
	public static Bitmap createPreviewBitmap(Context context, Bitmap bitmap, int maxWidth, int maxHeight) {
		if (context == null || bitmap == null) 
			return bitmap; 
		
		//Bitmap savedBitmap = bitmap; 
		
		bitmap = createRatioScaledBitmap(context, bitmap, maxWidth, maxHeight); 
		
		//if (savedBitmap != bitmap && savedBitmap != null) 
        //	savedBitmap.recycle(); 
		
		return bitmap; 
	}
	
	public static Bitmap createRatioScaledBitmap(Context context, Bitmap bitmap, int maxWidth, int maxHeight) {
		if (context == null || bitmap == null) 
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
        	bitmap = createScaledBitmap(context, bitmap, width, height); 
        
		return bitmap; 
	}

	public static Bitmap createScaledBitmap(Context context, Bitmap bitmap, int width, int height) {
    	return createScaledBitmap(context, bitmap, width, height, 0, false); 
    }
	
	public static Bitmap createScaledBitmap(Context context, Bitmap bitmap, int width, int height, int density) {
    	return createScaledBitmap(context, bitmap, width, height, density, false);
    }
    
	public static Bitmap createScaledBitmap(Context context, Bitmap bitmap, int width, int height, int density, boolean forceCreate) {
		return createScaledBitmap(context, bitmap, width, height, density, FITTYPE_NONE, forceCreate); 
	}
	
	public static Bitmap createZoomOutScaledBitmap(Context context, Bitmap bitmap, int width, int height) {
		return createScaledBitmap(context, bitmap, width, height, 0, FITTYPE_ZOOMOUT, false); 
	}
	
	public static Bitmap createZoomOutScaledBitmap(Context context, Bitmap bitmap, int width, int height, int density) {
		return createScaledBitmap(context, bitmap, width, height, density, FITTYPE_ZOOMOUT, false); 
	}
	
	public static Bitmap createZoomInScaledBitmap(Context context, Bitmap bitmap, int width, int height) {
		return createScaledBitmap(context, bitmap, width, height, 0, FITTYPE_ZOOMIN, false); 
	}
	
	public static Bitmap createZoomInScaledBitmap(Context context, Bitmap bitmap, int width, int height, int density) {
		return createScaledBitmap(context, bitmap, width, height, density, FITTYPE_ZOOMIN, false); 
	}
	
	public static int FITTYPE_NONE = 0; 
	public static int FITTYPE_ZOOMOUT = 1; 
	public static int FITTYPE_ZOOMIN = 2; 
	
    public static Bitmap createScaledBitmap(Context context, Bitmap bitmap, int width, int height, int density, int fitType, boolean forceCreate) {
    	if (bitmap == null || context == null || width <= 0 || height <= 0) 
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
        	Bitmap endImage = Bitmap.createScaledBitmap(bitmap, actualWidth, actualHeight, true);
        	if (targetDensity > 0 && endImage != null)
        		endImage.setDensity(targetDensity);
        	
        	return endImage;
        } catch (OutOfMemoryError e) {
            return bitmap;
        }
    }
	
    public static Bitmap createShaderBitmap(Context context, Bitmap bitmap, int color) {
    	if (bitmap == null || context == null) 
    		return bitmap; 
    	
    	int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        
    	int actualWidth = bitmapWidth; 
        int actualHeight = bitmapHeight; 
        
        int startX = 0; 
        int startY = 0; 
        
        actualWidth += startX * 2;
        actualHeight += startY * 2; 
        
        Bitmap original = createScaledBitmap(context, bitmap, actualWidth, actualHeight, 0, true); 
        Canvas canvas = new Canvas(original);
        canvas.setBitmap(original);
        
        Paint paint = new Paint(); 
        Shader saveShader = paint.getShader(); 
        Xfermode saveMode = paint.getXfermode(); 
        
        LinearGradient shader = new LinearGradient(actualWidth/2, 0, actualHeight/2, actualHeight,
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
        	Bitmap endImage = Bitmap.createScaledBitmap(original, actualWidth, actualHeight, true);
        	if (endImage != null) 
        		endImage.setDensity(bitmap.getDensity());
        	
        	if (original != bitmap)
        		original.recycle();
        	
        	return endImage;
        } catch (OutOfMemoryError e) {
            return bitmap;
        }
    }
    
	public static Bitmap createGrayBitmap(Bitmap bitmap) {
        if (bitmap == null) return bitmap; 
        
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
	        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565); 
	        result.setPixels(pix, 0, w, 0, 0, w, h); 
	        
	        return result; 
        } catch (OutOfMemoryError e) {
        	return bitmap; 
        }
    }
	
    public static Bitmap createRoundedCornerBitmap(Context context, Bitmap bitmap, float roundPixels) {
    	return createRoundedCornerBitmap(context, bitmap, 0, roundPixels); 
    }
    
    public static Bitmap createRoundedCornerBitmap(Context context, Bitmap bitmap, int frameWidth, float roundPixels) {
    	if (context == null || bitmap == null) 
    		return bitmap; 
    	
    	final int width = bitmap.getWidth(); 
    	final int height = bitmap.getHeight(); 
    	
    	if (frameWidth < 0 || frameWidth >= width / 2 || frameWidth >= height / 2) 
    		frameWidth = 0; 
    	
    	try {
	    	Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
	    	Canvas canvas = new Canvas(output);
	
	    	final int color = 0xFF424242;
	    	final float roundPx = roundPixels;
	    	
	    	final Paint paint = new Paint();
	    	final Rect rect = new Rect(frameWidth, frameWidth, width-frameWidth, height-frameWidth);
	    	final RectF rectF = new RectF(rect);
	    	
	    	paint.setAntiAlias(true);
	    	canvas.drawARGB(0, 0, 0, 0); 
	    	
	    	paint.setColor(color);
	    	canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
	
	    	paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	    	canvas.drawBitmap(bitmap, rect, rect, paint);
	    	
	    	output.setDensity(bitmap.getDensity()); 
	    	
	    	return output; 
	    	
    	} catch (OutOfMemoryError e) {
        	return bitmap; 
        }
    }
    
    public static Bitmap createScaledCropBitmap(Context context, Bitmap bitmap, int width, int height) {
    	return createScaledCropBitmap(context, bitmap, width, height, -1, -1); 
    }
    
    public static Bitmap createScaledCropBitmap(Context context, Bitmap bitmap, int width, int height, int startX, int startY) {
    	return createScaledCropBitmap(context, bitmap, width, height, startX, startY, 0); 
    }
    
    public static Bitmap createScaledCropBitmap(Context context, Bitmap bitmap, int width, int height, int startX, int startY, int density) {
    	return createScaledCropFrameBitmap(context, bitmap, width, height, startX, startY, width, height, density); 
    }
    
    public static Bitmap createCenterScaledCropFrameBitmap(Context context, Bitmap bitmap, int width, int height, int outputWidth, int outputHeight) {
    	return createScaledCropFrameBitmap(context, bitmap, width, height, -1, -1, outputWidth, outputHeight, 0); 
    }
    
    public static Bitmap createScaledCropFrameBitmap(Context context, Bitmap bitmap, int width, int height, int startX, int startY, int outputWidth, int outputHeight) {
    	return createScaledCropFrameBitmap(context, bitmap, width, height, startX, startY, outputWidth, outputHeight, 0); 
    }
    
    public static Bitmap createScaledCropFrameBitmap(Context context, Bitmap bitmap, int width, int height, int startX, int startY, int outputWidth, int outputHeight, int density) {
    	return createScaledCropFrameBitmap(context, bitmap, width, height, startX, startY, outputWidth, outputHeight, density, true);
    }
    
    public static Bitmap createScaledCropFrameBitmap(Context context, Bitmap bitmap, int width, int height, int startX, int startY, int outputWidth, int outputHeight, int density, boolean autoScale) {
    	if (context == null || bitmap == null) 
    		return bitmap; 
    	
    	final float bitmapWidth = bitmap.getWidth(); 
    	final float bitmapHeight = bitmap.getHeight(); 
    	
    	float inputWidth = Utilities.getDisplaySize(context, width); 
    	float inputHeight = Utilities.getDisplaySize(context, height); 
    	
    	if (inputWidth <= 0) inputWidth = bitmapWidth; 
    	if (inputHeight <= 0) inputHeight = bitmapHeight; 
    	
    	float actualWidth = Utilities.getDisplaySize(context, outputWidth); 
    	float actualHeight = Utilities.getDisplaySize(context, outputHeight); 
    	
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
	    	Bitmap output = Bitmap.createBitmap((int)actualWidth, (int)actualHeight, Bitmap.Config.ARGB_8888);
	    	Canvas canvas = new Canvas(output);
	    	
	    	final Paint paint = new Paint();
	    	final Rect rectSrc = new Rect((int)left, (int)top, (int)right, (int)bottom);
	    	final RectF rectDst = new RectF(leftDst, topDst, rightDst, bottomDst);

	    	canvas.drawBitmap(bitmap, rectSrc, rectDst, paint);
	    	
	    	output.setDensity(defaultDensity); 
	    	
	    	return output; 
	    	
    	} catch (OutOfMemoryError e) {
        	return bitmap; 
        }
    }
    
    protected static Bitmap drawBubbleText(Context context, Bitmap bitmap, String text) {
    	return drawBubbleText(context, bitmap, text, 0); 
    }
    
    protected static Bitmap drawBubbleText(Context context, Bitmap bitmap, String text, int density) {
    	if (context == null || bitmap == null) 
    		return bitmap; 
    	
    	final int bitmapWidth = bitmap.getWidth(); 
    	final int bitmapHeight = bitmap.getHeight(); 
    	
		int defaultDensity = bitmap.getDensity(); 
        //int targetDensity = density <= 0 ? defaultDensity : density; 
    	
    	try {
    		final Bitmap output = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
	    	final Canvas canvas = new Canvas(output);
	    	final Paint paint = new Paint();
	    	
	    	canvas.drawBitmap(bitmap, 0, 0, paint); 
	    	
	    	final RectF rect = new RectF(10, 10, 60, 50); 
	    	paint.setColor(Color.RED); 
	    	canvas.drawRoundRect(rect, 20, 20, paint); 
	    	
	    	paint.setColor(Color.YELLOW); 
	    	paint.setTextSize(12); 
	    	canvas.drawText(text, 20, 20, paint); 
	    	
	    	output.setDensity(defaultDensity); 
	    	
	    	return output; 
	    	
    	} catch (OutOfMemoryError e) {
        	return bitmap; 
        }
    }
    
    public static Bitmap drawFaceBitmaps(Context context, int width, int height, Bitmap... bitmaps) {
    	if (context == null) return null; 
    	
    	final int bitmapWidth = width; 
    	final int bitmapHeight = height; 
    	
    	if (bitmapWidth <= 0 || bitmapHeight <= 0) 
    		return null; 
    	
    	Bitmap bitmapFrame = null; 
    	Bitmap bitmap1 = null; 
    	Bitmap bitmap2 = null; 
    	Bitmap bitmap3 = null; 
    	Bitmap bitmap4 = null; 
    	
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
    		final Bitmap output = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
	    	final Canvas canvas = new Canvas(output);
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
	    	
	    	return output; 
	    	
    	} catch (OutOfMemoryError e) {
        	return null; 
        }
    }
    
    private static void drawFaceBitmap(Canvas canvas, Bitmap bitmap, Paint paint, float startX, float startY, float width, float height) {
    	if (canvas != null && paint != null && bitmap != null) {
    		final Rect rectSrc = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()); 
    		final RectF rectDst = new RectF(startX, startY, width, height); 
    		
    		canvas.drawBitmap(bitmap, rectSrc, rectDst, paint); 
    	}
    }
    
    public static Bitmap createScaledCropBitmap(Context context, Bitmap bitmap, 
			int inWidth, int inHeight, int inFrameWidth) {
		if (context == null || bitmap == null) 
			return null; 
		
		final int frameWidth = Utilities.getDisplaySize(context, inFrameWidth * 2);
		final int bitmapWidth = Utilities.getDisplaySize(context, inWidth); 
		final int bitmapHeight = Utilities.getDisplaySize(context, inHeight); 
		final int cropWidth = bitmapWidth - frameWidth; 
		final int cropHeight = bitmapHeight - frameWidth; 
		
		int inputWidth = bitmap.getWidth(); 
		int inputHeight = bitmap.getHeight(); 
		
		Bitmap savedInput = bitmap; 
		
		if (inputWidth < bitmapWidth || inputHeight < bitmapHeight) 
			bitmap = BitmapUtil.createZoomInScaledBitmap(context, bitmap, bitmapWidth, bitmapHeight); 
		
		Bitmap output = BitmapUtil.createCenterScaledCropFrameBitmap(context, bitmap, 
				cropWidth, cropHeight, bitmapWidth, bitmapHeight); 
		
		if (bitmap != null && savedInput != bitmap && output != bitmap) 
			bitmap.recycle(); 
		
		return output; 
	}
    
}
