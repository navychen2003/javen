package org.javenstudio.provider.account.host;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.cocoka.widget.adapter.AbstractAdapter;

public class HostListAdapter extends AbstractAdapter<HostListItem> {

	private final Activity mActivity;
	
	public HostListAdapter(Activity activity, 
			HostListDataSets data, int resource) {
		super(activity, data, resource, new String[]{}, new int[]{});
		mActivity = activity;
	}
	
	public Activity getActivity() { return mActivity; }
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(getContext());
		View view = convertView;
		
		HostListItem item = (HostListItem)getItem(position);
		if (item != null) {
			int viewRes = item.getViewRes();
			if (view == null || view.getId() != viewRes)
				view = inflater.inflate(viewRes, null);
			
			item.bindView(getActivity(), view);
			//item.setBindView(getActivity().getActivity(), view);
			//view.setTag(item);
		}
		
		return view; 
	}
	
}
