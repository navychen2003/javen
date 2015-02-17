package org.javenstudio.cocoka.net.metrics;

import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB;

public class TMetrics extends SimpleMemoryDB.TEntity {

	public static class Table extends SimpleMemoryDB.TTable<TMetrics> {
		public static final String TABLE_NAME = "metrics"; 
		public static final String FIELD_NAME = "name"; 
		public static final String FIELD_DESCRIPTION = "description"; 
		
		public Table() { 
			super(TABLE_NAME, TMetrics.class);
		}
	}
	
	public String name = null; 
	public String description = null; 
	
	public TMetrics() { 
		super(); 
	}
	
	public TMetrics(long id) {
		super(id); 
	}
	
}
