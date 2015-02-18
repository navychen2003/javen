package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.EditText;

import org.javenstudio.common.util.InputDecoder;
import org.javenstudio.common.util.InputEncoder;

@SuppressWarnings({"unused"})
public class InputButton extends EditText implements ToolBar.Button {
	
	public static interface OnSoftInputShowListener {
		public void onShowBefore(); 
	}
	
	private OnSoftInputShowListener mOnSoftInputShowListener = null; 
	private boolean mEatLongTouchRelease = false; 
	private InputEncoder mEncoder = null; 
	private InputDecoder mDecoder = null; 
	private TextWatcher mWatcher = null; 
	private Html.ImageGetter mImageGetter = null; 
	private Html.TagHandler mTagHandler = null; 
	
	public InputButton(Context context) {
		super(context); 
	}
	
	public InputButton(Context context, AttributeSet attrs) {
		super(context, attrs); 
	}
	
	public InputButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle); 
		
	}
	
	@Override 
	public void setBackgroundResource(int resid) {
		ViewHelper.setBackgroundResource(this, resid); 
	}
	
	public void setOnSoftInputShowListener(OnSoftInputShowListener l) {
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
	
	public void setEncoder(InputEncoder encoder) {
		this.mEncoder = encoder; 
	}
	
	public void setDecoder(InputDecoder decoder) {
		this.mDecoder = decoder; 
	}
	
	public void setImageGetter(Html.ImageGetter getter) {
		this.mImageGetter = getter; 
	}
	
	public void setTagHandler(Html.TagHandler handler) {
		this.mTagHandler = handler; 
	}
	
	public String getInputText() {
		String text = super.getText().toString(); 
		
		if (mDecoder != null) 
			text = mDecoder.decode(text); 
		
		return text; 
	}
	
	public void setInputText(String text) {
		if (mEncoder != null) 
			text = mEncoder.encode(text); 
		
		if (mImageGetter != null || mTagHandler != null) {
			Spanned span = Html.fromHtml(text, mImageGetter, mTagHandler); 
			setText(span); 
		} else
			setText(text); 
	}
	
}
