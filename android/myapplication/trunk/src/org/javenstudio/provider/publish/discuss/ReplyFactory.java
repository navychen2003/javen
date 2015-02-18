package org.javenstudio.provider.publish.discuss;

public class ReplyFactory {

	public ReplyDataSets createReplyDataSets(ReplyProvider p) { 
		return new ReplyDataSets(new ReplyCursorFactory());
	}
	
	public ReplyBinder createReplyBinder(ReplyProvider p) { 
		return new ReplyBinder(p);
	}
	
}
