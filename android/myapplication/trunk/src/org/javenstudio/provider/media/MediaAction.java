package org.javenstudio.provider.media;

import java.util.List;

import org.javenstudio.android.app.ActionExecutor;
import org.javenstudio.android.app.R;
import org.javenstudio.android.app.SelectAction;
import org.javenstudio.android.app.SelectManager;
import org.javenstudio.android.app.SelectMode;
import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.DataException;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.Photo;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public class MediaAction {
	private static final Logger LOG = Logger.getLogger(MediaAction.class);

	public static class AlbumSelectAction implements SelectAction {

		@Override
		public int getSelectItemCount(SelectManager.SelectData[] items) {
			if (items == null) return 0;
			
			return getAlbumItemCount(items);
		}

		@Override
		public void onSelectChanged(SelectMode mode, SelectManager manager) {
			if (mode == null || manager == null) return;
			
			setSelectedAlbumCount(mode, manager.getSelectedItems());
		}

		@Override
		public void executeAction(DataAction action, SelectManager.SelectData item,
				ActionExecutor.ProgressListener listener) throws DataException {
			if (action == null || listener == null || item == null) 
				return;
			
			if (item instanceof Photo) { 
				//MediaAction.executePhotoAction(action, (Photo)item, listener);
				
			} else if (item instanceof PhotoSet) { 
				executePhotoSetAction(action, (PhotoSet)item, listener);
			}
		}

		@Override
		public CharSequence getActionConfirmTitle(DataAction action,
				SelectManager manager) {
			return null;
		}
		
		@Override
		public CharSequence getActionConfirmMessage(DataAction action,
				SelectManager manager) {
			if (action == null || manager == null) 
				return null;
			
			return ResourceHelper.getResources().getQuantityText(
					org.javenstudio.cocoka.app.R.plurals.delete_album_message, 
					manager.getSelectedCount());
		}
	}
	
	public static class PhotoSelectAction implements SelectAction {

		@Override
		public int getSelectItemCount(SelectManager.SelectData[] items) {
			if (items == null) return 0;
			
			return getPhotoItemCount(items);
		}

		@Override
		public void onSelectChanged(SelectMode mode, SelectManager manager) {
			if (mode == null || manager == null) return;
			
			setSelectedPhotoCount(mode, manager.getSelectedItems());
		}

		@Override
		public void executeAction(DataAction action, SelectManager.SelectData item,
				ActionExecutor.ProgressListener listener) throws DataException {
			if (action == null || listener == null || item == null) 
				return;
			
			if (item instanceof Photo) { 
				executePhotoAction(action, (Photo)item, listener);
				
			} else if (item instanceof PhotoSet) { 
				//executePhotoSetAction(action, (PhotoSet)item, listener);
			}
		}

		@Override
		public CharSequence getActionConfirmTitle(DataAction action,
				SelectManager manager) {
			return null;
		}
		
		@Override
		public CharSequence getActionConfirmMessage(DataAction action,
				SelectManager manager) {
			if (action == null || manager == null) 
				return null;
			
			return ResourceHelper.getResources().getQuantityText(
					org.javenstudio.cocoka.app.R.plurals.delete_photo_message, 
					manager.getSelectedCount());
		}
	}
	
	public static class LocalAlbumSelectAction implements SelectAction {

		@Override
		public int getSelectItemCount(SelectManager.SelectData[] items) {
			if (items == null) return 0;
			
			return getPhotoItemCount(items);
		}

		@Override
		public void onSelectChanged(SelectMode mode, SelectManager manager) {
			if (mode == null || manager == null) return;
			
			setSelectedPhotoCount(mode, manager.getSelectedItems());
		}

		@Override
		public void executeAction(DataAction action, SelectManager.SelectData item,
				ActionExecutor.ProgressListener listener) throws DataException {
			if (action == null || listener == null || item == null) 
				return;
			
			if (item instanceof Photo) { 
				executePhotoAction(action, (Photo)item, listener);
				
			} else if (item instanceof PhotoSet) { 
				executePhotoSetLocal(action, (PhotoSet)item, listener);
			}
		}

		@Override
		public CharSequence getActionConfirmTitle(DataAction action,
				SelectManager manager) {
			return null;
		}
		
		@Override
		public CharSequence getActionConfirmMessage(DataAction action,
				SelectManager manager) {
			if (action == null || manager == null) 
				return null;
			
			return ResourceHelper.getResources().getQuantityText(
					org.javenstudio.cocoka.app.R.plurals.delete_photo_message, 
					manager.getSelectedCount());
		}
	}
	
	private static void executePhotoSetAction(DataAction action, PhotoSet item, 
			ActionExecutor.ProgressListener listener) throws DataException { 
		if (action == null || listener == null || item == null) 
			return;
		
		if (listener.isCancelled()) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("executePhotoSetAction: action=" + action + " item=" + item);
		
		boolean success = false;
		switch (action) { 
		case DELETE: 
			success = item.delete();
			break;
		default:
			break;
		}
		
		listener.increaseProgress(1, success?1:0);
	}
	
	private static void executePhotoSetLocal(DataAction action, PhotoSet item, 
			ActionExecutor.ProgressListener listener) throws DataException { 
		if (action == null || listener == null || item == null) 
			return;
		
		if (listener.isCancelled()) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("executePhotoSetLocal: action=" + action + " item=" + item);
		
		PhotoSet photoSet = (PhotoSet)item;
		List<MediaItem> list = photoSet.getItemList(0, photoSet.getItemCount());
		
		if (list != null) { 
			for (MediaItem mediaItem : list) { 
				if (listener.isCancelled()) break;
				if (mediaItem != null && mediaItem instanceof Photo) 
					executePhotoAction(action, (Photo)mediaItem, listener);
			}
		}
	}
	
	private static void executePhotoAction(DataAction action, Photo item, 
			ActionExecutor.ProgressListener listener) throws DataException { 
		if (action == null || listener == null || item == null) 
			return;
		
		if (listener.isCancelled()) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("executePhotoAction: action=" + action + " item=" + item);
		
		boolean success = false;
		switch (action) { 
		case DELETE: 
			success = item.delete();
			break;
		default:
			break;
		}
		
		listener.increaseProgress(1, success?1:0);
	}
	
	private static int getPhotoItemCount(SelectManager.SelectData[] items) { 
		int itemCount = 0;
		
		for (int i=0; items != null && i < items.length; i++) { 
			SelectManager.SelectData item = items[i];
			if (item == null) continue;
			
			if (item instanceof Photo) { 
				itemCount ++;
				
			} else if (item instanceof PhotoSet) { 
				PhotoSet photoSet = (PhotoSet)item;
				itemCount += photoSet.getItemCount();
			}
		}
		
		return itemCount;
	}
	
	private static int getAlbumItemCount(SelectManager.SelectData[] items) { 
		int itemCount = 0;
		
		for (int i=0; items != null && i < items.length; i++) { 
			SelectManager.SelectData item = items[i];
			if (item == null) continue;
			
			if (item instanceof Photo) { 
				//itemCount ++;
				
			} else if (item instanceof PhotoSet) { 
				//PhotoSet photoSet = (PhotoSet)item;
				itemCount += 1; //photoSet.getItemCount();
			}
		}
		
		return itemCount;
	}
	
	private static void setSelectedPhotoCount(SelectMode mode, 
			SelectManager.SelectData[] items) { 
		setSelectedPhotoCount(mode, getPhotoItemCount(items));
	}
	
	private static void setSelectedPhotoCount(SelectMode mode, int count) { 
		if (mode == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("setSelectedPhotoCount: count=" + count);
		
		String title = String.format(ResourceHelper.getResources().getString(
				R.string.actionmode_selected_photo_title), "" + count);
		
		mode.setTitle(title);
	}
	
	private static void setSelectedAlbumCount(SelectMode mode, 
			SelectManager.SelectData[] items) { 
		setSelectedAlbumCount(mode, getAlbumItemCount(items));
	}
	
	private static void setSelectedAlbumCount(SelectMode mode, int count) { 
		if (mode == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("setSelectedAlbumCount: count=" + count);
		
		String title = String.format(ResourceHelper.getResources().getString(
				R.string.actionmode_selected_album_title), "" + count);
		
		mode.setTitle(title);
	}
	
}
