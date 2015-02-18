package org.javenstudio.cocoka.database.sqlite;

import java.util.Map;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IQuery;
import org.javenstudio.common.entitydb.nosql.SimpleCursor;

public class SQLiteEntityCursor<K extends IIdentity, T extends IEntity<K>> 
		extends SimpleCursor<K, T> implements Cursor {

	private DataSetObservable mDataSetObservable = new DataSetObservable();
    private ContentObservable mContentObservable = new ContentObservable();
	
	public SQLiteEntityCursor(SQLiteEntityTable<K,T> table, IQuery<K, T> query) {
		super(table, query);
	}
	
	@Override
	public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
		// Default implementation, uses getString
        String result = getString(columnIndex);
        if (result != null) {
            char[] data = buffer.data;
            if (data == null || data.length < result.length()) {
                buffer.data = result.toCharArray();
            } else {
                result.getChars(0, result.length(), data, 0);
            }
            buffer.sizeCopied = result.length();
        }
	}
	
	@Deprecated
    public boolean deleteRow() {
		remove(); return true; 
	}
	
	@Deprecated
    public boolean supportsUpdates() {
		return false; 
	}
	
	@Deprecated
    public boolean hasUpdates() {
		return false; 
	}
	
	@Deprecated
    public boolean updateBlob(int columnIndex, byte[] value) {
		return false; 
	}
	
	@Deprecated
    public boolean updateShort(int columnIndex, short value) {
		return false; 
	}
	
	@Deprecated
    public boolean updateInt(int columnIndex, int value) {
		return false; 
	}
	
	@Deprecated
    public boolean updateLong(int columnIndex, long value) {
		return false; 
	}
	
	@Deprecated
    public boolean updateFloat(int columnIndex, float value) {
		return false; 
	}
	
	@Deprecated
    public boolean updateDouble(int columnIndex, double value) {
		return false; 
	}
	
	@Deprecated
    public boolean updateToNull(int columnIndex) {
		return false; 
	}
	
	@Deprecated
    public boolean commitUpdates() {
		return false; 
	}
	
	@Deprecated
    public boolean commitUpdates(Map<? extends Long, ? extends Map<String,Object>> values) {
		return false; 
	}
	
	@Deprecated
    public void abortUpdates() {
		
	}
	
	public void deactivate() {
		close(); 
	}
	
	@Override
	public void notifyInvalidated() {
		super.notifyInvalidated(); 
		mDataSetObservable.notifyInvalidated(); 
	}
	
	@Override
	public void notifyEntitySetChange() {
		super.notifyEntitySetChange(); 
		mDataSetObservable.notifyChanged(); 
	}
	
	@Override
	public void notifyEntitiesChange(int count, int change) {
		super.notifyEntitiesChange(count, change); 
		mContentObservable.dispatchChange(false); 
	}
	
	@Override
	protected void onNotifyEntityChange(T data, int change, boolean queryMatch, boolean notifyMatch) {
		super.onNotifyEntityChange(data, change, queryMatch, notifyMatch); 
		mContentObservable.dispatchChange(false); 
	}
	
	@Override
	public void registerContentObserver(ContentObserver observer) {
		mContentObservable.registerObserver(observer);
	}
	
	@Override
	public void unregisterContentObserver(ContentObserver observer) {
		// cursor will unregister all observers when it close
        if (!isClosed()) {
            mContentObservable.unregisterObserver(observer);
        }
	}
	
	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		mDataSetObservable.registerObserver(observer);
	}
	
	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		mDataSetObservable.unregisterObserver(observer);
	}
	
	@Override
	public void setNotificationUri(ContentResolver cr, Uri uri) {
		throw new DBException("setNotificationUri() not support"); 
	}
	
	@Override
	public boolean getWantsAllOnMoveCalls() {
		return false; 
	}
	
	@Override
	public Bundle getExtras() {
        return Bundle.EMPTY;
    }
	
	@Override
	public Bundle respond(Bundle extras) {
        return Bundle.EMPTY;
    }

	@Override
	public int getType(int columnIndex) {
		return 0; // not implemented
	}
	
}
