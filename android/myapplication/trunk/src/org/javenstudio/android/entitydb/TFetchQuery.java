package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class TFetchQuery extends SQLiteEntityDB.TQuery<TFetch> {

	public TFetchQuery() {
		this(-1); 
	}
	
	public TFetchQuery(long uploadId) {
		super(TDefaultDB.getDatabase(), TFetch.class); 
		
		if (uploadId > 0) 
			whereAnd(newEqualsClause(TFetch.Table._ID, uploadId)); 
	}
	
	public void setStatus(int status) {
		whereAnd(newEqualsClause(TFetch.Table.STATUS, status));
	}
	
	public void setContentName(String name) {
		whereAnd(newEqualsClause(TFetch.Table.CONTENT_NAME, name));
	}
	
	public void setContentUri(String uri) {
		whereAnd(newEqualsClause(TFetch.Table.CONTENT_URI, uri));
	}
	
	public static TFetch queryFetch(long id) { 
		return TDefaultDB.queryEntity(TFetch.class, id);
	}
	
}
