package org.javenstudio.falcon.message;

import java.io.IOException;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.job.JobContext;

public interface MessageSource {

	public String getMessage();
	public String getMessageDetails();
	
	public int getSentCount();
	
	public void process(MessageJob job, JobContext jc) 
			throws IOException, ErrorException;
	
	public void close();
	
}
