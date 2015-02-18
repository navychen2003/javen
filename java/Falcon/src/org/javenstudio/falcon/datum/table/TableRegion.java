package org.javenstudio.falcon.datum.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.datum.table.store.DBRegion;
import org.javenstudio.falcon.datum.table.store.DBTableDescriptor;
import org.javenstudio.falcon.datum.table.store.Delete;
import org.javenstudio.falcon.datum.table.store.Get;
import org.javenstudio.falcon.datum.table.store.InternalScanner;
import org.javenstudio.falcon.datum.table.store.KeyValue;
import org.javenstudio.falcon.datum.table.store.Put;
import org.javenstudio.falcon.datum.table.store.Result;
import org.javenstudio.falcon.datum.table.store.Scan;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.fs.Path;

public class TableRegion implements IDatabase.Table {
	private static final Logger LOG = Logger.getLogger(TableRegion.class);

	private final TableManager mManager;
	private final TableRegionInfo mInfo;
	private final DBRegion mRegion;
	private final Integer mKey;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return TableRegion.this.getDatabase().getLock();
			}
			@Override
			public String getName() {
				return "Table(" + TableRegion.this.getTableName() + ")";
			}
		};
	
	TableRegion(TableManager manager, TableRegionInfo info, 
			Integer key) throws ErrorException { 
		if (manager == null || info == null || key == null) 
			throw new NullPointerException();
		mManager = manager;
		mInfo = info;
		mKey = key;
		
		Path tableDir = DBTableDescriptor.getTableDir(manager.getStoreDir(), 
				info.getRegionInfo().getTableDesc().getName());
		
		mRegion = DBRegion.newDBRegion(tableDir, 
				manager.getLog(), manager.getFs(), manager.getConf(), 
				info.getRegionInfo(), null);
		
		if (mRegion == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"TableRegion: " + info.getTableNameAsString() + " init error");
		}
		
		try {
			mRegion.initialize();
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public TableManager getDatabase() { return mManager; }
	public ILockable.Lock getLock() { return mLock; }
	
	public TableRegionInfo getTableInfo() { return mInfo; }
	public DBRegion getRegion() { return mRegion; }
	public Integer getKey() { return mKey; }
	
	@Override
	public String getTableName() { 
		return getTableInfo().getTableNameAsString();
	}
	
	@Override
	public synchronized void close() throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("close: " + getTableName());
		
		synchronized (mManager) {
			try {
				mManager.removeTable(this);
			} catch (IOException e) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		}
	}

	@Override
	public synchronized void flush(boolean syncLog) throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("flush: " + getTableName());
		
		getLock().lock(ILockable.Type.WRITE, null);
		try {
			synchronized (mManager) {
				try {
					flushRegion();
					if (syncLog) mManager.syncLog(true);
				} catch (IOException e) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
				}
			}
		} finally {
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	synchronized void flushRegion() throws IOException, ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("flushRegion: " + getTableName());
		
		getLock().lock(ILockable.Type.WRITE, null);
		try {
			if (getRegion().flushcache()) 
				getRegion().compactStores();
		} finally {
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	synchronized void closeRegion() throws IOException, ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("closeRegion: " + getTableName());
		
		getLock().lock(ILockable.Type.WRITE, null);
		try {
			flushRegion();
			getRegion().close();
		} finally {
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{key=" + mKey 
				+ ",name=" + getTableName() + ",region=" + mRegion + "}";
	}
	
	@Override
	public TableRow newRow(byte[] rowKey) throws ErrorException { 
		if (rowKey == null) throw new NullPointerException();
		if (rowKey.length == 0) throw new IllegalArgumentException("Row key is empty");
		
		return new TableRow(this, rowKey);
	}
	
	@Override
	public void update(IDatabase.Row row) throws ErrorException { 
		if (row == null) throw new NullPointerException();
		
		getLock().lock(ILockable.Type.WRITE, ILockable.Check.TOP);
		try {
			TableRow tr = (TableRow)row;
			Put put = tr.getPut();
			if (put == null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"No columns in the row");
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("update: table=" + getTableInfo().getTableNameAsString() 
						+ " row=" + put);
			}
			
			try {
				getRegion().put(put);
			} catch (IOException e) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		} finally {
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	@Override
	public void delete(IDatabase.Row row) throws ErrorException { 
		if (row == null) throw new NullPointerException();
		
		getLock().lock(ILockable.Type.WRITE, ILockable.Check.TOP);
		try {
			TableRow tr = (TableRow)row;
			Delete del = new Delete(tr.getKey());
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("delete: table=" + getTableInfo().getTableNameAsString() 
						+ " delete=" + del);
			}
			
			try {
				getRegion().delete(del, null, true);
			} catch (IOException e) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		} finally {
			getLock().unlock(ILockable.Type.WRITE);
		}
	}

	@Override
	public IDatabase.Query newQuery() throws ErrorException {
		return new TableQuery(this);
	}

	@Override
	public TableResult get(IDatabase.Query query) throws ErrorException {
		if (query == null) throw new NullPointerException();
		
		getLock().lock(ILockable.Type.READ, null);
		try {
			TableQuery tq = (TableQuery)query;
			try { 
				Get get = tq.getGet();
				if (get == null) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"No get defined in the query");
				}
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("get: table=" + getTableInfo().getTableNameAsString() 
							+ " get=" + get);
				}
				
				Result res = getRegion().get(get, null);
				if (res != null) 
					return new TableResult(this, res);
				
			} catch (IOException e) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
				
			}
		} finally {
			getLock().unlock(ILockable.Type.READ);
		}
		
		return null;
	}

	@Override
	public List<IDatabase.Result> query(IDatabase.Query query) 
			throws ErrorException {
		if (query == null) throw new NullPointerException();
		
		getLock().lock(ILockable.Type.READ, null);
		try {
			TableQuery tq = (TableQuery)query;
			
			ArrayList<IDatabase.Result> list = new ArrayList<IDatabase.Result>();
			InternalScanner scanner = null;
			
			try {
				Scan scan = tq.getScan();
				if (scan == null) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"No scan defined in the query");
				}
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("query: table=" + getTableInfo().getTableNameAsString() 
							+ " scan=" + scan);
				}
				
				scanner = getRegion().getScanner(scan);
				
				List<KeyValue> kvs = new ArrayList<KeyValue>();
				boolean done = false;
				do {
					kvs.clear();
					done = scanner.next(kvs);
					
					if (kvs.size() > 0) { 
						KeyValue[] vals = kvs.toArray(new KeyValue[kvs.size()]);
						byte[] key = vals[0].getKey();
						
						TableResult result = new TableResult(this, key, vals);
						list.add(result);
					}
				} while (done);
			} catch (IOException e) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
				
			} finally {
				try {
					if (scanner != null) 
						scanner.close();
				} catch (IOException e) { 
					if (LOG.isDebugEnabled())
						LOG.debug("query: error: " + e, e);
				}
			}
			
			return list;
		} finally {
			getLock().unlock(ILockable.Type.READ);
		}
	}
	
}
