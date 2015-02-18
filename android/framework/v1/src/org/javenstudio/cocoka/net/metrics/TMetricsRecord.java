package org.javenstudio.cocoka.net.metrics;

import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB;

public class TMetricsRecord extends SimpleMemoryDB.TEntity {

	public static class Table extends SimpleMemoryDB.TTable<TMetricsRecord> {
		public static final String TABLE_NAME = "metricsRecord"; 
		public static final String FIELD_METRICSKEY = "metricsKey"; 
		public static final String FIELD_NAME = "name"; 
		public static final String FIELD_TYPE = "type"; 
		
		public Table() { 
			super(TABLE_NAME, TMetricsRecord.class);
		}
	}
	
	public Long metricsKey = null; 
	public Long updateTime = null; 
	public String name = null; 
	public Integer type = null; 
	public Short shortValue = null; 
	public Integer intValue = null; 
	public Long longValue = null; 
	public Float floatValue = null; 
	public String stringValue = null; 
	
	public TMetricsRecord() { 
		super(); 
	}
	
	public TMetricsRecord(long id) {
		super(id); 
	}
	
}
