package org.anybox.android.library.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.anybox.android.library.PhotoShowActivity;
import org.anybox.android.library.R;
import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxSectionFile;
import org.javenstudio.provider.app.anybox.library.AnyboxLibraryProvider;
import org.javenstudio.provider.app.anybox.library.AnyboxPhotoSet;
import org.javenstudio.provider.app.anybox.library.AnyboxSectionFileItem;
import org.javenstudio.provider.library.ISectionList;

public class MySectionFileItem extends AnyboxSectionFileItem {
	private static final Logger LOG = Logger.getLogger(MySectionFileItem.class);
	
	private final ISectionList mSectionList;
	private final AnyboxSectionFile mSectionFile;
	
	public MySectionFileItem(AnyboxLibraryProvider p, 
			AnyboxSectionFile data, ISectionList list) {
		super(p, data);
		mSectionList = list;
		mSectionFile = data;
	}
	
	@Override
	protected boolean onImageItemClick(IActivity activity) { 
		ISectionList list = mSectionList;
		AnyboxSectionFile data = mSectionFile;
		
		if (list != null && data != null) {
			AnyboxPhotoSet photoSet = new AnyboxPhotoSet(list, data) { 
					@Override
					public AnyboxAccount getAccountUser() {
						return getProvider().getAccountUser();
					}
					@Override
					public Drawable getProviderIcon() { 
						return ResourceHelper.getResources().getDrawable(R.drawable.ic_nav_anybox);
					}
				};
			PhotoShowActivity.actionShow(activity.getActivity(), photoSet);
		}
		
		return true; 
	}
	
	@Override
	public void onDialogShow(AlertDialogBuilder builder, 
			DialogInterface dialog) {
		if (builder == null || dialog == null) return;
		if (!(dialog instanceof AlertDialog)) return;
		
		ImageView iconView = builder.getHelper().findIconView((AlertDialog)dialog);
		if (iconView != null) {
			ViewGroup.LayoutParams lp = iconView.getLayoutParams();
			if (lp != null) {
				lp.width = (int)ResourceHelper.getResources().getDimension(R.dimen.dialog_foldericon_width);
				lp.height = (int)ResourceHelper.getResources().getDimension(R.dimen.dialog_foldericon_width);
				
				if (LOG.isDebugEnabled())
					LOG.debug("onDialogShow: set icon view lp=" + lp);
				
				iconView.setLayoutParams(lp);
				iconView.requestLayout();
			}
		}
	}
	
}
