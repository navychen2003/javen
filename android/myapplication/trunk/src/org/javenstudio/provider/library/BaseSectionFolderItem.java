package org.javenstudio.provider.library;

import android.content.DialogInterface;

import org.javenstudio.android.app.ActionHelper;
import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.SelectManager;
import org.javenstudio.android.app.FileOperation.Operation;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.list.LibraryProvider;
import org.javenstudio.provider.library.section.SectionFolderItem;

public abstract class BaseSectionFolderItem extends SectionFolderItem 
		implements FileOperation.OnShowListener {
	private static final Logger LOG = Logger.getLogger(BaseSectionFolderItem.class);
	
	public BaseSectionFolderItem(LibraryProvider p, ISectionFolder data) {
		super(p, data);
	}
	
	public abstract FileOperation getOperation();
	
	@Override
	public LibraryProvider getProvider() {
		return (LibraryProvider)super.getProvider();
	}
	
	@Override
	public boolean isSelected(IActivity activity) { 
		if (activity != null && !activity.isDestroyed()) {
			ActionHelper helper = activity.getActionHelper();
			if (helper != null && helper.isSelectMode()) {
				SelectManager manager = getProvider().getSectionListDataSets().getSelectManager();
				if (manager != null) 
					return manager.isSelectedItem(getSectionData());
			}
		}
		return false; 
	}
	
	@Override
	public boolean onItemSelect(IActivity activity, boolean selected) { 
		SelectManager manager = getProvider().getSectionListDataSets().getSelectManager();
		if (manager != null) { 
			manager.setSelectedItem(getSectionData(), selected);
			onUpdateViewOnVisible(false);
			return true;
		}
		return false; 
	}
	
	@Override
	public SectionTouchListener getTouchListener(IActivity activity, Object binder) { 
		return new SectionTouchListener(activity, this); 
	}
	
	@Override
	public boolean onItemLongClick(final IActivity activity) { 
		if (activity == null || activity.isDestroyed()) return false;
		if (LOG.isDebugEnabled()) LOG.debug("onItemLongClick: item=" + this);
		
		FileOperation ops = getOperation();
		if (ops != null) {
			FileOperation.showOperationDialog(activity.getActivity(), ops, 
				getSectionData().getName(), 
				AppResources.getInstance().getSectionDialogIcon(getSectionData()), 
				new FileOperation.OnSelectListener() {
					@Override
					public void onOperationSelected(Operation op) {
						BaseSectionFolderItem.this.onOperationSelected(activity, op);
					}
				}, this);
			return true;
		}
		
		return false; 
	}
	
	protected void onOperationSelected(IActivity activity, 
			FileOperation.Operation op) {
		if (activity == null || op == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("onOperationSelected: item=" + this + " op=" + op);
		
		if (op == FileOperation.Operation.SELECT) {
			getProvider().startSelectMode(activity, this);
		} else if (op == FileOperation.Operation.DETAILS) {
			onItemInfoClick(activity);
		}
	}
	
	public void onDialogShow(AlertDialogBuilder builder, 
			DialogInterface dialog) {}
	
}
