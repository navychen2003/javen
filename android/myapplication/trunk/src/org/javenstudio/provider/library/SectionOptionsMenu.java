package org.javenstudio.provider.library;

import android.app.Activity;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuInflater;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.app.IOptionsMenu;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.IProviderActivity;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.library.details.SectionInfoItem;
import org.javenstudio.provider.library.details.SectionInfoProvider;

public abstract class SectionOptionsMenu implements IOptionsMenu {
	private static final Logger LOG = Logger.getLogger(SectionOptionsMenu.class);

	private Activity mActivity = null;
	private IMenu mMenu = null;
	private IMenuItem mShareItem = null;
	private IMenuItem mDownloadItem = null;
	private IMenuItem mDeleteItem = null;
	private IMenuItem mMoveItem = null;
	
	public abstract AccountApp getAccountApp();
	public AccountUser getAccountUser() { return getAccountApp().getAccount(); }
	
	protected void postUpdateOptionsMenu(final Activity activity) {
		if (activity == null) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					onUpdateOptionsMenu(activity);
				}
			});
	}
	
	@Override
	public boolean hasOptionsMenu(Activity activity) {
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Activity activity, IMenu menu, IMenuInflater inflater) {
		if (LOG.isDebugEnabled()) LOG.debug("onCreateOptionsMenu: activity=" + activity);
		
		inflater.inflate(R.menu.section_menu, menu);
		mShareItem = menu.findItem(R.id.section_action_share);
		mDownloadItem = menu.findItem(R.id.section_action_download);
		mDeleteItem = menu.findItem(R.id.section_action_delete);
		mMoveItem = menu.findItem(R.id.section_action_move);
		mMenu = menu;
		mActivity = activity;
		
		return true; 
	}
	
	@Override
    public boolean onPrepareOptionsMenu(Activity activity, IMenu menu) { 
		if (LOG.isDebugEnabled()) LOG.debug("onPrepareOptionsMenu: activity=" + activity);
		onUpdateOptionsMenu(activity);
		return true; 
	}
	
	@Override
    public boolean onOptionsItemSelected(Activity activity, IMenuItem item) { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("onPrepareOptionsMenu: activity=" + activity + " item=" + item);
		
		if (item.getItemId() == R.id.section_action_share) {
			return onShareItemSelected(activity);
		} else if (item.getItemId() == R.id.section_action_download) {
			return onDownloadItemSelected(activity);
		} else if (item.getItemId() == R.id.section_action_delete) {
			return onDeleteItemSelected(activity);
		} else if (item.getItemId() == R.id.section_action_move) {
			return onMoveItemSelected(activity);
		}
		
		return false;
	}

	@Override
	public boolean removeOptionsMenu(Activity activity) {
		if (activity == null || activity != mActivity) return false;
		if (LOG.isDebugEnabled()) LOG.debug("removeOptionsMenu: activity=" + activity);
		
		IMenu menu = mMenu;
		mMenu = null;
		mActivity = null;
		
		if (menu != null) { 
			menu.removeItem(R.id.section_action_share);
			menu.removeItem(R.id.section_action_download);
			menu.removeItem(R.id.section_action_delete);
			menu.removeItem(R.id.section_action_move);
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean onUpdateOptionsMenu(Activity activity) {
		if (LOG.isDebugEnabled()) LOG.debug("onUpdateOptionsMenu: activity=" + activity);
		
		SectionInfoProvider provider = getSectionInfoProvider(activity);
		if (provider != null) {
			SectionInfoItem item = provider.getSectionItem();
			if (item != null) {
				ISectionInfoData data = item.getSectionData();
				if (LOG.isDebugEnabled()) LOG.debug("onUpdateOptionsMenu: data=" + data);
				
				if (data != null) {
					IMenuItem shareItem = mShareItem;
					IMenuItem downloadItem = mDownloadItem;
					IMenuItem deleteItem = mDeleteItem;
					IMenuItem moveItem = mMoveItem;
					
					if (shareItem != null && !data.supportOperation(FileOperation.Operation.SHARE)) 
						shareItem.setVisible(false);
					
					if (downloadItem != null && !data.supportOperation(FileOperation.Operation.DOWNLOAD)) 
						downloadItem.setVisible(false);
					
					if (deleteItem != null && !data.supportOperation(FileOperation.Operation.DELETE))
						deleteItem.setVisible(false);
					
					if (moveItem != null && !data.supportOperation(FileOperation.Operation.MOVE))
						moveItem.setVisible(false);
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	protected boolean onShareItemSelected(Activity activity) {
		return true;
	}
	
	protected boolean onDownloadItemSelected(final Activity activity) {
		SectionInfoProvider provider = getSectionInfoProvider(activity);
		if (provider != null && activity instanceof IProviderActivity) {
			return provider.onDownloadClick((IProviderActivity)activity);
		}
		return false;
	}
	
	protected boolean onDeleteItemSelected(final Activity activity) {
		return false;
	}
	
	protected boolean onMoveItemSelected(final Activity activity) {
		return false;
	}
	
	protected SectionInfoProvider getSectionInfoProvider(Activity activity) {
		if (activity != null && activity instanceof IProviderActivity) {
			IProviderActivity menuactivity = (IProviderActivity)activity;
			Provider provider = menuactivity.getCurrentProvider();
			
			if (provider != null && provider instanceof SectionInfoProvider) {
				SectionInfoProvider p = (SectionInfoProvider)provider;
				return p;
			}
		}
		return null;
	}
	
}
