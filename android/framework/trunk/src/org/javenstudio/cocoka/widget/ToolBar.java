package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View; 
import android.view.ViewGroup; 
import android.graphics.drawable.Drawable; 
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.text.TextWatcher;

public class ToolBar extends LinearLayout implements ToolBarHelper.ViewAdapter {

	public static interface Button {
	}
	
	public static interface CheckedButton extends Button, Checkable {
		public boolean isChecked(); 
		public void setChecked(boolean checked); 
		public boolean isCheckable(); 
		public void setCheckable(boolean enable); 
		public void toggle();
	}
	
	public static interface OnCheckListener { 
		public void onChecked(View button, boolean checked); 
	}
	
	private ToolBarHelper mHelper; 
	private OnCheckListener mCheckListener; 
	private boolean mRadioCheckable = true; 
	
	public ToolBar(Context context) {
		super(context);
		initToolBar(); 
	}
 
    public ToolBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initToolBar(); 
    }
    
    protected void initToolBar() {
    	mHelper = new ToolBarHelper(this); 
    	mCheckListener = null; 
    }
    
    public ViewGroup getViewGroup() {
    	return this; 
    }
    
    public void setOnCheckListener(OnCheckListener listener) { 
    	mCheckListener = listener; 
    }
    
    public void setRadioCheckable(boolean enable) {
    	mRadioCheckable = enable; 
    }
    
    public void setChildChecked(CheckedButton button) {
    	mHelper.setChildChecked(button, mCheckListener, mRadioCheckable); 
    }
    
    public void clearChildChecked() { 
    	mHelper.clearChildChecked(mCheckListener); 
    }
    
    public ImageButton addImageButton(int resid) {
    	return addImageButton(resid, 0, 0); 
    }
    
    public ImageButton addImageButton(int resid, int imageid) {
    	return addImageButton(resid, imageid, 0); 
    }
    
    public ImageButton addImageButton(int resid, int imageid, int backgroundid) {
    	return mHelper.addImageButton(resid, imageid, backgroundid); 
    }
    
    public ImageButton addImageButton(int resid, View.OnClickListener listener) {
    	return addImageButton(resid, null, null, listener); 
    }
    
    public ImageButton addImageButton(int resid, Drawable image, View.OnClickListener listener) {
    	return addImageButton(resid, image, null, listener); 
    }
    
    public ImageButton addImageButton(int resid, Drawable image, Drawable background, View.OnClickListener listener) {
    	return mHelper.addImageButton(resid, image, background, listener); 
    }
    
    public CheckButton addCheckButton(int resid) {
    	return addCheckButton(resid, 0, 0); 
    }
    
    public CheckButton addCheckButton(int resid, int imageid) {
    	return addCheckButton(resid, imageid, 0); 
    }
    
    public CheckButton addCheckButton(int resid, int imageid, int backgroundid) {
    	return addCheckButton(resid, imageid, backgroundid, null); 
    }
    
    public CheckButton addCheckButton(int resid, int imageid, View.OnClickListener listener) {
    	return addCheckButton(resid, imageid, 0, listener); 
    }
    
    public CheckButton addCheckButton(int resid, int imageid, int backgroundid, View.OnClickListener listener) {
    	return mHelper.addCheckButton(resid, imageid, backgroundid, listener); 
    }

    public CheckButton addCheckButton(int resid, View.OnClickListener listener) {
    	return addCheckButton(resid, null, null, listener); 
    }
    
    public CheckButton addCheckButton(int resid, Drawable image, View.OnClickListener listener) {
    	return addCheckButton(resid, image, null, listener); 
    }
    
    public CheckButton addCheckButton(int resid, Drawable image, Drawable background, View.OnClickListener listener) {
    	return mHelper.addCheckButton(resid, image, background, listener); 
    }
    
    public TextButton addTextButton(int resid) {
    	return addTextButton(resid, 0, 0, null);
    }
    
    public TextButton addTextButton(int resid, int textid) {
    	return addTextButton(resid, textid, 0, null);
    }
    
    public TextButton addTextButton(int resid, View.OnClickListener listener) {
    	return addTextButton(resid, 0, 0, listener);
    }
    
    public TextButton addTextButton(int resid, int textid, View.OnClickListener listener) {
    	return addTextButton(resid, textid, 0, listener);
    }
    
    public TextButton addTextButton(int resid, int textid, int backgroundid, View.OnClickListener listener) {
    	return mHelper.addTextButton(resid, textid, backgroundid, listener); 
    }
    
    public TextButton addTextButton(int resid, CharSequence text) {
    	return addTextButton(resid, text, null, null); 
    }
    
    public TextButton addTextButton(int resid, CharSequence text, View.OnClickListener listener) {
    	return addTextButton(resid, text, null, listener); 
    }
    
    public TextButton addTextButton(int resid, CharSequence text, Drawable background, View.OnClickListener listener) {
    	return mHelper.addTextButton(resid, text, background, listener); 
    }
    
    public InputButton addInputButton(int resid) {
    	return addInputButton(resid, 0, 0, null);
    }
    
    public InputButton addInputButton(int resid, TextWatcher watcher) {
    	return addInputButton(resid, 0, 0, watcher);
    }
    
    public InputButton addInputButton(int resid, int textid, int backgroundid, TextWatcher watcher) {
    	return mHelper.addInputButton(resid, textid, backgroundid, watcher); 
    }
    
    public SpannedEditor addSpannedEditor(int resid) {
    	return addSpannedEditor(resid, 0, 0, null);
    }
    
    public SpannedEditor addSpannedEditor(int resid, TextWatcher watcher) {
    	return addSpannedEditor(resid, 0, 0, watcher);
    }
    
    public SpannedEditor addSpannedEditor(int resid, int textid, int backgroundid, TextWatcher watcher) {
    	return mHelper.addSpannedEditor(resid, textid, backgroundid, watcher); 
    }
    
    public TextButton addLeftImageTextButton(int resid) {
    	return addLeftImageTextButton(resid, 0, 0, 0); 
    }
    
    public TextButton addLeftImageTextButton(int resid, int imageid, int textid) {
    	return addLeftImageTextButton(resid, imageid, textid, 0); 
    }
    
    public TextButton addLeftImageTextButton(int resid, int imageid, int textid, int backgroundid) {
    	return mHelper.addLeftImageTextButton(resid, imageid, textid, backgroundid); 
    }
    
    public TextButton addLeftImageTextButton(int resid, Drawable image, CharSequence text) {
    	return addLeftImageTextButton(resid, image, text, 0); 
    }
    
    public TextButton addLeftImageTextButton(int resid, Drawable image, CharSequence text, int backgroundid) {
    	return mHelper.addLeftImageTextButton(resid, image, text, backgroundid); 
    }
    
    public TextButton addTopImageTextButton(int resid) {
    	return addTopImageTextButton(resid, 0, 0, 0); 
    }
    
    public TextButton addTopImageTextButton(int resid, int imageid, int textid) {
    	return addTopImageTextButton(resid, imageid, textid, 0); 
    }
    
    public TextButton addTopImageTextButton(int resid, int imageid, int textid, int backgroundid) {
    	return mHelper.addTopImageTextButton(resid, imageid, textid, backgroundid); 
    }
    
    public TextButton addTopImageTextButton(int resid, Drawable image, CharSequence text) {
    	return addTopImageTextButton(resid, image, text, 0); 
    }
    
    public TextButton addTopImageTextButton(int resid, Drawable image, CharSequence text, int backgroundid) {
    	return mHelper.addTopImageTextButton(resid, image, text, backgroundid); 
    }
    
    public TextButton addRightImageTextButton(int resid) {
    	return addRightImageTextButton(resid, 0, 0, 0); 
    }
    
    public TextButton addRightImageTextButton(int resid, int imageid, int textid) {
    	return addRightImageTextButton(resid, imageid, textid, 0); 
    }
    
    public TextButton addRightImageTextButton(int resid, int imageid, int textid, int backgroundid) {
    	return mHelper.addRightImageTextButton(resid, imageid, textid, backgroundid); 
    }
    
    public TextButton addRightImageTextButton(int resid, Drawable image, CharSequence text) {
    	return addRightImageTextButton(resid, image, text, 0); 
    }
    
    public TextButton addRightImageTextButton(int resid, Drawable image, CharSequence text, int backgroundid) {
    	return mHelper.addRightImageTextButton(resid, image, text, backgroundid); 
    }
    
    public TextButton addBottomImageTextButton(int resid) {
    	return addBottomImageTextButton(resid, 0, 0, 0); 
    }
    
    public TextButton addBottomImageTextButton(int resid, int imageid, int textid) {
    	return addBottomImageTextButton(resid, imageid, textid, 0); 
    }
    
    public TextButton addBottomImageTextButton(int resid, int imageid, int textid, int backgroundid) {
    	return mHelper.addBottomImageTextButton(resid, imageid, textid, backgroundid); 
    }
    
    public TextButton addBottomImageTextButton(int resid, Drawable image, CharSequence text) {
    	return addBottomImageTextButton(resid, image, text, 0); 
    }
    
    public TextButton addBottomImageTextButton(int resid, Drawable image, CharSequence text, int backgroundid) {
    	return mHelper.addBottomImageTextButton(resid, image, text, backgroundid); 
    }
    
    public View addViewButton(int resid) { 
    	return addViewButton(resid, 0); 
    }
    
    public View addViewButton(int resid, int backgroundid) { 
    	return mHelper.addViewButton(resid, backgroundid); 
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
