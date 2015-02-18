package org.javenstudio.android.information;

import java.util.HashMap;
import java.util.Map;

import android.app.Application;

import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.reader.ReaderHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.cocoka.widget.model.BaseModel;
import org.javenstudio.cocoka.widget.model.Controller;
import org.javenstudio.cocoka.widget.model.Model;
import org.javenstudio.common.util.Logger;

public final class InformationModel extends BaseModel {
	private static final Logger LOG = Logger.getLogger(InformationModel.class);
	
	private final Map<String, InformationSource> mSourceMap; 
	private boolean mNextPage = false;
	
	public InformationModel(final Application app) { 
		super(app); 
		mSourceMap = new HashMap<String, InformationSource>(); 
	}
	
	@Override
	public void loadNextPage(Callback callback, int totalCount, int lastVisibleItem) { 
		if (LOG.isDebugEnabled()) {
			LOG.debug("loadNextPage: totalCount=" + totalCount 
					+ " lastVisibleItem=" + lastVisibleItem);
		}
		
		if (totalCount > 0 && lastVisibleItem > 0 && lastVisibleItem >= totalCount - 1) { 
			final InformationController ctrl = (InformationController)getInitializedController(); 
			final String location = ctrl.getLocation(); 
			
			if (location != null && location.length() > 0) { 
				InformationSource item = getSource(location); 
				if (item != null && item.hasNextPage()) {
					mNextPage = true;
					startLoad(callback, false);
				}
			}
		}
	}
	
	@Override
	protected boolean shouldStartLoad(Controller controller, Model.Callback callback) { 
		return true; 
	}
	
	public synchronized InformationSource getSource(String location) { 
		if (location == null || location.length() == 0) 
			return null; 
		
		InformationSource item = mSourceMap.get(location); 
		if (item == null) { 
			item = InformationRegistry.createSourceFor(location); 
			if (item != null) 
				mSourceMap.put(location, item); 
		}
		
		return item; 
	}
	
	public synchronized boolean existSource(String location) { 
		if (location == null || location.length() == 0) 
			return false; 
		
		return mSourceMap.containsKey(location); 
	}
	
	@Override 
	protected final void runWorkOnThread(Controller controller, 
			Model.Callback callback) { 
		if (controller == null || !(controller instanceof InformationController)) 
			return; 
		
		final InformationController ctrl = (InformationController)controller; 
		final String location = ctrl.getLocation(); 
		
		if (location != null && location.length() > 0) { 
			InformationSource item = getSource(location); 
			if (item != null) {
				ReloadType type = mNextPage ? ReloadType.NEXTPAGE : 
					(isForceReload() ? ReloadType.FORCE : ReloadType.DEFAULT);
				
				if (item.shouldReload() || isForceReload() || mNextPage) 
					loadInformationOnThread(ctrl, item, type); 
			}
		}
		
		mNextPage = false;
	}
	
	private void loadInformationOnThread(final InformationController controller, 
			final InformationSource item, ReloadType type) { 
		if (controller == null || item == null) 
			return; 
		
		if (type == ReloadType.NEXTPAGE) { 
			String location = item.getNextLocation();
			if (location != null && location.length() > 0) { 
				if (LOG.isDebugEnabled())
					LOG.debug("loadInformationOnThread: loadNextPage: " + location);
				
				fetchInformationItem(item, location, item.getReloadType() == ReloadType.FORCE , false);
			}
		} else {
			final String location = controller.getFetchLocation(); 
			if (LOG.isDebugEnabled())
				LOG.debug("loadInformationOnThread: reloadPages: " + location);
			
			item.onReloadPages(type);
			fetchInformationItem(item, location, type == ReloadType.FORCE, true);
			
			final String[] locations = ReaderHelper.getInformationLocationList(location);
			if (locations != null) { 
				for (String location2 : locations) {
					if (LOG.isDebugEnabled())
						LOG.debug("loadInformationOnThread: load location: " + location2);
					
					fetchInformationItem(item, location2, type == ReloadType.FORCE, false);
				}
			}
		}
	}
	
