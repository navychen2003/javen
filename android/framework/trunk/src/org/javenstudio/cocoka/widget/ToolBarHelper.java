package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;

import org.javenstudio.cocoka.android.ResourceHelper;

//@SuppressWarnings({"unused"})
public class ToolBarHelper {

	public static interface ViewAdapter {
		public Context getContext(); 
		public ViewGroup getViewGroup(); 
		public int getChildCount(); 
		public View getChildAt(int index); 
		public void addView(View child); 
	}
	
	private final ViewAdapter mAdapter; 
	
	public ToolBarHelper(ViewAdapter adapter) {
		mAdapter = adapter; 
	}
	
	public void clearChildChecked(ToolBar.OnCheckListener listener) { 
    	for (int i=0; i < mAdapter.getChildCount(); i++) {
    		View child = mAdapter.getChildAt(i); 
    		setChildViewChecked(listener, child, null, false); 
    		
    		//if (child != null && child instanceof ToolBar.CheckedButton) {
    		//	ToolBar.CheckedButton btn = (ToolBar.CheckedButton)child; 
			//	btn.setChecked(false); 
			//	if (listener != null) 
			//		listener.onChecked(child, false); 
    		//}
    	}
	}
	
	protected void setChildViewChecked(ToolBar.OnCheckListener listener, 
			View view, Object ignore, boolean checked) { 
		if (view == null || view == ignore) return; 
		
		if (view instanceof ViewGroup) { 
			ViewGroup group = (ViewGroup)view; 
			for (int i=0; i < group.getChildCount(); i++) { 
				View child = group.getChildAt(i); 
				setChildViewChecked(listener, child, ignore, checked); 
			}
		}
		
		if (view instanceof Checkable) { 
			((Checkable)view).setChecked(checked); 
			if (listener != null) 
				listener.onChecked(view, checked); 
		}
	}
	
    public void setChildChecked(ToolBar.CheckedButton button, 
    		ToolBar.OnCheckListener listener, boolean radio) {
    	if (button == null || !button.isCheckable()) 
    		return; 
    	
    	if (radio) {
	    	for (int i=0; i < mAdapter.getChildCount(); i++) {
	    		View child = mAdapter.getChildAt(i); 
	    		setChildViewChecked(listener, child, button, false); 
	    		
	    		//if (child != null && child instanceof ToolBar.CheckedButton) {
	    		//	ToolBar.CheckedButton btn = (ToolBar.CheckedButton)child; 
	    		//	if (btn != button) { 
	    		//		btn.setChecked(false); 
	    		//		if (listener != null) 
	    		//			listener.onChecked(child, false); 
	    		//	}
	    		//}
	    	}
    	}
    	
    	button.setChecked(true); 
    	if (listener != null && button instanceof View) 
			listener.onChecked((View)button, true); 
    }
	
    public ImageButton addImageButton(int resid, int imageid, int backgroundid) {
    	if (resid == 0) return null; 
    	
    	Drawable image = imageid != 0 ? 
    			ResourceHelper.getResourceContext().getDrawable(imageid) : null; 
    	Drawable background = backgroundid != 0 ? 
    			ResourceHelper.getResourceContext().getDrawable(backgroundid) : null; 
    	
    	return addImageButton(resid, image, background, null); 
    }
    
    public ImageButton addImageButton(int resid, Drawable image, 
    		Drawable background, View.OnClickListener listener) {
    	if (resid == 0) return null; 
    	
    	ImageButton button = (ImageButton)ResourceHelper.getResourceContext().inflateView(
    			resid, mAdapter.getViewGroup(), false);
    	
    	if (image != null) 
    		button.setImageDrawable(image); 
    	
    	if (background != null) 
    		button.setBackground(background); 
    	
    	if (listener != null) 
    		button.setOnClickListener(listener); 
    	
    	mAdapter.addView(button); 
    	
    	return button; 
    }
    
    public CheckButton addCheckButton(int resid, int imageid, 
    		int backgroundid, View.OnClickListener listener) {
    	if (resid == 0) return null; 
    	
    	Drawable image = imageid != 0 ? 
    			ResourceHelper.getResourceContext().getDrawable(imageid) : null; 
    	Drawable background = backgroundid != 0 ? 
    			ResourceHelper.getResourceContext().getDrawable(backgroundid) : null; 
    	
    	return addCheckButton(resid, image, background, listener); 
    }
    
    public CheckButton addCheckButton(int resid, Drawable image, 
    		Drawable background, View.OnClickListener listener) {
    	if (resid == 0) return null; 
    	
    	CheckButton button = (CheckButton)ResourceHelper.getResourceContext().inflateView(
    			resid, mAdapter.getViewGroup(), false);
    	
    	if (image != null) 
    		button.setButtonDrawable(image); 
    	
    	if (background != null) 
    		button.setBackground(background); 
    	
    	if (listener != null) 
    		button.setOnClickListener(listener); 
    	
    	mAdapter.addView(button); 
    	
    	return button; 
    }
    
    public TextButton addTextButton(int resid, int textid, 
    		int backgroundid, View.OnClickListener listener) {
    	if (resid == 0) return null; 
    	
    	String text = textid != 0 ? 
    			ResourceHelper.getResourceContext().getString(textid) : null; 
    	Drawable background = backgroundid != 0 ? 
    			ResourceHelper.getResourceContext().getDrawable(backgroundid) : null; 
    	
    	return addTextButton(resid, text, background, listener); 
    }
    
