package org.javenstudio.provider.account.dashboard;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.app.ColumnAdapter;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.IActivityListener;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.cocoka.widget.AdvancedAdapter;
import org.javenstudio.cocoka.widget.adapter.AbstractAdapter;
import org.javenstudio.common.util.Logger;

public class DashboardAdapter extends ColumnAdapter implements IActivityListener {
	private static final Logger LOG = Logger.getLogger(DashboardAdapter.class);
	
	static DashboardAdapter createAdapter(final IActivity activity, 
			final DashboardDataSets dataSets, final DataBinder.BinderCallback callback, 
			int resource, int colsize) { 
		if (activity == null || dataSets == null || callback == null) 
			return null;
		
		final DashboardAdapter adapter = new DashboardAdapter(activity.getActivity(), 
				dataSets, resource, colsize) { 
				@Override
			    public void registerDataSetObserver(DataSetObserver observer) {
					super.registerDataSetObserver(observer);
					activity.getCallback().addListener(this);
				}
				@Override
				public void unregisterDataSetObserver(DataSetObserver observer) {
					super.unregisterDataSetObserver(observer);
					activity.getCallback().removeListener(this);
				}
			};
		
		adapter.setViewBinder(new AdvancedAdapter.ViewBinder() {
				@Override
				public void onViewBinded(AdvancedAdapter.DataSet dataSet, View view, int position) {
					final DashboardDataSets dataSets = adapter.getDataSets();
					final int colsize = adapter.getColumnSize();
					
					if (LOG.isDebugEnabled())
						LOG.debug("bindRowView: container=" + view + " row=" + position + " colsize=" + colsize);
					
					DashboardItem[] items = new DashboardItem[colsize];
					
					for (int i=0; i < colsize; i++) { 
						DashboardItem item = dataSets.getDashboardItemAt(i + position);
						if (item != null) item.onVisibleChanged(true);
						items[i] = item;
					}
					
					DataBinder.bindItemView(callback, activity, (ViewGroup)view, items);
				}
			});
		
		return adapter;
	}
	
	private DashboardAdapter(Context context, DashboardDataSets data,
            int resource, int colsize) {
		this(new AdapterImpl(context, data, resource), colsize);
	}
	
	private final AdapterImpl mImpl;
	private final int mColumnSize;
	
	private DashboardAdapter(AdapterImpl impl, int colsize) {
		super(impl);
		mColumnSize = colsize > 0 ? colsize : 1;
		mImpl = impl;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent); 
		
		return view; 
	}
	
	public int getColumnSize() { return mColumnSize; }
	private AdapterImpl getImpl() { return mImpl; }
	
	@Override
	public int getColumnCount(int row) { 
		return getColumnSize(); 
	}
	
	@Override
	public int getRowCount() { 
		final int totalCount = getAdapter().getCount();
		final int rem = totalCount % getColumnSize();
		int count = totalCount / getColumnSize();
		if (rem > 0) count ++;
		return count;
	}
	
	@Override
	public int getRowPosition(int row) { 
		if (row <= 0) return 0;
		int position = row * getColumnSize();
		return position;
	}
	
	@Override
	public void onActivityStop(Activity activity) { 
		getImpl().onActivityStop();
	}
	
	public DashboardDataSets getDataSets() { 
		return (DashboardDataSets)getImpl().getDataSets();
	}
	
	public DashboardDataSet getDataSet(int position) { 
		return (DashboardDataSet)getImpl().getDataSet(position);
	}
	
	public void setViewBinder(AdvancedAdapter.ViewBinder viewBinder) { 
		getImpl().setViewBinder(viewBinder);
	}
	
	public void onFirstVisibleChanged(int firstVisibleItem, int visibleItemCount) { 
		int firstItem = firstVisibleItem;
		int visibleCount = visibleItemCount;
		int colsize = getColumnSize();
		
		if (colsize > 1) { 
			firstItem = getRowPosition(firstItem - 1) + 1;
			visibleCount = visibleCount * colsize;
		}
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("onFirstVisibleChanged: firstItem=" + firstVisibleItem 
					+ " visibleCount=" + visibleItemCount + " firstItem2=" + firstItem 
					+ " visibleCount2=" + visibleCount + " colsize=" + colsize);
		}
		
		getDataSets().setFirstVisibleItem(firstItem);
		getImpl().onVisibleChanged(firstItem, visibleCount);
	}
	
	public int getFirstVisibleItem() { 
		int colsize = getColumnSize();
		int firstItem = -1;
		
		int firstVisibleItem = getDataSets().getFirstVisibleItem();
		if (firstVisibleItem >= 0 && firstVisibleItem <= getDataSets().getCount()) 
			firstItem = 1 + (firstVisibleItem - 1) / colsize;
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("getFirstVisibleItem: firstItem2=" + firstVisibleItem 
					+ " firstItem=" + firstItem + " colsize=" + colsize);
		}
		
		return firstItem;
	}
	
	private static class AdapterImpl extends AbstractAdapter<DashboardItem> { 
		public AdapterImpl(Context context, DashboardDataSets data, int resource) {
			super(context, data, resource, new String[]{}, new int[]{});
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent); 
			
			return view; 
		}
		
		final void onActivityStop() { 
			for (int i=0; i < getCount(); i++) {
				DashboardDataSet dataSet = (DashboardDataSet)getDataSet(i);
				DashboardItem item = dataSet != null ? dataSet.getDashboardItem() : null;
				if (item != null) 
					item.onVisibleChanged(false);
			}
		}
		
		final void onVisibleChanged(int firstVisibleItem, int visibleItemCount) { 
			final int visibleFrom = firstVisibleItem - 1; // visibleItem from 1
			final int visibleTo = visibleFrom + visibleItemCount;
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("onVisibleChanged: visibleFrom=" + visibleFrom 
						+ " visibleTo=" + visibleTo);
			}
			
			for (int i=0; i < getCount(); i++) { 
				DashboardDataSet dataSet = (DashboardDataSet)getDataSet(i);
				DashboardItem item = dataSet != null ? dataSet.getDashboardItem() : null;
				if (item != null) {
					boolean visible = (i >= visibleFrom && i < visibleTo);
					boolean changed = item.onVisibleChanged(visible);
					
					if (changed && LOG.isDebugEnabled()) {
						LOG.debug("onVisibleChanged: itemPos=" + i + " item=" + item 
								+ " visible=" + visible + " changed=" + changed);
					}
					
					if (changed && visible) 
						item.onUpdateViewsOnVisible(false);
				}
			}
		}
	}
	
}
