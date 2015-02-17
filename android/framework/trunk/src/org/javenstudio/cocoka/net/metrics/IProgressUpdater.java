package org.javenstudio.cocoka.net.metrics;

public interface IProgressUpdater extends IMetricsUpdater {

	public void setProgress(float p); 
	public void setProgressInformation(String text); 
	public void refreshProgress(); 
	
	public void invalidate(); 
	public void postInvalidate(); 
	public void requestLayout(); 
	
}
