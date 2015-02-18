package org.anybox.android.library.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.anybox.android.library.R;
import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.app.anybox.AnyboxSectionFolder;
import org.javenstudio.provider.app.anybox.library.AnyboxLibraryProvider;
import org.javenstudio.provider.app.anybox.library.AnyboxSectionFolderItem;

public class MySectionFolderItem extends AnyboxSectionFolderItem {
	private static final Logger LOG = Logger.getLogger(MySectionFolderItem.class);

	public MySectionFolderItem(AnyboxLibraryProvider p, 
			AnyboxSectionFolder data) {
		super(p, data);
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
