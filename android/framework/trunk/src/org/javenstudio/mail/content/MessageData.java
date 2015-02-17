package org.javenstudio.mail.content;

public interface MessageData {

	public String getFrom(); 
	public String getTo(); 
	public String getCc(); 
	public String getBcc(); 
	public String getReplyTo(); 
	
	public long getTimeStamp(); 
	public String getMessageId(); 
	public String getSubject(); 
	
	public boolean isReplyMessage(); 
	public boolean isForwardMessage(); 
	
	public MessageBodyData getMessageBody();
	public MessageAttachmentData[] getMessageAttachments();
	
}
