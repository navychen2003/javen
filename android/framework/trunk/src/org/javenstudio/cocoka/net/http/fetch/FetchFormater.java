package org.javenstudio.cocoka.net.http.fetch;

import org.javenstudio.cocoka.net.metrics.IMetricsFormater;
import org.javenstudio.cocoka.net.metrics.IProgressUpdater;
import org.javenstudio.cocoka.net.metrics.IMetricsUpdater;
import org.javenstudio.cocoka.net.metrics.MetricsContext;

public class FetchFormater implements IMetricsFormater {

	public FetchFormater() {} 
	
	@Override 
	public void format(IMetricsUpdater updater, MetricsContext context) {
		if (updater == null || context == null) 
			return; 
		
		if (updater instanceof IProgressUpdater && context instanceof FetchContext) {
			IProgressUpdater pd = (IProgressUpdater)updater; 
			FetchContext dc = (FetchContext)context; 
			
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
	
	protected String formatInformation(FetchContext context) { 
		return context.getInformation(); 
	}
	
}
