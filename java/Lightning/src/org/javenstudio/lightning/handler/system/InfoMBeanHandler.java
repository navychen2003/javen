package org.javenstudio.lightning.handler.system;

import java.io.IOException;
import java.io.StringReader;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.IOUtils;
import org.javenstudio.falcon.util.InfoMBean;
import org.javenstudio.falcon.util.InfoMBeanRegistry;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.request.parser.XMLResponseParser;
import org.javenstudio.lightning.response.Response;

public class InfoMBeanHandler extends AdminHandlerBase {

	private final Core mCore;
	
	public InfoMBeanHandler(Core core) { 
		mCore = core;
		
		if (core == null) 
			throw new NullPointerException();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		checkAuth(req, IUserClient.Op.ACCESS);
		
		NamedList<NamedList<NamedList<Object>>> cats = getMBeanInfo(req);
		
	    if (req.getParams().getBool("diff", false)) {
	    	ContentStream body = null;
	    	try {
	    		body = req.getContentStreams().iterator().next();
	    	} catch (Throwable ex) {
	    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    				"missing content-stream for diff");
	    	}
	    	
	    	NamedList<NamedList<NamedList<Object>>> ref = fromXML(body);
	      
	    	// Normalize the output 
	    	Response wrap = mCore.createResponse(req, rsp.getResponseOutput());
	    	wrap.add("info-mbeans", cats);
	    	
	    	cats = (NamedList<NamedList<NamedList<Object>>>)
	    			mCore.getParsedResponse(req, wrap).get("info-mbeans");
	      
	    	// Get rid of irrelevant things
	    	ref = normalize(ref);
	    	cats = normalize(cats);
	      
	    	// Only the changes
	    	boolean showAll = req.getParams().getBool("all", false);
	    	rsp.add("info-mbeans", getDiff(ref,cats, showAll));
	    	
	    } else {
	    	rsp.add("info-mbeans", cats);
	    }
	    
