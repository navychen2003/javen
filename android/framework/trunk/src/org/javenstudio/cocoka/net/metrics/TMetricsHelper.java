package org.javenstudio.cocoka.net.metrics;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.EntityException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB;
import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB.TCursor;
import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB.TEntity;
import org.javenstudio.common.entitydb.type.LongIdentity;

public class TMetricsHelper {

	public static TCursor<TMetrics> queryMetrics() { 
		return new MetricsQuery().query();
	}
	
	public static TCursor<TMetricsRecord> queryRecords() { 
		return queryRecords(-1);
	}
	
	public static TCursor<TMetricsRecord> queryRecords(long metricsId) { 
		return new RecordQuery(metricsId).query();
	}
	
	public static class MetricsQuery extends SimpleMemoryDB.TQuery<TMetrics> {

		public MetricsQuery() {
			super(TMetricsDB.getDatabase(), TMetrics.class); 
		}
		
		public void setMetricsName(String name) {
			if (name != null && name.length() > 0) 
				whereAnd(newEqualsClause(TMetrics.Table.FIELD_NAME, name)); 
		}
		
	}
	
	public static class RecordQuery extends SimpleMemoryDB.TQuery<TMetricsRecord> {

		public RecordQuery(long metricsId) {
			super(TMetricsDB.getDatabase(), TMetricsRecord.class); 
			
			if (metricsId != -1)
				whereAnd(newEqualsClause(TMetricsRecord.Table.FIELD_METRICSKEY, metricsId)); 
		}
		
		public void setRecordName(String name) {
			if (name != null && name.length() > 0) 
				whereAnd(newEqualsClause(TMetricsRecord.Table.FIELD_NAME, name)); 
		}
		
		public void setRecordType(int type) { 
			whereAnd(newEqualsClause(TMetricsRecord.Table.FIELD_TYPE, type)); 
		}
		
	}
	
	public static class MetricsUpdater extends SimpleMemoryDB.TUpdater {
		private TMetrics mMetrics = null; 
		private LongIdentity mMetricsIdentity = null; 
		
		public MetricsUpdater() {
			super(TMetricsDB.getDatabase()); 
		}
		
		public void commitUpdate() { 
			try { 
				saveOrUpdate();
			} catch (EntityException e) { 
				throw new DBException("update metrics error", e);
			}
		}
		
		@Override
		protected TEntity[] getEntities() {
			return new TEntity[]{ mMetrics }; 
		}
		
		@Override
		protected void onInserted(IEntity<?> data, IIdentity id) {
			onInsertOrUpdated(data, id);
		}
		
		@Override
		protected void onUpdated(IEntity<?> data, IIdentity id) {
			onInsertOrUpdated(data, id);
		}
		
		private void onInsertOrUpdated(IEntity<?> data, IIdentity id) {
			if (data == mMetrics && id != null && id instanceof LongIdentity) 
				mMetricsIdentity = (LongIdentity)id; 
		}
		
		public long getId() {
			LongIdentity id = getIdentity(); 
			return id != null ? id.longValue() : -1;
		}
		
		public LongIdentity getIdentity() {
			return mMetricsIdentity; 
		}
		
		private TMetrics getMetrics() {
			if (mMetrics == null) 
				mMetrics = new TMetrics(); 
			return mMetrics; 
		}
		
		public void setName(String name) {
			getMetrics().name = name; 
		}
		
		public void setDescription(String desc) {
			getMetrics().description = desc; 
		}
		
	}
	
	public static class RecordUpdater extends SimpleMemoryDB.TUpdater {
		private final TMetrics mMetrics; 
		private final TMetricsRecord mMetricsRecord; 
		private TMetricsRecord mUpdateRecord = null; 
		private LongIdentity mRecordIdentity = null; 
		
		public RecordUpdater(TMetrics metrics) {
			this(metrics, null); 
		}
		
		public RecordUpdater(TMetrics metrics, TMetricsRecord record) {
			super(TMetricsDB.getDatabase()); 
			mMetrics = metrics; 
			mMetricsRecord = record; 
		}
		
		public void commitUpdate() { 
			try { 
				saveOrUpdate();
			} catch (EntityException e) { 
				throw new DBException("update metrics error", e);
			}
		}
		
		@Override
		protected TEntity[] getEntities() {
			return new TEntity[]{ mUpdateRecord }; 
		}
		
		@Override
		protected void onInserted(IEntity<?> data, IIdentity id) {
			onInsertOrUpdated(data, id);
		}
		
		@Override
		protected void onUpdated(IEntity<?> data, IIdentity id) {
			onInsertOrUpdated(data, id);
		}
		
		private void onInsertOrUpdated(IEntity<?> data, IIdentity id) {
			if (data == mUpdateRecord && id != null && id instanceof LongIdentity) 
				mRecordIdentity = (LongIdentity)id; 
		}
		
		public long getId() {
			LongIdentity id = getIdentity(); 
			return id != null ? id.longValue() : -1;
		}
		
		public LongIdentity getIdentity() {
			return mRecordIdentity; 
		}
		
		private synchronized TMetricsRecord getRecord() {
			if (mUpdateRecord == null) {
				if (mMetricsRecord != null) {
					mUpdateRecord = new TMetricsRecord(mMetricsRecord.getId()); 
				} else {
					mUpdateRecord = new TMetricsRecord(); 
					mUpdateRecord.metricsKey = new Long(mMetrics.getId()); 
				}
			} 
			return mUpdateRecord; 
		}
		
		public void setName(String name) {
			getRecord().name = name; 
		}
		
		public void setType(int type) { 
			getRecord().type = type; 
		}
		
		private void setUpdateTime() {
			getRecord().updateTime = System.currentTimeMillis(); 
		}
		
		public void setShortValue(short value) {
			getRecord().shortValue = value; 
			setUpdateTime(); 
		}
		
		public void incrShortValue(short value) {
			if (mMetricsRecord != null && mMetricsRecord.shortValue != null) {
				value += mMetricsRecord.shortValue; 
			}
			setShortValue(value); 
		}
		
		public void setIntValue(int value) {
			getRecord().intValue = value; 
			setUpdateTime(); 
		}
		
		public void incrIntValue(int value) {
			if (mMetricsRecord != null && mMetricsRecord.intValue != null) {
				value += mMetricsRecord.intValue; 
			}
			setIntValue(value); 
		}
		
		public void setLongValue(long value) {
			getRecord().longValue = value; 
			setUpdateTime(); 
		}
		
		public void incrLongValue(long value) {
			if (mMetricsRecord != null && mMetricsRecord.longValue != null) {
				value += mMetricsRecord.longValue; 
			}
			setLongValue(value); 
		}
		
		public void setFloatValue(float value) {
			getRecord().floatValue = value; 
			setUpdateTime(); 
		}
		
		public void incrFloatValue(float value) {
			if (mMetricsRecord != null && mMetricsRecord.floatValue != null) {
				value += mMetricsRecord.floatValue; 
			}
			setFloatValue(value); 
		}
		
		public void setStringValue(String value) {
			getRecord().stringValue = value; 
			setUpdateTime(); 
		}
		
	}
	
}
