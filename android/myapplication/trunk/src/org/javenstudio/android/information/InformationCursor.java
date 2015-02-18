package org.javenstudio.android.information;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class InformationCursor extends ListDataSetCursor<Information> {

	public interface ActionDataSetGetter { 
		public InformationDataSet getActionDataSet(); 
	}
	
	private final Map<String, InformationDataSet> mInformations; 
	
	public InformationCursor() {
		mInformations = new HashMap<String, InformationDataSet>(); 
	} 
	
	@Override 
	protected void onDataSetted(AbstractDataSet<Information> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof InformationDataSet) 
			addInformationDataSet((InformationDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<Information> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof InformationDataSet) 
			addInformationDataSet((InformationDataSet)data); 
	}
	
	private void addInformationDataSet(InformationDataSet dataSet) { 
		if (dataSet != null) { 
			Information data = dataSet.getInformation(); 
			if (data != null) { 
				//IImageList list = album.getImageList(); 
				//if (list != null) { 
				//	synchronized (this) { 
				//		mInformations.put(list.getLocation(), dataSet); 
				//	}
				//} 
			} 
		}
	}
	
	public InformationDataSet getInformationDataSet(String location) { 
		synchronized (this) { 
			return location != null ? mInformations.get(location) : null; 
		}
	}
	
	@Override 
	protected void onCleared() { 
		super.onCleared(); 
		
		synchronized (this) { 
			mInformations.clear(); 
		}
	}
	
}
