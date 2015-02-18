package org.javenstudio.provider.people.contact;

import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.ProviderCallback;

public class ContactProvider extends ProviderBase {

	private final ContactBinder mBinder;
	private final ContactDataSets mDataSets;
	
	private OnContactClickListener mItemClickListener = null;
	private OnContactClickListener mViewClickListener = null;
	private OnContactClickListener mUserClickListener = null;
	
	public ContactProvider(String name, int iconRes) { 
		this(name, iconRes, new ContactFactory());
	}
	
	public ContactProvider(String name, int iconRes, 
			ContactFactory factory) { 
		super(name, iconRes);
		mBinder = factory.createContactBinder(this);
		mDataSets = factory.createContactDataSets(this);
	}

	public ContactDataSets getContactDataSets() { return mDataSets; }
	
	public void setOnContactItemClickListener(OnContactClickListener l) { mItemClickListener = l; }
	public OnContactClickListener getOnContactItemClickListener() { return mItemClickListener; }
	
	public void setOnContactViewClickListener(OnContactClickListener l) { mViewClickListener = l; }
	public OnContactClickListener getOnContactViewClickListener() { return mViewClickListener; }
	
	public void setOnContactUserClickListener(OnContactClickListener l) { mUserClickListener = l; }
	public OnContactClickListener getOnContactUserClickListener() { return mUserClickListener; }
	
	@Override
	public ProviderBinder getBinder() {
		return mBinder;
	}

	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) {}
	
	protected void postClearDataSets() { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getContactDataSets().clear();
				}
			});
	}
	
	protected void postAddContactItem(final ProviderCallback callback, 
			final ContactItem item) { 
		if (callback == null || item == null) 
			return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getContactDataSets().addContactItem(item, false);
					callback.getController().getModel().callbackOnDataSetUpdate(item); 
				}
			});
	}
	
	protected void postNotifyChanged() { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getContactDataSets().notifyContentChanged(true);
					getContactDataSets().notifyDataSetChanged();
				}
			});
	}
	
}
