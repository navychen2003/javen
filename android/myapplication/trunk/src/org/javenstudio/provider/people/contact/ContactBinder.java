package org.javenstudio.provider.people.contact;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.provider.ProviderBinderBase;

public class ContactBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback {
	//private static final Logger LOG = Logger.getLogger(ContactBinder.class);
	
	private final ContactProvider mProvider;
	
	public ContactBinder(ContactProvider provider) {
		mProvider = provider;
	}

	public final ContactProvider getProvider() { return mProvider; }

	protected ContactDataSets getContactDataSets() { 
		return getProvider().getContactDataSets();
	}
	
	protected OnContactClickListener getOnContactClickListener() { 
		return getProvider().getOnContactItemClickListener();
	}
	
	@Override
	protected View inflateView(IActivity activity, LayoutInflater inflater, ViewGroup container) { 
		return inflater.inflate(R.layout.provider_list, container, false);
	}
	
	@Override
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.provider_list_listview);
	}
	
	@Override
	public ListAdapter createListAdapter(final IActivity activity) { 
		if (activity == null) return null;
		
		return ContactAdapter.createAdapter(activity, mProvider.getContactDataSets(), 
				this, R.layout.provider_container, 
				getColumnSize(activity));
	}
	
	protected int getColumnSize(IActivity activity) { 
		int size = activity.getResources().getInteger(R.integer.list_column_size);
		return size > 1 ? size : 1;
	}
	
	protected float getColumnSpace(IActivity activity) { 
		return activity.getResources().getDimension(R.dimen.list_column_space_size);
	}
	
	@Override
	protected void onFirstVisibleChanged(IActivity activity, ListAdapter adapter, 
			AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) { 
		if (adapter == null || !(adapter instanceof ContactAdapter)) 
			return;
		
		ContactAdapter contactAdapter = (ContactAdapter)adapter;
		contactAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null || !(adapter instanceof ContactAdapter)) 
			return -1;
		
		ContactAdapter contactAdapter = (ContactAdapter)adapter;
		return contactAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		return getContactItemViewRes();
	}
	
	@Override
	public void addItemView(IActivity activity, DataBinderItem item, 
			ViewGroup container, View view, int index, int count) { 
		if (index > 0) { 
			container.addView(new View(activity.getActivity()), 
					new LinearLayout.LayoutParams((int)getColumnSpace(activity), 
							LinearLayout.LayoutParams.MATCH_PARENT));
		}
		
		if (view == null) 
			view = new View(activity.getActivity());
		
		container.addView(view, 
				new LinearLayout.LayoutParams(0, 
						LinearLayout.LayoutParams.WRAP_CONTENT, 1));
	}
	
	@Override
	public final void bindItemView(final IActivity activity, 
			DataBinderItem item, View view) {
		final ContactItem contactItem = (ContactItem)item;
		onUpdateViews(contactItem, view);
		
		final OnContactClickListener listener = getOnContactClickListener();
		if (listener != null) { 
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						listener.onContactClick(activity.getActivity(), contactItem);
					}
				});
		}
	}
	
	@Override
	public final void updateItemView(final IActivity activity, 
			DataBinderItem item, View view) {
	}
	
	@Override
	public void onItemViewBinded(IActivity activity, DataBinderItem item) {}
	
	protected int getContactItemViewRes() { return R.layout.contact_item; }
	
	protected void onUpdateViews(ContactItem item) { 
		if (item == null) return;
		onUpdateViews(item, item.getBindView());
	}
	
	protected void onUpdateViews(ContactItem item, View view) { 
		if (item == null || view == null) return;
		
		TextView titleView = (TextView)view.findViewById(R.id.contact_item_title);
		if (titleView != null) 
			titleView.setText(item.getTitle());
		
		TextView contentView = (TextView)view.findViewById(R.id.contact_item_text);
		if (contentView != null)
			contentView.setText(item.getSummary());
		
		TextView userView = (TextView)view.findViewById(R.id.contact_item_user_name);
		if (userView != null)
			userView.setText(item.getUserName());
		
		TextView dateView = (TextView)view.findViewById(R.id.contact_item_date);
		if (dateView != null)
			dateView.setText(item.getCreateDate());
		
		ImageView imageView = (ImageView)view.findViewById(R.id.contact_item_user_avatar);
		if (imageView != null) {
			imageView.setImageDrawable(item.getUserIcon(100, 100));
			imageView.setOnClickListener(item.getUserClickListener());
		}
		
		ImageView logoView = (ImageView)view.findViewById(R.id.contact_item_logo);
		if (logoView != null) {
			Drawable d = item.getProviderIcon();
			if (d != null) { 
				logoView.setImageDrawable(d); 
				logoView.setVisibility(View.VISIBLE);
			} else
				logoView.setVisibility(View.GONE);
		}
	}
	
}
