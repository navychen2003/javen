package org.javenstudio.android.information;

import org.javenstudio.cocoka.android.ResourceHelper;

public class InformationItem implements InformationOne.Item {

	public static interface ItemBinder { 
		public InformationBinder getBinder(InformationItem item);
	}
	
	private final long mIdentity = ResourceHelper.getIdentity();
	public final long getIdentity() { return mIdentity; }
	
	private final ItemBinder mBinder;
	private final InformationOne[] mItems;
	private final InformationDataSets mDataSets; 
	private final InformationOne mFirst;
	private final String mLocation;
	
	public InformationItem(ItemBinder binder, String location, 
			InformationOne[] items) { 
		mBinder = binder;
		mItems = items;
		mLocation = location;
		mDataSets = createDataSets(); 
		
		if (items == null) 
			throw new NullPointerException("Information list is null");
		
		InformationOne first = null;
		
		for (int i=0; i < items.length; i++) { 
			InformationOne one = items[i];
			if (one == null) continue;
			if (first == null) first = one;
			
			mDataSets.addInformation(one, false); 
		}
		
		mFirst = first;
		
		if (first == null) 
			throw new NullPointerException("Information first one is null");
	}
	
	protected InformationDataSets createDataSets() { 
		return new InformationDataSets(createCursorFactory()); 
	}
	
	protected InformationCursorFactory createCursorFactory() { 
		return new InformationCursorFactory(); 
	}
	
	public final String getLocation() { return mLocation; }
	public final InformationOne[] getInformations() { return mItems; }
	public final InformationOne getFirstInformation() { return mFirst; }
	
	@Override
	public Object getAttribute(String key) { 
		return getFirstInformation().getAttribute(key);
	}
	
	@Override
	public void setAttribute(String key, Object val) { 
		getFirstInformation().setAttribute(key, val);
	}
	
	public final InformationDataSets getInformationDataSets() { 
		return mDataSets; 
	}
	
	public final InformationBinder getBinder() { 
		return mBinder.getBinder(this); 
	}
	
	protected synchronized void clearDataSets() { 
		getInformationDataSets().clear(); 
	}
	
	protected synchronized void notifyDataSets() { 
		getInformationDataSets().notifyContentChanged(true);
		getInformationDataSets().notifyDataSetChanged();
	}
	
	protected void postAddInformation(final InformationModel model) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					for (int i=0; mItems != null && i < mItems.length; i++) { 
						InformationOne one = mItems[i];
						if (one == null) continue;
						
						getInformationDataSets().addInformation(one, false); 
						model.callbackOnDataSetUpdate(one); 
					}
				}
			});
	}
	
	protected void postClearDataSets() { 
		ResourceHelper.getHandler().post(new Runnable() { 
				public void run() { 
					clearDataSets(); 
				}
			});
	}
	
	protected void postNotifyDataSets() { 
		ResourceHelper.getHandler().post(new Runnable() { 
				public void run() { 
					notifyDataSets();
				}
			});
	}
	
}
