package org.javenstudio.provider.app.picasa;

import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.people.user.UserActionProvider;

final class PicasaUserComments extends UserActionProvider {

	public PicasaUserComments(PicasaUser user, PicasaCommentProvider p) { 
		super(user, p, ResourceHelper.getResources().getString(R.string.label_action_comment));
	}
	
	@Override
	public boolean bindListView(IActivity activity) { 
		PicasaCommentProvider p = (PicasaCommentProvider)getProvider();
		if (p != null) {
			PicasaCommentBinder binder = (PicasaCommentBinder)p.getBinder();
			if (binder != null) { 
				binder.bindListView(activity, 
						getTabItem().getListView(), getAdapter(activity));
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public ListAdapter getAdapter(IActivity activity) { 
		PicasaCommentProvider p = (PicasaCommentProvider)getProvider();
		if (p != null && activity != null) {
			PicasaCommentBinder binder = (PicasaCommentBinder)p.getBinder();
			if (binder != null) 
				return binder.createListAdapter(activity);
		}
		
		return null;
	}
	
}
