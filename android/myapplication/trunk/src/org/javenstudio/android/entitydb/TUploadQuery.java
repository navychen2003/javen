package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class TUploadQuery extends SQLiteEntityDB.TQuery<TUpload> {

	public TUploadQuery() {
		this(-1); 
	}
	
	public TUploadQuery(long uploadId) {
		super(TDefaultDB.getDatabase(), TUpload.class); 
		
		if (uploadId > 0) 
			whereAnd(newEqualsClause(TUpload.Table._ID, uploadId)); 
	}
	
	public void setStatus(int status) {
		whereAnd(newEqualsClause(TUpload.Table.STATUS, status));
	}
	
	public void setContentName(String name) {
		whereAnd(newEqualsClause(TUpload.Table.CONTENT_NAME, name));
	}
	
	public void setContentUri(String uri) {
		whereAnd(newEqualsClause(TUpload.Table.CONTENT_URI, uri));
	}
	
	public void setDestPrefix(String prefix) {
		whereAnd(newEqualsClause(TUpload.Table.DEST_PREFIX, prefix));
	}
	
	public void setDestAccount(String account) {
		whereAnd(newEqualsClause(TUpload.Table.DEST_ACCOUNT, account));
	}
	
	public void setDestAccountId(String accountId) {
		whereAnd(newEqualsClause(TUpload.Table.DEST_ACCOUNTID, accountId));
	}
	
	public static TUpload queryUpload(long id) { 
		return TDefaultDB.queryEntity(TUpload.class, id);
	}
	
}
