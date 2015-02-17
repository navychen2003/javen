package org.javenstudio.cocoka.widget;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.javenstudio.cocoka.android.ResourceHelper;

@SuppressWarnings("unused")
public class PopupDialogAdapter extends BaseAdapter {

	public static interface Callback {
    	public void onItemClick(); 
    }
	
	public static interface ViewBinder { 
		public void bindItemView(ListItem item, View view);
	}
	
	/**
     * Specific item in our list.
     */
    public static class ListItem implements Callback {
        private final int mTextResourceId;
        private final int mIconResourceId;
        private final int mBackgroundResourceId;
        private final Callback mCallback;
        private int mType = 0;
        
        public ListItem(int textResourceId) { 
        	this(textResourceId, 0);
        }
        
        public ListItem(int textResourceId, int iconResourceId) { 
        	this(textResourceId, iconResourceId, null); 
        }
        
        public ListItem(int textResourceId, int iconResourceId, Callback action) {
        	this(textResourceId, iconResourceId, 0, action);
        }
        
        public ListItem(int textResourceId, int iconResourceId, int backgroundResourceId) { 
        	this(textResourceId, iconResourceId, backgroundResourceId, null);
        }
        
        public ListItem(int textResourceId, int iconResourceId, int backgroundResourceId, Callback action) {
        	mTextResourceId = textResourceId;
            mIconResourceId = iconResourceId;
            mBackgroundResourceId = backgroundResourceId;
            mCallback = action;
        }
        
        public void setType(int type) { mType = type; }
        public int getType() { return mType; }
        
        public CharSequence getText() { 
        	return mTextResourceId != 0 ? 
        			ResourceHelper.getResourceContext().getString(mTextResourceId) : null;
        }
        
        public Drawable getIcon() { 
        	return mIconResourceId != 0 ? 
        			ResourceHelper.getResourceContext().getDrawable(mIconResourceId) : null;
        }
        
        public Drawable getBackground() { 
        	return mBackgroundResourceId != 0 ? 
        			ResourceHelper.getResourceContext().getDrawable(mBackgroundResourceId) : null;
        }
        
        @Override
        public void onItemClick() { 
        	if (mCallback != null) 
        		mCallback.onItemClick();
        }
        
        protected int getViewResource() { 
        	return 0;
        }
    }
	
	private final Context mContext; 
	private final LayoutInflater mInflater;
    private final ArrayList<ListItem> mItems = new ArrayList<ListItem>();
    private final int mDialogId; 
    private final int mItemLayoutId; 
    private ViewBinder mViewBinder = null;
    
    public PopupDialogAdapter(Context context, int dialogId, int itemLayoutId, ListItem[] items) {
        super();

        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDialogId = dialogId; 
        mItemLayoutId = itemLayoutId; 
        
        for (int i=0; items != null && i < items.length; i++) {
        	ListItem item = items[i]; 
        	if (item == null) continue; 
        	
        	mItems.add(item); 
        }
        
    }

    void addItem(ListItem item) { 
    	if (item != null) 
    		mItems.add(item); 
    }
    
    public void setViewBinder(ViewBinder binder) { 
    	mViewBinder = binder;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItem item = (ListItem) getItem(position);
        if (item == null) 
        	return null; 
        
        int resourceId = item.getViewResource(); 
        if (resourceId == 0) 
        	resourceId = mItemLayoutId;
        
        if (convertView == null) 
            convertView = mInflater.inflate(resourceId, parent, false);
        
        ViewBinder binder = mViewBinder; 
        if (binder != null) { 
        	binder.bindItemView(item, convertView);
        	
        } else if (convertView instanceof TextView) { 
	        TextView textView = (TextView) convertView;
	        textView.setTag(item);
	        textView.setText(item.getText());
	        textView.setCompoundDrawablesWithIntrinsicBounds(item.getIcon(), null, null, null);
        }
        
        return convertView;
    }

    public int getCount() {
        return mItems.size();
    }

    public Object getItem(int position) {
    	if (position >= 0 && position < mItems.size())
    		return mItems.get(position);
    	else 
    		return null; 
    }

    public long getItemId(int position) {
    	return position; 
    }
    
    public Callback getItemCallback(int position) {
    	return (ListItem)getItem(position); 
    }
}
