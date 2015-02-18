package org.javenstudio.provider.app.flickr;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;
import org.javenstudio.provider.people.group.IGroupData;

final class YGroupEntry extends BaseEntry implements IGroupData {

	private final int mIconRes;
	
	public String groupId;
	public String name;
	
	public String eighteenplus;
	public String iconserver;
	public String iconfarm;
	
	public String members;
	public String pool_count;
	public String topic_count;
	
	public int countMembers;
	public int countPool;
	public int countTopic;
	
	public YGroupEntry(int iconRes) { 
		mIconRes = iconRes;
	}
	
	public String getGroupId() { return groupId; }
	public String getGroupName() { return name; }
	public String getTitle() { return getGroupName(); }
	
	public int getMemberCount() { return countMembers; }
	public int getTopicCount() { return countTopic; }
	public int getPhotoCount() { return countPool; }

	@Override
	public String getSubTitle() { 
		return String.format(ResourceHelper.getResources().getString(R.string.details_group_summary), 
				getMemberCount(), getTopicCount());
	}
	
	@Override
	public String getAvatarLocation() { 
		return FlickrHelper.getIconURL(groupId, iconfarm, iconserver); 
	}
	
	@Override
	public View.OnClickListener getGroupClickListener() { 
		return null; 
	}
	
	@Override
	public Drawable getProviderIcon() { 
		if (mIconRes != 0) 
			return ResourceHelper.getResources().getDrawable(mIconRes);
		return null;
	}
	
	
	static YGroupEntry parseEntry(Node node, int iconRes) { 
		YGroupEntry entry = new YGroupEntry(iconRes);
		
		entry.groupId = node.getAttribute("nsid");
		entry.name = node.getAttribute("name");
		
		entry.eighteenplus = node.getAttribute("eighteenplus");
		entry.iconserver = node.getAttribute("iconserver");
		entry.iconfarm = node.getAttribute("iconfarm");
		
		entry.members = node.getAttribute("members");
		entry.pool_count = node.getAttribute("pool_count");
		entry.topic_count = node.getAttribute("topic_count");
		
		entry.countMembers = parseInt(entry.members);
		entry.countPool = parseInt(entry.pool_count);
		entry.countTopic = parseInt(entry.topic_count);
		
		return entry;
	}
	
	static YGroupEntry readEntry(DataInput in, int iconRes) throws IOException { 
		if (in == null) return null;
		
		YGroupEntry entry = new YGroupEntry(iconRes);
		
		entry.groupId = in.readUTF();
		entry.name = in.readUTF();
		
		entry.eighteenplus = in.readUTF();
		entry.iconserver = in.readUTF();
		entry.iconfarm = in.readUTF();
		
		entry.members = in.readUTF();
		entry.pool_count = in.readUTF();
		entry.topic_count = in.readUTF();
		
		entry.countMembers = in.readInt();
		entry.countPool = in.readInt();
		entry.countTopic = in.readInt();
		
		return entry;
	}
	
	static void writeEntry(DataOutput out, YGroupEntry entry) throws IOException { 
		if (out == null || entry == null) return;
		
		out.writeUTF(getString(entry.groupId));
		out.writeUTF(getString(entry.name));
		
		out.writeUTF(getString(entry.eighteenplus));
		out.writeUTF(getString(entry.iconserver));
		out.writeUTF(getString(entry.iconfarm));
		
		out.writeUTF(getString(entry.members));
		out.writeUTF(getString(entry.pool_count));
		out.writeUTF(getString(entry.topic_count));
		
		out.writeInt(entry.countMembers);
		out.writeInt(entry.countPool);
		out.writeInt(entry.countTopic);
	}
	
}
