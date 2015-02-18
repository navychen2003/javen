package org.javenstudio.android.data.image;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;

import org.javenstudio.cocoka.util.ImageRefreshable;

public abstract class ImageRefresher {

	private final List<ImageRefreshable> mRefreshers; 
	private final String mWorkName;
	private boolean mRefreshing = false;
	
	public ImageRefresher(String name) { 
		mWorkName = "RefreshImage:" + name;
		mRefreshers = new ArrayList<ImageRefreshable>(); 
	}
	
	protected boolean isRefreshEnabled() { return true; }
	protected abstract void findImage();
	protected abstract boolean isImageFound();
	protected abstract Drawable getImageDrawable();
	
	public final void startRefresh() { 
		synchronized (this) {
			if (mRefreshing || !isRefreshEnabled()) 
				return;
			mRefreshing = true;
		
			ImageTask.runTask(mWorkName, new Runnable() {
					@Override
					public void run() {
						onRefreshRunning(); 
					}
				});
		}
	}
	
	private void onRefreshRunning() { 
		synchronized (this) {
			findImage();
			mRefreshing = false;
		}
	}
	
	public final void postRefreshDrawable() {
		ImageTask.postHandler(new Runnable() {
				public void run() { 
					refreshDrawable(); 
				}
			});
	}
	
	public final void addRefresher(final ImageRefreshable r) { 
		synchronized (mRefreshers) { 
			boolean found = false;
			
			for (int i=0; i < mRefreshers.size(); ) { 
				ImageRefreshable refresher = mRefreshers.get(i); 
				if (refresher != null) { 
					if (refresher == r) found = true;
					i ++; continue; 
				} else {
					mRefreshers.remove(i); 
				}
			}
			
			if (r != null && !found) 
				mRefreshers.add(r); 
		}
	}
	
	private void refreshDrawable() { 
		synchronized (mRefreshers) { 
			if (!isImageFound()) return; 
			
			for (int i=0; i < mRefreshers.size(); ) { 
				ImageRefreshable refresher = mRefreshers.get(i); 
				if (refresher != null && refresher.refreshDrawable(getImageDrawable())) { 
					i ++; continue; 
				} else {
					mRefreshers.remove(i); 
				}
			}
		}
	}
	
}
