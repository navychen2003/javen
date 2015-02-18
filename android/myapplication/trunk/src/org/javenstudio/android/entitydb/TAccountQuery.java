package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class TAccountQuery extends SQLiteEntityDB.TQuery<TAccount> {

	public TAccountQuery() {
		this(-1); 
	}
	
	public TAccountQuery(long accountId) {
		super(TDefaultDB.getDatabase(), TAccount.class); 
		
		if (accountId > 0) 
			whereAnd(newEqualsClause(TAccount.Table._ID, accountId)); 
	}
	
	public void setFlag(int flag) {
		whereAnd(newEqualsClause(TAccount.Table.FLAG, flag));
	}
	
	public void setStatus(int status) {
		whereAnd(newEqualsClause(TAccount.Table.STATUS, status));
	}
	
	public void setUsername(String name) {
		whereAnd(newEqualsClause(TAccount.Table.USERNAME, name));
	}
	
	public void setEmail(String name) {
		whereAnd(newEqualsClause(TAccount.Table.EMAIL, name));
	}
	
	public void setPrefix(String prefix) {
		whereAnd(newEqualsClause(TAccount.Table.PREFIX, prefix));
	}
	
	public static TAccount queryAccount(long id) { 
		return TDefaultDB.queryEntity(TAccount.class, id);
	}
	
}
