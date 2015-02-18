package org.javenstudio.android.data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.NetworkHelper;
import org.javenstudio.android.app.ActivityHelper;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.SlideDrawable;
import org.javenstudio.android.data.image.ImageHelper;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.cocoka.graphics.BaseDrawable;
import org.javenstudio.cocoka.graphics.BitmapCache;
import org.javenstudio.common.util.Logger;

public abstract class DataBinder {
	private static final Logger LOG = Logger.getLogger(DataBinder.class);
	
	private static final AtomicLong sBindVersion = new AtomicLong(0);
	
	public static long increaseBindVersion(Context activity) { 
		long version = sBindVersion.incrementAndGet();
		ImageHelper.increaseImageVersion(activity);
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("increaseBindVersion: version=" + version);
		
		BitmapCache.getInstance().recycleBitmapsIfNecessary();
		
		return version;
	}
	
	public static long getBindVersion() { 
		return sBindVersion.get();
	}
	
	public static boolean requestDownload(HttpImage image) { 
		return requestDownload(null, image, true);
	}
	
	public static boolean requestDownload(IActivity activity, HttpImage image) { 
		return requestDownload(activity, image, false);
	}
	
	public static boolean requestDownload(IActivity activity, 
			HttpImage image, boolean nocheck) { 
		if (image == null) return false;
		
		if (nocheck || NetworkHelper.getInstance().isWifiAvailable() || 
			(HttpImageItem.checkNotDownload(image) && (activity != null && 
			activity.getActivityHelper().confirmAutoFetch(true)))) {
			if (nocheck || NetworkHelper.getInstance().isNetworkAvailable()) {
				HttpImageItem.requestDownload(image, false);
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean requestDownload(Collection<HttpImageItem> images) { 
		return requestDownload(null, images, true);
	}
	
	public static boolean requestDownload(IActivity activity, 
			Collection<HttpImageItem> images) { 
		return requestDownload(activity, images, false);
	}
	
	private static boolean requestDownload(IActivity activity, 
			Collection<HttpImageItem> images, boolean nocheck) { 
		if (images == null) return false;
		
		if (nocheck || NetworkHelper.getInstance().isWifiAvailable() || 
			(HttpImageItem.checkNotDownload(images) && (activity != null && 
			activity.getActivityHelper().confirmAutoFetch(true)))) {
			if (nocheck || NetworkHelper.getInstance().isNetworkAvailable()) {
				HttpImageItem.requestDownload(images, false);
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean requestDownload(HttpImageItem[] images) { 
		return requestDownload((ActivityHelper.HelperApp)null, images, true);
	}
	
	public static boolean requestDownload(IActivity activity, HttpImageItem[] images) { 
		return requestDownload(activity, images, false);
	}
	
	public static boolean requestDownload(ActivityHelper.HelperApp activity, 
			HttpImageItem[] images, boolean nocheck) { 
		if (images == null) return false;
		
		if (nocheck || NetworkHelper.getInstance().isWifiAvailable() || 
			(HttpImageItem.checkNotDownload(images) && (activity != null && 
			activity.getActivityHelper().confirmAutoFetch(true)))) {
			if (nocheck || NetworkHelper.getInstance().isNetworkAvailable()) {
				HttpImageItem.requestDownload(images, false);
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean requestDownload(Activity activity, 
			HttpImageItem[] images, boolean nocheck) { 
		ActivityHelper.HelperApp app = (activity != null && activity instanceof ActivityHelper.HelperApp) ?
				(ActivityHelper.HelperApp)activity : null;
		return requestDownload(app, images, nocheck);
	}
	
	public static boolean requestDownload(HttpImageItem image) { 
		return requestDownload(null, image, true);
	}
	
	public static boolean requestDownload(IActivity activity, HttpImageItem image) { 
		return requestDownload(activity, image, false);
	}
	
	private static boolean requestDownload(IActivity activity, 
			HttpImageItem image, boolean nocheck) { 
		if (image == null) return false;
		
		if (nocheck || NetworkHelper.getInstance().isWifiAvailable() || 
			(HttpImageItem.checkNotDownload(image) && (activity != null && 
			activity.getActivityHelper().confirmAutoFetch(true)))) {
			if (nocheck || NetworkHelper.getInstance().isNetworkAvailable()) {
				HttpImageItem.requestDownload(image, false);
				return true;
			}
		}
		
		return false;
	}
	
	public static class VersionRef<T> { 
		private final T mData;
		private final long mVersion;
		
		public VersionRef(T data) { 
			mData = data;
			mVersion = getBindVersion();
		}
		
		public T get() { 
			if (mVersion != getBindVersion()) 
				return null;
			else
				return mData;
		}
		
		protected T getData() { 
			return mData;
		}
	}
	
	private static final Map<String, ViewRef> sBindMap = 
			new HashMap<String, ViewRef>();
	
	static View getBindView(String key, long id) { 
		ViewRef ref = getBindViewRef(key, id); 
		if (ref != null) {
			Activity activity = ref.getActivity();
			if (activity != null && !activity.isDestroyed())
				return ref.getView();
		}
		return null;
	}
	
	static ViewRef getBindViewRef(String key, long id) { 
		synchronized (sBindMap) { 
			final ViewRef ref = sBindMap.get(key);
			final long version = getBindVersion();
			if (ref != null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("getBindViewRef: key=" + key + " id=" + id 
							+ " bindVersion=" + version + " viewRef=" + ref);
				}
				
				Activity activity = ref.getActivity();
				if (activity == null || activity.isDestroyed())
					return null;
				
				if (ref.getItemId() == id) // && ref.getVersion() == version) 
					return ref;
			}
			return null;
		}
	}
	
	static void setBindView(Activity activity, View view, String key, long id) { 
		if (activity == null || key == null) return;
		
		synchronized (sBindMap) { 
			ViewRef viewRef = null;
			List<ViewRef> notUsed = null;
			
			for (ViewRef ref : sBindMap.values()) { 
				View v = ref.getView();
				if (v == null) {
					if (notUsed == null)
						notUsed = new ArrayList<ViewRef>();
					
					notUsed.add(ref);
					
				} else if (v == view) 
					viewRef = ref;
			}
			
			if (notUsed != null) { 
				for (ViewRef ref : notUsed) { 
					sBindMap.remove(ref.getKey());
					
					//if (LOG.isDebugEnabled()) 
					//	LOG.debug("remove ViewRef: " + ref.toString0());
				}
			}
			
			if (view != null && viewRef != null && 
				viewRef.getView() == view && viewRef.getActivity() == activity) { 
				
				if (viewRef.getItemId() == id && key.equals(viewRef.getKey())) { 
					if (LOG.isDebugEnabled()) {
						LOG.debug("reuse ViewRef: viewCount=" + sBindMap.size() 
								+ " " + viewRef.toString0());
					}
					return;
				}
				
				String oldKey = viewRef.getKey();
				viewRef.mKey = key;
				viewRef.mItemId = id;
				
				sBindMap.remove(oldKey);
				sBindMap.put(key, viewRef);
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("add ViewRef: viewCount=" + sBindMap.size() 
							+ " " + viewRef.toString0() + " oldKey=" + oldKey);
				}
				
			} else if (view != null) {
				viewRef = new ViewRef(activity, view, key, id);
				sBindMap.put(key, viewRef);
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("add ViewRef: viewCount=" + sBindMap.size() 
							+ " " + viewRef.toString0());
				}
				
			} else { 
				ViewRef ref = sBindMap.remove(key);
				
				if (ref != null && LOG.isDebugEnabled()) {
					LOG.debug("remove ViewRef: viewCount=" + sBindMap.size() 
							+ " " + ref.toString0());
				}
			}
		}
	}
	
	public static final class ViewRef { 
		private final WeakReference<Activity> mActivityRef;
		private final WeakReference<View> mViewRef;
		private final long mVersion;
		private String mKey = null;
		private long mItemId = 0;
		
		private ViewRef(Activity activity, View view, String key, long id) { 
			mActivityRef = new WeakReference<Activity>(activity);
			mViewRef = new WeakReference<View>(view);
			mVersion = getBindVersion();
			mKey = key;
			mItemId = id;
		}
		
		public Activity getActivity() { return mActivityRef.get(); }
		public View getView() { return mViewRef.get(); }
		public long getVersion() { return mVersion; }
		
		public String getKey() { return mKey; }
		public long getItemId() { return mItemId; }
		
		@Override
		public String toString() { 
			return "ViewRef{" + toString0() + "}";
		}
		
		private String toString0() { 
			Activity activity = getActivity();
			String activityStr = null;
			if (activity != null) {
				if (activity.isDestroyed()) activityStr = "destroyed";
				else activityStr = activity.toString();
			}
			return "key=" + getKey() + " id=" + getItemId() 
					+ " view=" + getView() + " activity=" + activityStr 
					+ " version=" + getVersion();
		}
	}
	
	public static interface BinderCallback { 
		public int getItemViewRes(IActivity activity, DataBinderItem item);
		
		public void addItemView(IActivity activity, DataBinderItem item, 
				ViewGroup container, View view, int index, int count);
		
		public void bindItemView(IActivity activity, DataBinderItem item, View view);
		public void updateItemView(IActivity activity, DataBinderItem item, View view);
		public void onItemViewBinded(IActivity activity, DataBinderItem item);
	}
	
	public static void bindItemView(BinderCallback callback, 
			IActivity activity, ViewGroup container, DataBinderItem... items) { 
		if (callback == null || activity == null || items == null || container == null) 
			return;
		
		container.removeAllViews();
		
		for (int i=0; i < items.length; i++) { 
			DataBinderItem item = items[i];
			if (item != null) 
				bindItemView(callback, activity, container, item, i, items.length);
			else
				callback.addItemView(activity, null, container, null, i, items.length);
		}
		
		container.requestLayout();
	}
	
	private static void bindItemView(BinderCallback callback, 
			IActivity activity, ViewGroup container, DataBinderItem item, 
			int index, int count) { 
		if (callback == null || activity == null || item == null || container == null) 
			return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("bindItemView: item=" + item + " container=" + container 
					+ " index=" + index + " count=" + count);
		}
		
		View view = item.getBindView();
		if (view == null) {
			LayoutInflater inflater = LayoutInflater.from(activity.getActivity());
			view = inflater.inflate(callback.getItemViewRes(activity, item), container, false);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("bindItemView: inflate view, item=" + item
						+ " view=" + view);
			}
		}
		
		ViewGroup parent = (ViewGroup)view.getParent();
		if (parent != null) parent.removeView(view);
		
		callback.addItemView(activity, item, container, view, index, count);
		item.setBindView(activity.getActivity(), view);
		
		if (view.getTag() != item) { 
			if (LOG.isDebugEnabled()) { 
				LOG.debug("bindItemView: bind item, item=" + item 
						+ " view=" + view);
			}
			
			callback.bindItemView(activity, item, view);
			view.setTag(item);
		} else {
			if (LOG.isDebugEnabled()) { 
				LOG.debug("bindItemView: update item, item=" + item 
						+ " view=" + view);
			}
			
			callback.updateItemView(activity, item, view);
		}
		
		callback.onItemViewBinded(activity, item);
		
		view.requestLayout();
		container.requestLayout();
	}
	
	public static void onImageDrawablePreBind(Drawable d, View view) { 
		if (d == null) return;
		
		if (d instanceof SlideDrawable) {
			final SlideDrawable sd = (SlideDrawable)d;
			if (sd.getNumberOfFrames() > 1)
				sd.resetFrame(0, false);
			
		} else if (d instanceof BaseDrawable) { 
			final BaseDrawable bd = (BaseDrawable)d;
			bd.setParentView(view);
		}
	}
	
	public static void onImageDrawableBinded(Drawable d, boolean restartSlide) { 
		onImageDrawableBinded(d, null, restartSlide);
	}
	
	public static void onImageDrawableBinded(Drawable d, Drawable old, boolean restartSlide) { 
		if (d == null) return;
		
		//final Drawable old = getDrawableRef();
		//setImageDrawable(d);
		
		if (d instanceof SlideDrawable) {
			final SlideDrawable sd = (SlideDrawable)d;
			
			boolean started = false;
			if ((old != sd || restartSlide) && sd.getNumberOfFrames() > 1) 
				started = sd.showSlide(false); //old == null || restartSlide);
			
			if (!started) { 
				sd.resetFrame(255, true);
				sd.invalidateSelf();
			}
			
		} else 
			d.invalidateSelf();
	}
	
}
