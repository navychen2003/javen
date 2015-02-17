package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.javenstudio.cocoka.android.OptionsMenu;
import org.javenstudio.cocoka.android.ResourceHelper;

public class TabOptionsMenu extends PopupWindow implements OptionsMenu {

	private final GridView mBodyView, mTitleView; 
	private final LinearLayout mLayout; 
	private final MenuTitleAdapter mTitleAdapter; 
	private final MenuBodyAdapter[] mBodyAdapters; 
	private int mSelectedTitle = 0; 

	public TabOptionsMenu(Context context, MenuTitleAdapter titleAdapter, 
			MenuBodyAdapter[] bodyAdapters, int aniTabMenu) { 
		this(context, titleAdapter, bodyAdapters, 5, 0x00444444, 0xCC000000, aniTabMenu); 
	}
	
	public TabOptionsMenu(Context context, MenuTitleAdapter titleAdapter, MenuBodyAdapter[] bodyAdapters, 
			int padding, int colorBgTabMenu, int colorBgBody, int aniTabMenu) { 
		super(context);
 
		mTitleAdapter = titleAdapter;
		mBodyAdapters = bodyAdapters; 
		
		mLayout = new LinearLayout(context);
		mLayout.setOrientation(LinearLayout.VERTICAL);
		mLayout.setPadding(padding, padding, padding, padding);
		
		mTitleView = new GridView(context);
		mTitleView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		mTitleView.setNumColumns(titleAdapter.getCount());
		mTitleView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		mTitleView.setVerticalSpacing(1);
		mTitleView.setHorizontalSpacing(1);
		mTitleView.setGravity(Gravity.CENTER);
		//mTitleView.setOnItemClickListener(titleClick);
		mTitleView.setAdapter(titleAdapter);
		mTitleView.setSelector(new ColorDrawable(Color.TRANSPARENT));
		
		mBodyView = new GridView(context);
		mBodyView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		mBodyView.setSelector(new ColorDrawable(Color.TRANSPARENT));
		mBodyView.setBackgroundColor(colorBgBody); 
		mBodyView.setNumColumns(4);
		mBodyView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		mBodyView.setVerticalSpacing(5);
		mBodyView.setHorizontalSpacing(5);
		mBodyView.setPadding(5, 5, 5, 5);
		mBodyView.setGravity(Gravity.CENTER);
		//mBodyView.setOnItemClickListener(bodyClick);
		
		mLayout.addView(mTitleView);
		mLayout.addView(mBodyView);
 
		setContentView(mLayout);
		setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
		setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		setBackgroundDrawable(new ColorDrawable(colorBgTabMenu)); //menu background
		if (aniTabMenu != 0)
			setAnimationStyle(aniTabMenu);
		setFocusable(true); //menu must have focus else cannot dispatch event
		update(); 
		
		final PopupWindow window = this; 
		mLayout.setFocusable(true); 
		mLayout.setFocusableInTouchMode(true); 
		mLayout.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) { 
					if (keyCode == KeyEvent.KEYCODE_MENU) { 
						if (window.isShowing() && event.getAction() == KeyEvent.ACTION_DOWN) { 
							window.dismiss(); return true; 
						}
					}
					return false;
				}
			});
		
		mTitleView.setOnItemClickListener(new AdapterView.OnItemClickListener() { 
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					setTitleSelected(arg2); 
				}
			});
		
		if (bodyAdapters == null || bodyAdapters.length <= 1) 
			mTitleView.setVisibility(View.GONE); 
	}

	public void showMenuAt(View parent) { 
		showAtLocation(parent, Gravity.BOTTOM, 0, 0); 
	}
	
	public void hideMenu() { 
		dismiss(); 
	}
	
	public void setTitleItemClickListener(AdapterView.OnItemClickListener listener) { 
		mTitleView.setOnItemClickListener(listener); 
	}
	
	public void setBodyItemClickListener(AdapterView.OnItemClickListener listener) { 
		mBodyView.setOnItemClickListener(listener); 
	}
	
	public void setTitleVisibility(int visibility) { 
		mTitleView.setVisibility(visibility); 
	}
	
	public synchronized void setTitleSelected(int index) {
		if (index >= 0 && index < mBodyAdapters.length) { 
			mTitleView.setSelection(index);
			mTitleAdapter.setFocus(index);
			mBodyView.setAdapter(mBodyAdapters[index]);
			mSelectedTitle = index; 
		}
	}

	public int getTitleCount() { 
		return mTitleAdapter != null ? mTitleAdapter.getCount() : 0; 
	}
	
	public int getTitleSelected() { 
		return mSelectedTitle; 
	}
	
	public void setBodySelected(int index, int colorSelBody) {
		int count = mBodyView.getChildCount();
		for (int i=0; i < count; i++) {
			if (i != index) 
				((LinearLayout)mBodyView.getChildAt(i)).setBackgroundColor(Color.TRANSPARENT);
		}
		((LinearLayout)mBodyView.getChildAt(index)).setBackgroundColor(colorSelBody);
	}

	public MenuTitleAdapter getMenuTitleAdapter() { 
		return mTitleAdapter; 
	}
	
	static public class MenuBodyAdapter extends BaseAdapter {
		private final Context mContext;
		private final int mFontColor, mFontSize;
		private final String[] mTexts;
		private final int[] mResID;
		private final int mBackgroundColor; 
		private final int mBackgroundSelColor; 
		
		public MenuBodyAdapter(Context context, String[] texts, int[] resID) {
			this(context, texts, resID, 13, 0xFFFFFFFF, 0xFF123456, Color.GRAY); 
		}
		
		/**
		 * Body of menu
		 * @param context context
		 * @param texts text list
		 * @param resID icon resouce list
		 * @param fontSize font size
		 * @param color font color
		 */
		public MenuBodyAdapter(Context context, String[] texts, int[] resID, 
				int fontSize, int fontColor, int bgColor, int bgSelColor) {
			mContext = context;
			mFontColor = fontColor;
			mTexts = texts;
			mFontSize = fontSize;
			mResID = resID;
			mBackgroundColor = bgColor; 
			mBackgroundSelColor = bgSelColor; 
		}
		
		public int getCount() {
			return mTexts != null ? mTexts.length : 0;
		}
		
		public Object getItem(int position) {
			return makeMenyBody(position);
		}
		
		public long getItemId(int position) {
			return position;
		}

		private LinearLayout makeMenyBody(int position) {
			int imgRes = mResID != null && position >= 0 && position < mResID.length ? mResID[position] : 0; 
			String txt = mTexts != null && position >= 0 && position < mTexts.length ? mTexts[position] : null; 
			
			LinearLayout result = new LinearLayout(mContext);
			result.setOrientation(LinearLayout.VERTICAL);
			result.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL);
			result.setLayoutParams(new GridView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)); 
			result.setPadding(5, 5, 5, 5);
 
			if ((txt != null && txt.length() > 0) || imgRes != 0) { 
				result.setBackground(getBackgroundColorDrawable(mContext, mBackgroundColor, mBackgroundSelColor)); 
				
				TextView text = new TextView(mContext);
				text.setText(txt);
				text.setTextSize(mFontSize);
				text.setTextColor(mFontColor);
				text.setGravity(Gravity.CENTER);
				text.setPadding(5, 5, 5, 5);
				
				ImageView img = new ImageView(mContext);
				if (imgRes != 0) 
					img.setBackground(ResourceHelper.getResourceContext().getDrawable(imgRes));
				
				result.addView(img, new LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				result.addView(text, new LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			}
			
			return result;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			return makeMenyBody(position);
		}
	}

	static public class MenuTitleAdapter extends BaseAdapter {
		private final Context mContext;
        private final int mFontColor, mSelFontColor, mUnselBgColor, mSelBgColor;
        private final TextView[] mTitles;
        
        public MenuTitleAdapter(Context context, String[] titles) {
        	this(context, titles, 16, 0xFF222222, Color.WHITE, Color.GRAY, 0xCC000000); 
        }
        
        /**
         * Title of menu
         * @param context context
         * @param titles title tab text
         * @param fontSize font size
         * @param fontcolor font color
         * @param unselcolor unselected color
         * @param selcolor selected color
         */
        public MenuTitleAdapter(Context context, String[] titles, int fontSize,
                				int fontcolor, int selfontcolor, int unselbgcolor, int selbgcolor) {
            mContext = context;
            mFontColor = fontcolor;
            mSelFontColor = selfontcolor; 
            mUnselBgColor = unselbgcolor;
            mSelBgColor = selbgcolor;
            mTitles = new TextView[titles.length]; 
            
            for (int i = 0; i < titles.length; i++) {
            	mTitles[i] = new TextView(mContext);
            	mTitles[i].setText(titles[i]);
            	mTitles[i].setTextSize(fontSize);
            	mTitles[i].setTextColor(fontcolor);
            	mTitles[i].setGravity(Gravity.CENTER);
            	mTitles[i].setPadding(10, 10, 10, 10);
            }
        }
        
        public int getCount() {
            return mTitles != null ? mTitles.length : 0;
        }
        
        public Object getItem(int position) {
            return mTitles[position];
        }
        
        public long getItemId(int position) {
            return mTitles[position].getId();
        }
        
        private void setFocus(int index) {
        	for(int i=0; i < mTitles.length; i++) {
        		if (i != index) {
        			mTitles[i].setBackground(new ColorDrawable(mUnselBgColor)); 
        			mTitles[i].setTextColor(mFontColor); 
        		}
        	}
        	mTitles[index].setBackgroundColor(mSelBgColor); 
        	mTitles[index].setTextColor(mSelFontColor); 
        }
 
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = mTitles[position];
            } else {
                v = convertView;
            }
            return v;
        }
	}

	public static Drawable getBackgroundColorDrawable(Context context, int color, int colorPressed) {
		Highlights highlights = new Highlights(context); 
    	
    	Highlights.DrawableSetting normal = new Highlights.DrawableSetting(); 
    	normal.mColor = color; 
    	//normal.mStrokeColor = Color.WHITE; 
    	//normal.mStrokeWidth = 1; 
    	
    	Highlights.DrawableSetting pressed = new Highlights.DrawableSetting(); 
    	pressed.mColor = colorPressed; 
    	//pressed.mStrokeColor = Color.YELLOW; 
    	//pressed.mStrokeWidth = 2; 
    	
    	highlights.setNormal(normal); 
    	highlights.setPressed(pressed); 
    	
    	return highlights.getDrawable(50, 50); 
	}
	
}
