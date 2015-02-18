package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class THostQuery extends SQLiteEntityDB.TQuery<THost> {

	public THostQuery() {
		this(-1); 
	}
	
	public THostQuery(long accountId) {
		super(TDefaultDB.getDatabase(), THost.class); 
		
		if (accountId > 0) 
			whereAnd(newEqualsClause(THost.Table._ID, accountId)); 
	}
	
	public void setFlag(int flag) {
		whereAnd(newEqualsClause(THost.Table.FLAG, flag));
	}
	
	public void setStatus(int status) {
		whereAnd(newEqualsClause(THost.Table.STATUS, status));
	}
	
	public void setPrefix(String prefix) {
		whereAnd(newEqualsClause(THost.Table.PREFIX, prefix));
	}
	
	public void setClusterId(String clusterId) {
		whereAnd(newEqualsClause(THost.Table.CLUSTERID, clusterId));
	}
	
	public void setClusterDomain(String clusterDomain) {
		whereAnd(newEqualsClause(THost.Table.CLUSTERDOMAIN, clusterDomain));
	}
	
	public void setHostKey(String hostKey) {
		whereAnd(newEqualsClause(THost.Table.HOSTKEY, hostKey));
	}
	
	public void setHostAddr(String hostAddr) {
		whereAnd(newEqualsClause(THost.Table.HOSTADDR, hostAddr));
	}
	
	public static THost queryHost(long id) { 
		return TDefaultDB.queryEntity(THost.class, id);
	}
	
}
