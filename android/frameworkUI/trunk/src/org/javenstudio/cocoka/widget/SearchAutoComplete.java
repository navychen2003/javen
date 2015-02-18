package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;

/**
 * Local subclass for AutoCompleteTextView.
 * @hide
 */
public class SearchAutoComplete extends AutoCompleteTextView {

    protected int mThreshold;
    protected SearchView mSearchView;

    public SearchAutoComplete(Context context) {
        super(context);
        mThreshold = getThreshold();
    }

    public SearchAutoComplete(Context context, AttributeSet attrs) {
        super(context, attrs);
        mThreshold = getThreshold();
    }

    public SearchAutoComplete(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mThreshold = getThreshold();
    }

    void setSearchView(SearchView searchView) {
        mSearchView = searchView;
    }

    @Override
    public void setThreshold(int threshold) {
        super.setThreshold(threshold);
        mThreshold = threshold;
    }

    /**
     * Returns true if the text field is empty, or contains only whitespace.
     */
    protected boolean isEmpty() {
        return TextUtils.getTrimmedLength(getText()) == 0;
    }

    /**
     * We override this method to avoid replacing the query box text when a
     * suggestion is clicked.
     */
    @Override
    protected void replaceText(CharSequence text) {
    }

    /**
     * We override this method to avoid an extra onItemClick being called on
     * the drop-down's OnItemClickListener by
     * {@link AutoCompleteTextView#onKeyUp(int, KeyEvent)} when an item is
     * clicked with the trackball.
     */
    @Override
    public void performCompletion() {
    }

    /**
     * We override this method to be sure and show the soft keyboard if
     * appropriate when the TextView has focus.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (hasWindowFocus && mSearchView.hasFocus() && getVisibility() == VISIBLE) {
            InputMethodManager inputManager = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(this, 0);
            // If in landscape mode, then make sure that
            // the ime is in front of the dropdown.
            if (SearchView.isLandscapeMode(getContext())) {
            	SearchView.ensureImeVisible(this, true);
            }
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        mSearchView.onTextFocusChanged();
    }

    /**
     * We override this method so that we can allow a threshold of zero,
     * which ACTV does not.
     */
    @Override
    public boolean enoughToFilter() {
        return mThreshold <= 0 || super.enoughToFilter();
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // special case for the back key, we do not even try to send it
            // to the drop down list but instead, consume it immediately
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                KeyEvent.DispatcherState state = getKeyDispatcherState();
                if (state != null) {
                    state.startTracking(event, this);
                }
                return true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                KeyEvent.DispatcherState state = getKeyDispatcherState();
                if (state != null) {
                    state.handleUpEvent(event);
                }
                if (event.isTracking() && !event.isCanceled()) {
                    mSearchView.clearFocus();
                    mSearchView.setImeVisibility(false);
                    return true;
                }
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

}