    public TextButton addTextButton(int resid, CharSequence text, 
    		Drawable background, View.OnClickListener listener) {
    	if (resid == 0) return null; 
    	
    	TextButton button = (TextButton)ResourceHelper.getResourceContext().inflateView(
    			resid, mAdapter.getViewGroup(), false);

    	if (background != null) 
    		button.setBackground(background); 
    	
    	if (listener != null)
    		button.setOnClickListener(listener); 
    	
    	if (text != null)
    		button.setText(text); 
    	
    	//button.setSingleLine(); 
    	
    	mAdapter.addView(button); 
    	
    	return button; 
    }
    
    public InputButton addInputButton(int resid, int textid, 
    		int backgroundid, TextWatcher watcher) {
    	if (resid == 0) return null; 
    	
    	InputButton button = (InputButton)ResourceHelper.getResourceContext().inflateView(
    			resid, mAdapter.getViewGroup(), false);

    	if (backgroundid != 0) 
    		button.setBackground(ResourceHelper.getResourceContext().getDrawable(backgroundid)); 
    	
    	if (textid != 0) 
    		button.setText(ResourceHelper.getResourceContext().getString(textid)); 
    	
    	if (watcher != null)
    		button.addTextChangedListener(watcher);
    	
    	//button.setSingleLine(); 
    	
    	mAdapter.addView(button); 
    	
    	return button; 
    }
    
    public SpannedEditor addSpannedEditor(int resid, int textid, 
    		int backgroundid, TextWatcher watcher) {
    	if (resid == 0) return null; 
    	
    	SpannedEditor button = (SpannedEditor)ResourceHelper.getResourceContext().inflateView(
    			resid, mAdapter.getViewGroup(), false);

    	if (backgroundid != 0) 
    		button.setBackground(ResourceHelper.getResourceContext().getDrawable(backgroundid)); 
    	
    	if (textid != 0) 
    		button.setText(ResourceHelper.getResourceContext().getString(textid)); 
    	
    	if (watcher != null)
    		button.addTextChangedListener(watcher);
    	
    	//button.setSingleLine(); 
    	
    	mAdapter.addView(button); 
    	
    	return button; 
    }
    
    public TextButton addLeftImageTextButton(int resid, int imageid, int textid, int backgroundid) {
    	return addImageTextButton(resid, imageid, textid, backgroundid, 0); 
    }
    
    public TextButton addTopImageTextButton(int resid, int imageid, int textid, int backgroundid) {
    	return addImageTextButton(resid, imageid, textid, backgroundid, 1); 
    }
    
    public TextButton addRightImageTextButton(int resid, int imageid, int textid, int backgroundid) {
    	return addImageTextButton(resid, imageid, textid, backgroundid, 2); 
    }
    
    public TextButton addBottomImageTextButton(int resid, int imageid, int textid, int backgroundid) {
    	return addImageTextButton(resid, imageid, textid, backgroundid, 3); 
    }
    
    private TextButton addImageTextButton(int resid, int imageid, int textid, int backgroundid, int position) {
    	if (resid == 0) return null; 
    	
    	Drawable image = imageid != 0 ? ResourceHelper.getResourceContext().getDrawable(imageid) : null; 
    	String text = textid != 0 ? ResourceHelper.getResourceContext().getString(textid) : null; 
    	
    	return addImageTextButton(resid, image, text, backgroundid, position); 
    }
    
    public TextButton addLeftImageTextButton(int resid, Drawable image, CharSequence text, int backgroundid) { 
    	return addImageTextButton(resid, image, text, backgroundid, 0); 
    }
    
    public TextButton addTopImageTextButton(int resid, Drawable image, CharSequence text, int backgroundid) { 
    	return addImageTextButton(resid, image, text, backgroundid, 1); 
    }
    
    public TextButton addRightImageTextButton(int resid, Drawable image, CharSequence text, int backgroundid) { 
    	return addImageTextButton(resid, image, text, backgroundid, 2); 
    }
    
    public TextButton addBottomImageTextButton(int resid, Drawable image, CharSequence text, int backgroundid) { 
    	return addImageTextButton(resid, image, text, backgroundid, 3); 
    }
    
    private TextButton addImageTextButton(int resid, Drawable image, 
    		CharSequence text, int backgroundid, int position) {
    	if (resid == 0) return null; 
    	
    	TextButton button = (TextButton)ResourceHelper.getResourceContext().inflateView(
    			resid, mAdapter.getViewGroup(), false);
    	
    	if (image != null) { 
    		switch (position) { 
    		case 1: 
    			button.setCompoundDrawablesWithIntrinsicBounds(null, image, null, null); 
    			break; 
    		case 2: 
    			button.setCompoundDrawablesWithIntrinsicBounds(null, null, image, null); 
    			break; 
    		case 3: 
    			button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, image); 
    			break; 
    		default: 
    			button.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null); 
    			break; 
    		}
    	}
    	
    	if (text != null) 
    		button.setText(text); 
    	
    	button.setSingleLine(); 
    	
    	if (backgroundid != 0) 
    		button.setBackground(ResourceHelper.getResourceContext().getDrawable(backgroundid)); 
    	
    	mAdapter.addView(button); 
    	
    	return button; 
    }
    
    public View addViewButton(int resid, int backgroundid) {
    	if (resid == 0) return null; 
    	
    	View button = ResourceHelper.getResourceContext().inflateView(
    			resid, mAdapter.getViewGroup(), false);
    	
    	if (backgroundid != 0) 
    		button.setBackground(ResourceHelper.getResourceContext().getDrawable(backgroundid)); 
    	
    	mAdapter.addView(button); 
    	
    	return button; 
    }
    
}
