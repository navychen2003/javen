package org.javenstudio.falcon.datum.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.javenstudio.common.util.Logger;

public class ImageUtils {
	private static final Logger LOG = Logger.getLogger(ImageUtils.class);

	public static String[] getThumbnailCacheKeys(String key) { 
		if (key == null || key.length() == 0) return null;
		
		Set<String> list = new HashSet<String>();
		String[] suffixs = new String[] {"", "t"};
		
		for (String suffix : suffixs) { 
			addThumbnailKeys(list, key, suffix);
			addThumbnailKeys(list, key + "_s", suffix);
		}
		
		return list.toArray(new String[list.size()]);
	}
	
	private static void addThumbnailKeys(Collection<String> list, 
			String key, String suffix) { 
		if (key == null || suffix == null) return;
		
		for (int i=0; i < sSizeList.length; i++) {
			for (int j=i; j < sSizeList.length; j++) {
				int sizeW = sSizeList[i];
				int sizeH = sSizeList[j];
				addThumbnailKeys(list, key, suffix, sizeW, sizeH);
			}
		}
	}
	
	private static void addThumbnailKeys(Collection<String> list, 
			String key, String suffix, int width, int height) { 
		if (key == null || suffix == null) return;
		list.add(key + "_" + width + suffix);
		if (width != height) {
			list.add(key + "_" + height + suffix);
			list.add(key + "_" + width + "x" + height + suffix);
			list.add(key + "_" + height + "x" + width + suffix);
		}
	}
	
	private static final int[] sSizeList = new int[]{ 
			64, 128, 192, 256, 512, 1024, 2048
		};
	
	public static int normalizeSize(int size) { 
		if (size <= 0) size = 0;
		else if (size <= 64) size = 64;
		else if (size <= 128) size = 128;
		else if (size <= 192) size = 192;
		else if (size <= 256) size = 256;
		else if (size <= 512) size = 512;
		else if (size <= 1024) size = 1024;
		else if (size <= 2048) size = 2048;
		else size = 4096;
		return size;
	}
	
	public static BufferedImage readImage(InputStream input) throws IOException { 
		return ImageIO.read(input);
	}
	
	public static void writeImage(RenderedImage image, String format, 
			OutputStream output) throws IOException { 
		ImageIO.write(image, format, output);
	}
	
	public static int[] readImageSize(InputStream input) throws IOException { 
		try { 
			BufferedImage image = ImageUtils.readImage(input);
			if (image != null) { 
				return new int[] { 
						image.getWidth(), image.getHeight() 
					};
			}
		} finally { 
			if (input != null) input.close();
		}
		
		return new int[] { 0, 0 };
	}
	
	public static int[] resizeImage(InputStream input, 
			OutputStream output, String format, 
			int width, int height, boolean fillRect, boolean trimRect) 
			throws IOException {
		if (input == null || output == null) 
			return null;
		
		BufferedImage inputImage = readImage(input);
		BufferedImage outputImage = resizeImage(inputImage, width, height, fillRect, trimRect);
		
		int[] widthHeight = null; 
		if (outputImage != null) 
			widthHeight = new int[]{outputImage.getWidth(), outputImage.getHeight()};
		else 
			widthHeight = new int[]{width, height};
		
		writeImage(outputImage, format, output);
		
		return widthHeight;
	}
	
	private static BufferedImage resizeImage(BufferedImage input, 
			int width, int height, boolean fillRect, boolean trimRect) 
			throws IOException {
		return resizeImage(input, width, height, null, fillRect, trimRect);
	}
	
