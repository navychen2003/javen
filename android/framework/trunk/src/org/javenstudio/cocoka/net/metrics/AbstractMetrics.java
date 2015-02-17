package org.javenstudio.cocoka.net.metrics;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.cocoka.net.metrics.TMetricsHelper.MetricsQuery;
import org.javenstudio.cocoka.net.metrics.TMetricsHelper.RecordQuery;
import org.javenstudio.cocoka.net.metrics.TMetricsHelper.RecordUpdater;
import org.javenstudio.cocoka.net.metrics.TMetricsHelper.MetricsUpdater;
import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB.TCursor;

public abstract class AbstractMetrics implements IMetrics {

	private final Map<String, WeakReference<TMetricsRecord>> mRecords; 
	private TMetrics mMetrics = null; 
	
	public AbstractMetrics() {
		mRecords = new HashMap<String, WeakReference<TMetricsRecord>>(); 
	} 
	
	private RecordUpdater getProvider(String recordName, int type) {
		synchronized (this) {
			if (mMetrics == null) {
				String metricsName = getMetricsName(); 
				if (metricsName == null || metricsName.length() == 0) 
					return null; 
				
				TMetrics metrics = null; 
				
				MetricsQuery query = new MetricsQuery(); 
				query.setMetricsName(metricsName); 
				TCursor<TMetrics> cursor = query.query(); 
				try {
					if (cursor.hasNext()) 
						metrics = cursor.next(); 
				} finally {
					cursor.close(); 
				}
				
				if (metrics == null) {
					MetricsUpdater p = new MetricsUpdater(); 
					p.setName(metricsName); 
					p.commitUpdate();
					
					metrics = TMetricsDB.queryEntity(TMetrics.class, p.getId()); 
				}
				
				mMetrics = metrics; 
			}
			
			if (mMetrics == null) 
				throw new RuntimeException("metrics entity is null"); 
			
			final String recordKey = recordName + "/" + type; 
			
			WeakReference<TMetricsRecord> recordRef = mRecords.get(recordKey); 
			TMetricsRecord record = null; 
			if (recordRef != null) 
				record = recordRef.get(); 
			
			if (record == null) {
				RecordQuery query = new RecordQuery(mMetrics.getId()); 
				query.setRecordName(recordName); 
				query.setRecordType(type); 
				TCursor<TMetricsRecord> cursor = query.query(); 
				try {
					if (cursor.hasNext()) { 
						record = cursor.next(); 
						if (record != null) {
							recordRef = new WeakReference<TMetricsRecord>(record); 
							mRecords.put(recordKey, recordRef); 
						}
					}
				} finally {
					cursor.close(); 
				}
				
				if (record == null) {
					RecordUpdater p = new RecordUpdater(mMetrics); 
					p.setName(recordName); 
					p.setType(type); 
					p.commitUpdate();
					
					record = TMetricsDB.queryEntity(TMetricsRecord.class, p.getId()); 
				}
				
				if (record == null) 
					throw new RuntimeException("metrics record entity is null"); 
			}
			
			return new RecordUpdater(mMetrics, record); 
		}
	}
	
	@Override 
	public void setMetric(String recordName, int type, String recordValue) {
		RecordUpdater p = getProvider(recordName, type); 
		if (p != null) { 
			//p.setType(type); 
			p.setStringValue(recordValue); 
			p.commitUpdate(); 
		}
	}
	
	@Override 
	public void setMetric(String recordName, int type, int recordValue) {
		RecordUpdater p = getProvider(recordName, type); 
		if (p != null) { 
			//p.setType(type); 
			p.setIntValue(recordValue); 
			p.commitUpdate(); 
		}
	}
	
	@Override 
	public void incrMetric(String recordName, int type, int recordValue) {
		RecordUpdater p = getProvider(recordName, type); 
		if (p != null) { 
			//p.setType(type); 
			p.incrIntValue(recordValue); 
			p.commitUpdate(); 
		}
	}
	
	@Override 
	public void setMetric(String recordName, int type, long recordValue) {
		RecordUpdater p = getProvider(recordName, type); 
		if (p != null) { 
			//p.setType(type); 
			p.setLongValue(recordValue); 
			p.commitUpdate(); 
		}
	}
	
	@Override 
	public void incrMetric(String recordName, int type, long recordValue) {
		RecordUpdater p = getProvider(recordName, type); 
		if (p != null) { 
			//p.setType(type); 
			p.incrLongValue(recordValue); 
			p.commitUpdate(); 
		}
	}
	
	@Override 
	public void setMetric(String recordName, int type, short recordValue) {
		RecordUpdater p = getProvider(recordName, type); 
		if (p != null) { 
			//p.setType(type); 
			p.setShortValue(recordValue); 
			p.commitUpdate(); 
		}
	}
	
	@Override 
	public void incrMetric(String recordName, int type, short recordValue) {
		RecordUpdater p = getProvider(recordName, type); 
		if (p != null) { 
			//p.setType(type); 
			p.incrShortValue(recordValue); 
			p.commitUpdate(); 
		}
	}
	
	@Override 
	public void setMetric(String recordName, int type, float recordValue) {
		RecordUpdater p = getProvider(recordName, type); 
		if (p != null) { 
			//p.setType(type); 
			p.setFloatValue(recordValue); 
			p.commitUpdate(); 
		}
	}
	
	@Override 
	public void incrMetric(String recordName, int type, float recordValue) {
		RecordUpdater p = getProvider(recordName, type); 
		if (p != null) { 
			//p.setType(type); 
			p.incrFloatValue(recordValue); 
			p.commitUpdate(); 
		}
	}
	
	@Override 
	public void update() {
		// do nothing
	}
	
	@Override 
	public void remove() {
		// do nothing
	}
}
