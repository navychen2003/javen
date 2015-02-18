package org.javenstudio.provider.library.select;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.cocoka.widget.adapter.AbstractAdapter;

public class SelectListAdapter extends AbstractAdapter<SelectListItem> {

	private final Activity mActivity;
	private final ISelectCallback mCallback;
	
	public SelectListAdapter(Activity activity, ISelectCallback callback, 
			SelectListDataSets data, int resource) {
		super(activity, data, resource, new String[]{}, new int[]{});
		mActivity = activity;
		mCallback = callback;
	}
	
	public Activity getActivity() { return mActivity; }
	public ISelectCallback getCallback() { return mCallback; }
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(getContext());
		View view = convertView;
		
		SelectListItem item = (SelectListItem)getItem(position);
		if (item != null) {
			int viewRes = item.getViewRes();
			if (view == null || view.getId() != viewRes)
				view = inflater.inflate(viewRes, null);
			
			item.bindView(getActivity(), getCallback(), view);
			//item.setBindView(getActivity().getActivity(), view);
			//view.setTag(item);
		}
		
		return view; 
	}
	
}
