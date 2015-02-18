package org.javenstudio.provider.library.select;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public abstract class SelectOperation {
	private static final Logger LOG = Logger.getLogger(SelectOperation.class);

	private final SelectListDataSets mDataSets;
	
	private AlertDialogBuilder mBuilder = null;
	private AlertDialog mDialog = null;
	
	public SelectOperation() {
		mDataSets = new SelectListDataSets();
	}
	
	public abstract DataApp getDataApp();
	public abstract SelectFolderItem getCurrentFolder();
	public SelectListDataSets getDataSets() { return mDataSets; }
	
	public boolean showSelectDialog(final Activity activity, 
			final ISelectCallback callback) {
		if (callback == null || activity == null || activity.isDestroyed()) 
			return false;
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity);
		builder.setCancelable(true);
		builder.setTitle(callback.getSelectTitle());
		builder.setAdapter(createAdapter(activity, callback), null);
		
		builder.setPositiveButton(R.string.dialog_cancel_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		
		builder.setNegativeButton(R.string.dialog_select_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						//onDialogSelectClick(dialog);
						callback.onActionSelect(activity, SelectOperation.this);
					}
				});
		
		builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					onDialogDismiss(dialog);
					mBuilder = null;
					mDialog = null;
				}
			});
		
		builder.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					onDialogShow(dialog);
				}
			});
		
		mBuilder = builder;
		mDialog = builder.show(activity);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("showSelectDialog: activity=" + activity 
					+ " dialog=" + mDialog);
		}
		
		return true; 
	}
	
	protected void onDialogShow(DialogInterface dialog) {
		if (dialog == null || !(dialog instanceof AlertDialog)) 
			return;
		
		AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {
	    		private int mFirstVisibleItem = -1;
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
				}
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {
					if (firstVisibleItem != mFirstVisibleItem) { 
						mFirstVisibleItem = firstVisibleItem;
						onFirstVisibleChanged(view, firstVisibleItem, 
								visibleItemCount, totalItemCount);
					}
				}
	        };
	        
	    ListView listView = ((AlertDialog)dialog).getListView();
	    if (listView != null) listView.setOnScrollListener(scrollListener);
	}
	
	protected void onFirstVisibleChanged(View view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("onFirstVisibleChanged: firstVisibleItem=" + firstVisibleItem 
					+ " visibleItemCount=" + visibleItemCount + " totalItemCount=" + totalItemCount);
		}
	}
	
	protected void onDialogDismiss(DialogInterface dialog) {}
	//protected void onDialogSelectClick(DialogInterface dialog) {}
	
	public boolean isDialogShown() { return mDialog != null; }
	
	public void dismissDialog() { 
		AlertDialog dialog = mDialog;
		if (dialog != null) dialog.dismiss();
	}
	
	public void setDialogTitle(CharSequence title, Drawable icon, 
			View.OnClickListener iconListener, View.OnClickListener refreshListener) {
		AlertDialogBuilder builder = mBuilder;
		AlertDialog dialog = mDialog;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDialogTitle: dialog=" + dialog + " title=" + title 
					+ " iconListener=" + iconListener + " refreshListener=" + refreshListener);
		}
		
		if (dialog != null && builder != null) {
			builder.getHelper().setDialogTitle(dialog, title);
			builder.getHelper().setDialogIcon(dialog, icon);
			builder.getHelper().setDialogIconListener(dialog, iconListener);
			builder.getHelper().setDialogIndicator(dialog, iconListener != null ? 
					AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_dialog_back_indicator) :
						0);
			builder.getHelper().setDialogRefreshListener(dialog, 
					refreshListener, refreshListener != null);
			onDialogTitleChanged(builder, dialog);
		}
	}
	
	protected void onDialogTitleChanged(AlertDialogBuilder builder, 
			AlertDialog dialog) {}
	
	public void postSetDialogTitle(final CharSequence title, 
			final Drawable icon, final View.OnClickListener iconListener, 
			final View.OnClickListener refreshListener) {
		if (isDialogShown() == false) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					setDialogTitle(title, icon, iconListener, refreshListener);
				}
			});
	}
	
	public void showProgress(boolean show) {
		AlertDialogBuilder builder = mBuilder;
		AlertDialog dialog = mDialog;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("showProgress: dialog=" + dialog + " show=" + show);
		}
		
		if (dialog != null && builder != null) {
			builder.getHelper().showDialogProgressBar(dialog, show, !show);
		}
	}
	
	public void postShowProgress(final boolean show) {
		if (isDialogShown() == false) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					showProgress(show);
				}
			});
	}
	
	public ListAdapter createAdapter(Activity activity, 
			ISelectCallback callback) {
		if (activity == null || callback == null) 
			return null;
		
		getDataSets().clear();
		addDataList(null, callback.getRootList(this));
		
		return new SelectListAdapter(activity, callback, getDataSets(), 
				R.layout.select_list_file_item);
	}
	
	public final void addDataList(SelectFolderItem folder, 
			ISelectData[] datas) {
		if (datas == null) return;
		
		ISelectData prev = null;
		if (getDataSets().getCount() > 0) {
			SelectListItem item = getDataSets().getSelectListItemAt(
					getDataSets().getCount() -1);
			
			if (item != null) prev = item.getData();
		}
		
		for (ISelectData data : datas) {
			if (data == null) continue;
			
			SelectListItem categoryItem = createCategoryItem(folder, data, prev);
			SelectListItem dataItem = createSelectItem(folder, data);
			
			if (dataItem != null) {
				if (categoryItem != null) getDataSets().addData(categoryItem);
				getDataSets().addData(dataItem);
				prev = data;
			}
		}
	}
	
	protected abstract SelectListItem createCategoryItem(SelectFolderItem folder, 
			ISelectData data, ISelectData prev);
	
	protected abstract SelectListItem createSelectItem(SelectFolderItem folder, 
			ISelectData data);
	
	public void postAddItem(final SelectListItem item) {
		if (item == null || item.getOperation() != this)
			return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					getDataSets().addData(item);
				}
			});
	}
	
	public void postAddDataList(final SelectFolderItem folder, 
			final ISelectData[] datas) {
		if (datas == null || datas.length == 0)
			return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					addDataList(folder, datas);
				}
			});
	}
	
	public void postClearData() {
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					getDataSets().clear();
				}
			});
	}
	
	protected SelectCategoryItem createCategoryItem(SelectFolderItem parent, int titleRes) {
		return new SelectCategoryItem(this, parent, 
				ResourceHelper.getResources().getString(titleRes));
	}
	
	public int getItemViewBackgroundRes(boolean selected) {
		return AppResources.getInstance().getDrawableRes(
				selected ? AppResources.drawable.section_item_background_selected : 
					AppResources.drawable.section_item_background);
	}
	
	public void startLoader(SelectFolderItem folder, ReloadType type) {}
	
	public void onRefreshClick(Activity activity, ISelectCallback callback, 
			SelectFolderItem item) {
		if (activity == null || callback == null || item == null) return;
		if (LOG.isDebugEnabled()) LOG.debug("onRefreshClick: item=" + item);
		
		callback.onItemSelect(activity, null);
		startLoader(item, ReloadType.FORCE);
	}
	
	public void onParentClick(Activity activity, ISelectCallback callback, 
			SelectListItem item) {
		if (activity == null || callback == null) return;
		if (LOG.isDebugEnabled()) LOG.debug("onParentClick: item=" + item);
		
		callback.onItemSelect(activity, null);
		
		if (item == null) {
			postSetDialogTitle(
					callback.getSelectTitle(), 
					null, null, null);
			
			ISelectData[] roots = callback.getRootList(this);
			postClearData();
			postAddDataList(null, roots);
			if (roots == null || roots.length == 0) 
				onEmptyState(null);
			
			return;
		}
		
		if (item instanceof SelectFolderItem)
			item.onItemClick(activity, callback);
	}
	
	public void onEmptyState(SelectFolderItem folder) {}
	
}
