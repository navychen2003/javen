package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;

import org.javenstudio.cocoka.android.ResourceHelper;

public class PopupMenu extends PopupWindow implements View.OnKeyListener {

	private final FrameLayout mLayout; 
	private final ProgressBar mProgressBar; 
	private final View mView; 
	
	public PopupMenu(Context context, int contentView) { 
		this(context, contentView, 0); 
	}
	
	public PopupMenu(Context context, int contentView, int background) { 
		this(context, contentView, background, 0); 
	}
	
	public PopupMenu(Context context, int contentView, int background, int animation) { 
		this(context, contentView, 
				background != 0 ? ResourceHelper.getResourceContext().getDrawable(background) : null, 
				animation); 
	}
	
	public PopupMenu(Context context, int contentView, Drawable background) { 
		this(context, contentView, background, 0); 
	}
	
	public PopupMenu(Context context, int contentView, Drawable background, int animation) { 
		super(context); 
		
		mLayout = new FrameLayout(context); 
		setContentView(mLayout);
		
		mLayout.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		mLayout.setMinimumWidth(50); 
		mLayout.setMinimumHeight(50); 
		
		mView = ResourceHelper.getResourceContext().inflateView(contentView, mLayout, false); 
		mLayout.addView(mView, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)); 
		
		mProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall); 
		mProgressBar.setPadding(0, 0, 0, 20); 
		mLayout.addView(mProgressBar, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM)); 
		mProgressBar.setVisibility(View.GONE); 
		
		setWidth(ViewGroup.LayoutParams.WRAP_CONTENT); 
		setHeight(ViewGroup.LayoutParams.WRAP_CONTENT); 
		setBackgroundDrawable(background); 
		setFocusable(true); //menu must have focus else cannot dispatch event
		if (animation != 0) 
			setAnimationStyle(animation); 
		
		setOutsideTouchable(true); 
		onCreated(); 
		update(); 
		
		mLayout.setFocusable(true); 
		mLayout.setFocusableInTouchMode(true); 
		mLayout.setOnKeyListener(this); 
	}
	
	protected void onCreated() { 
		// Have the system blur any windows behind this one.
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
        //        WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	}
	
	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) { 
		if (keyCode == KeyEvent.KEYCODE_MENU) { 
			if (onMenuPressed(keyCode, event)) 
				return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK) { 
			if (onBackPressed(keyCode, event)) 
				return true;
		}
		return false;
	}
	
	protected boolean onMenuPressed(int keyCode, KeyEvent event) { 
		return false;
	}
	
	protected boolean onBackPressed(int keyCode, KeyEvent event) { 
		if (isShowing() && event.getAction() == KeyEvent.ACTION_DOWN) { 
			dismiss(); return true; 
		}
		return false;
	}
	
	@Override
	public void setOutsideTouchable(boolean touchable) { 
		super.setOutsideTouchable(touchable); 
		
		View layout = getContentLayout(); 
		if (layout != null) { 
			View.OnClickListener listener = null; 
			if (touchable) { 
				listener = new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							dismiss(); 
						}
					};
			}
			
			layout.setOnClickListener(listener); 
		}
	}
	
	public final ViewGroup getContentLayout() { 
		return mLayout; 
	}
	
	public final View getContentView() { 
		return mView; 
	}
	
	public final View findViewById(int viewId) { 
		return getContentView().findViewById(viewId); 
	}
	
	public final ProgressBar getProgressBar() { 
		return mProgressBar; 
	}
	
	public final void showProgressBar(boolean show) { 
		if (show) 
			mProgressBar.setVisibility(View.VISIBLE); 
		else
			mProgressBar.setVisibility(View.GONE); 
	}
	
	public void showAtLeft(View decorView, View view) { 
		if (decorView != null && view != null) { 
			int[] location = new int[2]; 
			view.getLocationInWindow(location); 
			
			int x = location[0]; 
			int y = location[1] + view.getHeight(); 
			
			showAtLocation(decorView, Gravity.LEFT|Gravity.TOP, x, y);
		}
	}
	
	public void showAtRight(View decorView, View view) { 
		if (decorView != null && view != null) { 
			int[] location = new int[2]; 
			view.getLocationInWindow(location); 
			
			int x = location[0] + view.getWidth() - getWidth(); 
			int y = location[1] + view.getHeight(); 
			
			showAtLocation(decorView, Gravity.LEFT|Gravity.TOP, x, y);
		}
	}
	
	public void showAtCenter(View decorView, View view) { 
		if (view != null) { 
			showAtLocation(view, Gravity.CENTER, 0, 0);
		}
	}
	
	public void showAtMiddle(View decorView, View view) { 
		if (decorView != null && view != null) { 
			int[] location = new int[2]; 
			view.getLocationInWindow(location); 
			
			int x = location[0] + view.getWidth()/2 - getWidth()/2; 
			int y = location[1] + view.getHeight(); 
			
			showAtLocation(decorView, Gravity.LEFT|Gravity.TOP, x, y);
			//showAsDropDown(view);
		}
	}
	
	public void showAtBottom(View decorView) { 
		if (decorView != null) { 
			showAtLocation(decorView, Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM, 0, 0); 
		}
	}
	
}
