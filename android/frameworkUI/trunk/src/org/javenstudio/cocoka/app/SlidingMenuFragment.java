package org.javenstudio.cocoka.app;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.javenstudio.cocoka.app.SlidingMenuActivity.OnMenuScrolledListener;
import org.javenstudio.cocoka.app.SlidingMenuActivity.OnMenuVisibilityChangeListener;
import org.javenstudio.common.util.Logger;

public class SlidingMenuFragment extends ListFragment 
		implements OnMenuVisibilityChangeListener, OnMenuScrolledListener {
	private static final Logger LOG = Logger.getLogger(SlidingMenuFragment.class);

	public interface ISlidingMenuActivity { 
		public ActionItem[] getNavigationItems();
		public void setOnMenuVisibilityChangeListener(OnMenuVisibilityChangeListener listener);
		public void setOnMenuScrolledListener(OnMenuScrolledListener listener);
	}
	
	public ISlidingMenuActivity getSlidingMenuActivity() { 
		return (ISlidingMenuActivity)getActivity();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
		getSlidingMenuActivity().setOnMenuVisibilityChangeListener(this);
		getSlidingMenuActivity().setOnMenuScrolledListener(this);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		ViewGroup view = (ViewGroup)inflater.inflate(R.layout.slidingmenu_list, null);
		
		View header = onCreateHeaderView(inflater, container, savedInstanceState);
		if (header != null) {
			ViewGroup headerGroup = (ViewGroup)view.findViewById(R.id.slidingmenu_menu_header);
			View headerDivider = (View)view.findViewById(R.id.slidingmenu_menu_header_divider);
			
			headerGroup.addView(header, new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			headerGroup.setVisibility(View.VISIBLE);
			headerDivider.setVisibility(View.VISIBLE);
		}
		
		View bottom = onCreateFooterView(inflater, container, savedInstanceState);
		if (bottom != null) {
			ViewGroup bottomGroup = (ViewGroup)view.findViewById(R.id.slidingmenu_menu_bottom);
			View bottomDivider = (View)view.findViewById(R.id.slidingmenu_menu_bottom_divider);
			
			bottomGroup.addView(bottom, new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			bottomGroup.setVisibility(View.VISIBLE);
			bottomDivider.setVisibility(View.VISIBLE);
		}
		
		View behind = onCreateBehindView(inflater, container, savedInstanceState);
		if (behind != null) {
			ViewGroup behindGroup = (ViewGroup)view.findViewById(R.id.slidingmenu_menu_behind);
			
			behindGroup.addView(behind, new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			behindGroup.setVisibility(View.VISIBLE);
		}
		
		View above = onCreateAboveView(inflater, container, savedInstanceState);
		if (above != null) {
			ViewGroup aboveGroup = (ViewGroup)view.findViewById(R.id.slidingmenu_menu_above);
			
			aboveGroup.addView(above, new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			aboveGroup.setVisibility(View.VISIBLE);
		}
		
		return view;
	}

	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		RefreshListView.disableOverscrollGlowEdge(getListView());
	}
	
	@Override
	public ListView getListView() {
		return super.getListView();
	}
	
	public View getHeaderView() {
		return getView().findViewById(R.id.slidingmenu_menu_header);
	}
	
	public View getHeaderDividerView() {
		return getView().findViewById(R.id.slidingmenu_menu_header_divider);
	}
	
	public View getFooterView() {
		return getView().findViewById(R.id.slidingmenu_menu_bottom);
	}
	
	public View getFooterDividerView() {
		return getView().findViewById(R.id.slidingmenu_menu_bottom_divider);
	}
	
	protected View onCreateHeaderView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		return null;
	}
	
	protected View onCreateFooterView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		return null;
	}
	
	protected View onCreateBehindView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		return null;
	}
	
	protected View onCreateAboveView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		return null;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setMenuListAdapter();
	}
	
	protected void setMenuListAdapter() {
		ActionItem[] items = getSlidingMenuActivity().getNavigationItems();
		if (items != null && items.length > 0) {
			if (LOG.isDebugEnabled()) 
				LOG.debug("onActivityCreated: navigationItems: " + items.length);
			
			SlidingMenuAdapter adapter = new SlidingMenuAdapter(getActivity(), items);
			setListAdapter(adapter);
		}
	}

	@Override
	public void setListAdapter(ListAdapter adapter) {
		if (LOG.isDebugEnabled()) LOG.debug("setListAdapter: adapter=" + adapter);
		super.setListAdapter(adapter);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) { 
		onMenuListItemClick(position);
	}
	
	protected void onMenuListItemClick(int position) {
		ActionItem[] items = getSlidingMenuActivity().getNavigationItems();
		
		if (items != null && position >= 0 && position < items.length) { 
			ActionItem item = items[position];
			ActionItem.OnClickListener listener = item.getOnClickListener();
			if (listener != null) 
				listener.onActionClick();
		}
	}

	@Override
	public void onMenuVisibilityChanged(int visibility) {
	}

	@Override
	public void onMenuScrolled(float percentOpen) {
	}
	
}
