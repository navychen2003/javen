package org.javenstudio.provider.publish.comment;

import org.javenstudio.android.data.comment.IMediaComment;
import org.javenstudio.android.data.comment.IMediaComments;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.data.ChangeNotifier;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.MapDataSetCursor;

public class CommentCursor extends MapDataSetCursor<CommentItem> 
		implements ChangeNotifier {

	public interface Model { 
		public IMediaComments getComments();
		public CommentDataSets getCommentDataSets();
		public void onDataSetChanged();
	}
	
	private final Model mModel;
	
	public CommentCursor(Model model) { 
		mModel = model;
		mModel.getComments().addNotifier(this);
	}

	@Override
	public void onChange(boolean selfChange) { 
		requery();
		postNotifyDataSetChanged();
	}
	
	@Override
	public int getCount() { 
		return mModel.getComments().getCommentCount(); 
	}
	
	@Override 
	public void close() { 
		super.close(); 
		mModel.getComments().removeNotifier(this); 
	}
	
	private void postNotifyDataSetChanged() {
		ResourceHelper.getHandler().post(new Runnable() { 
				public void run() { 
					mModel.onDataSetChanged(); 
				}
			});
	}
	
	@Override 
	protected AbstractDataSet<CommentItem> createDataSet(int position) { 
		IMediaComment record = mModel.getComments().getCommentAt(position);
		if (record != null) {
			return mModel.getCommentDataSets()
					.createDataSet(new CommentItem(record)); 
		}
		
		return null; 
	}
	
}
