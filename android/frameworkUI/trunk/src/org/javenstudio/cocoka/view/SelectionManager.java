package org.javenstudio.cocoka.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.javenstudio.cocoka.data.IMediaItem;
import org.javenstudio.cocoka.data.IMediaObject;
import org.javenstudio.cocoka.data.IMediaSet;
import org.javenstudio.cocoka.opengl.GLActivity;

public class SelectionManager {

    public static final int ENTER_SELECTION_MODE = 1;
    public static final int LEAVE_SELECTION_MODE = 2;
    public static final int SELECT_ALL_MODE = 3;

    private Set<IMediaObject> mClickedSet;
    private IMediaSet mSourceMediaSet;
    private SelectionListener mListener;
    private boolean mInverseSelection;
    private boolean mIsAlbumSet;
    private boolean mInSelectionMode;
    private boolean mAutoLeave = true;
    private int mTotal;

    public interface SelectionListener {
        public void onSelectionModeChange(int mode);
        public void onSelectionChange(IMediaItem item, boolean selected);
    }

    public SelectionManager(GLActivity activity, boolean isAlbumSet) {
        mClickedSet = new HashSet<IMediaObject>();
        mIsAlbumSet = isAlbumSet;
        mTotal = -1;
    }

    // Whether we will leave selection mode automatically once the number of
    // selected items is down to zero.
    public void setAutoLeaveSelectionMode(boolean enable) {
        mAutoLeave = enable;
    }

    public void setSelectionListener(SelectionListener listener) {
        mListener = listener;
    }

    public void selectAll() {
        mInverseSelection = true;
        mClickedSet.clear();
        enterSelectionMode();
        if (mListener != null) mListener.onSelectionModeChange(SELECT_ALL_MODE);
    }

    public void deSelectAll() {
        leaveSelectionMode();
        mInverseSelection = false;
        mClickedSet.clear();
    }

    public boolean inSelectAllMode() {
        return mInverseSelection;
    }

    public boolean inSelectionMode() {
        return mInSelectionMode;
    }

    public void enterSelectionMode() {
        if (mInSelectionMode) return;

        mInSelectionMode = true;
        if (mListener != null) 
        	mListener.onSelectionModeChange(ENTER_SELECTION_MODE);
    }

    public void leaveSelectionMode() {
        if (!mInSelectionMode) return;

        mInSelectionMode = false;
        mInverseSelection = false;
        mClickedSet.clear();
        if (mListener != null) 
        	mListener.onSelectionModeChange(LEAVE_SELECTION_MODE);
    }

    public boolean isItemSelected(IMediaItem item) {
        return mInverseSelection ^ mClickedSet.contains(item);
    }

    private int getTotalCount() {
        if (mSourceMediaSet == null) return -1;

        if (mTotal < 0) {
            mTotal = mIsAlbumSet
                    ? mSourceMediaSet.getSubSetCount()
                    : mSourceMediaSet.getItemCount();
        }
        
        return mTotal;
    }

    public int getSelectedCount() {
        int count = mClickedSet.size();
        if (mInverseSelection) {
            count = getTotalCount() - count;
        }
        return count;
    }

    public void toggle(IMediaItem item) {
        if (mClickedSet.contains(item)) {
            mClickedSet.remove(item);
        } else {
            enterSelectionMode();
            mClickedSet.add(item);
        }

        // Convert to inverse selection mode if everything is selected.
        int count = getSelectedCount();
        if (count == getTotalCount()) {
            selectAll();
        }

        if (mListener != null) mListener.onSelectionChange(item, isItemSelected(item));
        if (count == 0 && mAutoLeave) {
            leaveSelectionMode();
        }
    }

    private static void expandMediaSet(List<IMediaObject> items, IMediaSet set) {
        int subCount = set.getSubSetCount();
        for (int i = 0; i < subCount; i++) {
            expandMediaSet(items, set.getSubSetAt(i));
        }
        int total = set.getItemCount();
        int batch = 50;
        int index = 0;

        while (index < total) {
            int count = index + batch < total
                    ? batch
                    : total - index;
            List<IMediaItem> list = set.getItemList(index, count);
            for (IMediaItem item : list) {
                items.add(item);
            }
            index += batch;
        }
    }

    public List<IMediaObject> getSelected(boolean expandSet) {
        ArrayList<IMediaObject> selected = new ArrayList<IMediaObject>();
        if (mIsAlbumSet) {
            if (mInverseSelection) {
                int total = getTotalCount();
                for (int i = 0; i < total; i++) {
                    IMediaSet set = mSourceMediaSet.getSubSetAt(i);
                    if (!mClickedSet.contains(set)) {
                        if (expandSet) {
                            expandMediaSet(selected, set);
                        } else {
                            selected.add(set);
                        }
                    }
                }
            } else {
                for (IMediaObject item : mClickedSet) {
                    if (expandSet && item instanceof IMediaSet) {
                        expandMediaSet(selected, (IMediaSet)item);
                    } else {
                        selected.add(item);
                    }
                }
            }
        } else {
            if (mInverseSelection) {
                int total = getTotalCount();
                int index = 0;
                while (index < total) {
                    int count = Math.min(total - index, 500); //MediaSet.MEDIAITEM_BATCH_FETCH_COUNT);
                    List<IMediaItem> list = mSourceMediaSet.getItemList(index, count);
                    for (IMediaItem item : list) {
                        if (!mClickedSet.contains(item)) selected.add(item);
                    }
                    index += count;
                }
            } else {
                for (IMediaObject item : mClickedSet) {
                    selected.add(item);
                }
            }
        }
        return selected;
    }

    public void setSourceMediaSet(IMediaSet set) {
        mSourceMediaSet = set;
        mTotal = -1;
    }
    
}
