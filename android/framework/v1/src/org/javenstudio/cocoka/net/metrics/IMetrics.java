package org.javenstudio.cocoka.net.metrics;

public interface IMetrics {

	  public String getMetricsName(); 
	
	  /**
	   * Sets the named metric to the specified value.
	   *
	   * @param metricName name of the metric
	   * @param metricValue new value of the metric
	   * @throws MetricsException if the metricName or the type of the metricValue 
	   * conflicts with the configuration
	   */
	  public void setMetric(String metricName, int type, String metricValue);
	
	  /**
	   * Sets the named metric to the specified value.
	   *
	   * @param metricName name of the metric
	   * @param metricValue new value of the metric
	   * @throws MetricsException if the metricName or the type of the metricValue 
	   * conflicts with the configuration
	   */
	  public void setMetric(String metricName, int type, int metricValue);
	    
	  /**
	   * Sets the named metric to the specified value.
	   *
	   * @param metricName name of the metric
	   * @param metricValue new value of the metric
	   * @throws MetricsException if the metricName or the type of the metricValue 
	   * conflicts with the configuration
	   */
	  public void setMetric(String metricName, int type, long metricValue);
	    
	  /**
	   * Sets the named metric to the specified value.
	   *
	   * @param metricName name of the metric
	   * @param metricValue new value of the metric
	   * @throws MetricsException if the metricName or the type of the metricValue 
	   * conflicts with the configuration
	   */
	  public void setMetric(String metricName, int type, short metricValue);
	    
	  /**
	   * Sets the named metric to the specified value.
	   *
	   * @param metricName name of the metric
	   * @param metricValue new value of the metric
	   * @throws MetricsException if the metricName or the type of the metricValue 
	   * conflicts with the configuration
	   */
	  //public void setMetric(String metricName, int type, byte metricValue);
	    
	  /**
	   * Sets the named metric to the specified value.
	   *
	   * @param metricName name of the metric
	   * @param metricValue new value of the metric
	   * @throws MetricsException if the metricName or the type of the metricValue 
	   * conflicts with the configuration
	   */
	  public void setMetric(String metricName, int type, float metricValue);
	    
	  /**
	   * Increments the named metric by the specified value.
	   *
	   * @param metricName name of the metric
	   * @param metricValue incremental value
	   * @throws MetricsException if the metricName or the type of the metricValue 
	   * conflicts with the configuration
	   */
	  public void incrMetric(String metricName, int type, int metricValue);
	    
	  /**
	   * Increments the named metric by the specified value.
	   *
	   * @param metricName name of the metric
	   * @param metricValue incremental value
	   * @throws MetricsException if the metricName or the type of the metricValue 
	   * conflicts with the configuration
	   */
	  public void incrMetric(String metricName, int type, long metricValue);
	    
	  /**
	   * Increments the named metric by the specified value.
	   *
	   * @param metricName name of the metric
	   * @param metricValue incremental value
	   * @throws MetricsException if the metricName or the type of the metricValue 
	   * conflicts with the configuration
	   */
	  public void incrMetric(String metricName, int type, short metricValue);
	    
	  /**
	   * Increments the named metric by the specified value.
	   *
	   * @param metricName name of the metric
	   * @param metricValue incremental value
	   * @throws MetricsException if the metricName or the type of the metricValue 
	   * conflicts with the configuration
	   */
	  //public void incrMetric(String metricName, int type, byte metricValue);
	    
	  /**
	   * Increments the named metric by the specified value.
	   *
	   * @param metricName name of the metric
	   * @param metricValue incremental value
	   * @throws MetricsException if the metricName or the type of the metricValue 
	   * conflicts with the configuration
	   */
	  public void incrMetric(String metricName, int type, float metricValue);
	    
	  /**
	   * Updates the table of buffered data which is to be sent periodically.
	   * If the tag values match an existing row, that row is updated; 
	   * otherwise, a new row is added.
	   */
	  public void update();
	    
	  /**
	   * Removes, from the buffered data table, all rows having tags 
	   * that equal the tags that have been set on this record. For example,
	   * if there are no tags on this record, all rows for this record name
	   * would be removed.  Or, if there is a single tag on this record, then
	   * just rows containing a tag with the same name and value would be removed.
	   */
	  public void remove();
	
}
