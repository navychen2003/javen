package org.javenstudio.android.mail.content;

import org.javenstudio.mail.content.MessageBodyData;

public interface BodyContent extends MessageBodyData {

	public long getId(); 
	public long getMessageKey(); 
	
	public String getTextContent(); 
	public String getHtmlContent(); 
	public String getTextReply(); 
	public String getHtmlReply(); 
	public String getIntroText(); 
	
	public BodyContent startUpdate(); 
    public void commitUpdates(); 
    public void commitDelete(); 
	
    public void setMessageKey(long messageId); 
	
    public void setTextContent(String text); 
    public void setHtmlContent(String text); 
    public void setTextReply(String text); 
    public void setHtmlReply(String text); 
    public void setIntroText(String text); 
    
}
