package org.javenstudio.cocoka.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.app.BaseResources;
import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.data.IMediaItem;
import org.javenstudio.cocoka.opengl.BottomControls;
import org.javenstudio.common.util.Logger;

public class PhotoBottomControls extends BottomControls {
	private static final Logger LOG = Logger.getLogger(PhotoBottomControls.class);

	private IMediaItem mMediaItem = null;
	
	public PhotoBottomControls(Context context, Delegate delegate, ViewGroup layout) {
		super(context, delegate, layout);
	}
    
    @Override
    protected View findControlsView(View view) { 
    	return view != null ? view.findViewById(R.id.photopage_bottom_controls) : null;
    }
    
    @Override
    protected View findBodyView(View view) { 
    	return view != null ? view.findViewById(R.id.photopage_bottom_body) : null;
    }
    
    @Override
    protected View inflateContainerView(LayoutInflater inflater, ViewGroup root) { 
    	return inflater.inflate(R.layout.photopage_bottom_main, root, false);
    }
    
    @Override
    protected ViewGroup.LayoutParams createContainerLayoutParams() { 
    	return new RelativeLayout.LayoutParams(
    			RelativeLayout.LayoutParams.MATCH_PARENT, 
    			RelativeLayout.LayoutParams.MATCH_PARENT);
    }
    
    @Override
    protected void initViews(LayoutInflater inflater, View view) { 
    	super.initViews(inflater, view);
    	
    	//final Resources res = mDelegate.getActivity().getResources();
    	
    	//int height = res.getDisplayMetrics().heightPixels -
    	//		res.getDimensionPixelSize(R.dimen.photopage_bottom_controls_height);
    	
    	View controls = findControlsView(view);
    	if (controls != null) {
    		int backgroundRes = BaseResources.getInstance().getDrawableRes(BaseResources.drawable.photo_controls_background);
    		if (backgroundRes != 0) controls.setBackgroundResource(backgroundRes);
    	}
    	
    	FrameLayout body = (FrameLayout)findBodyView(view);
    	View details = inflater.inflate(R.layout.photopage_details, body, false);
    	
    	body.addView(details, new FrameLayout.LayoutParams(
    			FrameLayout.LayoutParams.MATCH_PARENT, 
    			FrameLayout.LayoutParams.MATCH_PARENT));
    	
    }
    
    protected void refreshBodyViews(IMediaItem item, View view) { 
    	if (item == null || view == null) return;
    	
    	final View body = findBodyView(view);
    	final ViewGroup actionLayout = (ViewGroup)view.findViewById(R.id.photopage_details_actionlayout);
    	final ViewGroup contentLayout = (ViewGroup)view.findViewById(R.id.photopage_details_contentlayout);
    	
    	if (body == null || actionLayout == null || contentLayout == null) 
    		return;
    	
    	final Activity activity = mDelegate.getActivity();
    	final Resources res = activity.getResources();
    	
    	if (item != mMediaItem) { 
    		if (LOG.isDebugEnabled())
    			LOG.debug("init MediaItem details: " + item);
    		
    		LayoutInflater inflater = LayoutInflater.from(mDelegate.getActivity());
    		ActionItem[] items = item.getControls().getActionItems(mDelegate.getActivity());
    		actionLayout.removeAllViews();
    		
    		PhotoAction defaultItem = null;
    		
    		for (int i=0; items != null && i < items.length; i++) { 
    			final PhotoAction actionItem = (PhotoAction)items[i];
    			if (actionItem == null) continue;
    			
    			TextView actionView = (TextView)inflater.inflate(
    					R.layout.photopage_action, actionLayout, false);
    			
    			int backgroundRes = BaseResources.getInstance().getDrawableRes(BaseResources.drawable.photo_action_background);
    			if (backgroundRes != 0) actionView.setBackgroundResource(backgroundRes);
    			
    			actionView.setText(actionItem.getTitle());
    			actionView.setCompoundDrawablesWithIntrinsicBounds(actionItem.getIconRes(), 0, 0, 0);
    			
    			if (defaultItem == null || actionItem.isDefault()) 
    				defaultItem = actionItem;
    			
    			actionView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							actionItem.actionClick(activity, contentLayout);
						}
					});
    			
