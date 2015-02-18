package org.javenstudio.falcon.user.auth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.raptor.io.Writable;

abstract class Buffer implements Writable {

	public static void readBuffer(Buffer data, byte[] buffer, int offset, 
			int length) throws ErrorException { 
		ByteArrayInputStream bais = new ByteArrayInputStream(buffer, offset, length);
		DataInputStream dis = new DataInputStream(bais);
		try { 
			data.readFields(dis);
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		} finally { 
			try {
				bais.close();
			} catch (Throwable ignore) {
			}
		}
	}
	
	public static byte[] encode(Buffer buffer) throws ErrorException { 
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			buffer.write(dos);
			dos.flush();
			baos.flush();
			return baos.toByteArray();
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		} finally {
			try {
				dos.close();
				baos.close();
			} catch (Throwable ignore) {
			}
		}
	}
	
}
