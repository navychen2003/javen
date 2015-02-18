package org.javenstudio.falcon.datum.table;

import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.datum.table.store.DBRegionInfo;

public class TableRegionInfo implements IDatabase.TableInfo {

	private final DBRegionInfo mInfo;
	
	public TableRegionInfo(DBRegionInfo info) { 
		if (info == null) throw new NullPointerException();
		mInfo = info;
	}
	
	public DBRegionInfo getRegionInfo() { 
		return mInfo;
	}
	
	@Override
	public byte[] getTableName() {
		return getRegionInfo().getRegionName();
	}

	@Override
	public String getTableNameAsString() { 
		return getRegionInfo().getRegionNameAsString();
	}
	
	@Override
	public long getTableId() { 
		return getRegionInfo().getRegionId();
	}
	
	@Override
	public String toString() { 
		return "Table{" + getRegionInfo().toString() + "}";
	}
	
}
