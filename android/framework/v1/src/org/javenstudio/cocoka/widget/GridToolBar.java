package org.javenstudio.cocoka.widget;

import java.util.List; 
import java.util.ArrayList; 

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListAdapter;

import org.javenstudio.cocoka.widget.SimpleAdapter;

public class GridToolBar extends GridView {

	public static class GridData implements SimpleAdapter.SimpleData {
		public Object get(Object key) {
			return null; 
		}
	}
	
	public static class GridAdapter extends SimpleAdapter.SimpleAdapterImpl {
		public GridAdapter(Context context, GridDataSets data,
	            int resource, String[] from, int[] to) {
			super(context, data, resource, from, to);
		}
	}
	
	public static class GridDataSets extends SimpleAdapter.SimpleDataSets {
		public GridDataSets(SimpleAdapter.SimpleCursorFactory factory) {
			super(factory); 
		}
	}
	
	public static class GridCursorFactory implements SimpleAdapter.SimpleCursorFactory {
		public GridCursorFactory() {} 
		
		public SimpleAdapter.SimpleCursor create() {
			return new ListGridCursor(); 
		}
	}
	
	public static class ListGridCursor implements SimpleAdapter.SimpleCursor {
		protected List<SimpleAdapter.SimpleDataSet> mList = new ArrayList<SimpleAdapter.SimpleDataSet>(); 
		
		public ListGridCursor() { }
		
		public boolean requery() { return true; } 
		public boolean isClosed() { return false; } 
		public void close() {} 
		
		public int getCount() { return mList.size(); } 
		
		public void recycle() {
			synchronized (this) {
				if (mList == null) 
					return; 
				
				for (SimpleAdapter.SimpleDataSet dataSet : mList) {
					if (dataSet == null) 
						continue; 
					
					dataSet.recycle(); 
				}
			}
		}
		
		public SimpleAdapter.SimpleDataSet getDataSet(int position) { 
			return position >= 0 && position < mList.size() ? mList.get(position) : null; 
		}
		
		public int getDataId(int position) { return position; }
		
		public void setDataSet(int position, SimpleAdapter.SimpleDataSet data) {
			synchronized (this) {
				if (position >= 0 && position < mList.size()) {
					if (data != null) {
						mList.set(position, data); 
					}
				}
			}
		}
		
		public void addDataSet(SimpleAdapter.SimpleDataSet data) {
			synchronized (this) {
				if (data != null) {
					mList.add(data); 
				}
			}
		}
		
		public void clear() {
			synchronized (this) {
				if (mList != null && mList.size() > 0) {
					mList.clear(); 
				}
			}
		}
	}
	
	public GridToolBar(Context context) {
		super(context); 
		initToolBar(); 
	}
	
	public GridToolBar(Context context, AttributeSet attrs) {
		super(context, attrs); 
		initToolBar(); 
	}
	
	public GridToolBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle); 
		initToolBar(); 
	}
	
	protected void initToolBar() {
    	// do nothing
    }
    
	@Override 
	public void setBackgroundResource(int resid) {
		ViewHelper.setBackgroundResource(this, resid); 
	}
	
	protected GridCursorFactory createCursorFactory() {
		return new GridCursorFactory(); 
	}
	
	protected ListAdapter createAdapter(int resource, String[] from, int[] to) {
		GridCursorFactory factory = createCursorFactory(); 
    	GridDataSets dataSets = new GridDataSets(factory); 
    	
		return new GridAdapter(getContext(), dataSets, resource, from, to); 
	}
    
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
    	checkView(child); 
        super.addView(child, index, params);
        initView(child); 
    }

    @Override
    public void addView(View child) {
    	checkView(child); 
        super.addView(child);
        initView(child); 
    }

    @Override
    public void addView(View child, int index) {
    	checkView(child); 
        super.addView(child, index);
        initView(child); 
    }

    @Override
    public void addView(View child, int width, int height) {
    	checkView(child); 
        super.addView(child, width, height);
        initView(child); 
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
    	checkView(child); 
        super.addView(child, params);
        initView(child); 
    }
    
    protected void initView(View child) {
        // do nothing
    }
    
    protected void checkView(View child) {
    	//if (!(child instanceof Button)) {
        //    throw new IllegalArgumentException("A ToolBar can only have ToolBar.Button children.");
        //}
    }
    
}
