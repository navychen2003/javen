package org.javenstudio.falcon.datum.data;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.falcon.datum.IScreenshot;
import org.javenstudio.raptor.io.Text;
import org.javenstudio.raptor.io.Writable;

public class FileScreenshot implements Writable, IScreenshot {
	public static final int SIZE_4K = 4096;
	public static final int SIZE_FHD = 2048;
	public static final int SIZE_HD = 1024;
	public static final int SIZE_SD = 512;
	
	private Text mName;
	private Text mMimeType;
	private int mWidth;
	private int mHeight;
	private byte[] mBuffer;
	
	public FileScreenshot() {}
	
	public FileScreenshot(String name, String mimeType, 
			int width, int height, byte[] buffer) { 
		if (name == null || mimeType == null || width < 0 || height < 0 || buffer == null)
			throw new IllegalArgumentException("Input error");
		mName = new Text(name);
		mMimeType = new Text(mimeType);
		mWidth = width;
		mHeight = height;
		mBuffer = buffer;
	}
	
	public Text getName() { return mName; }
	public Text getMimeType() { return mMimeType; }
	public int getWidth() { return mWidth; }
	public int getHeight() { return mHeight; }
	
	public byte[] getBuffer() { return mBuffer; }
	public int getBufferSize() { 
		return mBuffer != null ? mBuffer.length : 0; 
	}
	
	public int getSize() { 
		return mWidth > mHeight ? mWidth : mHeight; 
	}
	
	public int getImageWidth() { return getWidth(); }
	public int getImageHeight() { return getHeight(); }
	public byte[] getImageBuffer() { return getBuffer(); }
	
	@Override
	public String getImageName() { 
		Text name = mName;
		return name != null ? name.toString() : null;
	}
	
	@Override
	public String getImageType() { 
		Text mimeType = mMimeType;
		return mimeType != null ? mimeType.toString() : null;
	}
	
	@Override
	public InputStream openImage() { 
		byte[] buffer = mBuffer;
		if (buffer != null && buffer.length > 0)
			return new ByteArrayInputStream(buffer);
		else
			return null;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		mName.write(out);
		mMimeType.write(out);
		out.writeInt(mWidth);
		out.writeInt(mHeight);
		out.writeInt(mBuffer != null ? mBuffer.length : 0);
		if (mBuffer != null && mBuffer.length > 0)
			out.write(mBuffer);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		mName = Text.read(in);
		mMimeType = Text.read(in);
		mWidth = in.readInt();
		mHeight = in.readInt();
		int len = in.readInt();
		if (len > 0) {
			byte[] buffer = new byte[len];
			in.readFully(buffer);
			mBuffer = buffer;
		} else
			mBuffer = new byte[0];
	}
	
	public static FileScreenshot read(DataInput in) throws IOException { 
		FileScreenshot data = new FileScreenshot();
		data.readFields(in);
		return data;
	}
	
}
