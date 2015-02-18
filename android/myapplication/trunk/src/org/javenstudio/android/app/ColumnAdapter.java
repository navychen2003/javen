package org.javenstudio.android.app;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

import org.javenstudio.common.util.Logger;

public class ColumnAdapter implements ListAdapter, SpinnerAdapter {
	private static final Logger LOG = Logger.getLogger(ColumnAdapter.class);

	private final BaseAdapter mAdapter;
	
	public ColumnAdapter(BaseAdapter adapter) { 
		if (adapter == null) throw new NullPointerException("adapter is null");
		mAdapter = adapter;
	}
	
	public final BaseAdapter getAdapter() { 
		return mAdapter; 
	}
	
	public int getColumnCount(int row) { 
		return 1; 
	}
	
	public int getRowCount() { 
		return mAdapter.getCount(); 
	}
	
	public int getRowPosition(int row) { 
		return row;
	}
	
	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		if (LOG.isDebugEnabled())
			LOG.debug("registerDataSetObserver: adapter=" + this + " observer=" + observer);
		
		mAdapter.registerDataSetObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		if (LOG.isDebugEnabled())
			LOG.debug("unregisterDataSetObserver: adapter=" + this + " observer=" + observer);
		
		mAdapter.unregisterDataSetObserver(observer);
	}

	public void notifyDataSetChanged() {
		if (LOG.isDebugEnabled())
			LOG.debug("notifyDataSetChanged: adapter=" + this);
		
		mAdapter.notifyDataSetChanged();
	}
	
	public void notifyDataSetInvalidated() {
		if (LOG.isDebugEnabled())
			LOG.debug("notifyDataSetInvalidated: adapter=" + this);
		
		mAdapter.notifyDataSetInvalidated();
	}
	
	@Override
	public boolean hasStableIds() {
		return mAdapter.hasStableIds();
	}

	@Override
	public boolean isEmpty() {
		return mAdapter.isEmpty();
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		return mAdapter.areAllItemsEnabled();
	}
	
	@Override
	public int getCount() {
		return getRowCount();
	}

	@Override
	public Object getItem(int row) {
		return mAdapter.getItem(getRowPosition(row));
	}

	@Override
	public long getItemId(int row) {
		return mAdapter.getItemId(getRowPosition(row));
	}

	@Override
	public View getView(int row, View convertView, ViewGroup parent) {
		return mAdapter.getView(getRowPosition(row), convertView, parent);
	}

	@Override
	public View getDropDownView(int row, View convertView, ViewGroup parent) {
		return mAdapter.getDropDownView(getRowPosition(row), convertView, parent);
	}

	@Override
	public int getItemViewType(int row) {
		return 0;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}
	
	@Override
	public boolean isEnabled(int row) {
		return false;
	}

}
