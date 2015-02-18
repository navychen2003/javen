package org.javenstudio.provider.publish.information;

import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.parser.util.XmlParser;
import org.javenstudio.common.parser.xml.RssXml;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;

final class RssProvider extends TextProvider {
	private static final Logger LOG = Logger.getLogger(RssProvider.class);

	private final String mLocation;
	private final TextDataSets mDataSets;
	
	public RssProvider(String name, int iconRes, 
			String location, boolean gridBinder) { 
		super(name, iconRes, gridBinder);
		mLocation = location;
		mDataSets = new TextDataSets(new TextCursorFactory());
	}
	
	public final String getLocation() { return mLocation; }
	public final TextDataSets getDataSets() { return mDataSets; }
	
	@Override
	public synchronized void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		startLoad(callback, getLocation(), type == ReloadType.FORCE);
	}
	
	@Override
	protected void onLoaded(final ProviderCallback callback, 
			String location, String content) { 
		if (location == null || content == null) 
			return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getDataSets().clear();
				}
			});
		
		try { 
			if (content != null && content.length() > 0) 
				parseContent(callback, content);
			
		} catch (Throwable e) { 
			callback.getController().getModel().onExceptionCatched(e);
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
	}
	
	private void parseContent(final ProviderCallback callback, 
			String content) throws Throwable { 
		RssXml.Handler handler = new RssXml.Handler(); 
		XmlParser parser = new XmlParser(handler); 
		parser.parse(content); 
		
		RssXml rss = handler.getEntity(); 
		RssXml.Item[] items = rss.getChannel().getItems(); 
		
		for (int i=0; items != null && i < items.length; i++) { 
			RssXml.Item item = items[i]; 
			if (item == null) continue; 
			
			String title = item.getFirstChildValue("title"); 
			String link = item.getFirstChildValue("link"); 
			String desc = item.getFirstChildValue("description"); 
			String pubdate = item.getFirstChildValue("pubdate"); 
			String author = item.getFirstChildValue("author"); 
			String contentEncoded = item.getFirstChildValue("content:encoded"); 
			
			String date = Utilities.formatDate(pubdate); 
			if (author != null && author.length() > 0) 
				date = date + " " + author; 
			
			final RssItem entry = new RssItem(this);
			entry.setTitle(title); 
			entry.setDate(date); 
			entry.setSummary(desc); 
			entry.setLink(link); 
			
			if (contentEncoded != null && contentEncoded.length() > 0)
				entry.setContent(contentEncoded);
			
			ResourceHelper.getHandler().post(new Runnable() {
					@Override
					public void run() { 
						getDataSets().addTextItem(entry, false); 
						callback.getController().getModel().callbackOnDataSetUpdate(entry); 
					}
				});
		}
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getDataSets().notifyContentChanged(true);
					getDataSets().notifyDataSetChanged();
				}
			});
	}
	
}
