package org.javenstudio.provider.publish;

import org.javenstudio.provider.publish.discuss.TopicFactory;
import org.javenstudio.provider.publish.discuss.TopicProvider;

public class BaseTopicProvider extends TopicProvider {

	public BaseTopicProvider(String name, int iconRes, 
			TopicFactory factory) { 
		super(name, iconRes, factory);
	}
	
}
