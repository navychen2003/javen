package org.javenstudio.lightning.core.search;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.dataimport.ImportContext;
import org.javenstudio.falcon.search.dataimport.ImportProcessorBase;
import org.javenstudio.falcon.search.dataimport.ImportRequest;
import org.javenstudio.falcon.search.dataimport.ImportRow;
import org.javenstudio.lightning.util.SimpleFileBuilder;

public class FileImportProcessor extends ImportProcessorBase {
	static final Logger LOG = Logger.getLogger(FileImportProcessor.class);
	
	public static final String SIMPLE_NAME = "simple";
	
	private final SimpleBuilder mBuilder = new SimpleBuilder();
	
	public FileImportProcessor(ImportContext context, String entityName) 
			throws ErrorException { 
		super(context, entityName);
	}
	
    @Override
    public void init(ImportRequest req) throws ErrorException { 
    	String[] paths = req.getParams().getParams("path");
    	for (int i=0; paths != null && i < paths.length; i++) { 
    		String path = paths[i];
    		if (path == null || path.length() == 0) 
    			continue;
    		
    		StringTokenizer st = new StringTokenizer(path, " \t\r\n,;");
    		while (st.hasMoreTokens()) { 
    			String token = st.nextToken();
    			if (token != null && token.length() > 0) {
    				try {
    					mBuilder.addPath(token);
    				} catch (IOException ex) { 
    					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
    				}
    			}
    		}
    	}
    }
	
	@Override
    public synchronized ImportRow nextRow() throws ErrorException {
		try {
			return mBuilder.nextDoc();
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
    }
	
	@Override
	public synchronized void close() { 
		try {
			mBuilder.close();
		} catch (Throwable ex) { 
			if (LOG.isErrorEnabled())
				LOG.error("close error: " + ex.toString(), ex);
		}
	}
	
	static class SimpleBuilder extends SimpleFileBuilder<ImportRow> {

		@Override
		protected ImportRow newDoc() {
			return new ImportRow();
		}

		@Override
		protected void addField(ImportRow doc, String name, Object val) {
			doc.addField(name, val);
		}

		@Override
		protected void onWrapErr(File file, Throwable ex) throws IOException {
			if (LOG.isWarnEnabled())
				LOG.warn("File: " + file + " to ImportRow failed: " + ex.toString(), ex);
		}
	}
	
}
