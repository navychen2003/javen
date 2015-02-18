package org.javenstudio.android.app;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.DataException;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IActionMode;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuInflater;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.common.util.Logger;

public class SelectMode implements IActionMode.Callback, 
		SelectManager.ChangeListener {
	private static final Logger LOG = Logger.getLogger(SelectMode.class);
	
	public static interface Callback extends ActionExecutor.ActionProcessor { 
		public boolean isActionEnabled(DataAction action);
		public boolean handleAction(SelectMode mode, DataAction action, 
				SelectManager.SelectData[] items);
		
		public SelectManager getSelectManager();
		public void onSelectChanged(SelectMode mode);
		
		public void onCustomViewBinded(SelectMode mode, View view);
		public void onActionModeCreate(SelectMode mode);
		public void onActionModeDestroy(SelectMode mode);
		
		public CharSequence getActionConfirmTitle(DataAction action);
		public CharSequence getActionConfirmMessage(DataAction action);
	}
	
	public static abstract class AbstractCallback implements Callback { 
		public boolean isActionEnabled(DataAction action) { return false; }
		
		public abstract SelectManager getSelectManager();
		public void onSelectChanged(SelectMode mode) {}
		
		public void onCustomViewBinded(SelectMode mode, View view) {}
		public void onActionModeCreate(SelectMode mode) {}
		public void onActionModeDestroy(SelectMode mode) {}
		
		public CharSequence getActionConfirmTitle(DataAction action) { return null; }
		public CharSequence getActionConfirmMessage(DataAction action) { return null; }
		
		public int getSelectItemCount(SelectManager.SelectData[] items) {
			return items != null ? items.length : 0;
		}
    	
    	public void executeAction(DataAction action, SelectManager.SelectData item, 
    			ActionExecutor.ProgressListener listener) throws DataException {
    	}
    	
    	public void onActionDone(DataAction action, int progress, int success) {}
	}
	
	private final ActionHelper mHelper;
	
	private IActionMode mActionMode = null;
	private Callback mCallback = null;
	
	private TextView mTitleView = null;
	private TextView mSubTitleView = null;
	
	public SelectMode(ActionHelper helper) { 
		if (helper == null) throw new NullPointerException();
		mHelper = helper;
	}
	
	public ActionHelper getActionHelper() { return mHelper; }
	
	@Override
	public synchronized void onSelectChanged(final SelectManager manager) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					Callback callback = mCallback;
					if (callback != null) 
						callback.onSelectChanged(SelectMode.this);
				}
			});
	}
	
	public synchronized boolean isActionMode() { 
		return mActionMode != null;
	}
	
    public synchronized void finishActionMode() {
    	IActionMode mode = mActionMode;
    	
    	if (LOG.isDebugEnabled())
			LOG.debug("finishActionMode: mode=" + mode);
    	
    	if (mode != null)
    		mode.finish();
    }
	
	public synchronized final boolean startActionMode(Callback callback) {
		if (callback == null) return false;
		mCallback = callback;
		
		SelectManager manager = callback.getSelectManager();
		if (manager != null) 
			manager.setChangeListener(this);
		
		IActionMode mode = getActionHelper().startActionMode(this);
		
		if (LOG.isDebugEnabled())
			LOG.debug("startActionMode: mode=" + mode + " callback=" + callback);
		
		mActionMode = mode;
		mTitleView = null;
		mSubTitleView = null; 
		
		if (mode != null) { 
			LayoutInflater inflater = LayoutInflater.from(getActionHelper().getActivity());
			View view = getCustomView(inflater);
			mode.setCustomView(view);
			
			mTitleView = (TextView)findCustomTitleView(view);
			mSubTitleView = (TextView)findCustomSubTitleView(view);
			
			callback.onCustomViewBinded(this, view);
			onSelectChanged(manager);
		}
		
		return mode != null;
	}
	
	protected View getCustomView(LayoutInflater inflater) {
		return inflater.inflate(org.javenstudio.cocoka.app.R.layout.actionbar_custom_item, null);
	}
	
	protected View findCustomTitleView(View view) {
		return view.findViewById(org.javenstudio.cocoka.app.R.id.actionbar_custom_item_title);
	}
	
	protected View findCustomSubTitleView(View view) {
		return view.findViewById(org.javenstudio.cocoka.app.R.id.actionbar_custom_item_subtitle);
	}
	
	public TextView getTitleView() { return mTitleView; }
	public TextView getSubTitleView() { return mSubTitleView; }
	
	public void setTitle(CharSequence text) { 
		TextView view = mTitleView;
		if (view != null) view.setText(text);
	}
	
	public void setSubTitle(CharSequence text) { 
		TextView view = mSubTitleView;
		if (view != null) view.setText(text);
	}
	
	@Override
	public synchronized boolean onCreateActionMode(IActionMode mode, IMenu menu) {
		if (LOG.isDebugEnabled())
			LOG.debug("onCreateActionMode: mode=" + mode + " menu=" + menu);
		
		Callback callback = mCallback;
		if (callback != null) callback.onActionModeCreate(this);
		
		if (mode != null && menu != null && callback != null) { 
			IMenuInflater inflater = mode.getMenuInflater();
			if (inflater != null) { 
				onInflateActionMenu(mode, menu, inflater, callback);
				
				IActivity activity = getActionHelper().getIActivity();
				if (activity != null) {
					activity.getActivityHelper().lockOrientationOnActionStarting();
					activity.getActivityHelper().onActionModeCreate();
				}
				
				return true;
			}
		}
		
		return false;
	}

	protected void onInflateActionMenu(IActionMode mode, IMenu menu, 
			IMenuInflater inflater, Callback callback) {
		inflater.inflate(R.menu.select_menu, menu);
		
		IMenuItem deleteItem = menu.findItem(R.id.select_action_delete);
		if (deleteItem != null) 
			deleteItem.setVisible(callback.isActionEnabled(DataAction.DELETE));
		
		IMenuItem moveItem = menu.findItem(R.id.select_action_move);
		if (moveItem != null) 
			moveItem.setVisible(callback.isActionEnabled(DataAction.MOVE));
		
		IMenuItem shareItem = menu.findItem(R.id.select_action_share);
		if (shareItem != null) 
			shareItem.setVisible(callback.isActionEnabled(DataAction.SHARE));
		
		IMenuItem downloadItem = menu.findItem(R.id.select_action_download);
		if (downloadItem != null) 
			downloadItem.setVisible(callback.isActionEnabled(DataAction.DOWNLOAD));
	}
	
	@Override
	public boolean onPrepareActionMode(IActionMode mode, IMenu menu) {
		if (LOG.isDebugEnabled())
			LOG.debug("onPrepareActionMode: mode=" + mode + " menu=" + menu);
		
		return false;
	}

	@Override
	public boolean onActionItemClicked(IActionMode mode, IMenuItem item) {
		if (LOG.isDebugEnabled())
			LOG.debug("onActionItemClicked: mode=" + mode + " item=" + item);
		
		Callback callback = mCallback;
		
		if (mode != null && item != null && callback != null) { 
			if (item.getItemId() == R.id.select_action_delete) { 
				getActionHelper().getActionExecutor().actionDeleteConfirm(this, callback);
				return true;
			} else if (item.getItemId() == R.id.select_action_move) {
				getActionHelper().getActionExecutor().actionMoveConfirm(this, callback);
				return true;
			} else if (item.getItemId() == R.id.select_action_share) {
				getActionHelper().getActionExecutor().actionShareConfirm(this, callback);
				return true;
			} else if (item.getItemId() == R.id.select_action_download) {
				getActionHelper().getActionExecutor().actionDownloadConfirm(this, callback);
				return true;
			}
		}
		
		return false;
	}

	@Override
	public synchronized void onDestroyActionMode(IActionMode mode) {
		if (LOG.isDebugEnabled())
			LOG.debug("onDestroyActionMode: mode=" + mode);
		
		Callback callback = mCallback;
		if (callback != null) { 
			callback.onActionModeDestroy(this);
			
			SelectManager manager = callback.getSelectManager();
			if (manager != null) 
				manager.setChangeListener(null);
		}
		
		mActionMode = null;
		mTitleView = null;
		mSubTitleView = null; 
		
		IActivity activity = getActionHelper().getIActivity();
		if (activity != null) { 
			activity.setContentFragment();
			activity.getActivityHelper().unlockOrientationOnActionFinished();
			activity.getActivityHelper().onActionModeDestroy();
		}
		
		getActionHelper().onActionModeDestroy();
	}
	
}
