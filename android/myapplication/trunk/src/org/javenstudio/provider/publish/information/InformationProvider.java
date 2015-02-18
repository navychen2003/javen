package org.javenstudio.provider.publish.information;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.information.Information;
import org.javenstudio.android.information.InformationBinder;
import org.javenstudio.android.information.InformationNavItem;
import org.javenstudio.android.information.activity.NavigationActionItem;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.cocoka.widget.model.NavigationGroup;
import org.javenstudio.cocoka.widget.model.NavigationInfo;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.ProviderModel;

public class InformationProvider extends ProviderBase {
	private static final Logger LOG = Logger.getLogger(InformationProvider.class);
	
	private final NavigationGroup mGroupItem;
	private InformationNavItem mSelectItem = null;
	
	public InformationProvider(NavigationGroup item) { 
		super(item.getInfo().getName(), Information.getItemIconRes(item.getInfo()));
		mGroupItem = item;
		
		if (item.getChildCount() <= 1) 
			mSelectItem = (InformationNavItem)item.getChildAt(0);
	}

	@Override
	public IMenuOperation getMenuOperation() { 
		final InformationNavItem item = mSelectItem;
		if (item != null) { 
			IMenuOperation mo = item.getMenuOperation();
			if (mo != null) return mo;
		}
		
		return super.getMenuOperation();
	}
	
	@Override
	public ProviderBinder getBinder() {
		final InformationNavItem item = mSelectItem;
		if (item != null) { 
			final InformationBinder binder = item.getBinder();
			if (binder != null) {
				return new ProviderBinder() {
					@Override
					public void bindBehindAbove(IActivity activity, LayoutInflater inflater, 
							Bundle savedInstanceState) {
						binder.bindBehindAbove(activity, inflater);
					}
					@Override
					public View bindView(IActivity activity, LayoutInflater inflater,
							ViewGroup container, Bundle savedInstanceState) {
						View rootView = binder.inflateView(activity, inflater, container);
						binder.bindView(activity, item, rootView);
						return rootView;
					}
				};
			}
		}
		
		return null;
	}
	
	@Override
	public String getTitle() { 
		final NavigationGroup group = mGroupItem;
		if (group != null && group.getChildCount() <= 1) { 
			InformationNavItem item = mSelectItem;
			if (item != null) 
				return item.getInfo().getTitle();
		}
		
		return null; 
	}
	
	@Override
	public ActionItem[] getActionItems(final IActivity activity) { 
		final NavigationGroup group = mGroupItem;
		if (group == null || group.getChildCount() <= 1) 
			return null;
		
		final ActionItem[] items = new ActionItem[group.getChildCount()];
		
		for (int i=0; i < items.length; i++) { 
			final InformationNavItem item = (InformationNavItem)group.getChildAt(i);
			
			final NavigationInfo info = item.getInfo();
			final int iconRes = Information.getItemIconRes(info);
			final Drawable icon = Information.getItemIcon(info, iconRes);
			
			final String name = info.getName();
			final String title = info.getTitle();
			final String subtitle = info.getSubTitle();
			final String dropdownTitle = info.getDropdownTitle();
			
			NavigationActionItem actionItem = new NavigationActionItem(
					name, iconRes, icon, null, item);
			
			actionItem.setTitle(title);
			actionItem.setSubTitle(subtitle);
			actionItem.setDropdownTitle(dropdownTitle);
			
			items[i] = actionItem;
		}
		
		return items; 
	}
	
	@Override
	public boolean onActionItemSelected(IActivity activity, int position, long id) { 
		ActionItem item = activity.getActionHelper().getActionItem(position);
		
		if (item != null && item instanceof NavigationActionItem) { 
			NavigationActionItem actionItem = (NavigationActionItem)item;
			InformationNavItem navItem = (InformationNavItem)actionItem.getNavigationItem();
			
			boolean changed = mSelectItem != navItem;
			mSelectItem = navItem;
			
			if (changed) activity.setContentFragment();
		}
		
		return true; 
	}
	
	@Override
	public boolean hasNextPage() { 
		final InformationNavItem item = mSelectItem;
		if (item != null) { 
			String location = item.getNextLocation();
			if (location != null && location.length() > 0)
				return true;
		}
		
		return false; 
	}
	
