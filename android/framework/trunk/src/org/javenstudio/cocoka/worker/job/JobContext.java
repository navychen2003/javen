package org.javenstudio.cocoka.worker.job;

public interface JobContext {
	public boolean isCancelled();
	public void setCancelListener(JobCancelListener listener);
	public boolean setMode(int mode);
}
