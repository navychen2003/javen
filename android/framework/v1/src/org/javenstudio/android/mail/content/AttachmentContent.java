package org.javenstudio.android.mail.content;

import org.javenstudio.mail.content.MessageAttachmentData;

public interface AttachmentContent extends MessageAttachmentData {

	// Bits used in mFlags
    // Instruct Rfc822Output to 1) not use Content-Disposition and 2) use multipart/alternative
    // with this attachment.  This is only valid if there is one and only one attachment and
    // that attachment has this flag set
    public static final int FLAG_ICS_ALTERNATIVE_PART = 1<<0;
    
    public static final String CONTENTURI_NULL = "NIL"; 
    
	
	public long getId(); 
	public long getAccountKey(); 
	public long getMessageKey(); 
	
	public int getFlags(); 
	public String getMimeType(); 
	public String getFileName(); 
	public long getSize(); 
	
	public String getContentId(); 
	public String getContentType(); 
	public String getContentTransferEncoding(); 
	
	public String getLocation(); 
	public String getEncoding(); 
	public byte[] getContentBytes(); 
	public String getContentUri(); 
	
	public AttachmentContent startUpdate(); 
    public void commitUpdates(); 
    public void commitDelete(); 
	
    public void setAccountKey(long messageId); 
    public void setMessageKey(long messageId); 
    
    public void setFlags(int flags); 
    public void setMimeType(String type); 
    public void setFileName(String name); 
    public void setSize(long size); 
    
    public void setContentId(String id); 
    public void setContentType(String type); 
    public void setContentTransferEncoding(String encoding); 
    
    public void setLocation(String location); 
    public void setEncoding(String encoding); 
    public void setContentUri(String uri); 
    
}