	@Override
	public void reloadOnThread(final ProviderCallback pc, 
			ReloadType type, long reloadId) { 
		final InformationNavItem item = mSelectItem;
		if (item == null) return;
		
		int dataCount = item.getDataSets().getCount();
		
		if (type == ReloadType.NEXTPAGE && dataCount > 0) { 
			String location = item.getNextLocation();
			if (location != null && location.length() > 0) { 
				if (LOG.isDebugEnabled())
					LOG.debug("reloadOnThread: loadNextPage: " + location);
				
				loadInformationList(pc, item.getReloadType(), item, location, false);
			}
		} else {
			if (type != ReloadType.FORCE && dataCount > 0)
				return;
			
			String location = item.getLocation();
			if (LOG.isDebugEnabled())
				LOG.debug("reloadOnThread: reloadPages: " + location);
			
			item.onReloadPages(type);
			loadInformationList(pc, type, item, location, true);
			
			for (int i=2; i < 10; i++) {
				final String location2 = item.getLocation(Information.ATTR_LOCATION + i);
				if (location2 != null && location2.length() > 0) {
					if (LOG.isDebugEnabled())
						LOG.debug("reloadOnThread: load location" + i + ": " + location2);
					
					loadInformationList(pc, type, item, location2, false);
				}
			}
		}
	}
	
	private void loadInformationList(final ProviderCallback pc, ReloadType type, 
			final InformationNavItem item, final String location, boolean first) {
		if (location == null || location.length() == 0) 
			return; 
		
		pc.getController().getModel().callbackInvoke(
				ProviderModel.ACTION_ONFETCHSTART, null); 
		
		try { 
			Uri uri = Uri.parse(location);
			String scheme = uri.getScheme();
			
			if (scheme != null && scheme.equalsIgnoreCase("assets")) { 
				loadInformationFile(pc, item, location, uri.getHost(), first); 
				
			} else if (scheme != null && scheme.equalsIgnoreCase("html")) { 
				loadInformationHtml(pc, item, location, first); 
				
			} else { 
				fetchInformationList(pc, item, location, type == ReloadType.FORCE, first);
			}
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("loadInformationList: error: " + e, e);
		}
		
		pc.getController().getModel().callbackInvoke(
				ProviderModel.ACTION_ONFETCHSTOP, null); 
	}
	
	private void loadInformationHtml(final ProviderCallback pc, 
			final InformationNavItem item, final String location, boolean first) {
		if (location == null || location.length() == 0) 
			return; 
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadInformationHtml: location=" + location);
		
		String content = null;
		
		try {
			Object data = item.getInfo().getAttribute(Information.ATTR_HTML);
			if (data != null) 
				content = data.toString();
			
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("loadInformationHtml: error: " + e, e);
			
		}
		
		item.onFetched(pc.getController().getModel(), location, content, first); 
	}
	
	private void loadInformationFile(final ProviderCallback pc, 
			final InformationNavItem item, final String location, 
			final String filename, boolean first) {
		if (location == null || filename == null || filename.length() == 0) 
			return; 
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadInformationFile: filename=" + filename);
		
		String content = null;
		InputStream is = null;
		
		try {
			is = ResourceHelper.getContext().getAssets().open(filename);
			if (is != null) { 
				BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
				StringBuffer sbuf = new StringBuffer();
				String line = null;
				
				while ((line = br.readLine()) != null) { 
					sbuf.append(line);
					sbuf.append("\r\n");
				}
				
				content = sbuf.toString();
			}
		} catch (IOException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("loadInformationFile: error: " + e, e);
			
		} finally { 
			Utils.closeSilently(is);
		}
		
		item.onFetched(pc.getController().getModel(), location, content, first); 
	}
	
	private void fetchInformationList(final ProviderCallback pc, 
			final InformationNavItem item, final String location, 
			final boolean refetch, final boolean first) {
		if (location == null || location.length() == 0) 
			return; 
		
		if (LOG.isDebugEnabled())
			LOG.debug("fetchInformationList: location=" + location);
		
		//pc.getController().getModel().callbackInvoke(
		//		ProviderModel.ACTION_ONFETCHSTART, null); 
		
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
					item.onFetched(pc.getController().getModel(), location, content, first); 
					//pc.getController().getModel().callbackInvoke(
					//		ProviderModel.ACTION_ONFETCHSTOP, null); 
				}
				@Override
				public void onHttpException(HttpException e) { 
					pc.getController().getModel().onExceptionCatched(e); 
				}
			};
		
		callback.setRefetchContent(refetch);
		callback.setSaveContent(true);
		
		FetchHelper.removeFailed(location);
		FetchHelper.fetchHtml(location, callback);
		
		//pc.getController().getModel().callbackInvoke(
		//		ProviderModel.ACTION_ONFETCHSTOP, null); 
	}

}
