package org.javenstudio.provider.library;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.section.SectionListItem;

public class SectionTouchListener implements View.OnClickListener, 
		View.OnLongClickListener, View.OnTouchListener, GestureDetector.OnGestureListener {
	private static final Logger LOG = Logger.getLogger(SectionTouchListener.class);

	private final IActivity mActivity;
	private final SectionListItem mItem;
	private final GestureDetector mGestureDetector;
	
	public SectionTouchListener(IActivity activity, SectionListItem item) {
		if (activity == null || item == null) throw new NullPointerException();
		mActivity = activity;
		mItem = item;
		mGestureDetector = new GestureDetector(activity.getActivity(), this);
	}
	
	public IActivity getActivity() { return mActivity; }
	public SectionListItem getSectionItem() { return mItem; }
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	}

	@Override
	public boolean onLongClick(View v) {
		if (LOG.isDebugEnabled()) LOG.debug("onLongClick");
		if (getActivity().getActionHelper().isActionMode()) return false;
		return getSectionItem().onItemLongClick(getActivity());
	}

	@Override
	public void onClick(View v) {
		if (LOG.isDebugEnabled()) LOG.debug("onClick");
		if (getActivity().getActionHelper().isActionMode()) {
			getSectionItem().onItemSelect(getActivity(), 
					!getSectionItem().isSelected(getActivity()));
		} else {
			getSectionItem().onItemClick(getActivity());
		}
	}

	@Override
	public boolean onDown(MotionEvent e) {
		if (LOG.isDebugEnabled()) LOG.debug("onDown: e=" + e);
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		if (LOG.isDebugEnabled()) LOG.debug("onShowPress");
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (LOG.isDebugEnabled()) LOG.debug("onSingleTapUp");
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, 
			float distanceX, float distanceY) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("onScroll: distanceX=" + distanceX 
					+ " distanceY=" + distanceY);
		}
		
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		if (LOG.isDebugEnabled()) LOG.debug("onLongPress");
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, 
			float velocityX, float velocityY) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("onFling: velocityX=" + velocityX 
					+ " velocityY=" + velocityY);
		}
		
		return false;
	}

}
