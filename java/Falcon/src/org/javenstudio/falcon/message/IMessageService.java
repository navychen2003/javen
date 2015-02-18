package org.javenstudio.falcon.message;

import org.javenstudio.falcon.ErrorException;

public interface IMessageService {

	public MessageManager getManager();
	
	public String getType();
	public String[] getFolderNames();
	
	public IMessage.Builder newMessage(String folderName, String streamId) throws ErrorException;
	public IMessage.Builder modifyMessage(String messageId) throws ErrorException;
	
	public IMessageSet getMessages(IMessageQuery query) throws ErrorException;
	public IMessage getMessage(String messageId) throws ErrorException;
	
	public IMessage moveMessage(String messageId, String folderTo) throws ErrorException;
	public IMessage deleteMessage(String messageId) throws ErrorException;
	
	public IMessage postSend(IMessage.Builder builder) throws ErrorException;
	public IMessageSet getMessages(String streamId, String folderName) throws ErrorException;
	public IMessageSet getNewMessages() throws ErrorException;
	
	public void flushMessages() throws ErrorException;
	public void close();
	
}
