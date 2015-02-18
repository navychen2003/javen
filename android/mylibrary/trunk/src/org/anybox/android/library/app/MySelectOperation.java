package org.anybox.android.library.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.anybox.android.library.MyResources;
import org.anybox.android.library.R;
import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.FilterType;
import org.javenstudio.android.app.SortType;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.app.anybox.library.AnyboxSelectOperation;
import org.javenstudio.provider.library.select.ISelectCallback;
import org.javenstudio.provider.library.select.ISelectData;
import org.javenstudio.provider.library.select.SelectFolderItem;
import org.javenstudio.provider.library.select.SelectListItem;
import org.javenstudio.provider.library.select.SelectOperation;

public abstract class MySelectOperation extends AnyboxSelectOperation {
	private static final Logger LOG = Logger.getLogger(MySelectOperation.class);

	public MySelectOperation() {}
	
	public abstract DataApp getDataApp();
	
	public SortType getSortType() { return MyResources.sSortType; }
	public FilterType getFilterType() { return MyResources.sFilterType; }
	
	@Override
	protected void onDialogTitleChanged(AlertDialogBuilder builder, 
			AlertDialog dialog) {
		if (builder == null || dialog == null) return;
		
		ImageView iconView = builder.getHelper().findIconView(dialog);
		if (iconView != null) {
			ViewGroup.LayoutParams lp = iconView.getLayoutParams();
			if (lp != null) {
				lp.width = (int)ResourceHelper.getResources().getDimension(R.dimen.dialog_foldericon_width);
				lp.height = (int)ResourceHelper.getResources().getDimension(R.dimen.dialog_foldericon_width);
				
				if (LOG.isDebugEnabled())
					LOG.debug("onDialogTitleChanged: set icon view lp=" + lp);
				
				iconView.setLayoutParams(lp);
				iconView.requestLayout();
			}
		}
	}
	
	@Override
	public void onEmptyState(SelectFolderItem folder) {
		SelectEmptyItem item = new SelectEmptyItem(this, folder, 
				ResourceHelper.getResources().getString(R.string.select_empty_title));
		postAddItem(item);
	}
	
	private static class SelectEmptyItem extends SelectListItem 
			implements ISelectData {
		private final CharSequence mTitle;
		
		public SelectEmptyItem(SelectOperation op, 
				SelectFolderItem parent, CharSequence title) {
			super(op, parent);
			mTitle = title;
		}
		
		public ISelectData getData() { return this; }
		public CharSequence getTitle() { return mTitle; }
		
		@Override
		public String getName() {
			CharSequence title = mTitle;
			return title != null ? title.toString() : null;
		}
		
		@Override
		public int getViewRes() {
			return R.layout.select_empty;
		}

		@Override
		public void bindView(Activity activity, ISelectCallback cb, View view) {
			if (activity == null || cb == null || view == null) return;
			
			final TextView titleView = (TextView)view.findViewById(R.id.select_empty_title);
			if (titleView != null) {
				titleView.setText(getTitle());
				titleView.setVisibility(View.VISIBLE);
			}
		}

		@Override
		public void updateView(View view, boolean restartSlide) {
		}
	}
	
}
