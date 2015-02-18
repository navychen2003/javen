package org.javenstudio.cocoka.widget;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.speech.RecognizerIntent;
import android.support.v4.view.KeyEventCompat;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView.OnEditorActionListener;

import org.javenstudio.cocoka.app.R;

/**
 * A widget that provides a user interface for the user to enter a search query and submit a request
 * to a search provider. Shows a list of query suggestions or results, if available, and allows the
 * user to pick a suggestion or result to launch into.
 *
 * <p>
 * When the SearchView is used in an ActionBar as an action view for a collapsible menu item, it
 * needs to be set to iconified by default using {@link #setIconifiedByDefault(boolean)
 * setIconifiedByDefault(true)}. This is the default, so nothing needs to be done.
 * </p>
 * <p>
 * If you want the search field to always be visible, then call setIconifiedByDefault(false).
 * </p>
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about using {@code SearchView}, read the
 * <a href="{@docRoot}guide/topics/search/index.html">Search</a> developer guide.</p>
 * </div>
 *
 * @see android.view.MenuItem#SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
 * @attr ref android.R.styleable#SearchView_iconifiedByDefault
 * @attr ref android.R.styleable#SearchView_imeOptions
 * @attr ref android.R.styleable#SearchView_inputType
 * @attr ref android.R.styleable#SearchView_maxWidth
 * @attr ref android.R.styleable#SearchView_queryHint
 */
public abstract class SearchView extends LinearLayout {

    protected static final boolean DBG = false;
    protected static final String LOG_TAG = "MySearchView";

    public static final int REQUESTCODE_SEARCH = 50001;
    public static final int REQUESTCODE_SUGGESTION = 50002;
    public static final int REQUESTCODE_VOICWEBSEARCH = 50003;
    public static final int REQUESTCODE_VOICAPPSEARCH = 50004;
    
    /**
     * Private constant for removing the microphone in the keyboard.
     */
    protected static final String IME_OPTION_NO_MICROPHONE = "nm";

    protected OnQueryTextListener mOnQueryChangeListener;
    protected OnCloseListener mOnCloseListener;
    protected OnFocusChangeListener mOnQueryTextFocusChangeListener;
    protected OnSuggestionListener mOnSuggestionListener;
    protected OnClickListener mOnSearchClickListener;
    protected OnStartActivityListener mOnStartActivityListener;

    protected boolean mIconifiedByDefault;
    protected boolean mIconified;
    protected CursorAdapter mSuggestionsAdapter;
    protected View mSearchButton;
    protected View mSubmitButton;
    protected View mSearchPlate;
    protected View mSubmitArea;
    protected ImageView mCloseButton;
    protected View mSearchEditFrame;
    protected View mVoiceButton;
    protected SearchAutoComplete mQueryTextView;
    protected View mDropDownAnchor;
    protected ImageView mSearchHintIcon;
    protected boolean mSubmitButtonEnabled;
    protected CharSequence mQueryHint;
    protected boolean mQueryRefinement;
    protected boolean mClearingFocus;
    protected int mMaxWidth;
    protected boolean mVoiceButtonEnabled;
    protected CharSequence mOldQueryText;
    protected CharSequence mUserQuery;
    protected boolean mExpandedInActionView;
    protected int mCollapsedImeOptions;

    protected SearchableInfo mSearchable;
    protected Bundle mAppSearchData;

