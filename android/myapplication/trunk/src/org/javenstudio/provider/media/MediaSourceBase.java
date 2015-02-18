package org.javenstudio.provider.media;

import android.view.View;

import org.javenstudio.android.app.ActionExecutor;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.SelectAction;
import org.javenstudio.android.app.SelectManager;
import org.javenstudio.android.app.SelectMode;
import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.DataException;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.provider.ProviderCallback;

public abstract class MediaSourceBase implements IMediaSource, 
		SelectMode.Callback {
	//private static final Logger LOG = Logger.getLogger(MediaSourceBase.class);
	
	private final String mName;
	private final SelectManager mSelectManager;
	
	private int mIconRes = 0;
	private String mTitle = null;
	private String mSubTitle = null;
	private boolean mDefault = false;
	
	public MediaSourceBase(String name, SelectManager manager) { 
		mName = name; 
		mSelectManager = manager != null ? manager : new SelectManager();
	}
	
	public final String getName() { return mName; }
	
	public int getIconRes() { return mIconRes; }
	public void setIconRes(int res) { mIconRes = res; }
	
	public String getTitle(IActivity activity) { return mTitle; }
	public void setTitle(String title) { mTitle = title; }
	
	public String getSubTitle(IActivity activity) { return mSubTitle; }
	public void setSubTitle(String title) { mSubTitle = title; }
	
	public boolean isDefault() { return mDefault; }
	public void setDefault(boolean def) { mDefault = def; }
	
	public void reloadData(ProviderCallback callback, ReloadType type, long reloadId) {}
	
	public boolean isActionEnabled(DataAction action) { return false; }
	public boolean handleAction(SelectMode mode, DataAction action, 
			SelectManager.SelectData[] items) { return false; }
	
	public SelectManager getSelectManager() { return mSelectManager; }
	public SelectAction getSelectAction() { return null; }
	
	@Override
	public int getSelectItemCount(SelectManager.SelectData[] items) { 
		SelectAction selectAction = getSelectAction();
		return selectAction != null ? selectAction.getSelectItemCount(items) : 0; 
	}
	
	@Override
	public void onSelectChanged(SelectMode mode) { 
		SelectAction selectAction = getSelectAction();
		if (selectAction != null) 
			selectAction.onSelectChanged(mode, getSelectManager());
	}
	
	@Override
	public void executeAction(DataAction action, SelectManager.SelectData item, 
			ActionExecutor.ProgressListener listener) throws DataException { 
		SelectAction selectAction = getSelectAction();
		if (selectAction != null) 
			selectAction.executeAction(action, item, listener);
	}
	
	@Override
	public CharSequence getActionConfirmTitle(DataAction action) { 
		SelectAction selectAction = getSelectAction();
		if (selectAction != null)
			return selectAction.getActionConfirmTitle(action, getSelectManager());
		return null;
	}
	
	@Override
	public CharSequence getActionConfirmMessage(DataAction action) { 
		SelectAction selectAction = getSelectAction();
		if (selectAction != null)
			return selectAction.getActionConfirmMessage(action, getSelectManager());
		return null;
	}
	
	@Override
	public void onCustomViewBinded(SelectMode mode, View view) {
	}
	
	@Override
	public void onActionModeCreate(SelectMode mode) {
	}
	
	@Override
	public void onActionModeDestroy(SelectMode mode) {
	}
	
}
