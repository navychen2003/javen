package org.javenstudio.android.reader;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import org.xml.sax.InputSource;

import org.javenstudio.android.SourceHelper;
import org.javenstudio.cocoka.storage.Storage;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.common.parser.util.XmlParser;
import org.javenstudio.common.parser.xml.OpmlXml;
import org.javenstudio.common.util.Logger;
import org.javenstudio.util.StringUtils;

public class SubscribeSources {
	private static final Logger LOG = Logger.getLogger(SubscribeSources.class);

	public static class GroupItem { 
		private final List<SubscribeItem> mItems = new ArrayList<SubscribeItem>();
		
		private final String mGroupName;
		private final String mGroupTitle;
		private final String mGroupSourceName;
		
		public GroupItem(String groupName, String groupTitle, String sourceName) { 
			mGroupName = groupName; 
			mGroupTitle = groupTitle;
			mGroupSourceName = sourceName != null ? sourceName : "default";
		}
		
		public SubscribeItem addItem(SubscribeItem item) { 
			if (item == null) return item;
			synchronized (mItems) { 
				for (SubscribeItem si : mItems) { 
					if (item == si) return item;
				}
				mItems.add(item);
				return item;
			}
		}
		
		public SubscribeItem[] getItems() { 
			synchronized (mItems) { 
				return mItems.toArray(new SubscribeItem[mItems.size()]);
			}
		}
	}
	
	public static class SubscribeItem { 
		private final String mName;
		private final String mTitle;
		private final String mSubTitle;
		private final String mDropdownTitle;
		private final String mLocation;
		private final String mType;
		private final String mSourceName;
		private final String mCharset;
		
		public SubscribeItem(String name, String title, String subtitle, 
				String dropdownTitle, String location, String type, 
				String sourceName, String charset) { 
			mName = name;
			mTitle = title;
			mSubTitle = subtitle;
			mDropdownTitle = dropdownTitle;
			mLocation = location;
			mType = type;
			mSourceName = sourceName;
			mCharset = charset;
		}
	}
	
	private static final List<GroupItem> mGroups = new ArrayList<GroupItem>();
	
	public static int getSubscribeCount() { 
		synchronized (mGroups) { 
			int count = 0;
			for (GroupItem group : mGroups) { 
				if (group == null) continue;
				count += group.mItems.size();
			}
			return count;
		}
	}
	
	public static GroupItem[] getSubscribeGroups() { 
		synchronized (mGroups) { 
			return mGroups.toArray(new GroupItem[mGroups.size()]);
		}
	}
	
	public static GroupItem addSubscribeGroup(GroupItem item) { 
		if (item == null) return item;
		synchronized (mGroups) { 
			for (GroupItem gi : mGroups) { 
				if (gi == item) return item;
				if (gi.mGroupTitle.equals(item.mGroupTitle))
					return gi;
			}
			mGroups.add(item);
			return item;
		}
	}
	
	public static GroupItem addSubscribeGroup(
			String groupName, String groupTitle) { 
		return addSubscribeGroup(groupName, groupTitle, null);
	}
	
	public static GroupItem addSubscribeGroup(
			String groupName, String groupTitle, String sourceName) { 
		return addSubscribeGroup(new GroupItem(groupName, groupTitle, sourceName));
	}
	
	public static SubscribeItem addSubscribeItem(GroupItem group, 
			String title, String location, String type) { 
		return addSubscribeItem(group, title, title, null, title, 
				location, type, null, null);
	}
	
	public static SubscribeItem addSubscribeItem(GroupItem group, 
			String name, String title, String subtitle, String dropdownTitle, 
			String location) { 
		return addSubscribeItem(group, name, title, subtitle, dropdownTitle, 
				location, null, null, null);
	}
	
	public static SubscribeItem addSubscribeItem(GroupItem group, 
			String name, String title, String subtitle, String dropdownTitle, 
			String location, String sourceName) { 
		return addSubscribeItem(group, name, title, subtitle, dropdownTitle, 
				location, null, sourceName, null);
	}
	