	private void fetchInformationItem(final InformationSource item, 
			final String location, final boolean refetch, final boolean first) {
		callbackInvoke(InformationListModel.ACTION_ONFETCHSTART, null); 
		
		if (LOG.isDebugEnabled())
			LOG.debug("fetchInformationItem: location=" + location + " first=" + first);
		
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
					onInformationFetched(item, location, content, first); 
					callbackInvoke(InformationListModel.ACTION_ONFETCHSTOP, null); 
				}
				@Override
				public void onHttpException(HttpException e) { 
					onExceptionCatched(e); 
				}
			};
		
		callback.setRefetchContent(refetch);
		callback.setSaveContent(true);
		
		FetchHelper.removeFailed(location);
		FetchHelper.fetchHtml(location, callback);
		
		callbackInvoke(InformationListModel.ACTION_ONFETCHSTOP, null); 
	}
	
	private void onInformationFetched(InformationSource item, 
			String location, String content, boolean first) { 
		if (item == null || location == null || content == null || content.length() == 0) 
			return; 
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onInformationFetched: location=" + location + " length=" 
					+ content.length() + " first=" + first);
		}
		
		item.addFetchedLocation(location); 
		item.onFetched(this, location, content, first); 
	}
	
	public void postAddInformation(final InformationSource item, 
			final Information data, final boolean notify) { 
		if (item == null || data == null) 
			return; 

		if (LOG.isDebugEnabled()) 
			LOG.debug("postAddInformation: item=" + data + " source=" + item);
		
		final Information[] ones = data.copyImageOnes();
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					item.getInformationDataSets().addInformation(data, notify); 
					callbackOnDataSetUpdate(data); 
					
					for (int i=0; ones != null && i < ones.length; i++) { 
						Information one = ones[i];
						if (one == null) continue;
						
						item.getInformationDataSets().addInformation(one, notify); 
						callbackOnDataSetUpdate(one); 
					}
				}
			}); 
		
		loadInformationContent(item, data, null, isForceReload());
	}
	
	public void postNotifyChanged(final InformationSource item) { 
		if (item == null) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					item.getInformationDataSets().notifyContentChanged(true);
					item.getInformationDataSets().notifyDataSetChanged();
				}
			});
	}
	
	private void onInformationContentFetched(final InformationSource item, 
			Information data, String location, String content, boolean forceReload) { 
		if (item == null || data == null || location == null || 
			content == null || content.length() == 0) 
			return; 
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onInformationContentFetched: location=" + location + " length=" 
					+ content.length() + " forceReload=" + forceReload);
		}
		
		if (loadInformationContent(item, data, content, forceReload) == false) { 
			// found actual final content data
			item.onSubContentFetched(this, data, location, content);
			
			final Information[] ones = data.copyImageOnes();
			
			ResourceHelper.getHandler().post(new Runnable() {
					@Override
					public void run() { 
						for (int i=0; ones != null && i < ones.length; i++) { 
							Information one = ones[i];
							if (one == null) continue;
							
							item.getInformationDataSets().addInformation(one, false); 
							callbackOnDataSetUpdate(one); 
						}
					}
				});
			
			callbackInvoke(InformationListModel.ACTION_ONCONTENTUPDATE, item); 
		}
	}
	
	private boolean loadInformationContent(final InformationSource item, 
			final Information data, String content, final boolean forceReload) { 
		final String contentPath = item.getSubContentPath(data, content);
		if (contentPath == null || contentPath.length() <= 0) 
			return false;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadInformationContent: item=" + data + " contentPath=" + contentPath);
		
		callbackInvoke(InformationListModel.ACTION_ONFETCHSTART, null); 
		
		HtmlCallback callback = new HtmlCallback() {
				@Override
				public String getDefaultContentCharset() { 
					String charset = item.getDefaultCharset(contentPath); 
					if (charset != null && charset.length() > 0) 
						return charset; 
					return super.getDefaultContentCharset(); 
				}
				@Override
				public void onHtmlFetched(String content) {
					onInformationContentFetched(item, data, contentPath, content, forceReload); 
					callbackInvoke(InformationListModel.ACTION_ONFETCHSTOP, null); 
				}
				@Override
				public void onHttpException(HttpException e) { 
					onExceptionCatched(e); 
				}
			};
		
		callback.setRefetchContent(forceReload);
		callback.setSaveContent(true);
		
		FetchHelper.fetchHtml(contentPath, callback);
		callbackInvoke(InformationListModel.ACTION_ONFETCHSTOP, null); 
		
		return true;
	}
	
}
