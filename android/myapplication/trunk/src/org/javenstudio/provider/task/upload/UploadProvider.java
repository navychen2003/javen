package org.javenstudio.provider.task.upload;

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
import org.javenstudio.android.entitydb.content.UploadData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.task.TaskListProvider;

public class UploadProvider extends ProviderBase 
		implements UploadCursor.Model {
	//private static final Logger LOG = Logger.getLogger(UploadProvider.class);

	private final TaskListProvider mTaskList;
	private final UploadHandler mHandler;
	private final UploadBinder mBinder;
	private final UploadDataSets mDataSets;
	
	private OnUploadClickListener mItemClickListener = null;
	private OnUploadClickListener mViewClickListener = null;
	
	public UploadProvider(TaskListProvider group, 
			UploadHandler handler, String name, int iconRes) { 
		super(name, iconRes);
		if (group == null || handler == null) throw new NullPointerException();
		mTaskList = group;
		mHandler = handler;
		mBinder = createUploadBinder();
		mDataSets = new UploadDataSets(new UploadCursorFactory(this));
		
		mItemClickListener = new OnUploadClickListener() { 
				@Override
				public boolean onUploadClick(Activity activity, UploadItem item) {
					if (activity != null && item != null && item instanceof UploadFileItem) {
						showCancelDialog(activity, (UploadFileItem)item);
					}
					return true;
				}
			};
	}

	public TaskListProvider getTaskList() { return mTaskList; }
	public UploadHandler getUploadHandler() { return mHandler; }
	
	public AccountApp getAccountApp() { return getTaskList().getAccountApp(); }
	public AccountUser getAccountUser() { return getTaskList().getAccountUser(); }
	public DataApp getDataApp() { return getAccountApp().getDataApp(); }
	
	@Override
	public UploadBinder getBinder() {
		return mBinder;
	}
	
	@Override
	public UploadDataSets getUploadDataSets() { 
		return mDataSets; 
	}
	
	protected UploadBinder createUploadBinder() {
		return new UploadBinder(this);
	}
	
	@Override
	public UploadItem createUploadItem(UploadData data) { 
		return new UploadFileItem(this, data);
	}
	
	@Override
	public UploadItem createEmptyItem() {
		return null;
	}
	
	public void setOnItemClickListener(OnUploadClickListener l) { mItemClickListener = l; }
	public OnUploadClickListener getOnItemClickListener() { return mItemClickListener; }
	
	public void setOnViewClickListener(OnUploadClickListener l) { mViewClickListener = l; }
	public OnUploadClickListener getOnViewClickListener() { return mViewClickListener; }
	
	@Override
	public CharSequence getLastUpdatedLabel(IActivity activity) { 
		return AppResources.getInstance().formatRefreshTime(
				getUploadDataSets().getUploadItemCursor().getQueryTime());
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		if (type == ReloadType.FORCE) {
			ResourceHelper.getHandler().post(new Runnable() {
					@Override
					public void run() {
						getUploadDataSets().requery();
					}
				});
		}
	}
	
	private void showCancelDialog(Activity activity, UploadFileItem item) { 
		if (activity == null || item == null || item.isUploadFinished()) 
			return;
		
		final long uploadId = item.getUploadId();
		
		final String message = String.format(
				activity.getString(getUploadHandler().hasUploadInQueueRunning(uploadId) ? 
						R.string.upload_cancel_message_running : R.string.upload_cancel_message), 
				"\""+item.getContentName()+"\"");
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity)
			.setTitle(R.string.upload_cancel_title)
			.setIcon(AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_dialog_warning))
			.setMessage(Html.fromHtml(message))
			.setCancelable(true);
		
		//if (!getUploadHandler().hasUploadInQueueRunning(uploadId)) {
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
						getUploadHandler().cancelUpload(uploadId);
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
