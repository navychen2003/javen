package org.javenstudio.falcon.datum.table;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.datum.table.store.DBConstants;
import org.javenstudio.falcon.datum.table.store.DBLog;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;

public class TableManager implements IDatabase {
	private static final Logger LOG = Logger.getLogger(TableManager.class);

	private final IDatabase.Manager mManager;
	private final Configuration mConf;
	private final FileSystem mFs;
	
	private Path mStoreDir = null;
	private DBLog mLog = null;
	
	private final Map<Integer, TableRegion> mTables = 
			new HashMap<Integer, TableRegion>();
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return TableManager.this.getManager().getLock();
			}
			@Override
			public String getName() {
				return "TableManager(" + TableManager.this.getManager().getUserName() + ")";
			}
		};
	
	public TableManager(IDatabase.Manager manager) throws ErrorException { 
		if (manager == null) throw new NullPointerException();
		mManager = manager;
		mConf = manager.getConfiguration();
		mFs = manager.getDatabaseStore().getDatabaseFs();
		mStoreDir = mManager.getDatabasePath();
	}
	
	public IDatabase.Manager getManager() { return mManager; }
	public ILockable.Lock getLock() { return mLock; }
	
	public Configuration getConf() { return mConf; }
	public FileSystem getFs() { return mFs; }
	public Path getStoreDir() { return mStoreDir; }
	
	public synchronized DBLog getLog() throws ErrorException { 
		if (mLog == null) {
			getLock().lock(ILockable.Type.WRITE, null);
			try {
				try { 
					Path dbdir = mStoreDir;
					Path logdir = new Path(dbdir, DBConstants.DBREGION_LOGDIR_NAME); 
					Path oldLogDir = new Path(dbdir, DBConstants.DBREGION_OLDLOGDIR_NAME);
					
					mLog = new DBLog(mFs, logdir, oldLogDir, mConf, null);
				} catch (IOException e) { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
				}
			} finally {
				getLock().unlock(ILockable.Type.WRITE);
			}
		}
		return mLog; 
	}
	
	@Override
	public synchronized Table getTable(TableInfo info) 
			throws ErrorException { 
		if (info == null) throw new NullPointerException();
		
		TableRegionInfo regionInfo = (TableRegionInfo)info;
		Integer mapKey = Bytes.mapKey(regionInfo.getTableName());
		
		synchronized (mTables) { 
			TableRegion region = mTables.get(mapKey);
			if (region == null) { 
				if (LOG.isDebugEnabled())
					LOG.debug("getTable: new table: " + info);
				
				getLock().lock(ILockable.Type.WRITE, null);
				try {
					region = new TableRegion(this, regionInfo, mapKey);
					mTables.put(mapKey, region);
				} finally {
					getLock().unlock(ILockable.Type.WRITE);
				}
			}
			
			return region;
		}
	}
	
	@Override
	public synchronized boolean closeTable(TableInfo info) 
			throws ErrorException { 
		if (info == null) throw new NullPointerException();
		
		TableRegionInfo regionInfo = (TableRegionInfo)info;
		Integer mapKey = Bytes.mapKey(regionInfo.getTableName());
		
		synchronized (mTables) { 
			TableRegion region = mTables.get(mapKey);
			if (region != null) { 
				region.close();
				return true;
			}
			return false;
		}
	}
	
	synchronized void removeTable(TableRegion table) 
			throws IOException, ErrorException { 
		if (table == null) return;
		if (mTables.get(table.getKey()) != table) { 
			throw new IllegalArgumentException("TableRegion: " 
					+ table.getTableInfo().getTableNameAsString() 
					+ " (" + table.getKey() + ") is wrong");
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("removeTable: close table: " + table);
		
		synchronized (mTables) {
			table.closeRegion();
			mTables.remove(table.getKey());
		}
	}
	
	@Override
	public synchronized int getOpenedCount() { 
		synchronized (mTables) {
			return mTables.size();
		}
	}
	
	@Override
	public synchronized void syncLog(boolean force) throws ErrorException { 
		getLock().lock(ILockable.Type.WRITE, null);
		try {
			//try {
				if (mLog != null) mLog.sync(force);
			//} catch (IOException e) { 
			//	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			//}
		} finally {
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	@Override
	public synchronized void close() {
		if (LOG.isDebugEnabled()) LOG.debug("close");
		
		try {
			getLock().lock(ILockable.Type.WRITE, null);
			try {
				synchronized (mTables) {
					TableRegion[] tables = mTables.values().toArray(
							new TableRegion[mTables.size()]);
					
					if (tables != null) {
						for (TableRegion table : tables) { 
							try {
								if (table != null) table.close();
							} catch (Throwable e) { 
								if (LOG.isErrorEnabled()) {
									LOG.error("close: close table: " 
											+ table.getTableInfo().getTableNameAsString() 
											+ " error: " + e, e);
								}
							}
						}
					}
					
					mTables.clear();
				}
				
				try {
				  if (mLog != null) mLog.closeAndDelete();
				  mLog = null;
				} catch (IOException e) { 
					if (LOG.isWarnEnabled())
						LOG.warn("close: error: " + e, e);
				}
			} finally { 
				getLock().unlock(ILockable.Type.WRITE);
			}
		} catch (ErrorException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("close: error: " + e, e);
		}
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{storeDir=" + getStoreDir() 
				+ ",openedCount=" + getOpenedCount() + "}";
	}
	
}