	private static BufferedImage resizeImage(BufferedImage input, 
			final int outputWidth, final int outputHeight, Color bg, 
			boolean fillRect, boolean trimRect) 
			throws IOException {
		BufferedImage result = input;
		if (outputWidth <= 0 || outputHeight <= 0 || input == null) 
			return result;
		
        final int inputWidth = input.getWidth();
        final int inputHeight = input.getHeight();
        
        if (LOG.isDebugEnabled()) { 
        	LOG.debug("resizeImage: input=" + inputWidth + "x" + inputHeight 
        			+ " output=" + outputWidth + "x" + outputHeight 
        			+ " fill=" + fillRect + " trim=" + trimRect);
        }
        
    	if (outputWidth == outputHeight) {
    		if (inputHeight > outputHeight || inputWidth > outputWidth) {
	        	final double ratio;
	        	
	        	if (fillRect) { 
	        		if (inputHeight < inputWidth) 
		                ratio = (double)outputHeight / (double)inputHeight;
		            else 
		                ratio = (double)outputWidth / (double)inputWidth;
	        	} else {
		            if (inputHeight > inputWidth) 
		                ratio = (double)outputHeight / (double)inputHeight;
		            else 
		                ratio = (double)outputWidth / (double)inputWidth;
	        	}
	        	
	        	if (ratio > 0) {
		            AffineTransformOp op = new AffineTransformOp(
		            		AffineTransform.getScaleInstance(ratio, ratio), null);
		            
		            result = op.filter(input, null);
	        	}
    		}
    	} else { 
    		final double ratioInput = (double)inputWidth / (double)inputHeight;
    		final double ratioOutput = (double)outputWidth / (double)outputHeight;
    		
    		final double ratio;
    		if (ratioInput >= ratioOutput) { 
    			if (fillRect) {
    				if (inputHeight > outputHeight) 
        				ratio = (double)outputHeight / (double)inputHeight;
        			else
        				ratio = 0;
    			} else if (inputWidth > outputWidth) { 
    				ratio = (double)outputWidth / (double)inputWidth;
    			} else
    				ratio = 0;
    		} else { 
    			if (fillRect) {
    				if (inputWidth > outputWidth) 
        				ratio = (double)outputWidth / (double)inputWidth;
        			else
        				ratio = 0;
    			} else if (inputHeight > outputHeight) { 
    				ratio = (double)outputHeight / (double)inputHeight;
    			} else
    				ratio = 0;
    		}
    		
    		if (ratio > 0) {
    			AffineTransformOp op = new AffineTransformOp(
	            		AffineTransform.getScaleInstance(ratio, ratio), null);
	            
	            result = op.filter(input, null);
    		}
    	}
        
        // else { 
        //	result = (BufferedImage) input.getScaledInstance(width, height, 
        //			Image.SCALE_SMOOTH);
        //}
    	
    	if (fillRect && trimRect) { 
    		final Image itemp = result;
    		final int tempWidth = itemp.getWidth(null);
    		final int tempHeight = itemp.getHeight(null);
    		
            if (LOG.isDebugEnabled()) {
            	LOG.debug("resizeImage: trim, temp=" + tempWidth + "x" + tempHeight 
            			+ " output=" + outputWidth + "x" + outputHeight);
            }
            
    		if (tempWidth > outputWidth || tempHeight > outputHeight) { 
    			int dx = tempWidth - outputWidth;
        		int dy = tempHeight - outputHeight;
        		
        		int width = outputWidth;
        		int height = outputHeight;
        		
        		if (dx < 0) { 
        			width = tempWidth;
        			dx = 0;
        		}
        		
        		if (dy < 0) { 
        			height = tempHeight;
        			dy = 0;
        		}
        		
        		if (width != tempWidth || height != tempHeight) {
	    			BufferedImage image = new BufferedImage(width, height,
	                        BufferedImage.TYPE_INT_RGB);
	                
	                Graphics2D g = image.createGraphics();
	                //g.setColor(Color.WHITE);
	                //g.fillRect(0, 0, outputWidth, outputHeight);
	    			
	                g.drawImage(itemp, -dx/2, -dy/2, tempWidth, tempHeight, null);
	                
	                g.dispose(); 
	                result = image;
        		}
    		}
    	}
    	
        if (bg != null) {
        	final Image itemp = result;
        	
            BufferedImage image = new BufferedImage(outputWidth, outputHeight,
                    BufferedImage.TYPE_INT_RGB);
            
            Graphics2D g = image.createGraphics();
            g.setColor(bg);
            g.fillRect(0, 0, outputWidth, outputHeight);
            
            if (outputWidth == itemp.getWidth(null)) {
                g.drawImage(itemp, 0, (outputHeight - itemp.getHeight(null)) / 2, 
                		itemp.getWidth(null), itemp.getHeight(null), 
                		bg, null); 
                
            } else {
                g.drawImage(itemp, (outputWidth - itemp.getWidth(null)) / 2, 0, 
                		itemp.getWidth(null), itemp.getHeight(null), 
                		bg, null);
            }
            
            g.dispose(); 
            result = image;
        }
        
        return result;
    }
	
