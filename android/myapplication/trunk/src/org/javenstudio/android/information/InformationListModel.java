package org.javenstudio.android.information;

import android.app.Application;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.cocoka.widget.model.NavigationCallback;
import org.javenstudio.cocoka.widget.model.NavigationController;
import org.javenstudio.cocoka.widget.model.NavigationItem;
import org.javenstudio.cocoka.widget.model.NavigationModel;
import org.javenstudio.common.util.Logger;

public final class InformationListModel extends NavigationModel 
		implements InformationNavItem.NavModel {
	private static final Logger LOG = Logger.getLogger(InformationListModel.class);
	
	public static final int ACTION_ONFETCHSTART = 101; 
	public static final int ACTION_ONFETCHSTOP = 102; 
	public static final int ACTION_ONCONTENTUPDATE = 103; 
	
	public InformationListModel(Application app) { 
		super(app); 
	}
    
	@Override 
	protected NavigationItem[] createNavigationItems() { 
		return InformationRegistry.getNavigationItems(); 
	}
	
	@Override 
	protected NavigationItem createDefaultNavigationItem() { 
		return InformationRegistry.getDefaultNavigationItem(); 
	}
	
	@Override 
	protected final void runWorkOnThread(NavigationController controller, 
			NavigationCallback callback) { 
		if (controller == null || callback == null) 
			return; 
		
		if (controller instanceof InformationListController) { 
			InformationListController ctrl = (InformationListController)controller; 
			NavigationItem item = ctrl.getSelectedItem(); 
			
			if (item != null && item instanceof InformationNavItem) 
				loadInformationListOnThread(ctrl, (InformationNavItem)item); 
		}
	}
	
	private void loadInformationListOnThread(final InformationListController controller, 
			final InformationNavItem item) { 
		if (controller == null || item == null) return; 
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadInformationListOnThread: navItem attrs: " + item.getInfo().toString());
		
		fetchInformationList(item, item.getLocation(), true);
		
		for (int i=2; i < 10; i++) {
			final String location2 = item.getLocation(Information.ATTR_LOCATION + i);
			if (location2 != null && location2.length() > 0) 
				fetchInformationList(item, location2, false);
		}
	}
	
	private void fetchInformationList(final InformationNavItem item, 
			final String location, final boolean first) {
		if (location == null || location.length() == 0) 
			return; 
		
		callbackInvoke(ACTION_ONFETCHSTART, null); 
		
		HtmlCallback callback = new HtmlCallback() {
				@Override
				public String getDefaultContentCharset() { 
					String charset = item.getDefaultCharset(); 
					if (charset != null && charset.length() > 0) 
						return charset; 
					return super.getDefaultContentCharset(); 
				}
				@Override
				public void onHtmlFetched(String content) {
					item.onFetched(InformationListModel.this, location, content, first); 
					callbackInvoke(ACTION_ONFETCHSTOP, null); 
				}
				@Override
				public void onHttpException(HttpException e) { 
					onExceptionCatched(e); 
				}
			};
		
		callback.setRefetchContent(isForceReload());
		callback.setSaveContent(true);
		
		FetchHelper.removeFailed(location);
		FetchHelper.fetchHtml(location, callback);
		
		callbackInvoke(ACTION_ONFETCHSTOP, null); 
	}
	
	@Override
	public void postAddInformation(final InformationNavItem item, 
			final Information data, final boolean notify) { 
		if (item == null || data == null) 
			return; 

		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					item.getInformationDataSets().addInformation(data, notify); 
					callbackOnDataSetUpdate(data); 
				}
			});
	}
	
	@Override
	public void postNotifyChanged(final InformationNavItem item) { 
		if (item == null) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					item.getInformationDataSets().notifyContentChanged(true);
					item.getInformationDataSets().notifyDataSetChanged();
				}
			});
	}
	
}
