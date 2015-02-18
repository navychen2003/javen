package org.javenstudio.lightning.request;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.MultiMapParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;

public abstract class RequestBase extends Request {

	public RequestBase(RequestInput input, Params params) { 
		super(input, params);
	}
	
	@SuppressWarnings("rawtypes")
	public RequestBase(RequestInput input, String query, String qtype, 
			int start, int limit, Map args) {
		this(input, makeParams(query,qtype,start,limit,args));
	}

	public RequestBase(RequestInput input, NamedList<?> args) {
		super(input, Params.toParams(args));
	}

	public RequestBase(RequestInput input, Map<String,String[]> args) {
		super(input, new MultiMapParams(args));
	}
	
	public void setContentStreams(Iterable<ContentStream> streams) { 
		mStreams = streams;
	}
	
	public ModifiableParams getModifiableParams() throws ErrorException { 
		Params params = getParams();
		if (params != null && params instanceof ModifiableParams)
			return (ModifiableParams)params;
		
		ModifiableParams mp = new ModifiableParams(params);
		setParams(mp);
		
		return mp;
	}
	
	@Override
	public String getResponseWriterType() throws ErrorException { 
		return getParam(CommonParams.WT);
	}
	
	@Override
	public void setResponseWriterType(String type) throws ErrorException { 
		ModifiableParams params = getModifiableParams();
		params.set(CommonParams.WT, type);
	}
	
	@Override
	public HttpMethod getRequestMethod() { 
		return HttpMethod.getMethod(getRequestInput().getMethod());
	}
	
	@SuppressWarnings("rawtypes")
	static Params makeParams(String query, String qtype, 
			int start, int limit, Map args) {
	    Map<String,String[]> map = new HashMap<String,String[]>();
	    
	    for (Iterator iter = args.entrySet().iterator(); iter.hasNext();) {
	    	Map.Entry e = (Map.Entry)iter.next();
	    	
	    	String k = e.getKey().toString();
	    	Object v = e.getValue();
	    	
	    	if (v instanceof String[]) 
	    		map.put(k,(String[])v);
	    	else 
	    		map.put(k,new String[]{v.toString()});
	    }
	    
	    if (query != null) 
	    	map.put(CommonParams.Q, new String[]{query});
	    
	    if (qtype != null) 
	    	map.put(CommonParams.QT, new String[]{qtype});
	    
	    map.put(CommonParams.START, new String[]{Integer.toString(start)});
	    map.put(CommonParams.ROWS, new String[]{Integer.toString(limit)});
	    
	    return new MultiMapParams(map);
	}
	
}