    			actionItem.setActionItems(items);
    			actionItem.setBindedView(actionView);
    			actionLayout.addView(actionView);
    		}
    		
    		contentLayout.removeAllViews();
    		if (defaultItem != null) 
    			defaultItem.actionClick(activity, contentLayout);
    		
    		mMediaItem = item;
    	}
    	
    	if (mDelegate.canDisplayBottomControl(body) && actionLayout.getChildCount() > 0) {
    		body.setVisibility(View.VISIBLE);
    		view.setBackgroundColor(res.getColor(R.color.photo_main_background));
    		
    	} else {
    		body.setVisibility(View.INVISIBLE);
    		view.setBackgroundResource(0);
    	}
    }
    
    @Override
    protected void refreshViews(Object obj, View view) { 
    	IMediaItem item = (IMediaItem)obj;
    	if (item == null || view == null) return;
    	
    	refreshBodyViews(item, view);
    	
    	ImageView thumbView = (ImageView)view.findViewById(R.id.photopage_bottom_controls_thumb);
    	if (thumbView != null) 
    		thumbView.setImageDrawable(item.getThumbnailDrawable(100, 100));
    	
    	TextView titleView = (TextView)view.findViewById(R.id.photopage_bottom_controls_title);
    	if (titleView != null) 
    		titleView.setText(item.getControls().getTitle());
    	
    	TextView subtitleView = (TextView)view.findViewById(R.id.photopage_bottom_controls_subtitle);
    	if (subtitleView != null) 
    		subtitleView.setText(item.getControls().getSubTitle());
    	
    	View countView = view.findViewById(R.id.photopage_bottom_controls_counts);
    	boolean hasCount = false;
    	
    	TextView commentView = (TextView)view.findViewById(R.id.photopage_bottom_controls_comments);
    	if (commentView != null) { 
    		int count = item.getControls().getStatisticCount(IMediaItem.COUNT_COMMENT);
    		if (count > 0) { 
    			commentView.setText(""+count);
    			commentView.setVisibility(View.VISIBLE);
    			hasCount = true;
    		} else
    			commentView.setVisibility(View.GONE);
    	}
    	
    	TextView favoriteView = (TextView)view.findViewById(R.id.photopage_bottom_controls_favorites);
    	if (favoriteView != null) { 
    		int count = item.getControls().getStatisticCount(IMediaItem.COUNT_FAVORITE);
    		if (count > 0) { 
    			favoriteView.setText(""+count);
    			favoriteView.setVisibility(View.VISIBLE);
    			hasCount = true;
    		} else
    			favoriteView.setVisibility(View.GONE);
    	}
    	
    	TextView likeView = (TextView)view.findViewById(R.id.photopage_bottom_controls_likes);
    	if (likeView != null) { 
    		int count = item.getControls().getStatisticCount(IMediaItem.COUNT_LIKE);
    		if (count > 0) { 
    			likeView.setText(""+count);
    			likeView.setVisibility(View.VISIBLE);
    			hasCount = true;
    		} else
    			likeView.setVisibility(View.GONE);
    	}
    	
    	ImageView logoView = (ImageView)view.findViewById(R.id.photopage_bottom_controls_logo);
    	if (logoView != null) { 
    		Drawable logo = item.getControls().getProviderIcon();
    		if (logo != null) { 
    			logoView.setImageDrawable(logo);
    			logoView.setVisibility(View.VISIBLE);
    			hasCount = true;
    		} else
    			logoView.setVisibility(View.GONE);
    	}
    	
    	if (countView != null) { 
    		countView.setVisibility(hasCount ? View.VISIBLE : View.GONE);
    	}
    }
    
}
