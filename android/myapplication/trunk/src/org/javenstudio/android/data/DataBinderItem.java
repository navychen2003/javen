package org.javenstudio.android.data;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.app.SlideDrawable;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSetObject;
import org.javenstudio.common.util.Logger;

public abstract class DataBinderItem extends AbstractDataSetObject {
	private static final Logger LOG = Logger.getLogger(DataBinderItem.class);
	
	private DataBinder.VersionRef<Drawable> mDrawableRef = null;
	private int mImageViewWidth = 0; 
	private int mImageViewHeight = 0;
	private boolean mVisible = false;
	
	private final long mIdentity = ResourceHelper.getIdentity();
	public final long getIdentity() { return mIdentity; }
	
	@Override
	public Object get(Object key) {
		return null;
	}
	
	protected String getBindViewKey(String name) { 
		int orientation = ResourceHelper.getContext().getResources().getConfiguration().orientation;
		return getIdentity() + "-" + orientation + (name != null ? "-" + name : "");
	}
	
	public final void setBindView(Activity activity, View view) { 
		setBindView(activity, view, null);
	}
	
	public final void setBindView(Activity activity, View view, String name) { 
		DataBinder.setBindView(activity, view, getBindViewKey(name), getIdentity()); 
	}
	
	public final View getBindView() { 
		return getBindView((String)null);
	}
	
	public final View getBindView(String name) { 
		DataBinder.ViewRef ref = getBindViewRef(name);
		return ref != null ? ref.getView() : null;
	}
	
	protected final DataBinder.ViewRef getBindViewRef() { 
		return getBindViewRef(null);
	}
	
	protected final DataBinder.ViewRef getBindViewRef(String name) { 
		return DataBinder.getBindViewRef(getBindViewKey(name), getIdentity());
	}
	
	public void bindHeaderView(View view) {}
	
	public final void setImageViewWidth(int width) { mImageViewWidth = width; }
	public final int getImageViewWidth() { return mImageViewWidth; }
	
	public final void setImageViewHeight(int height) { mImageViewHeight = height; }
	public final int getImageViewHeight() { return mImageViewHeight; }
	
	public final Drawable getCachedImageDrawable() { 
		Drawable d = getDrawableRef(); 
		if (d != null) { 
			if (d instanceof SlideDrawable) { 
				SlideDrawable sd = (SlideDrawable)d;
				if (sd.isDirty()) 
					return null;
			}
		}
		return d;
	}
	
	private Drawable getDrawableRef() { 
		DataBinder.VersionRef<Drawable> ref = mDrawableRef;
		return ref != null ? ref.get() : null; 
	}
	
	public final void setCachedImageDrawable(Drawable d) { 
		Drawable old = getDrawableRef();
		if (old == d) return;
		
		if (old != null && old instanceof SlideDrawable) { 
			SlideDrawable sd = (SlideDrawable)old;
			sd.setDirty();
		}
		
		mDrawableRef = (d != null) ? new DataBinder.VersionRef<Drawable>(d) : null; 
		
		if (d != null && LOG.isDebugEnabled()) 
			LOG.debug("setImageDrawable: itemId=" + getIdentity() + " drawable=" + d);
	}
	
	public final boolean isVisible() { return mVisible; }
	
	public boolean onVisibleChanged(boolean visible) { 
		boolean changed = mVisible != visible;
		mVisible = visible; 
		if (!visible) { 
			//setBindView(null); // donot set null for updateViews
			
			Drawable d = getDrawableRef();
			if (d != null && d instanceof SlideDrawable) { 
				SlideDrawable fd = (SlideDrawable)d;
				fd.hideSlide();
			}
		}
		return changed;
	}
	
	public final void onRemove() { 
		setCachedImageDrawable(null);
	}
	
	public final void onImageDrawablePreBind(Drawable d, View view) { 
		if (d == null) return;
		
		DataBinder.onImageDrawablePreBind(d, view);
	}
	
	public final void onImageDrawableBinded(Drawable d, boolean restartSlide) { 
		if (d == null) return;
		
		final Drawable old = getDrawableRef();
		setCachedImageDrawable(d);
		
		DataBinder.onImageDrawableBinded(d, old, restartSlide);
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{id=" + getIdentity() + "}";
	}
	
}
