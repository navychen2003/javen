package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class TDownloadQuery extends SQLiteEntityDB.TQuery<TDownload> {

	public TDownloadQuery() {
		this(-1); 
	}
	
	public TDownloadQuery(long downloadId) {
		super(TDefaultDB.getDatabase(), TDownload.class); 
		
		if (downloadId > 0) 
			whereAnd(newEqualsClause(TDownload.Table._ID, downloadId)); 
	}
	
	public void setStatus(int status) {
		whereAnd(newEqualsClause(TDownload.Table.STATUS, status));
	}
	
	public void setContentName(String name) {
		whereAnd(newEqualsClause(TDownload.Table.CONTENT_NAME, name));
	}
	
	public void setContentUri(String uri) {
		whereAnd(newEqualsClause(TDownload.Table.CONTENT_URI, uri));
	}
	
	public void setSourcePrefix(String prefix) {
		whereAnd(newEqualsClause(TDownload.Table.SOURCE_PREFIX, prefix));
	}
	
	public void setSourceAccount(String account) {
		whereAnd(newEqualsClause(TDownload.Table.SOURCE_ACCOUNT, account));
	}
	
	public void setSourceAccountId(String accountId) {
		whereAnd(newEqualsClause(TDownload.Table.SOURCE_ACCOUNTID, accountId));
	}
	
	public static TDownload queryDownload(long id) { 
		return TDefaultDB.queryEntity(TDownload.class, id);
	}
	
}
