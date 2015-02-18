package org.javenstudio.provider.task.upload;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.UploadData;
import org.javenstudio.android.entitydb.content.UploadIterable;
import org.javenstudio.cocoka.database.SQLiteCursorObserver;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.MapDataSetCursor;

public class UploadCursor extends MapDataSetCursor<UploadItem> 
		implements SQLiteCursorObserver {

	public static interface Model { 
		public AccountApp getAccountApp();
		public AccountUser getAccountUser();
		
		public UploadDataSets getUploadDataSets();
		public UploadItem createUploadItem(UploadData data);
		public UploadItem createEmptyItem();
		
		//public void onCursorRequery();
	}
	
	private final Model mModel;
	private final UploadIterable mCursor;
	private long mQueryTime = 0;
	
	public UploadCursor(Model model) { 
		if (model == null) throw new NullPointerException();
		mModel = model;
		mQueryTime = System.currentTimeMillis();
		
		long accountId = 0;
		AccountUser user = model.getAccountUser();
		if (user != null) accountId = user.getAccountId();
		
		mCursor = ContentHelper.getInstance().getUploadCursor(Long.toString(accountId));
		mCursor.registerObserver(this);
	}
	
	public long getQueryTime() { return mQueryTime; }
	
	@Override
	public void notifyDataSetChanged() { 
		getUploadDataSets().notifyDataSetChanged(); 
	}
	
	@Override
	public boolean requery() { 
		super.requery(); 
		mQueryTime = System.currentTimeMillis();
		boolean result = mCursor.requery(true); 
		//mModel.onCursorRequery();
		return result;
	}
	
	@Override
	public int getCount() { 
		int count = mCursor.getCount(); 
		return count > 0 ? count : 1;
	}
	
	@Override
	public boolean isClosed() { 
		return mCursor.isClosed(); 
	}
	
	@Override
	public void close() { 
		mCursor.close(); 
	}
	
	protected UploadDataSets getUploadDataSets() { 
		return mModel.getUploadDataSets(); 
	}
	
	@Override 
	protected final AbstractDataSet<UploadItem> createDataSet(int position) { 
		if (position >= mCursor.getCount()) {
			return getUploadDataSets().createDataSet(
					mModel.createEmptyItem()); 
		}
		
		UploadData entity = mCursor.entityAt(position); 
		if (entity != null) {
			return getUploadDataSets().createDataSet(
					mModel.createUploadItem(entity)); 
		}
		
		return null; 
	}
	
}
