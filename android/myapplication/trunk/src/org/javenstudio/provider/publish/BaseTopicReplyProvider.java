package org.javenstudio.provider.publish;

import org.javenstudio.provider.publish.discuss.ReplyFactory;
import org.javenstudio.provider.publish.discuss.ReplyProvider;

public class BaseTopicReplyProvider extends ReplyProvider {

	public BaseTopicReplyProvider(String name, int iconRes, 
			ReplyFactory factory) { 
		super(name, iconRes, factory);
	}
	
}
