package org.javenstudio.cocoka.net.metrics;

public interface IMetricsUpdater {

	public void setMetricsFormater(IMetricsFormater formater); 
	public boolean updateMetrics(MetricsContext context); 
	
}
