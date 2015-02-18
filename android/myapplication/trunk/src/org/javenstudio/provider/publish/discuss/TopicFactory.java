package org.javenstudio.provider.publish.discuss;

public class TopicFactory {
	
	public TopicDataSets createTopicDataSets(TopicProvider p) { 
		return new TopicDataSets(new TopicCursorFactory());
	}
	
	public TopicBinder createTopicBinder(TopicProvider p) { 
		return new TopicBinder(p);
	}
	
}
