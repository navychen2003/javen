package org.javenstudio.android.information;

import org.javenstudio.cocoka.widget.model.NavigationInfo;
import org.javenstudio.common.parser.html.ContentHandlerFactory;
import org.javenstudio.common.parser.html.ContentTable;
import org.javenstudio.common.parser.html.ContentTableHandler;
import org.javenstudio.common.parser.html.HTMLHandler;
import org.javenstudio.common.parser.html.HTMLParser;
import org.javenstudio.common.util.Logger;

public abstract class BaseHtmlNavItem extends BaseInformationNavItem 
		implements ContentTableHandler {
	static final Logger LOG = Logger.getLogger(BaseHtmlNavItem.class);

	public BaseHtmlNavItem(NavBinder res, NavigationInfo info) { 
		this(res, info, false); 
	}
	
	public BaseHtmlNavItem(NavBinder res, NavigationInfo info, boolean selected) { 
		super(res, info, selected); 
	}
	
	protected HTMLHandler createHTMLHandler(ContentHandlerFactory factory) { 
		return new HTMLHandler(factory); 
	}
	
	@Override
	protected void parseInformation(String location, String content, boolean first) { 
		if (location == null || content == null || content.length() == 0) 
			return;
		
		try { 
			HTMLHandler a = createHTMLHandler(getContentFactory()); 
			
			onInitAnalyzer(a); 
			
			HTMLParser parser = HTMLParser.newParser(a); 
			parser.parse(content); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("parseInformation: parse done.");
			
			onInformationParsed(location, first);
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("parse information error" + e.toString(), e); 
		}
	}
	
	protected abstract void onInitAnalyzer(HTMLHandler a); 
	protected abstract ContentHandlerFactory getContentFactory(); 
	protected abstract Information onNewInformation(ContentTable content); 
	
	protected void onInformationParsed(String location, boolean first) {}
	
	@Override 
	public void handleTableCreate(ContentTable table) { 
		// do nothing
	}
	
	@Override 
	public void handleTableFinish(ContentTable table) { 
		addInformation(table); 
	}
	
	protected void addInformation(ContentTable content) { 
		NavModel model = getModel(); 
		if (model != null && content != null) { 
			Information info = onNewInformation(content); 
			if (info != null) 
				postAddInformation(model, info); 
		}
	}
	
}
