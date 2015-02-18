package org.javenstudio.provider.account.host;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ListAdapter;

import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.HostData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public abstract class HostDialogHelper extends BaseDialogHelper {
	private static final Logger LOG = Logger.getLogger(HostDialogHelper.class);

	private final HostListDataSets mDataSets;
	
	public HostDialogHelper() {
		mDataSets = new HostListDataSets();
	}
	
	public HostListDataSets getDataSets() { return mDataSets; }
	
	public abstract CharSequence getHostListTitle();
	public abstract CharSequence getHostSearchingTitle();
	
	public void actionSelect(Activity activity, HostListItem item) {}
	public void actionRemove(Activity activity, HostListItem item) {}
	
	public int getItemViewBackgroundRes(boolean selected) {
		return AppResources.getInstance().getDrawableRes(
				selected ? AppResources.drawable.section_item_background_selected : 
					AppResources.drawable.section_item_background);
	}
	
	public void refreshDataSets() {
		getDataSets().clear();
		
		HostData[] hosts = getHostList();
		if (hosts != null) {
			for (HostData data : hosts) {
				HostListItem item = createHostItem(data);
				if (item != null) getDataSets().addHostListItem(item);
			}
		}
	}
	
	public void postRefreshDataSets() {
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					refreshDataSets();
				}
			});
	}
	
	protected HostData[] getHostList() {
		return ContentHelper.getInstance().queryHosts();
	}
	
	protected ListAdapter createAdapter(Activity activity) {
		if (activity == null) return null;
		
		refreshDataSets();
		
		return new HostListAdapter(activity, getDataSets(), 
				R.layout.select_list_host_item);
	}
	
	protected HostListItem createHostItem(HostData data) {
		if (data == null) return null;
		return new HostListClusterItem(this, data);
	}
	
	public boolean showHostDialog(final Activity activity) {
		if (activity == null || activity.isDestroyed()) 
			return false;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("showHostDialog: activity=" + activity);
		
		final AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity);
		builder.setCancelable(false);
		builder.setTitle(getHostListTitle());
		builder.setAdapter(createAdapter(activity), null);
		
		builder.setPositiveButton(R.string.dialog_cancel_button, 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
		
		builder.setNegativeButton(R.string.dialog_addhost_button, 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					showAddDialog(activity);
				}
			});
		
		builder.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(final DialogInterface dialog) {
					onDialogShow(builder, dialog);
					onDialogRefresh(builder);
				}
			});
		
		builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					onDialogDismiss(builder, dialog);
					mDialog = null;
					mBuilder = null;
				}
			});
		
		builder.setRefreshListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onDialogRefresh(builder);
				}
			});
		
		AlertDialog dialog = builder.show(activity);
		mDialog = dialog;
		mBuilder = builder;
		
		return dialog != null; 
	}
	
	@Override
	protected void onDialogRefresh(AlertDialogBuilder builder) {
		//refreshDataSets();
		
		FinderHelper.scheduleFindHost(new FinderHelper.IFinderListener() {
				@Override
				public void onHostFinding() {
					postSetDialogTitle(getHostSearchingTitle());
					postShowDialogProgress(true, true);
				}
				@Override
				public void onHostFindDone() {
					postSetDialogTitle(getHostListTitle());
					postShowDialogProgress(false, true);
				}
				@Override
				public void onHostFound(String[] hostInfo) {
					HostDialogHelper.this.onHostFound(hostInfo);
				}
			});
	}
	
	protected void onHostFound(String[] hostInfo) {}
	
	protected boolean onHostUpdate(Activity activity, 
			String domain, String address, int port) {
		return false;
	}
	
	public boolean showAddDialog(final Activity activity) {
		AddDialogHelper helper = new AddDialogHelper() {
				@Override
				protected boolean updateHostData(Activity activity, String domain,
						String address, int port) {
					return onHostUpdate(activity, domain, address, port);
				}
			};
		return helper.showAddDialog(activity);
	}
	
}
