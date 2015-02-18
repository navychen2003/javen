package org.javenstudio.falcon.util.job;

public interface JobContext {
	public boolean isCancelled();
	public void setCancelListener(JobCancelListener listener);
	public boolean setMode(int mode);
}
