package org.javenstudio.provider.task.download;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.DownloadData;
import org.javenstudio.android.entitydb.content.DownloadIterable;
import org.javenstudio.cocoka.database.SQLiteCursorObserver;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.MapDataSetCursor;

public class DownloadCursor extends MapDataSetCursor<DownloadItem> 
		implements SQLiteCursorObserver {

	public static interface Model { 
		public AccountApp getAccountApp();
		public AccountUser getAccountUser();
		
		public DownloadDataSets getDownloadDataSets();
		public DownloadItem createDownloadItem(DownloadData data);
		public DownloadItem createEmptyItem();
		
		//public void onCursorRequery();
	}
	
	private final Model mModel;
	private final DownloadIterable mCursor;
	private long mQueryTime = 0;
	
	public DownloadCursor(Model model) { 
		mModel = model;
		mQueryTime = System.currentTimeMillis();
		
		long accountId = 0;
		AccountUser user = model.getAccountUser();
		if (user != null) accountId = user.getAccountId();
		
		mCursor = ContentHelper.getInstance().getDownloadCursor(Long.toString(accountId));
		mCursor.registerObserver(this);
	}
	
	public long getQueryTime() { return mQueryTime; }
	
	@Override
	public void notifyDataSetChanged() { 
		getDownloadDataSets().notifyDataSetChanged(); 
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
	
	protected DownloadDataSets getDownloadDataSets() { 
		return mModel.getDownloadDataSets(); 
	}
	
	@Override 
	protected final AbstractDataSet<DownloadItem> createDataSet(int position) { 
		if (position >= mCursor.getCount()) {
			return getDownloadDataSets().createDataSet(
					mModel.createEmptyItem()); 
		}
		
		DownloadData entity = mCursor.entityAt(position); 
		if (entity != null) {
			return getDownloadDataSets().createDataSet(
					mModel.createDownloadItem(entity)); 
		}
		
		return null; 
	}
	
}
