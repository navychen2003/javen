package org.javenstudio.falcon.util.job;

public class TaskQueueHelper {

	private static final AdvancedQueueFactory sFactory = new AdvancedQueueFactory();
	
	public static AdvancedQueueFactory getDefaultAdvancedQueueFactory() { 
		return sFactory;
	}
	
	public static AdvancedQueueFactory getDefaultFactory() {
		return getDefaultAdvancedQueueFactory();
	}

}
