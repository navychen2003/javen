package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.text.Spannable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.EditText;

public class SpannedEditor extends EditText implements ToolBar.Button {

	private InputButton.OnSoftInputShowListener mOnSoftInputShowListener = null; 
	private SpannedBuilder mBuilder = null; 
	private boolean mEatLongTouchRelease = false; 
	
	public SpannedEditor(Context context) {
		super(context); 
		initBuilder(); 
	}
	
	public SpannedEditor(Context context, AttributeSet attrs) {
		super(context, attrs); 
		initBuilder(); 
	}
	
	public SpannedEditor(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle); 
		initBuilder(); 
	}
	
	@Override 
	public void setBackgroundResource(int resid) {
		ViewHelper.setBackgroundResource(this, resid); 
	}
	
	protected void initBuilder() {
		this.mBuilder = new SpannedBuilder(this); 
	}

	public void setOnSoftInputShowListener(InputButton.OnSoftInputShowListener l) {
		this.mOnSoftInputShowListener = l; 
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event); 
	}
	
	@Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
		return super.onKeyUp(keyCode, event); 
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getAction();
		
		/*
         * Don't handle the release after a long press, because it will
         * move the selection away from whatever the menu action was
         * trying to affect.
         */
        if (action == MotionEvent.ACTION_UP) {
        	if (!mEatLongTouchRelease && mOnSoftInputShowListener != null) {
				if (action == MotionEvent.ACTION_UP && isEnabled() && isFocused() && 
					onCheckIsTextEditor() && getLayout() != null && 
					getText() instanceof Spannable) {
					mOnSoftInputShowListener.onShowBefore(); 
				}
			}
        	mEatLongTouchRelease = false;
        }

		return super.onTouchEvent(event); 
	}
	
	@Override
	public boolean performLongClick() {
		mEatLongTouchRelease = true; 
		return super.performLongClick(); 
	}
	
	public SpannedBuilder getBuilder() {
		return mBuilder; 
	}
	
}
