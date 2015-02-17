package org.javenstudio.cocoka.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.storage.fs.IFileSystem;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.util.Logger;

public class FileHelper {
	private static Logger LOG = Logger.getLogger(FileHelper.class);

	public static void saveFile(IFileSystem fs, IFile file, InputStream is, int bufferSize) {
		if (fs == null || file == null || is == null) 
			return; 
		
		if (bufferSize <= 0) 
			bufferSize = Storage.BUFFER_SIZE; 
		
		OutputStream out = null; 
		
		try {
			out = new BufferedOutputStream(fs.create(file), bufferSize); 
			
			byte[] buffer = new byte[bufferSize]; 
			int length = 0, bytes = 0; 
			while ((length = is.read(buffer)) >= 0) {
				if (length > 0) {
					out.write(buffer, 0, length); 
					bytes += length; 
				}
			}
			
			out.flush(); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("saved " + bytes + " bytes to file: " + file.getPath());
			
		} catch (Throwable e) {
			if (LOG.isErrorEnabled())
				LOG.error("save file error: " + file, e); 
			
		} finally {
			try {
				if (out != null) 
					out.close(); 
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	public static OutputStream createFile(IFileSystem fs, IFile file) {
		return createFile(fs, file, Storage.BUFFER_SIZE); 
	}
	
	public static OutputStream createFile(IFileSystem fs, IFile file, int bufferSize) {
		return createOrAppendFile(fs, file, bufferSize, false); 
	}
	
	public static OutputStream appendFile(IFileSystem fs, IFile file) {
		return appendFile(fs, file, Storage.BUFFER_SIZE); 
	}
	
	public static OutputStream appendFile(IFileSystem fs, IFile file, int bufferSize) {
		return createOrAppendFile(fs, file, bufferSize, true); 
	}
	
	private static OutputStream createOrAppendFile(IFileSystem fs, 
			IFile file, int bufferSize, boolean append) {
		if (fs == null || file == null) 
			return null; 
		
		if (bufferSize <= 0) 
			bufferSize = Storage.BUFFER_SIZE; 
		
		OutputStream out = null; 
		
		try {
			out = new BufferedOutputStream(append ? 
					fs.append(file) : fs.create(file), bufferSize); 
			
			if (LOG.isDebugEnabled())
				LOG.debug((append ? "append" : "created") + " file: " + file); 
			
		} catch (Throwable e) {
			out = null; 
			
			if (LOG.isErrorEnabled())
				LOG.error((append ? "append" : "create") + " file error: " + file, e); 
			
		} finally {
			// do nothing
		}
		
		return out; 
	}
	
	public static void saveBitmap(IFileSystem fs, IFile file, 
			BitmapRef bitmap, int bufferSize) {
		if (fs == null || file == null || bitmap == null) 
			return; 
		
		if (bufferSize <= 0) 
			bufferSize = Storage.BUFFER_SIZE; 
		
		OutputStream out = null; 
		
		try {
			String filename = file.getName(); 
			String extensionName = ""; 
			
			int pos = filename != null ? filename.lastIndexOf('.') : -1; 
			if (pos >= 0) 
				extensionName = filename.substring(pos+1); 
			
			String name = extensionName.toLowerCase(); 
			Bitmap.CompressFormat format = Bitmap.CompressFormat.PNG; 

			if ("jpg".equals(name) || "jpeg".equals(name)) 
				format = Bitmap.CompressFormat.JPEG; 
			else if ("png".equals(name))
				format = Bitmap.CompressFormat.PNG; 
			
			out = new BufferedOutputStream(fs.create(file), bufferSize); 
			bitmap.compress(format, 80, out); 
			out.flush(); 
			
		} catch (Throwable e) {
			if (LOG.isErrorEnabled())
				LOG.error("save picture error: " + file, e); 
			
		} finally {
			try {
				if (out != null) 
					out.close(); 
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	public static BitmapRef loadBitmap(BitmapHolder holder, 
			IFileSystem fs, IFile file, int bufferSize) {
		if (fs == null || file == null) 
			return null; 
		
		if (bufferSize <= 0) 
			bufferSize = Storage.BUFFER_SIZE; 
		
		BitmapRef bitmap = null; 
		InputStream is = null; 
		
		try {
			if (LOG.isDebugEnabled()) 
				LOG.debug("loadBitmap: " + file.getLocation() + " length=" + file.length());
			
			is = new BufferedInputStream(fs.open(file), bufferSize); 
			bitmap = createBitmap(holder, is); 
			
		} catch (java.io.FileNotFoundException e) {
			bitmap = null; 
			//LOG.warn("load picture failed not found: "+file); 
			
		} catch (Throwable e) {
			bitmap = null; 
			
			if (LOG.isErrorEnabled())
				LOG.error("load picture error: " + file, e); 
			
		} finally {
			try {
				if (is != null) 
					is.close(); 
			} catch (Exception ex) {
				// ignore
			}
		}
		
		return bitmap; 
	}
	
	static BitmapRef createBitmap(BitmapHolder holder, InputStream is) {
    	if (holder == null || is == null) 
    		return null; 
    	
    	try {
    		long length = is.available(); 
    		BitmapFactory.Options opt = BitmapRef.createBitmapOptions(length);
    		
    		if (LOG.isDebugEnabled()) 
				LOG.debug("createBitmap: length=" + length + " Options.inSampleSize=" + opt.inSampleSize);
    		
    		BitmapRef bitmap = BitmapRef.decodeStream(holder, is, null, opt); 
	    	if (bitmap != null) {
	    		bitmap.setDensity(Utilities.getDensityDpi(holder.getContext())); 
	    		//BitmapRefs.onBitmapCreated(bitmap);
	    	}
	    	
	    	return bitmap; 
    	} catch (IOException e) {
    		if (LOG.isErrorEnabled())
				LOG.error("load bitmap error: " + e.toString(), e); 
    		
    		return null; 
    		
    	} catch (OutOfMemoryError e) {
    		Utilities.handleOOM(e);
    		
    		return null; 
    	}
    }
	
	public static byte[] loadFile(IFileSystem fs, IFile file) {
		return loadFile(fs, file, Storage.BUFFER_SIZE); 
	}
	
	public static byte[] loadFile(IFileSystem fs, IFile file, int bufferSize) {
		if (fs == null || file == null) 
			return null; 
		
		if (file.length() >= Integer.MAX_VALUE) {
			if (LOG.isWarnEnabled())
				LOG.warn("file is too large to load: " + file); 
			
			return null; 
		}
		
		if (bufferSize <= 0) 
			bufferSize = Storage.BUFFER_SIZE; 
		
		InputStream is = null; 
		
		try {
			is = openFile(fs, file, bufferSize); 
			if (is != null) {
				byte[] content = new byte[(int)file.length()]; 
				if (is.read(content, 0, content.length) != file.length()) {
					if (LOG.isErrorEnabled())
						LOG.error("load file error: " + file); 
					
					return null; 
				}
				
				return content; 
			}
			
		} catch (Throwable e) {
			if (LOG.isErrorEnabled())
				LOG.error("load file error: " + file, e); 
			
		} finally {
			try {
				if (is != null) 
					is.close(); 
			} catch (Exception ex) {
				// ignore
			}
		}
		
		return null; 
	}
	
	public static InputStream openFile(IFileSystem fs, IFile file) {
		return openFile(fs, file, Storage.BUFFER_SIZE); 
	}
	
	public static InputStream openFile(IFileSystem fs, IFile file, int bufferSize) {
		if (fs == null || file == null) 
			return null; 
		
		if (bufferSize < 0) 
			bufferSize = Storage.BUFFER_SIZE; 
		
		InputStream is = null; 
		
		try {
			if (LOG.isDebugEnabled())
				LOG.debug("opening file: " + file + " bufferSize=" + bufferSize);
			
			if (bufferSize > 0) 
				is = new BufferedInputStream(fs.open(file), bufferSize); 
			else
				is = fs.open(file); 
			
		} catch (java.io.FileNotFoundException e) {
			is = null; 
			
			if (LOG.isDebugEnabled())
				LOG.debug("open file failed not found: " + file); 
			
		} catch (Exception e) {
			is = null; 
			
			if (LOG.isErrorEnabled())
				LOG.error("open file error: " + file, e); 
			
		} finally {
			// do nothing
		}
		
		return is; 
	}
	
}
