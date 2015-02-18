package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.database.ContentObserver;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.cocoka.android.ResourceHelper;

public class ExpandableAdapter extends BaseExpandableListAdapter { 
	
	public static final int STAT_NONE = 0; 
	public static final int STAT_GROUP_EXPAND_MIN = 1; 
	public static final int STAT_GROUP_EXPAND_MAX = 2; 
	public static final int STAT_CHILD_LAST = 3; 
	
	public static interface DataSet { 
		public Object get(Object key, int stat); 
		public Object getObject(); 
		public void setBindedView(View view); 
		public View getBindedView(); 
	}
	
	public static interface ChildDataSet extends DataSet {
	}
	
	public static interface GroupDataSet extends DataSet {
		public boolean requery();
		public boolean isClosed();
		public void close();
		
		public int getChildrenCount();
		public long getChildId(int childPosition); 
		public ChildDataSet getChildDataSet(int childPosition);
		public boolean isChildSelectable(int childPosition);
	}
	
	public static interface ExpandableDataSets {
		public boolean requery();
		public boolean isClosed();
		public void close();
		
		public long getGroupId(int groupPosition); 
		public int getGroupCount(); 
		public GroupDataSet getGroupDataSet(int groupPosition);
		public boolean hasStableIds();
		
		public void registerContentObserver(ContentObserver observer);
		public void unregisterContentObserver(ContentObserver observer);
		public void registerDataSetObserver(DataSetObserver observer);
		public void unregisterDataSetObserver(DataSetObserver observer);
	}
	
	public static interface ViewResource {
    	public int getGroupResource(int groupPosition, boolean isExpanded, Object data); 
    	public int getChildResource(int groupPosition, int childPosition, boolean isLastChild, Object data); 
    	public void onViewSetted(DataSet dataSet, String fromkey, View v, Object data); 
    }
	
	public static interface TextViewBinder {
		public boolean setViewText(String name, TextView view, String data); 
    }
	
	public static interface ViewBinder {
		public boolean setViewValue(DataSet dataSet, String name, View view, Object data, int stat); 
		public void onViewBinded(DataSet dataSet, View view, int stat); 
	}
	
	private final Context mContext; 
	private final ExpandableDataSets mDataSets; 
	private final LayoutInflater mInflater;
	
	protected final ChangeObserver mChangeObserver;
	protected final DataSetObserver mDataSetObserver = new MyDataSetObserver();
	protected boolean mDataValid;
	protected boolean mAutoRequery = true;
	
	private ViewResource mViewResource = null; 
	private ViewBinder mViewBinder = null;
	private TextViewBinder mTextBinder = null; 
	
