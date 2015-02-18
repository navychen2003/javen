package org.javenstudio.cocoka.net.http.download;

import org.javenstudio.cocoka.net.metrics.IMetricsFormater;
import org.javenstudio.cocoka.net.metrics.IProgressUpdater;
import org.javenstudio.cocoka.net.metrics.IMetricsUpdater;
import org.javenstudio.cocoka.net.metrics.MetricsContext;

public class DownloadFormater implements IMetricsFormater {

	public DownloadFormater() {} 
	
	@Override 
	public void format(IMetricsUpdater updater, MetricsContext context) {
		if (updater == null || context == null) 
			return; 
		
		if (updater instanceof IProgressUpdater && context instanceof DownloadContext) {
			IProgressUpdater pd = (IProgressUpdater)updater; 
			DownloadContext dc = (DownloadContext)context; 
			
			if (dc.isFinished()) {
				pd.setProgress(0); 
				pd.setProgressInformation(null); 
				
			} else { 
				pd.setProgress(dc.getProgress()); 
				pd.setProgressInformation(formatInformation(dc)); 
			}
			
			pd.refreshProgress(); 
		}
	}
	
	protected String formatInformation(DownloadContext context) { 
		return context.getInformation(); 
	}
	
}
