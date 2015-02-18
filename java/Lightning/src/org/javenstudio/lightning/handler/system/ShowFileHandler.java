package org.javenstudio.lightning.handler.system;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.util.ContentStreamBase;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.writer.RawResponseWriter;

public class ShowFileHandler extends AdminHandlerBase {
	static Logger LOG = Logger.getLogger(ShowFileHandler.class);

	public static final String HIDDEN = "hidden";
	public static final String USE_CONTENT_TYPE = "contentType";
	  
	private final Set<String> mHiddenFiles;
	private final Core mCore;
	
	public ShowFileHandler(Core core) { 
		mCore = core;
		mHiddenFiles = new HashSet<String>();
		
		if (core == null) 
			throw new NullPointerException();
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		checkAuth(req, IUserClient.Op.ACCESS);
		showFromFileSystem(req, rsp);
	}

	protected void showFromFileSystem(Request req, Response rsp) 
			throws ErrorException { 
		File adminFile = null;
	    
		final Core core = mCore;
	    final ContextLoader loader = core.getContextLoader();
	    File configdir = new File(loader.getConfigDir());
	    if (!configdir.exists()) {
	    	// TODO: maybe we should just open it this way to start with?
	    	try {
	    		configdir = new File(loader.getClassLoader().getResource(
	    				loader.getConfigDir()).toURI());
	    	} catch (URISyntaxException e) {
	    		throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
	    				"Can not access configuration directory!");
	    	}
	    }
	    
	    String fname = req.getParams().get("file", null);
	    if (fname == null) {
	    	adminFile = configdir;
	    	
	    } else {
	    	fname = fname.replace('\\', '/'); // normalize slashes
	    	if (mHiddenFiles.contains(fname.toUpperCase(Locale.ROOT))) {
	    		throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
	    				"Can not access: " + fname);
	    	}
	    	
	    	if (fname.indexOf("..") >= 0) {
	    		throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
	    				"Invalid path: " + fname);  
	    	}
	    	
	    	adminFile = new File(configdir, fname);
	    }
	    
	    // Make sure the file exists, is readable and is not a hidden file
	    if (!adminFile.exists()) {
	    	//throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    	//		"Can not find: " + adminFile.getName() + " [" + adminFile.getAbsolutePath() + "]");
	    	
	    	if (LOG.isWarnEnabled())
	    		LOG.warn("Can not find: " + adminFile.getName() + " [" + adminFile.getAbsolutePath() + "]");
	    	
	    	// Include the empty contents
	    	//The file logic depends on RawResponseWriter, so force its use.
	    	req.setResponseWriterType(RawResponseWriter.TYPE);

	    	ContentStreamBase content = new ContentStreamBase.StringStream("");
	    	content.setContentType(req.getParam(USE_CONTENT_TYPE));

	    	rsp.add(RawResponseWriter.CONTENT, content);
	    	
	    } else {
		    if (!adminFile.canRead() || adminFile.isHidden()) {
		    	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
		    			"Can not show: " + adminFile.getName() + " [" + adminFile.getAbsolutePath() + "]");
		    }
		    
		    // Show a directory listing
		    if (adminFile.isDirectory()) {
		    	int basePath = configdir.getAbsolutePath().length() + 1;
		    	NamedList<NamedMap<Object>> files = new NamedMap<NamedMap<Object>>();
		      
		    	for (File f : adminFile.listFiles()) {
		    		String path = f.getAbsolutePath().substring( basePath );
		    		path = path.replace('\\', '/'); // normalize slashes
		    		if (mHiddenFiles.contains(path.toUpperCase(Locale.ROOT))) 
		    			continue; // don't show 'hidden' files
		    		
		    		if (f.isHidden() || f.getName().startsWith(".")) 
		    			continue; // skip hidden system files...
		        
		    		NamedMap<Object> fileInfo = new NamedMap<Object>();
		    		files.add(path, fileInfo);
		    		if (f.isDirectory()) 
		    			fileInfo.add("directory", true); 
		    		else 
		    			fileInfo.add("size", f.length()); // TODO? content type
		        
		    		fileInfo.add("modified", new Date(f.lastModified()));
		    	}
		    	
		    	rsp.add("files", files);
		    	
		    } else {
		    	// Include the file contents
		    	//The file logic depends on RawResponseWriter, so force its use.
		    	req.setResponseWriterType(RawResponseWriter.TYPE);
	
		    	ContentStreamBase content = new ContentStreamBase.FileStream(adminFile);
		    	content.setContentType(req.getParam(USE_CONTENT_TYPE));
	
		    	rsp.add(RawResponseWriter.CONTENT, content);
		    }
	    }
	    
	    //rsp.setHttpCaching(false);
	}
	
}