	public static int[] createImage(OutputStream output, String format, 
			int width, int height, String title) throws IOException {
		return createImage(output, format, width, height, title, null);
	}
	
	public static int[] createImage(OutputStream output, String format, 
			int width, int height, String title, String subtitle) throws IOException {
		if (output == null) return null;
		
		BufferedImage outputImage = createImage(width, height, title, subtitle);
		
		int[] widthHeight = null; 
		if (outputImage != null) 
			widthHeight = new int[]{outputImage.getWidth(), outputImage.getHeight()};
		else 
			widthHeight = new int[]{width, height};
		
		writeImage(outputImage, format, output);
		
		return widthHeight;
	}
	
	public static BufferedImage createImage(int width, int height, 
			String title) throws IOException {
		return createImage(width, height, title, null, null, null);
	}
	
	public static BufferedImage createImage(int width, int height, 
			String title, String subtitle) throws IOException {
		return createImage(width, height, title, subtitle, null, null);
	}
	
	public static BufferedImage createImage(int width, int height, 
			String title, String subtitle, Color textColor, 
			Color backgroundColor) throws IOException {
		if (width <= 0 || height <= 0) 
			return null;
		
		if (textColor == null)
			textColor = Color.BLACK;
		if (backgroundColor == null)
			backgroundColor = Color.WHITE;
		
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        
        Graphics2D g = image.createGraphics();
        g.setColor(backgroundColor);
        g.fillRect(0, 0, width, height);
        
        if (title != null || subtitle != null) { 
        	int titleSize = 20, subtitleSize = 14;
        	if (width <= 150) {
        		titleSize = 12;
        		subtitleSize = 9;
        		
        	} else if (width <= 300) { 
        		titleSize = 18;
        		subtitleSize = 14;
        	}
        	
        	Font fontOrig = g.getFont();
        	
        	if (title != null && title.length() > 0) {
        		Font font = new Font(fontOrig.getFontName(), Font.BOLD, titleSize); 
        		String text = title;
        		
	        	g.setFont(font);
	        	g.setColor(textColor);
	        	
	        	FontMetrics fm = g.getFontMetrics(font);
	        	
	        	int stringWidth = fm.stringWidth(text); 
	        	//int stringHeight = fm.getHeight();
	        	//int stringAscent = fm.getAscent(); 
	        	//int stringDescent = fm.getDescent(); 
	        	int x = width / 2 - stringWidth / 2; 
	        	int y = height / 2; // + (stringAscent - stringDescent) / 2;
	        	
	        	if (x < 0) x = 0;
	        	if (y < 0) y = 0;
	        	
	        	g.drawString(text, x, y);
        	}
        	
        	if (subtitle != null && subtitle.length() > 0) { 
        		Font font = new Font(fontOrig.getFontName(), Font.PLAIN, subtitleSize); 
        		String text = subtitle;
        		
	        	g.setFont(font);
	        	g.setColor(textColor);
	        	
	        	FontMetrics fm = g.getFontMetrics(font);
	        	
	        	int stringWidth = fm.stringWidth(text); 
	        	//int stringHeight = fm.getHeight();
	        	int stringAscent = fm.getAscent(); 
	        	int stringDescent = fm.getDescent(); 
	        	int x = width / 2 - stringWidth / 2; 
	        	int y = height / 2 + stringAscent + (stringAscent - stringDescent) / 2;
	        	
	        	if (x < 0) x = 0;
	        	if (y < 0) y = 0;
	        	
	        	g.drawString(text, x, y);
        	}
        }
        
        g.dispose(); 
        return image;
    }
	
}
