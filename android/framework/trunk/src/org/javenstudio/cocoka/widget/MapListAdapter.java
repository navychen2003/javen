package org.javenstudio.cocoka.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.ContentObserver;
import android.database.DataSetObserver;
import android.view.View;

public class MapListAdapter extends AdvancedAdapter {
	
	public static class MapDataSet implements AdvancedAdapter.DataSet {
		private final ListDataSets mDataSets;
		private final int mPosition; 
		private View mBindedView = null; 
		
		MapDataSet(ListDataSets dataSets, int position) {
			mDataSets = dataSets;
			mPosition = position; 
		}
		
		Map<String, ?> getData() { 
			return mDataSets.getListItem(mPosition);
		}
		
		@Override 
		public Object get(Object key) {
			Map<String, ?> mapItem = getData();
			return mapItem != null ? mapItem.get(key) : null; 
		}
		
		@Override 
		public Object getObject() {
			return getData(); 
		}
		
		@Override 
		public boolean isEnabled() { 
			return true; 
		}
		
		@Override
		public final void setBindedView(View view) { 
			mBindedView = view;
			if (view != null) 
				view.setTag(this);
		}
		
		@Override
		public final View getBindedView() { 
			View view = mBindedView;
			if (view != null && view.getTag() == this) 
				return view;
			
			return null;
		}
	}
	
	private static class ListDataSets implements AdvancedAdapter.DataSets {
		private final List<Map<String, ?>> mListData;
		private final Map<Integer, MapDataSet> mMapDataSets;
		//private long mDataSetChangeTime = 0; 
		
		@SuppressWarnings({"unchecked"})
		ListDataSets(List<? extends Map<String, ?>> data) {
			mListData = (List<Map<String, ?>>)data; 
			mMapDataSets = new HashMap<Integer, MapDataSet>();
		}
		
		synchronized List<? extends Map<String, ?>> getList() {
			return mListData; 
		}
		
		synchronized Map<String, ?> getListItem(int position) { 
			return position >= 0 && position < mListData.size() ? mListData.get(position) : null;
		}
		
		public DataSets create(int count) {
			return new ListDataSets(new ArrayList<Map<String, ?>>(count));
		}
		
		public DataSets create(DataSets data) {
			if (data == null || !(data instanceof ListDataSets)) 
				return null; 
			
			List<? extends Map<String, ?>> list = ((ListDataSets)data).getList(); 
			return new ListDataSets(new ArrayList<Map<String, ?>>(list)); 
		}
		
		public boolean requery() { return true; } 
		public boolean isClosed() { return false; } 
		public void close() {} 
		public synchronized int getCount() { return mListData.size(); } 
		
		public synchronized DataSet getDataSet(int position) { 
			if (position < 0 || position >= mListData.size())
				return null;
			
			MapDataSet dataSet = mMapDataSets.get(position);
			if (dataSet == null) {
				dataSet = new MapDataSet(this, position); 
				mMapDataSets.put(position, dataSet);
			}
			
			return dataSet;
		}
		
		public int getDataId(int position) { return position; }
		
		public synchronized void addDataSet(DataSet data) {
			if (data != null && data instanceof MapDataSet) {
				Map<String, ?> mapItem = ((MapDataSet)data).getData();
				if (mapItem != null) { 
					boolean found = false;
					for (Map<String, ?> item : mListData) { 
						if (item == mapItem) { 
							found = true;
							break;
						}
					}
					if (!found) 
						mListData.add(mapItem);
				}
			}
		}
		
		//public final long getDataSetChangeTime() { 
		//	return mDataSetChangeTime; 
		//}
		
		public void registerContentObserver(ContentObserver observer) {}
		public void unregisterContentObserver(ContentObserver observer) {}
		
		public void registerDataSetObserver(DataSetObserver observer) {}
		public void unregisterDataSetObserver(DataSetObserver observer) {}
	}
	
 
    /**
     * Constructor
     * 
     * @param context The context where the View associated with this SimpleAdapter is running
     * @param data A List of Maps. Each entry in the List corresponds to one row in the list. The
     *        Maps contain the data for each row, and should include all the entries specified in
     *        "from"
     * @param resource Resource identifier of a view layout that defines the views for this list
     *        item. The layout file should include at least those named views defined in "to"
     * @param from A list of column names that will be added to the Map associated with each
     *        item.
     * @param to The views that should display column in the "from" parameter. These should all be
     *        TextViews. The first N views in this list are given the values of the first N columns
     *        in the from parameter.
     */
    public MapListAdapter(Context context, List<? extends Map<String, ?>> data,
            int resource, String[] from, int[] to) {
        super(context, new ListDataSets(data), resource, from, to);
    }

}