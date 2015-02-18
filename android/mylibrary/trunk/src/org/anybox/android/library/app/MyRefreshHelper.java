package org.anybox.android.library.app;

import org.anybox.android.library.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IRefreshView;

public abstract class MyRefreshHelper {

	public abstract int getLoadingDrawableRes();
	public abstract int getRefreshTitleColorRes();
	public abstract int getRefreshSubTitleColorRes();
	
	public abstract void initRefreshView(IRefreshView refreshView);
	
	public static MyRefreshHelper getDefault() { return GOAT; }
	
	public static final MyRefreshHelper DEFAULT = new DefaultHelper();
	public static final MyRefreshHelper GOAT = new GoatHelper();
	
	private static class DefaultHelper extends MyRefreshHelper {
		
		@Override
		public int getLoadingDrawableRes() {
			return R.drawable.ptr_rotate_spinner;
		}

		@Override
		public int getRefreshTitleColorRes() {
			return 0;
		}

		@Override
		public int getRefreshSubTitleColorRes() {
			return 0;
		}
		
		@Override
		public void initRefreshView(IRefreshView refreshView) {
		}
	}
	
	private static class GoatHelper extends MyRefreshHelper {

		@Override
		public int getLoadingDrawableRes() {
			return R.drawable.ani_goat;
		}

		@Override
		public int getRefreshTitleColorRes() {
			return R.color.ptr_title_color_white;
		}

		@Override
		public int getRefreshSubTitleColorRes() {
			return R.color.ptr_subtitle_color_white;
		}
		
		@Override
		public void initRefreshView(IRefreshView refreshView) {
			if (refreshView == null) return;
			
			refreshView.setTextColor(ResourceHelper.getResources().getColor(getRefreshTitleColorRes()));
			refreshView.setSubTextColor(ResourceHelper.getResources().getColor(getRefreshSubTitleColorRes()));
			refreshView.setLoadingDrawable(ResourceHelper.getResources().getDrawable(getLoadingDrawableRes())); 
		}
		
	}
	
}
