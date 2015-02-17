package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.database.ContentObserver;
import android.database.DataSetObserver;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.common.util.Logger;

public class AdvancedAdapter extends BaseAdapter implements Filterable {
	private static final Logger LOG = Logger.getLogger(AdvancedAdapter.class);
	
	public interface DataSet {
		public Object get(Object key); 
		public Object getObject(); 
		public boolean isEnabled(); 
		public void setBindedView(View view); 
		public View getBindedView(); 
	}
	
	public interface DataSets {
		public DataSets create(int count); 
		public DataSets create(DataSets data); 
		
		public boolean requery();
		public boolean isClosed();
		public void close();
		public int getCount();
		
		public DataSet getDataSet(int position); 
		public int getDataId(int position); 
		public void addDataSet(DataSet data); 
		
		public void registerContentObserver(ContentObserver observer);
		public void unregisterContentObserver(ContentObserver observer);
		public void registerDataSetObserver(DataSetObserver observer);
		public void unregisterDataSetObserver(DataSetObserver observer);
	}
	
	protected final Context mContext;
	
	protected boolean mDataValid;
	protected boolean mAutoRequery = true;
	
	protected final ChangeObserver mChangeObserver = new ChangeObserver();
	protected final DataSetObserver mDataSetObserver = new MyDataSetObserver();
	
    private int[] mTo;
    private String[] mFrom;
    private ViewBinder mViewBinder;
    private ViewResource mViewResource; 
    private TextViewBinder mTextBinder; 
 
    private DataSets mData;
 
    private int mResource;
    private int mDropDownResource;
    private LayoutInflater mInflater;
 
    private SimpleFilter mFilter;
    private DataSets mUnfilteredData;
 
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
    public AdvancedAdapter(Context context, DataSets data,
            int resource, String[] from, int[] to) {
    	boolean dataPresent = data != null; 
    	
        mData = data;
        mResource = mDropDownResource = resource;
        mFrom = from;
        mTo = to;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDataValid = dataPresent;
        mContext = context;
        //mChangeObserver = new ChangeObserver();
        mViewResource = null; 
        mTextBinder = null; 
        mViewBinder = null; 
        
        if (dataPresent) {
            //data.registerContentObserver(mChangeObserver);
            //data.registerDataSetObserver(mDataSetObserver);
        }
    }

    public final Context getContext() {
    	return mContext;
    }
    
    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
    	if (LOG.isDebugEnabled())
    		LOG.debug("registerDataSetObserver: adapter=" + this + " observer=" + observer);
    	
    	super.registerDataSetObserver(observer);
    	