	public static SubscribeItem addSubscribeItem(GroupItem group, 
			String name, String title, String subtitle, String dropdownTitle, 
			String location, String type, String sourceName, String charset) { 
		return group.addItem(new SubscribeItem(name, title, subtitle, dropdownTitle, 
				location, type, sourceName, charset));
	}
	
	public static void initSubscribeItems(Context context, int iconRes) { 
		if (context == null) return;
		GroupItem[] groups = getSubscribeGroups();
		
		for (int i=0; groups != null && i < groups.length; i++) { 
			GroupItem group = groups[i];
			if (group == null) continue;
			initSubscribeGroup(context, group, group.getItems(), iconRes);
		}
	}
	
	private static void initSubscribeGroup(Context context, 
			GroupItem groupItem, SubscribeItem[] items, int iconRes) { 
		if (context == null || groupItem == null || items == null || items.length == 0) 
			return;
		
		Object group = ReaderMethods.newNavigationGroup(context, 
				groupItem.mGroupName, groupItem.mGroupTitle, 
				SourceHelper.getSourceIconRes(groupItem.mGroupSourceName, iconRes));
		
		for (int i=0; items != null && i < items.length; i++) { 
			SubscribeItem item = items[i];
			if (item == null) continue;
			
			String type = item.mType;
			if (type == null || type.length() == 0 || type.equalsIgnoreCase("rss")) {
				ReaderMethods.addSubscribeRssItem(context, group, 
						item.mName, item.mTitle, item.mSubTitle, item.mDropdownTitle, item.mLocation, 
						SourceHelper.getSourceIconRes(item.mSourceName, iconRes), 
						ReaderMethods.newAttr(ReaderMethods.ATTR_DEFAULTCHARSET, item.mCharset)
					);
				
			} else if (type != null && type.equalsIgnoreCase("atom")) { 
				ReaderMethods.addSubscribeFeedItem(context, group, 
						item.mName, item.mTitle, item.mSubTitle, item.mDropdownTitle, item.mLocation, 
						SourceHelper.getSourceIconRes(item.mSourceName, iconRes), 
						ReaderMethods.newAttr(ReaderMethods.ATTR_DEFAULTCHARSET, item.mCharset)
					);
			}
		}
		
		ReaderMethods.addNavigationItem(group);
	}
	
	public static void saveSubscribeItems(Storage storage, GroupItem[] groups) { 
		if (storage == null || groups == null) 
			return;
		
		try {
			String opml = buildGroupOpml(groups, "Sample subscriptions");
			StorageFile file = storage.createFile(MimeType.TYPE_TEXT, "sample", "xml");
			if (file != null) { 
				OutputStream out = file.createFile();
				OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
				writer.write(opml);
				writer.flush();
				out.close();
				
				if (LOG.isDebugEnabled())
					LOG.debug("saveSubscribeItems: saved sample subscribes to " + file.getFilePath());
			}
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("saveSubscribeItems: save sample.xml error: " + e.toString(), e);
		}
	}
	
