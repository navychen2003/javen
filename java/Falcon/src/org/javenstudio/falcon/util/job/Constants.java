package org.javenstudio.falcon.util.job;

final class Constants {

	public static final int MIN_POOL_SIZE = 2; 
	public static final int MAX_POOL_SIZE = 100; 
	
	public static final int KEEPALIVE_SECONDS = 20; 
	
	public static final long MAX_IDLE_TIME = 10 * 60 * 1000; // 10 min
	
	public static final long WORKERTASK_RETAIN_TIME = 30 * 60 * 1000;
	
	public static final String THREAD_PREFIX = "queue"; 
	
}
