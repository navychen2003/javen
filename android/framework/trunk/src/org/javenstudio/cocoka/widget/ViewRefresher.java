package org.javenstudio.cocoka.widget;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import android.view.View;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.TimerRunner;
import org.javenstudio.common.util.Logger;

public class ViewRefresher extends TimerRunner {
	private static Logger LOG = Logger.getLogger(ViewRefresher.class);

	public static interface ViewBinder { 
		public String getKey(); 
		public boolean shouldRefresh(); 
		public boolean bindView(View view); 
	}
	
	private static class ViewRef { 
		
		protected final ViewBinder mBinder; 
		protected final WeakReference<View> mViewRef; 
		
		public ViewRef(ViewBinder binder, View view) { 
			mBinder = binder; 
			mViewRef = new WeakReference<View>(view); 
		}
		
		public final View getView() { 
			View view = mViewRef.get(); 
			if (view != null && view.getParent() != null) 
				return view; 
			else 
				return null; 
		}
		
		public final ViewBinder getBinder() { 
			return mBinder; 
		}
		
		public final boolean shouldRefresh() { 
			ViewBinder binder = getBinder(); 
			View view = getView(); 
			
			if (binder != null && view != null) 
				return binder.shouldRefresh(); 
			
			return false; 
		}
		
		public final boolean bindView() { 
			ViewBinder binder = getBinder(); 
			View view = getView(); 
			
			if (binder != null && view != null) 
				return binder.bindView(view); 
			
			return false; 
		}
	}
	
	private final HashMap<String, ViewRef> mViews; 
	private final String mName; 
	
	public ViewRefresher(Object obj, long delayMillis) { 
		this("ViewRefresher:"+obj.getClass().getName(), delayMillis);
	}
	
	public ViewRefresher(String name, long delayMillis) { 
		super(ResourceHelper.getHandler(), delayMillis); 
		
		mViews = new HashMap<String, ViewRef>(); 
		mName = name; 
	}
	
	@Override 
	public String getName() { 
		if (mName != null && mName.length() > 0) 
			return mName; 
		
		return super.getName(); 
	}
	
	public void addView(ViewBinder binder, View view) { 
		synchronized (this) { 
			if (view != null && binder != null && binder.shouldRefresh()) { 
				String key = binder.getKey(); 
				if (key != null) { 
					String[] keys = mViews.keySet().toArray(new String[0]); 
					
					for (int i=0; keys != null && i < keys.length; i++) { 
						String skey = keys[i]; 
						if (skey == null) continue; 
						ViewRef viewRef = mViews.get(skey); 
						if (viewRef == null) continue; 
						
						if (viewRef.getView() == view) { 
							mViews.remove(skey); 
							if (LOG.isDebugEnabled()) 
								LOG.debug("ViewRefresher: remove same view \""+skey+"\""); 
						}
					}
					
					mViews.put(key, new ViewRef(binder, view)); 
					if (LOG.isDebugEnabled()) 
						LOG.debug("ViewRefresher: add view \""+key+"\" with binder: "+binder.getClass().getName()); 
				} 
			}
		}
	}
	
	@Override 
	public void run() { 
		int count = 0; 
		
		synchronized (this) { 
			String[] keys = mViews.keySet().toArray(new String[0]); 
			
			for (int i=0; keys != null && i < keys.length; i++) { 
				String key = keys[i]; 
				if (key == null) continue; 
				ViewRef view = mViews.get(key); 
				if (view == null) continue; 
				
				if (view.shouldRefresh() && view.bindView()) { 
					count ++; continue; 
				} 
				
				View v = view.getView(); 
				if (v == null) { 
					mViews.remove(key); 
					if (LOG.isDebugEnabled()) 
						LOG.debug("ViewRefresher: remove view \""+key+"\""); 
				}
			}
		}
		
		if (count <= 0) 
			interrupt(); 
	}
	
}
