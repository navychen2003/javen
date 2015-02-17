package org.javenstudio.mail.example;

import org.javenstudio.mail.OutputBody;
import org.javenstudio.mail.Preference;
import org.javenstudio.mail.internet.MimeUtility;
import org.javenstudio.mail.transport.Transport;

public class MailPreference extends Preference {

	private final String mDeviceUID = "UID" + System.currentTimeMillis();
	private final Transport.SocketFactory mDefaultFactory = new MailSocketFactory();
	private final Transport.SocketFactory mSecureFactory = new MailSSLSocketFactory();
	
	public MailPreference() {}
	
	@Override
	public Transport.SocketFactory getSecureSocketFactory(boolean insecure) { 
		return mSecureFactory;
	}
	
	@Override
	public Transport.SocketFactory getSocketFactory() { 
		return mDefaultFactory;
	}
	
	@Override
	public String makeCommonImapId() { 
		String imapId = makeImapId(
				"com.leadtone.examples.mail", "1.0", 
        		"examples", "test", "debug", "leadtone", 
        		"network"); 
		
		return imapId;
	}
	
	@Override
    public String getDeviceUID() {
		return mDeviceUID;
	}
	
	@Override
    public MimeUtility.OutputBodyFactory getMimeOutputBodyFactory() { 
		return new MimeUtility.OutputBodyFactory() {
				@Override
				public OutputBody createDefaultOutputBody() {
					return new MailTempFileBody();
				}
			};
	}
	
}