    /*
    * SearchView can be set expanded before the IME is ready to be shown during
    * initial UI setup. The show operation is asynchronous to account for this.
    */
    protected Runnable mShowImeRunnable = new Runnable() {
        public void run() {
            InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                showSoftInputUnchecked(SearchView.this, imm, 0);
            }
        }
    };

    protected Runnable mUpdateDrawableStateRunnable = new Runnable() {
        public void run() {
            updateFocusedState();
        }
    };

    protected Runnable mReleaseCursorRunnable = new Runnable() {
        public void run() {
            if (mSuggestionsAdapter != null && mSuggestionsAdapter instanceof SuggestionsAdapter) {
                mSuggestionsAdapter.changeCursor(null);
            }
        }
    };

    // For voice searching
    protected final Intent mVoiceWebSearchIntent;
    protected final Intent mVoiceAppSearchIntent;

    // A weak map of drawables we've gotten from other packages, so we don't load them
    // more than once.
    protected final WeakHashMap<String, Drawable.ConstantState> mOutsideDrawablesCache =
            new WeakHashMap<String, Drawable.ConstantState>();

    /**
     * Callbacks for changes to the query text.
     */
    public interface OnQueryTextListener {

        /**
         * Called when the user submits the query. This could be due to a key press on the
         * keyboard or due to pressing a submit button.
         * The listener can override the standard behavior by returning true
         * to indicate that it has handled the submit request. Otherwise return false to
         * let the SearchView handle the submission by launching any associated intent.
         *
         * @param query the query text that is to be submitted
         *
         * @return true if the query has been handled by the listener, false to let the
         * SearchView perform the default action.
         */
        boolean onQueryTextSubmit(String query);

        /**
         * Called when the query text is changed by the user.
         *
         * @param newText the new content of the query text field.
         *
         * @return false if the SearchView should perform the default action of showing any
         * suggestions if available, true if the action was handled by the listener.
         */
        boolean onQueryTextChange(String newText);
    }

    public interface OnCloseListener {

        /**
         * The user is attempting to close the SearchView.
         *
         * @return true if the listener wants to override the default behavior of clearing the
         * text field and dismissing it, false otherwise.
         */
        boolean onClose();
    }

    /**
     * Callback interface for selection events on suggestions. These callbacks
     * are only relevant when a SearchableInfo has been specified by {@link #setSearchableInfo}.
     */
    public interface OnSuggestionListener {

        /**
         * Called when a suggestion was selected by navigating to it.
         * @param position the absolute position in the list of suggestions.
         *
         * @return true if the listener handles the event and wants to override the default
         * behavior of possibly rewriting the query based on the selected item, false otherwise.
         */
        boolean onSuggestionSelect(int position);

        /**
         * Called when a suggestion was clicked.
         * @param position the absolute position of the clicked item in the list of suggestions.
         *
         * @return true if the listener handles the event and wants to override the default
         * behavior of launching any intent or submitting a search query specified on that item.
         * Return false otherwise.
         */
        boolean onSuggestionClick(int position);
    }

    public interface OnStartActivityListener { 
    	boolean onStartActivity(Intent intent, int requestCode);
    }
    
    public SearchView(Context context) {
        this(context, null);
    }

    public SearchView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) 
            throw new IllegalStateException("SearchView is API 8+ only.");

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.abs__search_view, this, true);

        mSearchButton = findViewById(R.id.abs__search_button);
        mQueryTextView = (SearchAutoComplete) findViewById(R.id.abs__search_src_text);
        mQueryTextView.setSearchView(this);

        mSearchEditFrame = findViewById(R.id.abs__search_edit_frame);
        mSearchPlate = findViewById(R.id.abs__search_plate);
        mSubmitArea = findViewById(R.id.abs__submit_area);
        mSubmitButton = findViewById(R.id.abs__search_go_btn);
        mCloseButton = (ImageView) findViewById(R.id.abs__search_close_btn);
        mVoiceButton = findViewById(R.id.abs__search_voice_btn);
        mSearchHintIcon = (ImageView) findViewById(R.id.abs__search_mag_icon);

        mSearchButton.setOnClickListener(mOnClickListener);
        mCloseButton.setOnClickListener(mOnClickListener);
        mSubmitButton.setOnClickListener(mOnClickListener);
        mVoiceButton.setOnClickListener(mOnClickListener);
        mQueryTextView.setOnClickListener(mOnClickListener);

        mQueryTextView.addTextChangedListener(mTextWatcher);
        mQueryTextView.setOnEditorActionListener(mOnEditorActionListener);
        mQueryTextView.setOnItemClickListener(mOnItemClickListener);
        mQueryTextView.setOnItemSelectedListener(mOnItemSelectedListener);
        mQueryTextView.setOnKeyListener(mTextKeyListener);
        // Inform any listener of focus changes
        mQueryTextView.setOnFocusChangeListener(new OnFocusChangeListener() {
        	@Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mOnQueryTextFocusChangeListener != null) {
                    mOnQueryTextFocusChangeListener.onFocusChange(SearchView.this, hasFocus);
                }
            }
        });

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SherlockSearchView, 0, 0);
        setIconifiedByDefault(a.getBoolean(R.styleable.SherlockSearchView_iconifiedByDefault, true));
        int maxWidth = a.getDimensionPixelSize(R.styleable.SherlockSearchView_android_maxWidth, -1);
        if (maxWidth != -1) {
            setMaxWidth(maxWidth);
        }
        CharSequence queryHint = a.getText(R.styleable.SherlockSearchView_queryHint);
        if (!TextUtils.isEmpty(queryHint)) {
            setQueryHint(queryHint);
        }
        int imeOptions = a.getInt(R.styleable.SherlockSearchView_android_imeOptions, -1);
        if (imeOptions != -1) {
            setImeOptions(imeOptions);
        }
        int inputType = a.getInt(R.styleable.SherlockSearchView_android_inputType, -1);
        if (inputType != -1) {
            setInputType(inputType);
        }

        a.recycle();

        boolean focusable = true;

        a = context.obtainStyledAttributes(attrs, R.styleable.SherlockView, 0, 0);
        focusable = a.getBoolean(R.styleable.SherlockView_android_focusable, focusable);
        a.recycle();
        setFocusable(focusable);

        // Save voice intent for later queries/launching
        mVoiceWebSearchIntent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        mVoiceWebSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mVoiceWebSearchIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

        mVoiceAppSearchIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mVoiceAppSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        mDropDownAnchor = findViewById(mQueryTextView.getDropDownAnchor());
        if (mDropDownAnchor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mDropDownAnchor.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    		int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        adjustDropDownSizeAndPosition();
                    }
                });
            } else {
                mDropDownAnchor.getViewTreeObserver().addOnGlobalLayoutListener(
                		new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        adjustDropDownSizeAndPosition();
                    }
                });
            }
        }

        updateViewsVisibility(mIconifiedByDefault);
        updateQueryHint();
    }

    public void setSearchIcon(int iconRes) { 
        ImageView searchView = (ImageView)mSearchButton;
        searchView.setImageResource(iconRes);
    }
    
    /**
     * Sets the SearchableInfo for this SearchView. Properties in the SearchableInfo are used
     * to display labels, hints, suggestions, create intents for launching search results screens
     * and controlling other affordances such as a voice button.
     *
     * @param searchable a SearchableInfo can be retrieved from the SearchManager, for a specific
     * activity or a global search provider.
     */
    public void setSearchableInfo(SearchableInfo searchable) {
        mSearchable = searchable;
        if (mSearchable != null) {
            updateSearchAutoComplete();
            updateQueryHint();
        }
        // Cache the voice search capability
        mVoiceButtonEnabled = hasVoiceSearch();

        if (mVoiceButtonEnabled) {
            // Disable the microphone on the keyboard, as a mic is displayed near the text box
            // TODO: use imeOptions to disable voice input when the new API will be available
            mQueryTextView.setPrivateImeOptions(IME_OPTION_NO_MICROPHONE);
        }
        updateViewsVisibility(isIconified());
    }

    /**
     * Sets the APP_DATA for legacy SearchDialog use.
     * @param appSearchData bundle provided by the app when launching the search dialog
     * @hide
     */
    public void setAppSearchData(Bundle appSearchData) {
        mAppSearchData = appSearchData;
    }

    /**
     * Sets the IME options on the query text field.
     *
     * @see TextView#setImeOptions(int)
     * @param imeOptions the options to set on the query text field
     *
     * @attr ref android.R.styleable#SearchView_imeOptions
     */
    public void setImeOptions(int imeOptions) {
        mQueryTextView.setImeOptions(imeOptions);
    }

    /**
     * Returns the IME options set on the query text field.
     * @return the ime options
     * @see TextView#setImeOptions(int)
     *
     * @attr ref android.R.styleable#SearchView_imeOptions
     */
    public int getImeOptions() {
        return mQueryTextView.getImeOptions();
    }

    /**
     * Sets the input type on the query text field.
     *
     * @see TextView#setInputType(int)
     * @param inputType the input type to set on the query text field
     *
     * @attr ref android.R.styleable#SearchView_inputType
     */
    public void setInputType(int inputType) {
        mQueryTextView.setInputType(inputType);
    }

    /**
     * Returns the input type set on the query text field.
     * @return the input type
     *
     * @attr ref android.R.styleable#SearchView_inputType
     */
    public int getInputType() {
        return mQueryTextView.getInputType();
    }

    /** @hide */
    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        // Don't accept focus if in the middle of clearing focus
        if (mClearingFocus) return false;
        // Check if SearchView is focusable.
        if (!isFocusable()) return false;
        // If it is not iconified, then give the focus to the text field
        if (!isIconified()) {
            boolean result = mQueryTextView.requestFocus(direction, previouslyFocusedRect);
            if (result) {
                updateViewsVisibility(false);
            }
            return result;
        } else {
            return super.requestFocus(direction, previouslyFocusedRect);
        }
    }

    /** @hide */
    @Override
    public void clearFocus() {
        mClearingFocus = true;
        setImeVisibility(false);
        super.clearFocus();
        mQueryTextView.clearFocus();
        mClearingFocus = false;
    }

    /**
     * Sets a listener for user actions within the SearchView.
     *
     * @param listener the listener object that receives callbacks when the user performs
     * actions in the SearchView such as clicking on buttons or typing a query.
     */
    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }

    /**
     * Sets a listener to inform when the user closes the SearchView.
     *
     * @param listener the listener to call when the user closes the SearchView.
     */
    public void setOnCloseListener(OnCloseListener listener) {
        mOnCloseListener = listener;
    }

    /**
     * Sets a listener to inform when the focus of the query text field changes.
     *
     * @param listener the listener to inform of focus changes.
     */
    public void setOnQueryTextFocusChangeListener(OnFocusChangeListener listener) {
        mOnQueryTextFocusChangeListener = listener;
    }

    /**
     * Sets a listener to inform when a suggestion is focused or clicked.
     *
     * @param listener the listener to inform of suggestion selection events.
     */
    public void setOnSuggestionListener(OnSuggestionListener listener) {
        mOnSuggestionListener = listener;
    }

    /**
     * Sets a listener to inform when the search button is pressed. This is only
     * relevant when the text field is not visible by default. Calling {@link #setIconified
     * setIconified(false)} can also cause this listener to be informed.
     *
     * @param listener the listener to inform when the search button is clicked or
     * the text field is programmatically de-iconified.
     */
    public void setOnSearchClickListener(OnClickListener listener) {
        mOnSearchClickListener = listener;
    }

    /**
     * Returns the query string currently in the text field.
     *
     * @return the query string
     */
    public CharSequence getQuery() {
        return mQueryTextView.getText();
    }

    /**
     * Sets a query string in the text field and optionally submits the query as well.
     *
     * @param query the query string. This replaces any query text already present in the
     * text field.
     * @param submit whether to submit the query right now or only update the contents of
     * text field.
     */
    public void setQuery(CharSequence query, boolean submit) {
        mQueryTextView.setText(query);
        if (query != null) {
            mQueryTextView.setSelection(mQueryTextView.length());
            mUserQuery = query;
        }

        // If the query is not empty and submit is requested, submit the query
        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery();
        }
    }

    /**
     * Sets the hint text to display in the query text field. This overrides any hint specified
     * in the SearchableInfo.
     *
     * @param hint the hint text to display
     *
     * @attr ref android.R.styleable#SearchView_queryHint
     */
    public void setQueryHint(CharSequence hint) {
        mQueryHint = hint;
        updateQueryHint();
    }

    /**
     * Gets the hint text to display in the query text field.
     * @return the query hint text, if specified, null otherwise.
     *
     * @attr ref android.R.styleable#SearchView_queryHint
     */
    public CharSequence getQueryHint() {
        if (mQueryHint != null) {
            return mQueryHint;
        } else if (mSearchable != null) {
            CharSequence hint = null;
            int hintId = mSearchable.getHintId();
            if (hintId != 0) {
                hint = getContext().getString(hintId);
            }
            return hint;
        }
        return null;
    }

    /**
     * Sets the default or resting state of the search field. If true, a single search icon is
     * shown by default and expands to show the text field and other buttons when pressed. Also,
     * if the default state is iconified, then it collapses to that state when the close button
     * is pressed. Changes to this property will take effect immediately.
     *
     * <p>The default value is true.</p>
     *
     * @param iconified whether the search field should be iconified by default
     *
     * @attr ref android.R.styleable#SearchView_iconifiedByDefault
     */
    public void setIconifiedByDefault(boolean iconified) {
        if (mIconifiedByDefault == iconified) return;
        mIconifiedByDefault = iconified;
        updateViewsVisibility(iconified);
        updateQueryHint();
    }

    /**
     * Returns the default iconified state of the search field.
     * @return
     *
     * @attr ref android.R.styleable#SearchView_iconifiedByDefault
     */
    public boolean isIconfiedByDefault() {
        return mIconifiedByDefault;
    }

    /**
     * Iconifies or expands the SearchView. Any query text is cleared when iconified. This is
     * a temporary state and does not override the default iconified state set by
     * {@link #setIconifiedByDefault(boolean)}. If the default state is iconified, then
     * a false here will only be valid until the user closes the field. And if the default
     * state is expanded, then a true here will only clear the text field and not close it.
     *
     * @param iconify a true value will collapse the SearchView to an icon, while a false will
     * expand it.
     */
    public void setIconified(boolean iconify) {
        if (iconify) {
            onCloseClicked();
        } else {
            onSearchClicked();
        }
    }

    /**
     * Returns the current iconified state of the SearchView.
     *
     * @return true if the SearchView is currently iconified, false if the search field is
     * fully visible.
     */
    public boolean isIconified() {
        return mIconified;
    }

    /**
     * Enables showing a submit button when the query is non-empty. In cases where the SearchView
     * is being used to filter the contents of the current activity and doesn't launch a separate
     * results activity, then the submit button should be disabled.
     *
     * @param enabled true to show a submit button for submitting queries, false if a submit
     * button is not required.
     */
    public void setSubmitButtonEnabled(boolean enabled) {
        mSubmitButtonEnabled = enabled;
        updateViewsVisibility(isIconified());
    }

    /**
     * Returns whether the submit button is enabled when necessary or never displayed.
     *
     * @return whether the submit button is enabled automatically when necessary
     */
    public boolean isSubmitButtonEnabled() {
        return mSubmitButtonEnabled;
    }

    /**
     * Specifies if a query refinement button should be displayed alongside each suggestion
     * or if it should depend on the flags set in the individual items retrieved from the
     * suggestions provider. Clicking on the query refinement button will replace the text
     * in the query text field with the text from the suggestion. This flag only takes effect
     * if a SearchableInfo has been specified with {@link #setSearchableInfo(SearchableInfo)}
     * and not when using a custom adapter.
     *
     * @param enable true if all items should have a query refinement button, false if only
     * those items that have a query refinement flag set should have the button.
     *
     * @see SearchManager#SUGGEST_COLUMN_FLAGS
     * @see SearchManager#FLAG_QUERY_REFINEMENT
     */
    public void setQueryRefinementEnabled(boolean enable) {
        mQueryRefinement = enable;
        if (mSuggestionsAdapter instanceof SuggestionsAdapter) {
            ((SuggestionsAdapter) mSuggestionsAdapter).setQueryRefinement(
                    enable ? SuggestionsAdapter.REFINE_ALL : SuggestionsAdapter.REFINE_BY_ENTRY);
        }
    }

    /**
     * Returns whether query refinement is enabled for all items or only specific ones.
     * @return true if enabled for all items, false otherwise.
     */
    public boolean isQueryRefinementEnabled() {
        return mQueryRefinement;
    }

    /**
     * You can set a custom adapter if you wish. Otherwise the default adapter is used to
     * display the suggestions from the suggestions provider associated with the SearchableInfo.
     *
     * @see #setSearchableInfo(SearchableInfo)
     */
    public void setSuggestionsAdapter(CursorAdapter adapter) {
        mSuggestionsAdapter = adapter;

        mQueryTextView.setAdapter(mSuggestionsAdapter);
    }

    /**
     * Returns the adapter used for suggestions, if any.
     * @return the suggestions adapter
     */
    public CursorAdapter getSuggestionsAdapter() {
        return mSuggestionsAdapter;
    }

    /**
     * Makes the view at most this many pixels wide
     *
     * @attr ref android.R.styleable#SearchView_maxWidth
     */
    public void setMaxWidth(int maxpixels) {
        mMaxWidth = maxpixels;

        requestLayout();
    }

    /**
     * Gets the specified maximum width in pixels, if set. Returns zero if
     * no maximum width was specified.
     * @return the maximum width of the view
     *
     * @attr ref android.R.styleable#SearchView_maxWidth
     */
    public int getMaxWidth() {
        return mMaxWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Let the standard measurements take effect in iconified state.
        if (isIconified()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        switch (widthMode) {
            case MeasureSpec.AT_MOST:
                // If there is an upper limit, don't exceed maximum width (explicit or implicit)
                if (mMaxWidth > 0) {
                    width = Math.min(mMaxWidth, width);
                } else {
                    width = Math.min(getPreferredWidth(), width);
                }
                break;
            case MeasureSpec.EXACTLY:
                // If an exact width is specified, still don't exceed any specified maximum width
                if (mMaxWidth > 0) {
                    width = Math.min(mMaxWidth, width);
                }
                break;
            case MeasureSpec.UNSPECIFIED:
                // Use maximum width, if specified, else preferred width
                width = mMaxWidth > 0 ? mMaxWidth : getPreferredWidth();
                break;
        }
        widthMode = MeasureSpec.EXACTLY;
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, widthMode), heightMeasureSpec);
    }

    protected int getPreferredWidth() {
        return getContext().getResources()
                .getDimensionPixelSize(R.dimen.abs__search_view_preferred_width);
    }

    protected void updateViewsVisibility(final boolean collapsed) {
        mIconified = collapsed;
        // Visibility of views that are visible when collapsed
        final int visCollapsed = collapsed ? VISIBLE : GONE;
        // Is there text in the query
        final boolean hasText = !TextUtils.isEmpty(mQueryTextView.getText());

        mSearchButton.setVisibility(visCollapsed);
        updateSubmitButton(hasText);
        mSearchEditFrame.setVisibility(collapsed ? GONE : VISIBLE);
        mSearchHintIcon.setVisibility(mIconifiedByDefault ? GONE : VISIBLE);
        updateCloseButton();
        updateVoiceButton(!hasText);
        updateSubmitArea();
    }

    protected boolean hasVoiceSearch() {
        if (mSearchable != null && mSearchable.getVoiceSearchEnabled()) {
            Intent testIntent = null;
            if (mSearchable.getVoiceSearchLaunchWebSearch()) {
                testIntent = mVoiceWebSearchIntent;
            } else if (mSearchable.getVoiceSearchLaunchRecognizer()) {
                testIntent = mVoiceAppSearchIntent;
            }
            if (testIntent != null) {
                ResolveInfo ri = getContext().getPackageManager().resolveActivity(testIntent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                return ri != null;
            }
        }
        return false;
    }

    protected boolean isSubmitAreaEnabled() {
        return (mSubmitButtonEnabled || mVoiceButtonEnabled) && !isIconified();
    }

    protected void updateSubmitButton(boolean hasText) {
        int visibility = GONE;
        if (mSubmitButtonEnabled && isSubmitAreaEnabled() && hasFocus()
                && (hasText || !mVoiceButtonEnabled)) {
            visibility = VISIBLE;
        }
        mSubmitButton.setVisibility(visibility);
    }

    protected void updateSubmitArea() {
        int visibility = GONE;
        if (isSubmitAreaEnabled()
                && (mSubmitButton.getVisibility() == VISIBLE
                        || mVoiceButton.getVisibility() == VISIBLE)) {
            visibility = VISIBLE;
        }
        mSubmitArea.setVisibility(visibility);
    }

    protected void updateCloseButton() {
        final boolean hasText = !TextUtils.isEmpty(mQueryTextView.getText());
        // Should we show the close button? It is not shown if there's no focus,
        // field is not iconified by default and there is no text in it.
        final boolean showClose = hasText || (mIconifiedByDefault && !mExpandedInActionView);
        mCloseButton.setVisibility(showClose ? VISIBLE : GONE);
        mCloseButton.getDrawable().setState(hasText ? ENABLED_STATE_SET : EMPTY_STATE_SET);
    }

    protected void postUpdateFocusedState() {
        post(mUpdateDrawableStateRunnable);
    }

    protected void updateFocusedState() {
        boolean focused = mQueryTextView.hasFocus();
        mSearchPlate.getBackground().setState(focused ? FOCUSED_STATE_SET : EMPTY_STATE_SET);
        mSubmitArea.getBackground().setState(focused ? FOCUSED_STATE_SET : EMPTY_STATE_SET);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(mUpdateDrawableStateRunnable);
        post(mReleaseCursorRunnable);
        super.onDetachedFromWindow();
    }

    protected void setImeVisibility(final boolean visible) {
        if (visible) {
            post(mShowImeRunnable);
        } else {
            removeCallbacks(mShowImeRunnable);
            InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }
    }

    /**
     * Called by the SuggestionsAdapter
     * @hide
     */
    protected void onQueryRefine(CharSequence queryText) {
        setQuery(queryText);
    }

    protected final OnClickListener mOnClickListener = new OnClickListener() {
    	@Override
        public void onClick(View v) {
            if (v == mSearchButton) {
                onSearchClicked();
            } else if (v == mCloseButton) {
                onCloseClicked();
            } else if (v == mSubmitButton) {
                onSubmitQuery();
            } else if (v == mVoiceButton) {
                onVoiceClicked();
            } else if (v == mQueryTextView) {
                forceSuggestionQuery();
            }
        }
    };

    /**
     * Handles the key down event for dealing with action keys.
     *
     * @param keyCode This is the keycode of the typed key, and is the same value as
     *        found in the KeyEvent parameter.
     * @param event The complete event record for the typed key
     *
     * @return true if the event was handled here, or false if not.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mSearchable == null) {
            return false;
        }

        // if it's an action specified by the searchable activity, launch the
        // entered query with the action key
        // TODO SearchableInfo.ActionKeyInfo actionKey = mSearchable.findActionKey(keyCode);
        // TODO if ((actionKey != null) && (actionKey.getQueryActionMsg() != null)) {
        // TODO     launchQuerySearch(keyCode, actionKey.getQueryActionMsg(), mQueryTextView.getText()
        // TODO             .toString());
        // TODO     return true;
        // TODO }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * React to the user typing "enter" or other hardwired keys while typing in
     * the search box. This handles these special keys while the edit box has
     * focus.
     */
    View.OnKeyListener mTextKeyListener = new View.OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            // guard against possible race conditions
            if (mSearchable == null) {
                return false;
            }

            if (DBG) {
                Log.d(LOG_TAG, "mTextListener.onKey(" + keyCode + "," + event + "), selection: "
                        + mQueryTextView.getListSelection());
            }

            // If a suggestion is selected, handle enter, search key, and action keys
            // as presses on the selected suggestion
            if (mQueryTextView.isPopupShowing()
                    && mQueryTextView.getListSelection() != ListView.INVALID_POSITION) {
                return onSuggestionsKey(v, keyCode, event);
            }

            // If there is text in the query box, handle enter, and action keys
            // The search key is handled by the dialog's onKeyDown().
            if (!mQueryTextView.isEmpty() && KeyEventCompat.hasNoModifiers(event)) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        v.cancelLongPress();

                        // Launch as a regular search.
                        launchQuerySearch(KeyEvent.KEYCODE_UNKNOWN, null, mQueryTextView.getText()
                                .toString());
                        return true;
                    }
                }
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    // TODO SearchableInfo.ActionKeyInfo actionKey = mSearchable.findActionKey(keyCode);
                    // TODO if ((actionKey != null) && (actionKey.getQueryActionMsg() != null)) {
                    // TODO     launchQuerySearch(keyCode, actionKey.getQueryActionMsg(), mQueryTextView
                    // TODO             .getText().toString());
                    // TODO     return true;
                    // TODO }
                }
            }
            return false;
        }
    };

    /**
     * React to the user typing while in the suggestions list. First, check for
     * action keys. If not handled, try refocusing regular characters into the
     * EditText.
     */
    protected boolean onSuggestionsKey(View v, int keyCode, KeyEvent event) {
        // guard against possible race conditions (late arrival after dismiss)
        if (mSearchable == null) {
            return false;
        }
        if (mSuggestionsAdapter == null) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && KeyEventCompat.hasNoModifiers(event)) {
            // First, check for enter or search (both of which we'll treat as a
            // "click")
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SEARCH
                    || keyCode == KeyEvent.KEYCODE_TAB) {
                int position = mQueryTextView.getListSelection();
                return onItemClicked(position, KeyEvent.KEYCODE_UNKNOWN, null);
            }

            // Next, check for left/right moves, which we use to "return" the
            // user to the edit view
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // give "focus" to text editor, with cursor at the beginning if
                // left key, at end if right key
                // TODO: Reverse left/right for right-to-left languages, e.g.
                // Arabic
                int selPoint = (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) ? 0 : mQueryTextView
                        .length();
                mQueryTextView.setSelection(selPoint);
                mQueryTextView.setListSelection(0);
                mQueryTextView.clearListSelection();
                ensureImeVisible(mQueryTextView, true);

                return true;
            }

            // Next, check for an "up and out" move
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && 0 == mQueryTextView.getListSelection()) {
                // TODO: restoreUserQuery();
                // let ACTV complete the move
                return false;
            }

            // Next, check for an "action key"
            // TODO SearchableInfo.ActionKeyInfo actionKey = mSearchable.findActionKey(keyCode);
            // TODO if ((actionKey != null)
            // TODO         && ((actionKey.getSuggestActionMsg() != null) || (actionKey
            // TODO                 .getSuggestActionMsgColumn() != null))) {
            // TODO     // launch suggestion using action key column
            // TODO     int position = mQueryTextView.getListSelection();
            // TODO     if (position != ListView.INVALID_POSITION) {
            // TODO         Cursor c = mSuggestionsAdapter.getCursor();
            // TODO         if (c.moveToPosition(position)) {
            // TODO             final String actionMsg = getActionKeyMessage(c, actionKey);
            // TODO             if (actionMsg != null && (actionMsg.length() > 0)) {
            // TODO                 return onItemClicked(position, keyCode, actionMsg);
            // TODO             }
            // TODO         }
            // TODO     }
            // TODO }
        }
        return false;
    }

    /**
     * For a given suggestion and a given cursor row, get the action message. If
     * not provided by the specific row/column, also check for a single
     * definition (for the action key).
     *
     * @param c The cursor providing suggestions
     * @param actionKey The actionkey record being examined
     *
     * @return Returns a string, or null if no action key message for this
     *         suggestion
     */
    // TODO protected static String getActionKeyMessage(Cursor c, SearchableInfo.ActionKeyInfo actionKey) {
    // TODO     String result = null;
    // TODO     // check first in the cursor data, for a suggestion-specific message
    // TODO     final String column = actionKey.getSuggestActionMsgColumn();
    // TODO     if (column != null) {
    // TODO         result = SuggestionsAdapter.getColumnString(c, column);
    // TODO     }
    // TODO     // If the cursor didn't give us a message, see if there's a single
    // TODO     // message defined
    // TODO     // for the actionkey (for all suggestions)
    // TODO     if (result == null) {
    // TODO         result = actionKey.getSuggestActionMsg();
    // TODO     }
    // TODO     return result;
    // TODO }

    protected int getSearchIconId() {
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.searchViewSearchIcon,
                outValue, true);
        return outValue.resourceId;
    }

    protected CharSequence getDecoratedHint(CharSequence hintText) {
        // If the field is always expanded, then don't add the search icon to the hint
        if (!mIconifiedByDefault) return hintText;

        SpannableStringBuilder ssb = new SpannableStringBuilder("   "); // for the icon
        ssb.append(hintText);
        Drawable searchIcon = getContext().getResources().getDrawable(getSearchIconId());
        int textSize = (int) (mQueryTextView.getTextSize() * 1.25);
        searchIcon.setBounds(0, 0, textSize, textSize);
        ssb.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

    protected void updateQueryHint() {
        if (mQueryHint != null) {
            mQueryTextView.setHint(getDecoratedHint(mQueryHint));
        } else if (mSearchable != null) {
            CharSequence hint = null;
            int hintId = mSearchable.getHintId();
            if (hintId != 0) {
                hint = getContext().getString(hintId);
            }
            if (hint != null) {
                mQueryTextView.setHint(getDecoratedHint(hint));
            }
        } else {
            mQueryTextView.setHint(getDecoratedHint(""));
        }
    }

    /**
     * Updates the auto-complete text view.
     */
    protected void updateSearchAutoComplete() {
        // TODO mQueryTextView.setDropDownAnimationStyle(0); // no animation
        mQueryTextView.setThreshold(mSearchable.getSuggestThreshold());
        mQueryTextView.setImeOptions(mSearchable.getImeOptions());
        int inputType = mSearchable.getInputType();
        // We only touch this if the input type is set up for text (which it almost certainly
        // should be, in the case of search!)
        if ((inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) {
            // The existence of a suggestions authority is the proxy for "suggestions
            // are available here"
            inputType &= ~InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
            if (mSearchable.getSuggestAuthority() != null) {
                inputType |= InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
                // TYPE_TEXT_FLAG_AUTO_COMPLETE means that the text editor is performing
                // auto-completion based on its own semantics, which it will present to the user
                // as they type. This generally means that the input method should not show its
                // own candidates, and the spell checker should not be in action. The text editor
                // supplies its candidates by calling InputMethodManager.displayCompletions(),
                // which in turn will call InputMethodSession.displayCompletions().
                inputType |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            }
        }
        mQueryTextView.setInputType(inputType);
        if (mSuggestionsAdapter != null) {
            mSuggestionsAdapter.changeCursor(null);
        }
        // attach the suggestions adapter, if suggestions are available
        // The existence of a suggestions authority is the proxy for "suggestions available here"
        if (mSearchable.getSuggestAuthority() != null) {
            mSuggestionsAdapter = new SuggestionsAdapter(getContext(),
                    this, mSearchable, mOutsideDrawablesCache);
            mQueryTextView.setAdapter(mSuggestionsAdapter);
            ((SuggestionsAdapter) mSuggestionsAdapter).setQueryRefinement(
                    mQueryRefinement ? SuggestionsAdapter.REFINE_ALL
                    : SuggestionsAdapter.REFINE_BY_ENTRY);
        }
    }

    /**
     * Update the visibility of the voice button.  There are actually two voice search modes,
     * either of which will activate the button.
     * @param empty whether the search query text field is empty. If it is, then the other
     * criteria apply to make the voice button visible.
     */
    protected void updateVoiceButton(boolean empty) {
        int visibility = GONE;
        if (mVoiceButtonEnabled && !isIconified() && empty) {
            visibility = VISIBLE;
            mSubmitButton.setVisibility(GONE);
        }
        mVoiceButton.setVisibility(visibility);
    }

    protected final OnEditorActionListener mOnEditorActionListener = new OnEditorActionListener() {
        /**
         * Called when the input method default action key is pressed.
         */
    	@Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            onSubmitQuery();
            return true;
        }
    };

    protected void onTextChanged(CharSequence newText) {
        CharSequence text = mQueryTextView.getText();
        mUserQuery = text;
        boolean hasText = !TextUtils.isEmpty(text);
        updateSubmitButton(hasText);
        updateVoiceButton(!hasText);
        updateCloseButton();
        updateSubmitArea();
        if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener.onQueryTextChange(newText.toString());
        }
        mOldQueryText = newText.toString();
    }

    protected void onSubmitQuery() {
        CharSequence query = mQueryTextView.getText();
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener == null
                    || !mOnQueryChangeListener.onQueryTextSubmit(query.toString())) {
                if (mSearchable != null) {
                    launchQuerySearch(KeyEvent.KEYCODE_UNKNOWN, null, query.toString());
                    setImeVisibility(false);
                }
                dismissSuggestions();
            }
        }
    }

    protected void dismissSuggestions() {
        mQueryTextView.dismissDropDown();
    }

    protected void onCloseClicked() {
        CharSequence text = mQueryTextView.getText();
        if (TextUtils.isEmpty(text)) {
            if (mIconifiedByDefault) {
                // If the app doesn't override the close behavior
                if (mOnCloseListener == null || !mOnCloseListener.onClose()) {
                    // hide the keyboard and remove focus
                    clearFocus();
                    // collapse the search field
                    updateViewsVisibility(true);
                }
            }
        } else {
            mQueryTextView.setText("");
            mQueryTextView.requestFocus();
            setImeVisibility(true);
        }

    }

    protected void onSearchClicked() {
        updateViewsVisibility(false);
        mQueryTextView.requestFocus();
        setImeVisibility(true);
        if (mOnSearchClickListener != null) {
            mOnSearchClickListener.onClick(this);
        }
    }

    protected void onVoiceClicked() {
        // guard against possible race conditions
        if (mSearchable == null) {
            return;
        }
        SearchableInfo searchable = mSearchable;
        try {
            if (searchable.getVoiceSearchLaunchWebSearch()) {
                Intent webSearchIntent = createVoiceWebSearchIntent(mVoiceWebSearchIntent,
                        searchable);
                launchIntent(webSearchIntent, REQUESTCODE_VOICWEBSEARCH);
            } else if (searchable.getVoiceSearchLaunchRecognizer()) {
                Intent appSearchIntent = createVoiceAppSearchIntent(mVoiceAppSearchIntent,
                        searchable);
                launchIntent(appSearchIntent, REQUESTCODE_VOICAPPSEARCH);
            }
        } catch (ActivityNotFoundException e) {
            // Should not happen, since we check the availability of
            // voice search before showing the button. But just in case...
            Log.w(LOG_TAG, "Could not find voice search activity");
        }
    }

    void onTextFocusChanged() {
        updateViewsVisibility(isIconified());
        // Delayed update to make sure that the focus has settled down and window focus changes
        // don't affect it. A synchronous update was not working.
        postUpdateFocusedState();
        if (mQueryTextView.hasFocus()) {
            forceSuggestionQuery();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        postUpdateFocusedState();
    }

    /**
     * {@inheritDoc}
     */
    public void onActionViewCollapsed() {
        clearFocus();
        updateViewsVisibility(true);
        mQueryTextView.setImeOptions(mCollapsedImeOptions);
        mExpandedInActionView = false;
    }

    /**
     * {@inheritDoc}
     */
    public void onActionViewExpanded() {
        if (mExpandedInActionView) return;

        mExpandedInActionView = true;
        mCollapsedImeOptions = mQueryTextView.getImeOptions();
        mQueryTextView.setImeOptions(mCollapsedImeOptions | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        mQueryTextView.setText("");
        setIconified(false);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(SearchView.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(SearchView.class.getName());
    }

    protected void adjustDropDownSizeAndPosition() {
        if (mDropDownAnchor.getWidth() > 1) {
            Resources res = getContext().getResources();
            int anchorPadding = mSearchPlate.getPaddingLeft();
            Rect dropDownPadding = new Rect();
            int iconOffset = mIconifiedByDefault
                    ? res.getDimensionPixelSize(R.dimen.abs__dropdownitem_icon_width)
                    + res.getDimensionPixelSize(R.dimen.abs__dropdownitem_text_padding_left)
                    : 0;
            mQueryTextView.getDropDownBackground().getPadding(dropDownPadding);
            mQueryTextView.setDropDownHorizontalOffset(-(dropDownPadding.left + iconOffset)
                    + anchorPadding);
            mQueryTextView.setDropDownWidth(mDropDownAnchor.getWidth() + dropDownPadding.left
                    + dropDownPadding.right + iconOffset - (anchorPadding));
        }
    }

    protected boolean onItemClicked(int position, int actionKey, String actionMsg) {
        if (mOnSuggestionListener == null
                || !mOnSuggestionListener.onSuggestionClick(position)) {
            launchSuggestion(position, KeyEvent.KEYCODE_UNKNOWN, null);
            setImeVisibility(false);
            dismissSuggestions();
            return true;
        }
        return false;
    }

    protected boolean onItemSelected(int position) {
        if (mOnSuggestionListener == null
                || !mOnSuggestionListener.onSuggestionSelect(position)) {
            rewriteQueryFromSuggestion(position);
            return true;
        }
        return false;
    }

    protected final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        /**
         * Implements OnItemClickListener
         */
    	@Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (DBG) Log.d(LOG_TAG, "onItemClick() position " + position);
            onItemClicked(position, KeyEvent.KEYCODE_UNKNOWN, null);
        }
    };

    protected final OnItemSelectedListener mOnItemSelectedListener = new OnItemSelectedListener() {
        /**
         * Implements OnItemSelectedListener
         */
    	@Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (DBG) Log.d(LOG_TAG, "onItemSelected() position " + position);
            SearchView.this.onItemSelected(position);
        }

        /**
         * Implements OnItemSelectedListener
         */
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            if (DBG)
                Log.d(LOG_TAG, "onNothingSelected()");
        }
    };

    /**
     * Query rewriting.
     */
    protected void rewriteQueryFromSuggestion(int position) {
        CharSequence oldQuery = mQueryTextView.getText();
        Cursor c = mSuggestionsAdapter.getCursor();
        if (c == null) {
            return;
        }
        if (c.moveToPosition(position)) {
            // Get the new query from the suggestion.
            CharSequence newQuery = mSuggestionsAdapter.convertToString(c);
            if (newQuery != null) {
                // The suggestion rewrites the query.
                // Update the text field, without getting new suggestions.
                setQuery(newQuery);
            } else {
                // The suggestion does not rewrite the query, restore the user's query.
                setQuery(oldQuery);
            }
        } else {
            // We got a bad position, restore the user's query.
            setQuery(oldQuery);
        }
    }

    /**
     * Launches an intent based on a suggestion.
     *
     * @param position The index of the suggestion to create the intent from.
     * @param actionKey The key code of the action key that was pressed,
     *        or {@link KeyEvent#KEYCODE_UNKNOWN} if none.
     * @param actionMsg The message for the action key that was pressed,
     *        or <code>null</code> if none.
     * @return true if a successful launch, false if could not (e.g. bad position).
     */
    protected boolean launchSuggestion(int position, int actionKey, String actionMsg) {
        Cursor c = mSuggestionsAdapter.getCursor();
        if ((c != null) && c.moveToPosition(position)) {

            Intent intent = createIntentFromSuggestion(c, actionKey, actionMsg);

            // launch the intent
            launchIntent(intent, REQUESTCODE_SUGGESTION);

            return true;
        }
        return false;
    }
    
    public void setOnStartActivityListener(OnStartActivityListener l) { 
    	mOnStartActivityListener = l;
    }
    
    /**
     * Launches an intent, including any special intent handling.
     */
    protected void launchIntent(Intent intent, int requestCode) {
        if (intent == null) return;
        
        try {
            // If the intent was created from a suggestion, it will always have an explicit
            // component here.
        	OnStartActivityListener listener = mOnStartActivityListener;
        	if (listener == null || !listener.onStartActivity(intent, requestCode))
        		getContext().startActivity(intent);
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "Failed launch activity: " + intent, ex);
        }
    }

    /**
     * Sets the text in the query box, without updating the suggestions.
     */
    protected void setQuery(CharSequence query) {
        setText(mQueryTextView, query, true);
        // Move the cursor to the end
        mQueryTextView.setSelection(TextUtils.isEmpty(query) ? 0 : query.length());
    }

    protected void launchQuerySearch(int actionKey, String actionMsg, String query) {
        String action = Intent.ACTION_SEARCH;
        Intent intent = createIntent(action, null, null, query, actionKey, actionMsg);
        launchIntent(intent, REQUESTCODE_SEARCH);
    }

    /**
     * Constructs an intent from the given information and the search dialog state.
     *
     * @param action Intent action.
     * @param data Intent data, or <code>null</code>.
     * @param extraData Data for {@link SearchManager#EXTRA_DATA_KEY} or <code>null</code>.
     * @param query Intent query, or <code>null</code>.
     * @param actionKey The key code of the action key that was pressed,
     *        or {@link KeyEvent#KEYCODE_UNKNOWN} if none.
     * @param actionMsg The message for the action key that was pressed,
     *        or <code>null</code> if none.
     * @return The intent.
     */
    protected Intent createIntent(String action, Uri data, String extraData, String query,
    		int actionKey, String actionMsg) {
        // Now build the Intent
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // We need CLEAR_TOP to avoid reusing an old task that has other activities
        // on top of the one we want. We don't want to do this in in-app search though,
        // as it can be destructive to the activity stack.
        if (data != null) {
            intent.setData(data);
        }
        intent.putExtra(SearchManager.USER_QUERY, mUserQuery);
        if (query != null) {
            intent.putExtra(SearchManager.QUERY, query);
        }
        if (extraData != null) {
            intent.putExtra(SearchManager.EXTRA_DATA_KEY, extraData);
        }
        if (mAppSearchData != null) {
            intent.putExtra(SearchManager.APP_DATA, mAppSearchData);
        }
        if (actionKey != KeyEvent.KEYCODE_UNKNOWN) {
            intent.putExtra(SearchManager.ACTION_KEY, actionKey);
            intent.putExtra(SearchManager.ACTION_MSG, actionMsg);
        }
        intent.setComponent(mSearchable.getSearchActivity());
        return intent;
    }

    /**
     * Create and return an Intent that can launch the voice search activity for web search.
     */
    protected Intent createVoiceWebSearchIntent(Intent baseIntent, SearchableInfo searchable) {
        Intent voiceIntent = new Intent(baseIntent);
        ComponentName searchActivity = searchable.getSearchActivity();
        voiceIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, searchActivity == null ? null
                : searchActivity.flattenToShortString());
        return voiceIntent;
    }

    /**
     * Create and return an Intent that can launch the voice search activity, perform a specific
     * voice transcription, and forward the results to the searchable activity.
     *
     * @param baseIntent The voice app search intent to start from
     * @return A completely-configured intent ready to send to the voice search activity
     */
    protected Intent createVoiceAppSearchIntent(Intent baseIntent, SearchableInfo searchable) {
        ComponentName searchActivity = searchable.getSearchActivity();

        // create the necessary intent to set up a search-and-forward operation
        // in the voice search system.   We have to keep the bundle separate,
        // because it becomes immutable once it enters the PendingIntent
        Intent queryIntent = new Intent(Intent.ACTION_SEARCH);
        queryIntent.setComponent(searchActivity);
        PendingIntent pending = PendingIntent.getActivity(getContext(), 0, queryIntent,
                PendingIntent.FLAG_ONE_SHOT);

        // Now set up the bundle that will be inserted into the pending intent
        // when it's time to do the search.  We always build it here (even if empty)
        // because the voice search activity will always need to insert "QUERY" into
        // it anyway.
        Bundle queryExtras = new Bundle();

        // Now build the intent to launch the voice search.  Add all necessary
        // extras to launch the voice recognizer, and then all the necessary extras
        // to forward the results to the searchable activity
        Intent voiceIntent = new Intent(baseIntent);

        // Add all of the configuration options supplied by the searchable's metadata
        String languageModel = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
        String prompt = null;
        String language = null;
        int maxResults = 1;

        Resources resources = getResources();
        if (searchable.getVoiceLanguageModeId() != 0) {
            languageModel = resources.getString(searchable.getVoiceLanguageModeId());
        }
        if (searchable.getVoicePromptTextId() != 0) {
            prompt = resources.getString(searchable.getVoicePromptTextId());
        }
        if (searchable.getVoiceLanguageId() != 0) {
            language = resources.getString(searchable.getVoiceLanguageId());
        }
        if (searchable.getVoiceMaxResults() != 0) {
            maxResults = searchable.getVoiceMaxResults();
        }
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, searchActivity == null ? null
                : searchActivity.flattenToShortString());

        // Add the values that configure forwarding the results
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT, pending);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE, queryExtras);

        return voiceIntent;
    }

    /**
     * When a particular suggestion has been selected, perform the various lookups required
     * to use the suggestion.  This includes checking the cursor for suggestion-specific data,
     * and/or falling back to the XML for defaults;  It also creates REST style Uri data when
     * the suggestion includes a data id.
     *
     * @param c The suggestions cursor, moved to the row of the user's selection
     * @param actionKey The key code of the action key that was pressed,
     *        or {@link KeyEvent#KEYCODE_UNKNOWN} if none.
     * @param actionMsg The message for the action key that was pressed,
     *        or <code>null</code> if none.
     * @return An intent for the suggestion at the cursor's position.
     */
    protected Intent createIntentFromSuggestion(Cursor c, int actionKey, String actionMsg) {
        try {
            // use specific action if supplied, or default action if supplied, or fixed default
            String action = SuggestionsAdapter.getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_ACTION);

            if (action == null) {
                action = mSearchable.getSuggestIntentAction();
            }
            if (action == null) {
                action = Intent.ACTION_SEARCH;
            }

            // use specific data if supplied, or default data if supplied
            String data = SuggestionsAdapter.getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA);
            if (data == null) {
                data = mSearchable.getSuggestIntentData();
            }
            // then, if an ID was provided, append it.
            if (data != null) {
                String id = SuggestionsAdapter.getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
                if (id != null) {
                    data = data + "/" + Uri.encode(id);
                }
            }
            Uri dataUri = (data == null) ? null : Uri.parse(data);

            String query = SuggestionsAdapter.getColumnString(c, SearchManager.SUGGEST_COLUMN_QUERY);
            String extraData = SuggestionsAdapter.getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);

            return createIntent(action, dataUri, extraData, query, actionKey, actionMsg);
        } catch (RuntimeException e ) {
            int rowNum;
            try {                       // be really paranoid now
                rowNum = c.getPosition();
            } catch (RuntimeException e2 ) {
                rowNum = -1;
            }
            Log.w(LOG_TAG, "Search suggestions cursor at row " + rowNum +
                            " returned exception.", e);
            return null;
        }
    }

    protected void forceSuggestionQuery() {
        try {
            Method before = SearchAutoComplete.class.getMethod("doBeforeTextChanged");
            Method after = SearchAutoComplete.class.getMethod("doAfterTextChanged");
            before.setAccessible(true);
            after.setAccessible(true);
            before.invoke(mQueryTextView);
            after.invoke(mQueryTextView);
        } catch (Exception e) {
            // Oh well...
        }
    }

    static boolean isLandscapeMode(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Callback to watch the text field for empty/non-empty
     */
    protected TextWatcher mTextWatcher = new TextWatcher() {
    	@Override
        public void beforeTextChanged(CharSequence s, int start, int before, int after) { 
    	}
    	@Override
        public void onTextChanged(CharSequence s, int start, int before, int after) {
            SearchView.this.onTextChanged(s);
        }
    	@Override
        public void afterTextChanged(Editable s) {
        }
    };

    protected static void ensureImeVisible(AutoCompleteTextView view, boolean visible) {
        try {
            Method method = AutoCompleteTextView.class.getMethod("ensureImeVisible", boolean.class);
            method.setAccessible(true);
            method.invoke(view, visible);
        } catch (Exception e) {
            //Oh well...
        }
    }

    protected static void showSoftInputUnchecked(View view, InputMethodManager imm, int flags) {
        try {
            Method method = imm.getClass().getMethod("showSoftInputUnchecked", int.class, ResultReceiver.class);
            method.setAccessible(true);
            method.invoke(imm, flags, null);
        } catch (Exception e) {
            //Fallback to public API which hopefully does mostly the same thing
            imm.showSoftInput(view, flags);
        }
    }

    protected static void setText(AutoCompleteTextView view, CharSequence text, boolean filter) {
        try {
            Method method = AutoCompleteTextView.class.getMethod("setText", CharSequence.class, boolean.class);
            method.setAccessible(true);
            method.invoke(view, text, filter);
        } catch (Exception e) {
            //Fallback to public API which hopefully does mostly the same thing
            view.setText(text);
        }
    }
    
}
