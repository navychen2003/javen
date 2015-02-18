package org.javenstudio.mail.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.OutputBody;

public class MailTempFileBody implements OutputBody {

	@Override
	public InputStream getInputStream() throws MessagingException {
		return new ByteArrayInputStream(new byte[0]);
	}

	@Override
	public void writeTo(OutputStream out) throws IOException, MessagingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRequestStop() {
		return false;
	}

	@Override
	public void finishOutput(long count) {
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return new ByteArrayOutputStream();
	}

}
