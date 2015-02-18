package org.javenstudio.android.information;

import java.util.Map;

import org.javenstudio.common.parser.html.HTMLHandler;
import org.javenstudio.common.parser.html.ContentHandlerFactory;
import org.javenstudio.common.parser.html.HTMLParser;
import org.javenstudio.common.util.Logger;

public abstract class BaseInformationSource extends InformationSource {
	static final Logger LOG = Logger.getLogger(BaseInformationSource.class);

	protected InformationModel mModel = null; 
	
	public BaseInformationSource(SourceBinder binder, String location) { 
		super(binder, location); 
	}
	
	public BaseInformationSource(SourceBinder binder, String location, 
			Map<String, Object> attrs) { 
		super(binder, location, attrs); 
	}
	
	protected abstract void onInitAnalyzer(HTMLHandler a); 
	protected abstract ContentHandlerFactory getContentFactory(); 
	
	protected final InformationModel getModel() { 
		return mModel; 
	}
	
	protected HTMLHandler createHTMLHandler(ContentHandlerFactory factory) { 
		return new HTMLHandler(factory); 
	}
	
	protected void onInformationParsed(String location, boolean first) {}
	
	@Override 
	public final void onFetched(InformationModel model, 
			String location, String content, boolean first) { 
		if (model == null || location == null || content == null) 
			return; 
		
		mModel = model; 
		onAddInformationBegin(model, location, first); 
		
		try { 
			if (content != null && content.length() > 0) { 
				HTMLHandler handler = createHTMLHandler(getContentFactory()); 
				
				onInitAnalyzer(handler); 
				
				HTMLParser parser = HTMLParser.newParser(handler); 
				parser.parse(content); 
				
				if (LOG.isDebugEnabled())
					LOG.debug("parseInformation: parse done.");
				
				onInformationParsed(location, first);
			}
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("parse information error" + e.toString(), e); 
		}
		
		onAddInformationEnd(model, location, first); 
	}
	
	@Override 
	public void onSubContentFetched(InformationModel model, 
			Information data, String location, String content) { 
		if (model == null || data == null || location == null || content == null) 
			return; 
		
		try { 
			if (content != null && content.length() > 0) { 
				//HTMLHandler handler = new HTMLHandler(ContentFactory.EMPTY);
				//HTMLParser parser = HTMLParser.newParser(handler); 
				//parser.parse(content); 
				//content = handler.getCharacterBuilder().toString();
			}
			
			if (data instanceof InformationOne) {
				InformationOne info = (InformationOne)data;
				info.setSummary(content);
				
				if (LOG.isDebugEnabled()) 
					LOG.debug("Information: " + info.getLocation() + " content updated");
			}
			
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("parse content error" + e.toString(), e); 
		}
	}
	
}