	private static String buildGroupOpml(GroupItem[] groups, String title) { 
		StringBuilder sbuf = new StringBuilder();
		
		sbuf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
		sbuf.append("<opml version=\"1.0\">\r\n");
		sbuf.append("  <head>\r\n");
		sbuf.append("    <title>").append(StringUtils.HTMLEncode(title)).append("</title>\r\n");
		sbuf.append("  </head>\r\n");
		sbuf.append("  <body>\r\n");
		
		for (int i=0; groups != null && i < groups.length; i++) { 
			GroupItem group = groups[i];
			if (group == null) continue;
			
			String groupTitle = StringUtils.HTMLEncode(group.mGroupTitle);
			
			sbuf.append("    <outline title=\"").append(groupTitle)
				.append("\" text=\"").append(groupTitle).append("\">\r\n");
			
			SubscribeItem[] items = group.getItems();
			for (int j=0; items != null && j < items.length; j++) { 
				SubscribeItem item = items[j];
				if (item == null) continue;
				
				String itemTitle = item.mDropdownTitle;
				if (itemTitle == null || itemTitle.length() == 0) 
					itemTitle = item.mTitle;
				if (itemTitle == null || itemTitle.length() == 0) 
					itemTitle = item.mSubTitle;
				if (itemTitle == null || itemTitle.length() == 0) 
					itemTitle = item.mName;
				
				itemTitle = StringUtils.HTMLEncode(itemTitle);
				
				String htmlUrl = "";
				String xmlUrl = StringUtils.HTMLEncode(item.mLocation);
				String type = item.mType;
				
				if (type == null || type.length() == 0) 
					type = "rss";
				
				sbuf.append("      <outline title=\"").append(itemTitle)
					.append("\" text=\"").append(itemTitle)
					.append("\" htmlUrl=\"").append(htmlUrl)
					.append("\" xmlUrl=\"").append(xmlUrl)
					.append("\" type=\"").append(type)
					.append("\" />\r\n");
			}
			
			sbuf.append("    </outline>\r\n");
		}
		
		sbuf.append("  </body>\r\n");
		sbuf.append("</opml>\r\n");
		
		return sbuf.toString();
	}
	
	public static int loadSubscribeItems(Storage storage, 
			String groupName, String defaultTitle) { 
		if (storage == null) return 0;
		
		try {
			IFile[] files = storage.listFiles(storage.getDirectory());
			for (int i=0; files != null && i < files.length; i++) { 
				IFile file = files[i];
				if (file == null || !file.exists() || !file.isFile()) 
					continue;
				
				loadSubscribeFile(storage, file, groupName, defaultTitle);
			}
			
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("loadSubscribeItems: load error: " + e.toString(), e);
		}
		
		return getSubscribeCount();
	}
	
	private static void loadSubscribeFile(Storage storage, IFile file, 
			String groupName, String defaultTitle) throws Exception { 
		if (storage == null || file == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadSubscribeFile: " + file.getAbsolutePath());
		
		StorageFile sfile = storage.getFile(MimeType.TYPE_TEXT, file);
		InputStream in = sfile.openFile();
		
		OpmlXml.Handler handler = new OpmlXml.Handler(); 
		XmlParser parser = new XmlParser(handler); 
		parser.parse(new InputSource(in)); 
		Utils.closeSilently(in);
		
		OpmlXml opml = handler.getEntity(); 
		OpmlXml.Body body = null;
		if (opml != null) 
			body = opml.getBody();
		
		if (body == null) return;
		
		for (int i=0; i < body.getOutlineCount(); i++) { 
			OpmlXml.Outline outline = body.getOutlineAt(i);
			if (outline == null) continue;
			
			String title = outline.getAttribute("title");
			String xmlUrl = outline.getAttribute("xmlUrl");
			String type = outline.getAttribute("type");
			
			if (xmlUrl != null && xmlUrl.length() > 0) { 
				if (title != null && title.length() > 0) { 
					GroupItem defGroup = addSubscribeGroup(groupName, defaultTitle);
					addSubscribeItem(defGroup, title, xmlUrl, type);
				}
			}
			
			if (outline.getOutlineCount() <= 0) 
				continue;
			
			GroupItem group = addSubscribeGroup(groupName, title);
			
			for (int j=0; j < outline.getOutlineCount(); j++) { 
				OpmlXml.Outline child = outline.getOutlineAt(j);
				if (child == null) continue;
				
				String childTitle = child.getAttribute("title");
				String childXmlUrl = child.getAttribute("xmlUrl");
				String childType = child.getAttribute("type");
				
				if (childXmlUrl != null && childXmlUrl.length() > 0) { 
					if (childTitle != null && childTitle.length() > 0) 
						addSubscribeItem(group, childTitle, childXmlUrl, childType);
				}
			}
		}
	}
	
}
