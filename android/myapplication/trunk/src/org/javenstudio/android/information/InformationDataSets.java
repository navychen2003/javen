package org.javenstudio.android.information;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class InformationDataSets extends AbstractDataSets<Information> {
	private static final Logger LOG = Logger.getLogger(InformationDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public InformationDataSets(InformationCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<Information> createDataSets(IDataSetCursorFactory<Information> factory) { 
		return new InformationDataSets((InformationCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<Information> createDataSet(IDataSetObject data) { 
		return new InformationDataSet(this, (Information)data); 
	}
	
	public InformationDataSet getInformationDataSet(int position) { 
		return (InformationDataSet)getDataSet(position); 
	}
	
	public InformationDataSet getInformationDataSet(String location) { 
		return getInformationCursor().getInformationDataSet(location); 
	}
	
	public InformationCursor getInformationCursor() { 
		return (InformationCursor)getCursor(); 
	}
	
	public Information getInformationAt(int position) { 
		InformationDataSet dataSet = getInformationDataSet(position); 
		if (dataSet != null) 
			return dataSet.getInformation(); 
		
		return null; 
	}
	
	public void addInformation(Information item, boolean notify) { 
		addData(item, notify);
		
		if (LOG.isDebugEnabled())
			LOG.debug("addInformation: item=" + item);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			Information item = getInformationAt(i);
			if (item != null) 
				item.onRemove();
		}
		
		mFirstVisibleItem = -1;
		super.clear();
	}
	
	final void setFirstVisibleItem(int item) { 
		if (LOG.isDebugEnabled())
			LOG.debug("setFirstVisibleItem: firstItem=" + item);
		
		mFirstVisibleItem = item; 
	}
	
	final int getFirstVisibleItem() { return mFirstVisibleItem; }
	
}
