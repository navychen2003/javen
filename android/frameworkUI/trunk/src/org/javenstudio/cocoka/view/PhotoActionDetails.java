package org.javenstudio.cocoka.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.common.util.Logger;

public class PhotoActionDetails extends PhotoAction 
		implements IMediaDetails {
	private static final Logger LOG = Logger.getLogger(PhotoActionDetails.class);

	public static class DetailItem { 
		private final String mName;
		private final CharSequence mValue;
		
		public DetailItem(String name, CharSequence value) { 
			mName = name; 
			mValue = value;
		}
		
		public String getName() { return mName; }
		public CharSequence getValue() { return mValue; }
	}
	
	private final List<DetailItem> mDetailList = 
			new ArrayList<DetailItem>();
	
	private final Context mContext;
	private final GLPhotoActivity mActivity;
	private WeakReference<ListView> mListViewRef = null;
	
	public PhotoActionDetails(Context context, String name) { 
		this(context, name, 0);
	}
	
	public PhotoActionDetails(Context context, String name, int iconRes) { 
		super(name, iconRes);
		mContext = context;
		mActivity = (context instanceof GLPhotoActivity) ? 
				(GLPhotoActivity)context : null;
	}
	
	public final Context getContext() { return mContext; }
	
	@Override
	public void clear() { 
		synchronized (mDetailList) {
			mDetailList.clear();
		}
	}
	
	@Override
	public int getCount() {
		synchronized (mDetailList) {
			return mDetailList.size();
		}
	}
	
	@Override
	public void add(int nameRes, CharSequence value) { 
		add(mContext.getResources().getString(nameRes), value);
	}
	
	@Override
	public void add(String name, CharSequence value) { 
		if (name == null || name.length() == 0 || value == null || value.length() == 0) 
			return;
		
		synchronized (mDetailList) {
			mDetailList.add(new DetailItem(name, value));
		}
	}
	
	@Override
	public void onLoading() { 
		GLPhotoActivity activity = mActivity;
		if (activity != null)
			activity.postShowProgress(false);
	}
	
	@Override
	public void onLoaded() { 
		GLPhotoActivity activity = mActivity;
		if (activity != null)
			activity.postHideProgress(false);
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					postLoaded();
				}
			});
	}
	
	private void postLoaded() { 
		WeakReference<ListView> listViewRef = mListViewRef;
		ListView listView = listViewRef != null ? listViewRef.get() : null; 
		if (listView != null && isSelected()) 
			bindListView(ResourceHelper.getContext(), listView);
	}
	
	@Override
	protected void onActionClick(Activity activity, View root, boolean reclick) {
		super.onActionClick(activity, root, reclick);
		
		final ViewGroup container = (ViewGroup)root;
		final LayoutInflater inflater = LayoutInflater.from(activity);
		View view = inflater.inflate(R.layout.photopage_bottom_list, container, false);
		ListView listView = (ListView)view.findViewById(R.id.photopage_bottom_list);
		
		bindListView(activity, listView);
		container.addView(view);
	}
	
	private synchronized void bindListView(Context context, ListView listView) { 
		if (LOG.isDebugEnabled())
			LOG.debug("bindListView: name=" + getName() + " listView=" + listView);
		
		synchronized (mDetailList) {
			ListAdapter adapter = createAdapter(context, 
					mDetailList.toArray(new DetailItem[mDetailList.size()]));
			
			listView.setAdapter(adapter);
			mListViewRef = new WeakReference<ListView>(listView);
		}
	}
	
	protected ListAdapter createAdapter(Context context, DetailItem[] items) { 
		return new DetailAdapter(context, items);
	}
	
	static class DetailAdapter extends ArrayAdapter<DetailItem> { 
		public DetailAdapter(Context context, DetailItem[] items) {
			super(context, 0, items);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getView(position, convertView, false);
		}
		
		@Override
	    public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getView(position, convertView, true);
		}
		
		protected View getView(int position, View convertView, boolean dropDown) {
			//final Resources res = getContext().getResources();
			final LayoutInflater inflater = LayoutInflater.from(getContext());
			
			if (convertView == null) 
				convertView = inflater.inflate(R.layout.photopage_details_item, null);
			
			DetailItem item = getItem(position);
			
			TextView nameView = (TextView)convertView.findViewById(R.id.photopage_details_item_name);
			TextView valueView = (TextView)convertView.findViewById(R.id.photopage_details_item_value);
			
			nameView.setText(item.getName());
			valueView.setText(item.getValue());
			
			return convertView;
		}
	}
	
}