    	DataSets data = mData;
    	if (data != null) {
    		data.registerContentObserver(mChangeObserver);
            data.registerDataSetObserver(mDataSetObserver);
    	}
    }
    
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    	if (LOG.isDebugEnabled())
    		LOG.debug("unregisterDataSetObserver: adapter=" + this + " observer=" + observer);
    	
    	super.unregisterDataSetObserver(observer);
    	
    	DataSets data = mData;
    	if (data != null) {
    		data.unregisterContentObserver(mChangeObserver);
    		data.unregisterDataSetObserver(mDataSetObserver);
    	}
    }
    
    public void setAutoRequery(boolean autoRequery) {
    	mAutoRequery = autoRequery; 
    }
    
    /**
     * Returns the datasets.
     * @return the datasets.
     */
    public DataSets getDataSets() {
        return mData;
    }
    
    /**
     * @see android.widget.Adapter#getCount()
     */
    @Override 
    public int getCount() {
    	if (mDataValid && mData != null) {
    		return mData.getCount();
    	} else {
            return 0;
        }
    }
 
    @Override 
    public boolean isEnabled(int position) {
    	DataSet set = getDataSet(position); 
    	if (set != null) { 
    		return set.isEnabled(); 
    	} else { 
    		return false; 
    	}
    }
    
    /**
     * @see android.widget.Adapter#getItem(int)
     */
    @Override 
    public Object getItem(int position) {
    	DataSet set = getDataSet(position); 
    	if (set != null) {
    		return set.getObject(); 
    	} else {
    		return null; 
    	}
    }
    
    public DataSet getDataSet(int position) {
    	if (mDataValid && mData != null) {
    		return mData.getDataSet(position);
    	} else {
            return null;
        }
    }
 
    /**
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override 
    public long getItemId(int position) {
    	if (mDataValid && mData != null) {
    		return mData.getDataId(position);
    	} else {
    		return 0; 
    	}
    }

    /**
     * Called when the {@link ContentObserver} on the cursor receives a change notification.
     * The default implementation provides the auto-requery logic, but may be overridden by
     * sub classes.
     * 
     * @see ContentObserver#onChange(boolean)
     */
    protected void onContentChanged() {
        if (mAutoRequery && mData != null && !mData.isClosed()) {
            mDataValid = mData.requery();
        }
    }
    
    private int getResource(int position) {
    	int resource = mResource; 
    	ViewResource viewRes = mViewResource; 
    	
    	DataSet dataSet = getDataSet(position); 
    	
    	if (viewRes != null) {
    		Object data = dataSet != null ? dataSet.getObject() : null; 
    		if (data != null) { 
    			int res = viewRes.getResource(position, data); 
    			if (res != 0) 
    				resource = res; 
    		}
    	}
    	
    	return resource; 
    }
    
    /**
     * @see android.widget.Adapter#getView(int, View, ViewGroup)
     */
    @Override 
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, 0);
    }

    private View createViewFromResource(int position, View convertView,
            ViewGroup parent, int resource) {
    	final DataSet dataSet = getDataSet(position);
        final View v;
        
        if (dataSet != null) { 
			View view = dataSet.getBindedView(); 
			if (view != null && convertView == null) { 
				convertView = view;
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("createViewFromResource: use binded view," 
							+ " position=" + position + " convertView=" + view);
				}
			}
		}
        
        if (convertView == null) {
        	if (resource == 0) 
        		resource = getResource(position);
        	
            v = mInflater.inflate(resource, parent, false);
        } else {
            v = convertView;
        }
 
        //if (LOG.isDebugEnabled()) { 
        //	LOG.debug("createViewFromResource: position=" + position 
        //			+ " view=" + v + " convertView=" + convertView 
        //			+ " resource=" + resource);
        //}
        
        bindView(dataSet, position, v);
 
        if (dataSet != null) { 
        	dataSet.setBindedView(v); 
        }
        
        return v;
    }
 
    /**
     * <p>Sets the layout resource to create the drop down views.</p>
     *
     * @param resource the layout resource defining the drop down views
     * @see #getDropDownView(int, android.view.View, android.view.ViewGroup)
     */
    public void setDropDownViewResource(int resource) {
        mDropDownResource = resource;
    }
 
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mDropDownResource);
    }
 
    private void bindView(DataSet dataSet, int position, View view) {
        if (dataSet == null || view == null) 
            return;
 
        final ViewResource viewRes = mViewResource; 
        final ViewBinder binder = mViewBinder;
        final TextViewBinder textbinder = mTextBinder; 
        final String[] from = mFrom;
        final int[] to = mTo;
        final int count = to.length;
 
        for (int i = 0; i < count; i++) {
        	final String fromkey = from[i]; 
        	final int resid = to[i]; 
            final View v = view.findViewById(resid);
            if (v != null) {
                final Object data = dataSet.get(fromkey);
                
                boolean bound = false;
                if (binder != null) 
                    bound = binder.setViewValue(dataSet, fromkey, v, data);
 
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
                        //    throw new IllegalStateException(v.getClass().getName() 
                        //    		+ " should be bound to a Boolean, not a " 
                        //    		+ (data == null ? "<unknown type>" : data.getClass()));
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
	                        } else if (data instanceof BitmapRef) {
	                            setViewImage((ImageView) v, (BitmapRef) data); 
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
                	viewRes.onViewSetted(position, fromkey, v, data);
            }
        }
        
        if (binder != null) {
            binder.onViewBinded(dataSet, view, position);
        }
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
    
    public void setViewImage(ImageView v, BitmapRef value) {
        v.setImageBitmap(value.get());
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
 
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new SimpleFilter();
        }
        return mFilter;
    }
 
    public void setViewResource(ViewResource viewRes) {
    	this.mViewResource = viewRes; 
    }
    
    public static class ViewResource {
    	public int getResource(int position, Object data) { 
    		return 0;
    	}
    	
    	public void onViewSetted(int position, String fromkey, View v, Object data) { 
    		// do nothing
    	}
    }
    
    public static class TextViewBinder {
    	public boolean setViewText(String name, TextView view, String data) { 
    		return false;
    	}
    }
    
    /**
     * This class can be used by external clients of SimpleAdapter to bind
     * values to views.
     *
     * You should use this class to bind values to views that are not
     * directly supported by SimpleAdapter or to change the way binding
     * occurs for views supported by SimpleAdapter.
     *
     * @see SimpleAdapter#setViewImage(ImageView, int)
     * @see SimpleAdapter#setViewImage(ImageView, String)
     * @see SimpleAdapter#setViewText(TextView, String)
     */
    public static class ViewBinder {
        /**
         * Binds the specified data to the specified view.
         *
         * When binding is handled by this ViewBinder, this method must return true.
         * If this method returns false, SimpleAdapter will attempts to handle
         * the binding on its own.
         *
         * @param dataSet the dataSet of view to bind
         * @param name the name of view to bind the data
         * @param view the view to bind the data to
         * @param data the data to bind to the view
         *
         * @return true if the data was bound to the view, false otherwise
         */
    	public boolean setViewValue(DataSet dataSet, String name, View view, Object data) { 
    		return false;
    	}
        
    	public void onViewBinded(DataSet dataSet, View view, int position) { 
    		// do nothing
    	}
    }
 
    /**
     * <p>An array filters constrains the content of the array adapter with
     * a prefix. Each item that does not start with the supplied prefix
     * is removed from the list.</p>
     */
    private class SimpleFilter extends Filter {
 
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
 
            if (mUnfilteredData == null) {
                mUnfilteredData = mData.create(mData);
            }
 
            if (prefix == null || prefix.length() == 0) {
                DataSets list = mUnfilteredData;
                results.values = list;
                results.count = list.getCount();
            } else {
                String prefixString = prefix.toString().toLowerCase();
 
                DataSets unfilteredValues = mUnfilteredData;
                int count = unfilteredValues.getCount();
 
                DataSets newValues = mData.create(count);
 
                for (int i = 0; i < count; i++) {
                    DataSet h = unfilteredValues.getDataSet(i);
                    if (h != null) {
                        
                        int len = mTo.length;
 
                        for (int j=0; j<len; j++) {
                            String str =  (String)h.get(mFrom[j]);
                            
                            String[] words = str.split(" ");
                            int wordCount = words.length;
                            
                            for (int k = 0; k < wordCount; k++) {
                                String word = words[k];
                                
                                if (word.toLowerCase().startsWith(prefixString)) {
                                    newValues.addDataSet(h);
                                    break;
                                }
                            }
                        }
                    }
                }
 
                results.values = newValues;
                results.count = newValues.getCount();
            }
 
            return results;
        }
 
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //noinspection unchecked
        	DataSets dataSets = (DataSets) results.values;
        	DataSets oldSets = mData;
        	mData = dataSets;
        	
        	if (oldSets != null && oldSets != dataSets) {
        		oldSets.unregisterContentObserver(mChangeObserver);
        		oldSets.unregisterDataSetObserver(mDataSetObserver);
        	}
        	
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
    
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
        	if (LOG.isDebugEnabled()) 
        		LOG.debug("onContentChanged: selfChange=" + selfChange);
        	
            onContentChanged();
        }
    }

    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
        	if (LOG.isDebugEnabled()) 
        		LOG.debug("notifyDataSetChanged");
        	
            mDataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
        	if (LOG.isDebugEnabled()) 
        		LOG.debug("notifyDataSetInvalidated");
        	
            mDataValid = false;
            notifyDataSetInvalidated();
        }
    }
}
