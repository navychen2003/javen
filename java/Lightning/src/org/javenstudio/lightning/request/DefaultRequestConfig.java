package org.javenstudio.lightning.request;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.ContentStreamBase;
import org.javenstudio.falcon.util.ContextResource;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.core.CoreAdmin;
import org.javenstudio.lightning.request.parser.FormDataRequestParser;
import org.javenstudio.lightning.request.parser.MultipartRequestParser;
import org.javenstudio.lightning.request.parser.RawRequestParser;
import org.javenstudio.lightning.request.parser.SimpleRequestParser;
import org.javenstudio.lightning.request.parser.StandardRequestParser;

public class DefaultRequestConfig extends RequestConfig {

	// Should these constants be in a more public place?
	public static final String MULTIPART = "multipart";
	public static final String FORMDATA = "formdata";
	public static final String RAW = "raw";
	public static final String SIMPLE = "simple";
	public static final String STANDARD = "standard";
	
	private long mMultipartUploadLimitKB = 0;
	private long mFormdataUploadLimitKB = 0;
	private boolean mEnableRemoteStreams = false;
	private boolean mHandleSelect = true;
	
	public DefaultRequestConfig(ContextResource config) throws ErrorException { 
		super(config);
		
		mMultipartUploadLimitKB = config.getInt("requestDispatcher/requestParsers/@multipartUploadLimitInKB", 10 * 1024);
		mFormdataUploadLimitKB = config.getInt("requestDispatcher/requestParsers/@formdataUploadLimitInKB", 2 * 1024);
		mEnableRemoteStreams = config.getBool("requestDispatcher/requestParsers/@enableRemoteStreaming", false);
		// Let this filter take care of /select?xxx format
		mHandleSelect = config.getBool("requestDispatcher/@handleSelect", true);
	}
	
	public final boolean isEnableRemoteStreams() { return mEnableRemoteStreams; }
	public final boolean isHandleSelect() { return mHandleSelect; }
	
	public final long getMultipartUploadLimitKB() { return mMultipartUploadLimitKB; }
	public final long getFormdataUploadLimitKB() { return mFormdataUploadLimitKB; }
	
	@Override
	public void initParsers(RequestParsers parsers) throws ErrorException { 
		MultipartRequestParser multi = new MultipartRequestParser(getMultipartUploadLimitKB());
		FormDataRequestParser form = new FormDataRequestParser(getFormdataUploadLimitKB());
	    RawRequestParser raw = new RawRequestParser();
	    RequestParser standard = new StandardRequestParser(multi, form, raw);
	    
	    parsers.registerParser(MULTIPART, multi);
	    parsers.registerParser(FORMDATA, form);
	    parsers.registerParser(RAW, raw);
	    parsers.registerParser(SIMPLE, new SimpleRequestParser());
	    parsers.registerParser(STANDARD, standard);
	    parsers.registerParser("", standard);
	}
	
	@Override
	public Request buildRequest(RequestAcceptor acceptor, RequestInput input, 
			Params params, List<ContentStream> streams) throws ErrorException { 
	    // The content type will be applied to all streaming content
	    String contentType = params.get(CommonParams.STREAM_CONTENTTYPE);
	      
	    // Handle anything with a remoteURL
	    String[] strs = params.getParams(CommonParams.STREAM_URL);
	    if (strs != null) {
	    	if (!isEnableRemoteStreams()) {
	    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    				"Remote Streaming is disabled.");
	    	}
	    	
	    	for (final String url : strs) {
	    		URL url2 = null;
	    		try { 
	    			url2 = new URL(url); 
	    		} catch (IOException e) { 
	    			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
	    					e.toString(), e);
	    		}
	    		
	    		ContentStreamBase stream = new ContentStreamBase.URLStream(url2);
	    		if (contentType != null) 
	    			stream.setContentType(contentType);
	    		
	    		streams.add(stream);
	    	}
	    }
	    
	    // Handle streaming files
	    strs = params.getParams(CommonParams.STREAM_FILE);
	    if (strs != null) {
	    	if (!isEnableRemoteStreams()) {
	    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    				"Remote Streaming is disabled.");
	    	}
	    	
	    	for (final String file : strs) {
	    		ContentStreamBase stream = new ContentStreamBase.FileStream(new File(file));
	    		if (contentType != null) 
	    			stream.setContentType(contentType);
	    		
	    		streams.add( stream );
	    	}
	    }
	    
	    // Check for streams in the request parameters
	    strs = params.getParams(CommonParams.STREAM_BODY);
	    if (strs != null) {
	    	for (final String body : strs) {
	    		ContentStreamBase stream = new ContentStreamBase.StringStream(body);
	    		if (contentType != null) 
	    			stream.setContentType(contentType);
	    		
	    		streams.add(stream);
	    	}
	    }
	    
	    RequestBase req = createRequest(acceptor, input, params);
	    if (streams != null && streams.size() > 0) 
	    	req.setContentStreams(streams);
	    
	    return req;
	}
	
	protected RequestBase createRequest(RequestAcceptor acceptor, RequestInput input, 
			Params params) throws ErrorException { 
		if (acceptor instanceof Core) {
			return new QueryRequest((Core)acceptor, input, params);
			
		} else if (acceptor instanceof CoreAdmin) {
			return new AdminRequest((CoreAdmin)acceptor, input, params);
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"wrong request acceptor type: " + acceptor.getClass().getName()); 
			//return new QueryRequest(input, params);
		}
	}
	
}
