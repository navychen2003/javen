package org.javenstudio.provider;

import java.util.concurrent.atomic.AtomicLong;

import android.app.Application;

import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.information.Information;
import org.javenstudio.android.information.InformationNavItem;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.model.BaseModel;
import org.javenstudio.cocoka.widget.model.Controller;
import org.javenstudio.common.util.Logger;

public class ProviderModel extends BaseModel implements InformationNavItem.NavModel {
	private static final Logger LOG = Logger.getLogger(ProviderModel.class);
	
	public static final int ACTION_ONFETCHSTART = 201; 
	public static final int ACTION_ONFETCHSTOP = 202; 
	
	public static final AtomicLong sCounter = new AtomicLong(0);
	
	private boolean mNextPage = false;
	
	public ProviderModel(Application app) { 
		super(app); 
	}

	@Override
	public void loadNextPage(Callback callback, int totalCount, int lastVisibleItem) { 
		if (totalCount > 0 && lastVisibleItem > 0 && lastVisibleItem >= totalCount - 1) { 
			ProviderCallback pc = (ProviderCallback)callback;
			Provider p = pc.getProvider();
			
			if (p != null && p.hasNextPage()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("loadNextPage: totalCount=" + totalCount 
							+ " lastVisibleItem=" + lastVisibleItem);
				}
				
				mNextPage = true;
				startLoad(callback, false);
			}
		}
	}
	
	@Override
	protected boolean shouldStartLoad(Controller controller, Callback callback) { 
		if (callback != null && callback instanceof ProviderCallback)
			return true;
		
		return false;
	}
	
	@Override
	protected void runWorkOnThread(Controller controller, Callback callback) {
		if (callback != null && callback instanceof ProviderCallback) {
			ProviderCallback pc = (ProviderCallback)callback;
			Provider p = pc.getProvider();
			
			if (p != null) {
				ReloadType type = mNextPage ? ReloadType.NEXTPAGE : 
					(isForceReload() ? ReloadType.FORCE : ReloadType.DEFAULT);
				
				p.reloadOnThread(pc, type, sCounter.incrementAndGet());
			}
		}
		
		mNextPage = false;
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