	private final int mGroupResource; 
	private final int mChildResource; 
	private final int[] mGroupTo, mChildTo;
    private final String[] mGroupFrom, mChildFrom;
	
    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            onContentChanged();
        }
    }
    
    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
        	mDataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
        	mDataValid = false;
            notifyDataSetInvalidated();
        }
    }
    
    
	public ExpandableAdapter(Context context, ExpandableDataSets data, 
			int groupResource, String[] groupFrom, int[] groupTo, 
			int childResource, String[] childFrom, int[] childTo) {
		boolean dataPresent = data != null; 
		
		mContext = context; 
		mDataSets = data; 
		mDataValid = dataPresent;
		mGroupResource = groupResource; 
		mChildResource = childResource; 
		mGroupFrom = groupFrom; 
		mGroupTo = groupTo; 
		mChildFrom = childFrom; 
		mChildTo = childTo; 
		
		mChangeObserver = new ChangeObserver();
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		if (dataPresent) {
            data.registerContentObserver(mChangeObserver);
            data.registerDataSetObserver(mDataSetObserver);
        }
	}
	
	public final Context getContext() { return mContext; } 
	public final ExpandableDataSets getExpandableDataSets() { return mDataSets; } 
	
	/**
     * Called when the {@link ContentObserver} on the cursor receives a change notification.
     * The default implementation provides the auto-requery logic, but may be overridden by
     * sub classes.
     * 
     * @see ContentObserver#onChange(boolean)
     */
    protected void onContentChanged() {
        if (mAutoRequery && mDataSets != null && !mDataSets.isClosed()) {
            mDataValid = mDataSets.requery();
        }
    }
	
	public final void setViewResource(ViewResource viewRes) {
    	mViewResource = viewRes; 
    }
	
	@Override 
	public long getGroupId(int groupPosition) { 
		if (mDataValid && mDataSets != null)
			return mDataSets.getGroupId(groupPosition); 
		else 
			return 0; 
	}
	
	@Override 
	public long getChildId(int groupPosition, int childPosition) { 
		GroupDataSet groupDataSet = getGroupDataSet(groupPosition); 
		if (groupDataSet != null)
			return groupDataSet.getChildId(childPosition); 
		
		return 0; 
	}
	
	@Override 
	public int getGroupCount() { 
		if (mDataValid && mDataSets != null)
			return mDataSets.getGroupCount(); 
		else 
			return 0; 
	}
	
	@Override 
	public int getChildrenCount(int groupPosition) { 
		GroupDataSet groupDataSet = getGroupDataSet(groupPosition); 
		if (groupDataSet != null)
			return groupDataSet.getChildrenCount(); 
		else 
			return 0; 
	}
	
	@Override 
	public Object getGroup(int groupPosition) { 
		return getGroupDataSet(groupPosition); 
	}
	
	@Override 
	public Object getChild(int groupPosition, int childPosition) { 
		return getChildDataSet(groupPosition, childPosition); 
	}
	
	@Override 
	public boolean hasStableIds() { 
		if (mDataValid && mDataSets != null)
			return mDataSets.hasStableIds(); 
		else 
			return false; 
	}
	
	@Override 
	public boolean isChildSelectable(int groupPosition, int childPosition) { 
		GroupDataSet groupDataSet = getGroupDataSet(groupPosition); 
		if (groupDataSet != null)
			return groupDataSet.isChildSelectable(childPosition); 
		else 
			return false; 
	}
	
	public GroupDataSet getGroupDataSet(int groupPosition) { 
		if (mDataValid && mDataSets != null)
			return mDataSets.getGroupDataSet(groupPosition); 
		else 
			return null; 
	}
	
	public ChildDataSet getChildDataSet(int groupPosition, int childPosition) { 
		GroupDataSet groupDataSet = getGroupDataSet(groupPosition); 
		if (groupDataSet != null)
			return groupDataSet.getChildDataSet(childPosition); 
		else 
			return null; 
	}
	
	protected int getGroupResource(GroupDataSet dataSet, int groupPosition, boolean isExpanded) {
    	int resource = mGroupResource; 
    	ViewResource viewRes = mViewResource; 
    	
    	if (viewRes != null) {
    		Object data = dataSet != null ? dataSet.getObject() : null; 
    		if (data != null) { 
    			int res = viewRes.getGroupResource(groupPosition, isExpanded, data); 
    			if (res != 0) 
    				resource = res; 
    		}
    	}
    	
    	return resource; 
    }
	
	@Override 
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) { 
		final GroupDataSet dataSet = getGroupDataSet(groupPosition); 
		final int resource = getGroupResource(dataSet, groupPosition, isExpanded); 
		
		View view = null; 
		if (view == null) {
			int stat = isExpanded ? STAT_GROUP_EXPAND_MAX : STAT_GROUP_EXPAND_MIN; 
        	view = createViewFromResource(dataSet, convertView, parent, resource, mGroupFrom, mGroupTo, stat);
        }
        
        return view; 
	}
	
	protected int getChildResource(ChildDataSet dataSet, int groupPosition, int childPosition, boolean isLastChild) {
    	int resource = mChildResource; 
    	ViewResource viewRes = mViewResource; 
    	
    	if (viewRes != null) {
    		Object data = dataSet != null ? dataSet.getObject() : null; 
    		if (data != null) { 
    			int res = viewRes.getChildResource(groupPosition, childPosition, isLastChild, data); 
    			if (res != 0) 
    				resource = res; 
    		}
    	}
    	
    	return resource; 
    }
	
	@Override 
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) { 
		final ChildDataSet dataSet = getChildDataSet(groupPosition, childPosition); 
		final int resource = getChildResource(dataSet, groupPosition, childPosition, isLastChild); 
		
		View view = null; 
		if (view == null) {
			int stat = isLastChild ? STAT_CHILD_LAST : STAT_NONE; 
        	view = createViewFromResource(dataSet, convertView, parent, resource, mChildFrom, mChildTo, stat);
        }
        
        return view; 
	}
	
	private class DataSetTag { 
		public final DataSet mDataSet; 
		public final int mStat; 
		
		public DataSetTag(DataSet dataSet, int stat) { 
			mDataSet = dataSet; 
			mStat = stat; 
		}
	}
	
	protected View createViewFromResource(DataSet dataSet, View convertView,
            ViewGroup parent, int resource, String[] from, int[] to, int stat) {
		View v;
		
		if (dataSet != null) { 
			v = dataSet.getBindedView(); 
			if (v != null) { 
				Object tag = v.getTag(); 
				if (tag != null && tag instanceof DataSetTag) { 
					DataSetTag dt = (DataSetTag)tag;
					if (dataSet == dt.mDataSet && stat == dt.mStat)
						return v;
				}
				v = null;
			}
		}
		
        if (convertView == null || convertView.getId() != resource) {
            v = mInflater.inflate(resource, parent, false);
        } else {
            v = convertView;
        }

        bindView(dataSet, v, from, to, stat);
 
        if (dataSet != null) { 
        	v.setTag(new DataSetTag(dataSet, stat));
        	dataSet.setBindedView(v); 
        }
        
        return v;
    }
	
	public TextViewBinder getTextBinder() {
    	return mTextBinder; 
    }
    
    public void setTextBinder(TextViewBinder binder) {
    	mTextBinder = binder; 
    }
    
    /**
     * Returns the {@link ViewBinder} used to bind data to views.
     *
     * @return a ViewBinder or null if the binder does not exist
     *
     * @see #setViewBinder(android.widget.SimpleAdapter.ViewBinder)
     */
    public ViewBinder getViewBinder() {
        return mViewBinder;
    }
 
    /**
     * Sets the binder used to bind data to views.
     *
     * @param viewBinder the binder used to bind data to views, can be null to
     *        remove the existing binder
     *
     * @see #getViewBinder()
     */
    public void setViewBinder(ViewBinder viewBinder) {
        mViewBinder = viewBinder;
    }
	
	private void bindView(final DataSet dataSet, final View view, final String[] from, final int[] to, int stat) {
        if (dataSet == null || view == null) 
            return;
 
        final ViewResource viewRes = mViewResource; 
        final ViewBinder binder = mViewBinder;
        final TextViewBinder textbinder = mTextBinder; 
        final int count = to.length;
 
        for (int i = 0; i < count; i++) {
        	final String fromkey = from[i]; 
        	final int resid = to[i]; 
            final View v = view.findViewById(resid);
            if (v != null) {
                final Object data = dataSet.get(fromkey, stat);
                
                boolean bound = false;
                if (binder != null) 
                    bound = binder.setViewValue(dataSet, fromkey, v, data, stat);
 
                String text = data == null ? "" : data.toString();
                if (text == null) 
                    text = "";
                
                if (!bound) {
                	boolean binded = false; 
                	
                    if (v instanceof Checkable) {
                        if (data instanceof Boolean) {
                            ((Checkable) v).setChecked((Boolean) data);
                            binded = true;
                        }
                        //else if (v instanceof TextView) {
                        //    // Note: keep the instanceof TextView check at the bottom of these
                        //    // ifs since a lot of views are TextViews (e.g. CheckBoxes).
                        //    setViewText((TextView) v, text);
                        //} else {
                        //    throw new IllegalStateException(v.getClass().getName() +
                        //            " should be bound to a Boolean, not a " +
                        //            (data == null ? "<unknown type>" : data.getClass()));
                        //}
                    }
                    
                    if (binded == false) { 
	                    if (v instanceof TextView) {
	                        // Note: keep the instanceof TextView check at the bottom of these
	                        // if since a lot of views are TextViews (e.g. CheckBoxes).
	                    	if (textbinder != null) 
	                    		binded = textbinder.setViewText(fromkey, (TextView)v, text); 
	                    	if (binded == false) 
	                    		setViewText((TextView) v, text);
	                    	
	                    } else if (v instanceof ImageView) {
	                        if (data instanceof Integer) {
	                            setViewImage((ImageView) v, (Integer) data);
	                        } else if (data instanceof Bitmap) {
	                            setViewImage((ImageView) v, (Bitmap) data); 
	                        } else if (data instanceof Drawable) {
	                            setViewImage((ImageView) v, (Drawable) data); 
	                        } else {
	                            //setViewImage((ImageView) v, text);
	                        }
	                        
	                    } else {
	                        throw new IllegalStateException(v.getClass().getName() 
	                        		+ " is not a view that can be bounds by this AdvancedAdapter, " 
	                        		+ "you should setViewBinder() to binde it.");
	                    }
                    }
                }
                
                if (viewRes != null) 
                	viewRes.onViewSetted(dataSet, fromkey, v, data);
            }
        }
        
        if (binder != null) 
            binder.onViewBinded(dataSet, view, stat);
    }
	
	/**
     * Called by bindView() to set the image for an ImageView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to an ImageView.
     *
     * This method is called instead of {@link #setViewImage(ImageView, String)}
     * if the supplied data is an int or Integer.
     *
     * @param v ImageView to receive an image
     * @param value the value retrieved from the data set
     *
     * @see #setViewImage(ImageView, String)
     */
    public void setViewImage(ImageView v, int value) {
        v.setImageDrawable(ResourceHelper.getResourceContext().getDrawable(value));
    }
 
    /**
     * Called by bindView() to set the image for an ImageView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to an ImageView.
     *
     * By default, the value will be treated as an image resource. If the
     * value cannot be used as an image resource, the value is used as an
     * image Uri.
     *
     * This method is called instead of {@link #setViewImage(ImageView, int)}
     * if the supplied data is not an int or Integer.
     *
     * @param v ImageView to receive an image
     * @param value the value retrieved from the data set
     *
     * @see #setViewImage(ImageView, int) 
     */
    public void setViewImage(ImageView v, String value) {
        try {
            v.setImageDrawable(ResourceHelper.getResourceContext().getDrawable(Integer.parseInt(value)));
        } catch (NumberFormatException nfe) {
            v.setImageURI(Uri.parse(value));
        }
    }
    
    public void setViewImage(ImageView v, Bitmap value) {
        v.setImageBitmap(value);
    }
    
    public void setViewImage(ImageView v, Drawable value) {
        v.setImageDrawable(value);
    }
 
    /**
     * Called by bindView() to set the text for a TextView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to an TextView.
     *
     * @param v TextView to receive text
     * @param text the text to be set for the TextView
     */
    public void setViewText(TextView v, String text) {
        v.setText(text);
    }
	
}
