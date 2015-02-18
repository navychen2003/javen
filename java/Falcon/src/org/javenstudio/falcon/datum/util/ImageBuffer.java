package org.javenstudio.falcon.datum.util;

public final class ImageBuffer {
	
	public final byte[] data;
	public final int offset;
	public final int length;
	public final int width;
	public final int height;
	
	public ImageBuffer(byte[] data, int offset, int length, 
			int width, int height) { 
		this.data = data;
		this.offset = offset;
		this.length = length;
		this.width = width;
		this.height = height;
	}
	
	public static byte[] encode(ImageBuffer buf) { 
		byte[] buffer = new byte[8+buf.length-buf.offset];
		encodeInt(buffer, 0, buf.width);
		encodeInt(buffer, 4, buf.height);
		System.arraycopy(buf.data, buf.offset, buffer, 8, buf.length);
		return buffer;
	}
	
	public static ImageBuffer decode(byte[] buffer, int offset, int length) { 
		int width = decodeInt(buffer, 0+offset);
		int height = decodeInt(buffer, 4+offset);
		byte[] buf = new byte[length-8];
		System.arraycopy(buffer, 8+offset, buf, 0, length-8);
		return new ImageBuffer(buf, 0, buf.length, width, height);
	}
	
	private static void encodeInt(byte[] buffer, int offset, int num) { 
		for (int i=0; i < 4; i++) { 
			buffer[i+offset] = (byte)((num >> (8*i)) & 0xff);
		}
	}
	
	private static int decodeInt(byte[] buffer, int offset) { 
		int num = 0;
		for (int i=0; i < 4; i++) { 
			num += (buffer[i+offset] & 0xff) << (8*i);
		}
		return num;
	}
	
}