	    // never cache, no matter what init config looks like
	    //rsp.setHttpCaching(false); 
	}

	/**
	 * Take an array of any type and generate a Set containing the toString.
	 * Set is guarantee to never be null (but may be empty)
	 */
	protected Set<String> arrayToSet(Object[] arr) {
		HashSet<String> r = new HashSet<String>();
		if (arr == null) return r;
		for (Object o : arr) {
			if (o != null) 
				r.add(o.toString());
		}
		return r;
	}
	
	@SuppressWarnings("unchecked")
	static NamedList<NamedList<NamedList<Object>>> fromXML(ContentStream body) 
			throws ErrorException {
		if (body == null) 
			return new NamedList<NamedList<NamedList<Object>>>();
		
	    try {
	    	String content = IOUtils.toString(body.getReader());
	    	
		    int idx = content.indexOf("<response>");
		    if (idx < 0) {
		    	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
		    			"Body does not appear to be an XML response");
		    }
		    
	    	XMLResponseParser parser = new XMLResponseParser();
	    	
	    	return (NamedList<NamedList<NamedList<Object>>>)
	    			parser.processResponse(new StringReader(content.substring(idx)))
	    					.get("info-mbeans");
	    	
	    } catch (IOException ex) {
	    	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    			"Unable to read original XML", ex);
	    }
	}
	
	protected NamedList<NamedList<NamedList<Object>>> getMBeanInfo(Request req) 
			throws ErrorException {
		NamedList<NamedList<NamedList<Object>>> cats = 
				new NamedList<NamedList<NamedList<Object>>>();
	    
		InfoMBeanRegistry reg = mCore.getInfoRegistry();
		
	    String[] requestedCats = req.getParams().getParams("cat");
	    if (requestedCats == null || requestedCats.length == 0) {
	    	for (String cat : reg.getCategories()) {
	    		cats.add(cat, new NamedMap<NamedList<Object>>());
	    	}
	    } else {
	    	for (String catName : requestedCats) {
	    		cats.add(catName, new NamedMap<NamedList<Object>>());
	    	}
	    }
         
	    Set<String> requestedKeys = arrayToSet(req.getParams().getParams("key"));
	    
	    for (String key : reg.getKeySet()) {
	    	InfoMBean m = reg.getInfoMBean(key);
	    	if (m == null) continue;

	    	if (!(requestedKeys.isEmpty() || requestedKeys.contains(key))) 
	    		continue;

	    	NamedList<NamedList<Object>> catInfo = cats.get(m.getMBeanCategory());
	    	if (catInfo == null) continue;

	    	NamedList<Object> mBeanInfo = new NamedMap<Object>();
	    	mBeanInfo.add("class", m.getMBeanName());
	    	mBeanInfo.add("version", m.getMBeanVersion());
	    	mBeanInfo.add("description", m.getMBeanDescription());
	    	mBeanInfo.add("src", ""); //m.getMBeanSource());
      
	    	// Use an external form
	    	//URL[] urls = m.getDocs();
	    	//if (urls != null) {
	    	//	List<String> docs = new ArrayList<String>(urls.length);
	    	//	for (URL url : urls) {
	    	//		docs.add(url.toExternalForm());
	    	//	}
	    	//	mBeanInfo.add("docs", docs);
	    	//}
      
	    	if (req.getParams().getFieldBool(key, "stats", false))
	    		mBeanInfo.add("stats", m.getMBeanStatistics());
      
	    	catInfo.add(key, mBeanInfo);
	    }
	    
	    return cats;
	}
	
	protected NamedList<NamedList<NamedList<Object>>> getDiff(
			NamedList<NamedList<NamedList<Object>>> ref, 
			NamedList<NamedList<NamedList<Object>>> now,
			boolean includeAll) throws ErrorException {
	    
		NamedList<NamedList<NamedList<Object>>> changed = 
				new NamedList<NamedList<NamedList<Object>>>();
	    
	    // Cycle through each category
		for (int i=0; i < ref.size(); i++) {
			String category = ref.getName(i);
			
			NamedList<NamedList<Object>> ref_cat = ref.get(category);
			NamedList<NamedList<Object>> now_cat = now.get(category);
			
			if (now_cat != null) {
				String ref_txt = ref_cat + "";
				String now_txt = now_cat + "";
				
				if (!ref_txt.equals(now_txt)) {
					// Something in the category changed
					// Now iterate the real beans
					NamedList<NamedList<Object>> cat = new NamedMap<NamedList<Object>>();
					
					for (int j=0; j < ref_cat.size(); j++) {
						String name = ref_cat.getName(j);
						
						NamedList<Object> ref_bean = ref_cat.get(name);
						NamedList<Object> now_bean = now_cat.get(name);

						ref_txt = ref_bean + "";
						now_txt = now_bean + "";
						
						if (!ref_txt.equals(now_txt)) {
							// Calculate the differences
							NamedList<Object> diff = diffNamedList(ref_bean, now_bean);
							diff.add("_changed_", true); // flag the changed thing
							cat.add(name, diff);
							
						} else if (includeAll) {
							cat.add(name, ref_bean);
						}
					}
					
					if (cat.size() > 0) 
						changed.add(category, cat);
					
				} else if (includeAll) {
					changed.add(category, ref_cat);
				}
			}
		}
		
		return changed;
	}
  
	public NamedList<Object> diffNamedList(NamedList<Object> ref, 
			NamedList<Object> now) {
		NamedList<Object> out = new NamedMap<Object>();
		
		for (int i=0; i < ref.size(); i++) {
			String name = ref.getName(i);
			
			Object r = ref.getVal(i);
			Object n = now.remove(name);
			
			if (n == null) {
				if (r != null) 
					out.add("REMOVE " + name, r);
			} else {
				out.add(name, diffObject(r, n));
			}
		}

		for (int i=0; i < now.size(); i++) {
			String name = now.getName(i);
			Object v = now.getVal(i);
			
			if (v != null) 
				out.add("ADD " + name, v);
		}
		
		return out;
	}
  
	@SuppressWarnings("unchecked")
	public Object diffObject(Object ref, Object now) {
		if (ref instanceof NamedList) 
			return diffNamedList((NamedList<Object>)ref, (NamedList<Object>)now);
    
		if (ref.equals(now)) 
			return ref;
    
		StringBuilder str = new StringBuilder();
		str.append("Was: ").append(ref).append(", Now: ").append(now);
    
		if (ref instanceof Number) {
			NumberFormat nf = NumberFormat.getIntegerInstance(Locale.ROOT);
			
			if ((ref instanceof Double) || (ref instanceof Float)) 
				nf = NumberFormat.getInstance(Locale.ROOT);
      
			double dref = ((Number)ref).doubleValue();
			double dnow = ((Number)now).doubleValue();
			double diff = Double.NaN;
			
			if (Double.isNaN(dref)) 
				diff = dnow;
			else if (Double.isNaN(dnow)) 
				diff = dref;
			else 
				diff = dnow - dref;
			
			str.append(", Delta: ").append(nf.format(diff));
		}
		
		return str.toString();
	}
  
	/**
	 * The 'avgRequestsPerSecond' field will make everything look like it changed
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public NamedList normalize(NamedList input) {
		input.remove("avgRequestsPerSecond");
		
		for (int i=0; i < input.size(); i++) {
			Object v = input.getVal(i);
			
			if (v instanceof NamedList) 
				input.setVal(i, normalize((NamedList<Object>)v));
		}
		
		return input;
	}
	
}
