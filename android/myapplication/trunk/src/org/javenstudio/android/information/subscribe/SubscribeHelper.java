package org.javenstudio.android.information.subscribe;

import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.cocoka.widget.model.NavigationInfo;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.common.parser.util.XmlParser;
import org.javenstudio.common.parser.xml.FeedXml;
import org.javenstudio.common.parser.xml.OpmlXml;
import org.javenstudio.common.parser.xml.RdfXml;
import org.javenstudio.common.parser.xml.RssXml;
import org.javenstudio.common.util.Logger;
import org.javenstudio.android.information.Information;
import org.javenstudio.android.information.InformationNavItem;

public class SubscribeHelper {
	static final Logger LOG = Logger.getLogger(SubscribeHelper.class);

	public static final int ACTION_REFRESH_POPUPMENU = 201; 
	public static final int ACTION_FETCH_FAILED = 202; 
	
	public static void onRdfXmlFetched(final InformationNavItem.NavModel model, 
			final SubscribeNavItem subscribe, String content) { 
		if (model == null || subscribe == null || content == null || content.length() == 0) 
			return; 
		
		try { 
			RdfXml.Handler handler = new RdfXml.Handler(); 
			XmlParser parser = new XmlParser(handler); 
			parser.parse(content); 
			
			RdfXml rdf = handler.getEntity(); 
			RdfXml.Item[] items = rdf.getItems(); 
			
			for (int i=0; items != null && i < items.length; i++) { 
				RdfXml.Item item = items[i]; 
				if (item == null) continue; 
				
				String title = item.getFirstChildValue("title"); 
				String link = item.getFirstChildValue("link"); 
				String desc = item.getFirstChildValue("description"); 
				String date = item.getFirstChildValue("dc:date"); 
				String contentEncoded = item.getFirstChildValue("content:encoded"); 
				
				date = Utilities.formatDate(date); 
				
				SubscribeEntry entry = subscribe.newSubscribeEntry(); 
				entry.setTitle(title); 
				entry.setDate(date); 
				entry.setSummary(desc); 
				entry.setLink(link); 
				
				if (contentEncoded != null && contentEncoded.length() > 0)
					entry.setContent(contentEncoded);
				
				//Object summaryMode = subscribe.getInfo().getAttribute(Information.ATTR_SUMMARYMODE);
				//if (summaryMode != null)
				//	entry.setField(Information.ATTR_SUMMARYMODE, summaryMode);
				
				model.postAddInformation(subscribe, entry, false); 
			}
			
			model.postNotifyChanged(subscribe);
		} catch (Throwable e) { 
			//model.onExceptionCatched(e);
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
	}
	
	public static void onFeedXmlFetched(final InformationNavItem.NavModel model, 
			final SubscribeNavItem subscribe, String content) { 
		if (model == null || subscribe == null || content == null || content.length() == 0) 
			return; 
		
		try { 
			FeedXml.Handler handler = new FeedXml.Handler(); 
			XmlParser parser = new XmlParser(handler); 
			parser.parse(content); 
			
			FeedXml feed = handler.getEntity(); 
			FeedXml.Entry[] items = feed.getItems(); 
			
			for (int i=0; items != null && i < items.length; i++) { 
				FeedXml.Entry item = items[i]; 
				if (item == null) continue; 
				
				Node node = item.getFirstChild("link"); 
				
				String title = item.getFirstChildValue("title"); 
				String link = node != null ? node.getAttribute("href") : null; 
				String desc = item.getFirstChildValue("summary"); 
				String updated = item.getFirstChildValue("updated"); 
				String published = item.getFirstChildValue("published"); 
				String contentEncoded = item.getFirstChildValue("content"); 
				
				String date = updated != null && updated.length() > 0 ? updated : published; 
				date = Utilities.formatDate(date); 
				
				SubscribeEntry entry = subscribe.newSubscribeEntry(); 
				entry.setTitle(title); 
				entry.setDate(date); 
				entry.setSummary(desc); 
				entry.setLink(link); 
				
				if (contentEncoded != null && contentEncoded.length() > 0)
					entry.setContent(contentEncoded);
				
				//Object summaryMode = subscribe.getInfo().getAttribute(Information.ATTR_SUMMARYMODE);
				//if (summaryMode != null)
				//	entry.setField(Information.ATTR_SUMMARYMODE, summaryMode);
				
				model.postAddInformation(subscribe, entry, false); 
			}
			
			model.postNotifyChanged(subscribe);
		} catch (Throwable e) { 
			//model.onExceptionCatched(e);
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
	}
	
	public static void onRssXmlFetched(final InformationNavItem.NavModel model, 
			final SubscribeNavItem subscribe, String content) { 
		if (model == null || subscribe == null || content == null || content.length() == 0) 
			return; 
		
		try { 
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
				
				SubscribeEntry entry = subscribe.newSubscribeEntry(); 
				entry.setTitle(title); 
				entry.setDate(date); 
				entry.setSummary(desc); 
				entry.setLink(link); 
				
				if (contentEncoded != null && contentEncoded.length() > 0)
					entry.setContent(contentEncoded);
				
				//Object summaryMode = subscribe.getInfo().getAttribute(Information.ATTR_SUMMARYMODE);
				//if (summaryMode != null)
				//	entry.setField(Information.ATTR_SUMMARYMODE, summaryMode);
				
				model.postAddInformation(subscribe, entry, false); 
			}
			
			model.postNotifyChanged(subscribe);
		} catch (Throwable e) { 
			//model.onExceptionCatched(e);
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
	}
	
	public static void scheduleLoadSubscribeItems(final SubscribeNavGroup group, 
			final SubscribeNavItem.Factory factory, String location) { 
		if (group == null || factory == null || location == null || location.length() == 0) 
			return; 
		
		group.setLoading(true); 
		
		FetchHelper.scheduleFetchHtml(location, new HtmlCallback() {
				@Override
				public void onHtmlFetched(String content) {
					group.setLoading(false); 
					onOpmlXmlFetched(group, factory, content); 
				}
				@Override
				public void onHttpException(HttpException e) { 
					group.setLoading(false); 
					group.callback(ACTION_FETCH_FAILED, e); 
				}
			}); 
	}
	
	private static void onOpmlXmlFetched(final SubscribeNavGroup group, 
			final SubscribeNavItem.Factory factory, String content) { 
		if (group == null || factory == null || content == null || content.length() == 0) 
			return; 
		
		try { 
			OpmlXml.Handler handler = new OpmlXml.Handler(); 
			XmlParser parser = new XmlParser(handler); 
			parser.parse(content); 
			
			OpmlXml data = handler.getEntity(); 
			if (data != null && data.getBody().getOutlineCount() > 0) { 
				int count = 0; 
				group.clearChildItems(); 
				
				for (int i=0; i < data.getBody().getOutlineCount(); i++) { 
					OpmlXml.Outline outline = data.getBody().getOutlineAt(i); 
					if (outline == null) continue; 
					
					String text = outline.getAttribute("text"); 
					String title = outline.getAttribute("title"); 
					String xmlUrl = outline.getAttribute("xmlurl"); 
					
					if (title == null || title.length() == 0) 
						title = text; 
					
					if (text == null || text.length() == 0 || xmlUrl == null || xmlUrl.length() == 0) 
						continue; 
					
					NavigationInfo info = new NavigationInfo(text);
					info.setTitle(title);
					info.setAttribute(Information.ATTR_LOCATION, xmlUrl);
					
					SubscribeNavItem item = factory.create(info, false); 
					group.addChild(item); 
					
					count ++; 
				}
				
				if (count > 0) 
					group.callback(ACTION_REFRESH_POPUPMENU, null); 
			}
			
		} catch (Throwable e) { 
			//model.onExceptionCached(e);
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
	}
	
}
