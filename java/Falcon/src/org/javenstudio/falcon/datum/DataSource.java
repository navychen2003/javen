package org.javenstudio.falcon.datum;

import java.io.IOException;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.util.job.JobContext;

public interface DataSource {

	public static interface Collector {
		public void addContentId(String contentId);
	}
	
	public IUser getUser();
	public String getMessage();
	
	public void process(DataJob job, JobContext jc, Collector collector) 
			throws IOException, ErrorException;
	
	public void close();
	
}
