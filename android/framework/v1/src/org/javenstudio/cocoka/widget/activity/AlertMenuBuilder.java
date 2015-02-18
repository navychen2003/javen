package org.javenstudio.cocoka.widget.activity;

import android.view.View;

import org.javenstudio.cocoka.android.ResourceContext;
import org.javenstudio.cocoka.widget.PopupMenu;
import org.javenstudio.cocoka.widget.PopupMenuListener;

public class AlertMenuBuilder implements PopupMenuListener {

	public static final int POPUPMENU_ALERTDIALOG = 1; 
	
	public static class OnConfirmListener { 
		public CharSequence getTitle() { return null; }
		public CharSequence getCancelText() { return null; }
		public CharSequence getDiscardText() { return null; }
		public CharSequence getOkText() { return null; }
		public void onCancel() {}
		public void onDiscard() {}
		public void onOk() {}
	}
	
	public class BasePopupMenuListener implements PopupMenuListener { 
		@Override 
		public void showPopupMenuAt(int id, PopupMenu menu, final View view) { 
			AlertMenuBuilder.this.showPopupMenuAt(id, menu, view); 
		}
		
		@Override 
		public PopupMenu createPopupMenu(int id, final View view) { 
			return AlertMenuBuilder.this.createPopupMenu(id, view); 
		}
		
		@Override 
		public void onPopupMenuCreated(int id, PopupMenu menu, final View view) { 
			AlertMenuBuilder.this.onPopupMenuCreated(id, menu, view); 
		}
		
		@Override 
		public void onPopupMenuShow(int id, PopupMenu menu, final View view) { 
			AlertMenuBuilder.this.onPopupMenuShow(id, menu, view); 
		}
		
		@Override 
		public void onPopupMenuDismiss(int id, PopupMenu menu, final View view) { 
			AlertMenuBuilder.this.onPopupMenuDismiss(id, menu, view); 
		}
	}
	
	private final BaseActivity mActivity;
	
	public AlertMenuBuilder(BaseActivity activity) { 
		mActivity = activity;
	}
	
	public final BaseActivity getActivity() { 
		return mActivity;
	}
	
	public ResourceContext getResourceContext() { 
		return getActivity().getResourceContext();
	}
	
	@Override
	public void showPopupMenuAt(int id, PopupMenu menu, final View view) { 
		menu.showAtBottom(getActivity().getContentView()); 
	}
	
	@Override
	public PopupMenu createPopupMenu(int id, final View view) { 
		return null;
	}
	
	@Override
	public void onPopupMenuCreated(int id, PopupMenu menu, final View view) { 
		// do nothing
	}
	
	@Override
	public void onPopupMenuShow(int id, PopupMenu menu, final View view) { 
		// do nothing
	}
	
	@Override
	public void onPopupMenuDismiss(int id, PopupMenu menu, final View view) { 
		// do nothing
	}
	
	public void onPopupMenuRemoved(int id, PopupMenu menu) { 
		// do nothing
	}
	
	public void showConfirmMenu(final OnConfirmListener listener) { 
		getActivity().showPopupMenu(POPUPMENU_ALERTDIALOG, null, new BasePopupMenuListener() { 
				@Override 
				public void onPopupMenuShow(int id, final PopupMenu menu, final View view) { 
					bindConfirmMenu(menu, listener); 
				}
			});
	}
	
	protected void bindConfirmMenu(final PopupMenu menu, final OnConfirmListener listener) { 
		// do nothing
	}
	
}
