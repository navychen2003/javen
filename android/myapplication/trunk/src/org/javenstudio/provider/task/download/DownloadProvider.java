package org.javenstudio.provider.task.download;

import android.app.Activity;
import android.content.DialogInterface;
import android.text.Html;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.entitydb.content.DownloadData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.task.TaskListProvider;

public class DownloadProvider extends ProviderBase 
		implements DownloadCursor.Model {
	//private static final Logger LOG = Logger.getLogger(DownloadProvider.class);

	private final TaskListProvider mTaskList;
	private final DownloadHandler mHandler;
	private final DownloadBinder mBinder;
	private final DownloadDataSets mDataSets;
	
	private OnDownloadClickListener mItemClickListener = null;
	private OnDownloadClickListener mViewClickListener = null;
	
	public DownloadProvider(TaskListProvider group, 
			DownloadHandler handler, String name, int iconRes) { 
		super(name, iconRes);
		if (group == null || handler == null) throw new NullPointerException();
		mTaskList = group;
		mHandler = handler;
		mBinder = createDownloadBinder();
		mDataSets = new DownloadDataSets(new DownloadCursorFactory(this));
		
		mItemClickListener = new OnDownloadClickListener() { 
				@Override
				public boolean onDownloadClick(Activity activity, DownloadItem item) {
					if (activity != null && item != null && item instanceof DownloadFileItem) {
						showCancelDialog(activity, (DownloadFileItem)item);
					}
					return true;
				}
			};
	}

	public TaskListProvider getTaskList() { return mTaskList; }
	public DownloadHandler getDownloadHandler() { return mHandler; }
	
	public AccountApp getAccountApp() { return getTaskList().getAccountApp(); }
	public AccountUser getAccountUser() { return getTaskList().getAccountUser(); }
	public DataApp getDataApp() { return getAccountApp().getDataApp(); }
	
	@Override
	public DownloadBinder getBinder() {
		return mBinder;
	}
	
	@Override
	public DownloadDataSets getDownloadDataSets() { 
		return mDataSets; 
	}
	
	protected DownloadBinder createDownloadBinder() {
		return new DownloadBinder(this);
	}
	
	@Override
	public DownloadItem createDownloadItem(DownloadData data) { 
		return new DownloadFileItem(this, data);
	}
	
	@Override
	public DownloadItem createEmptyItem() {
		return null;
	}
	
	public void setOnItemClickListener(OnDownloadClickListener l) { mItemClickListener = l; }
	public OnDownloadClickListener getOnItemClickListener() { return mItemClickListener; }
	
	public void setOnViewClickListener(OnDownloadClickListener l) { mViewClickListener = l; }
	public OnDownloadClickListener getOnViewClickListener() { return mViewClickListener; }
	
	@Override
	public CharSequence getLastUpdatedLabel(IActivity activity) { 
		return AppResources.getInstance().formatRefreshTime(
				getDownloadDataSets().getDownloadItemCursor().getQueryTime());
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		if (type == ReloadType.FORCE) {
			ResourceHelper.getHandler().post(new Runnable() {
					@Override
					public void run() {
						getDownloadDataSets().requery();
					}
				});
		}
	}
	
	private void showCancelDialog(Activity activity, DownloadFileItem item) { 
		if (activity == null || item == null || item.isDownloadFinished()) 
			return;
		
		final long downloadId = item.getDownloadId();
		
		final String message = String.format(
				activity.getString(getDownloadHandler().hasDownloadInQueueRunning(downloadId) ? 
						R.string.download_cancel_message_running : R.string.download_cancel_message), 
				"\""+item.getContentName()+"\"");
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity)
			.setTitle(R.string.download_cancel_title)
			.setIcon(AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_dialog_warning))
			.setMessage(Html.fromHtml(message))
			.setCancelable(true);
		
		//if (!getDownloadHandler().hasDownloadInQueueRunning(downloadId)) {
		//	builder.setNegativeButton(R.string.dialog_noconfirm_button, 
		//			new DialogInterface.OnClickListener() {
		//				@Override
		//				public void onClick(DialogInterface dialog, int which) {
		//				}
		//			});
		//}
		
		builder.setNegativeButton(R.string.dialog_yes_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						getDownloadHandler().cancelDownload(downloadId);
					}
				});
		
		builder.setPositiveButton(R.string.dialog_no_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		
		builder.show(activity);
	}
	
}
