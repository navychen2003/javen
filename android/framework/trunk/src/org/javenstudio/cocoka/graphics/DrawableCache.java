package org.javenstudio.cocoka.graphics;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;

public class DrawableCache {
	//private static Logger LOG = Logger.getLogger(DrawableCache.class);

	private static class DrawableState {
		public WeakReference<Drawable> wr = null; 
		@SuppressWarnings("unused")
		public boolean purged = false; 
	}
	
	private final Object mLock = new Object(); 
	private final List<DrawableState> mDrawableCache = new ArrayList<DrawableState>();
	
	public DrawableCache() {} 
	
	public void addDrawable(Drawable d) {
		if (d == null) return; 
		
		synchronized (mLock) {
			List<DrawableState> cache = mDrawableCache; 
			for (int i=0; i < cache.size(); i++) getDrawable(i); 
			
			DrawableState ds = new DrawableState(); 
			ds.wr = new WeakReference<Drawable>(d); 
			
			cache.add(ds); 
		}
	}
	
	public int getCount() {
		synchronized (mLock) {
			return mDrawableCache.size(); 
		}
	}
	
	public Drawable getDrawable(int location) {
		synchronized (mLock) {
			List<DrawableState> cache = mDrawableCache; 
			Drawable d = null; 
			
			if (location >= 0 && location < cache.size()) {
				DrawableState ds = cache.get(location); 
				if (ds != null && ds.wr != null) 
					d = ds.wr.get();
				if (d == null) {
					cache.remove(location); 
					//LOG.warn("cached drawable at location "+location+" has purged"); 
				}
			}
			
			return d; 
		}
	}
	
}
