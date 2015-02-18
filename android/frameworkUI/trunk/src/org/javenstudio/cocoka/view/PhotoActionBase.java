package org.javenstudio.cocoka.view;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.data.LoadCallback;
import org.javenstudio.common.util.Logger;

public abstract class PhotoActionBase extends PhotoAction 
		implements LoadCallback {
	private static final Logger LOG = Logger.getLogger(PhotoActionBase.class);

	private final Context mContext;
	private final GLPhotoActivity mActivity;
	
	private WeakReference<ListView> mListViewRef = null;
	
	public PhotoActionBase(Context context, String name, int iconRes) { 
		super(name, iconRes);
		mContext = context;
		mActivity = (context instanceof GLPhotoActivity) ? 
				(GLPhotoActivity)context : null;
	}
	
	public final Context getContext() { return mContext; }
	
	@Override
	public void onLoading() { 
		if (LOG.isDebugEnabled())
			LOG.debug("onLoading");
		
		GLPhotoActivity activity = mActivity;
		if (activity != null)
			activity.postShowProgress(false);
	}
	
	@Override
	public void onLoaded() { 
		if (LOG.isDebugEnabled())
			LOG.debug("onLoaded");
		
		GLPhotoActivity activity = mActivity;
		if (activity != null)
			activity.postHideProgress(false);
		
		//postBindListView();
	}
	
	protected void postBindListView() {
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					onPostBindListView();
				}
			});
	}
	
	protected void onPostBindListView() { 
		WeakReference<ListView> listViewRef = mListViewRef;
		ListView listView = listViewRef != null ? listViewRef.get() : null; 
		if (listView != null && isSelected()) 
			bindListView(ResourceHelper.getContext(), listView, false);
	}
	
	@Override
	protected void onActionClick(Activity activity, View root, boolean reclick) {
		super.onActionClick(activity, root, reclick);
		
		final ViewGroup container = (ViewGroup)root;
		final LayoutInflater inflater = LayoutInflater.from(activity);
		View view = inflater.inflate(R.layout.photopage_bottom_list, container, false);
		ListView listView = (ListView)view.findViewById(R.id.photopage_bottom_list);
		
		bindListView(activity, listView, reclick);
		container.addView(view);
	}
	
	protected synchronized void bindListView(Context context, 
			ListView listView, boolean reclick) { 
		if (LOG.isDebugEnabled())
			LOG.debug("bindListView: name=" + getName() + " listView=" + listView);
		
		synchronized (this) {
			listView.setAdapter(createAdapter(context, reclick));
			mListViewRef = new WeakReference<ListView>(listView);
		}
	}
	
	protected ListAdapter createAdapter(Context context, boolean reclick) { 
		return null;
	}
	
}
