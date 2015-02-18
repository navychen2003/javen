package org.javenstudio.falcon.search.handler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.dataimport.ImportContext;
import org.javenstudio.falcon.search.dataimport.ImportWriterImpl;
import org.javenstudio.falcon.search.dataimport.Importer;
import org.javenstudio.falcon.search.dataimport.ImportRequest;
import org.javenstudio.falcon.search.dataimport.ImportWriter;
import org.javenstudio.falcon.search.params.UpdateParams;
import org.javenstudio.falcon.search.update.UpdateProcessor;
import org.javenstudio.falcon.search.update.UpdateProcessorChain;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.ContentStreamBase;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;

public class ImportHandler {
	static final Logger LOG = Logger.getLogger(ImportHandler.class);

	private final Importer mImporter;
	
	private NamedList<?> mInitArgs = null;
	
	public ImportHandler(ImportContext context) { 
  		mImporter = new Importer(context);
  	}
  	
  	public void init(NamedList<?> args) throws ErrorException {
  		mInitArgs = args;
  	}
  	
  	public void handleRequestBody(ISearchRequest req, ISearchResponse rsp) 
  			throws ErrorException {
  	    //TODO: figure out why just the first one is OK...
  	    ContentStream contentStream = null;
  	    Iterable<ContentStream> streams = req.getContentStreams();
  	    if (streams != null){
  	    	for (ContentStream stream : streams) {
  	    		contentStream = stream;
  	    		break;
  	    	}
  	    }
  		
  		Params params = req.getParams();
  		ImportRequest request = new ImportRequest(params, contentStream);
  		
		String command = request.getCommand();
		String message = "";
		
		//if (LOG.isDebugEnabled())
		//	LOG.debug("handle DataImport command: " + command);
		
		if (ImportContext.SHOW_CONF_CMD.equals(command)) { 
			String dataConfig = mImporter.getContext().toConfigXml();
			if (dataConfig == null || dataConfig.length() == 0) {
				rsp.add("status", ImportContext.MSG.NO_CONFIG_FOUND);
				
			} else { 
		        // Modify incoming request params to add wt=raw
		        ModifiableParams rawParams = new ModifiableParams(req.getParams());
		        rawParams.set(CommonParams.WT, "raw");
		        req.setParams(rawParams);
		        
		        ContentStreamBase content = new ContentStreamBase.StringStream(dataConfig);
		        rsp.addContent(content);
			}
			
			return;
		}
		
		rsp.add("initArgs", mInitArgs);
		if (command != null) 
			rsp.add("command", command);
		
	    // If importer is still null
	    if (mImporter == null) {
	    	rsp.add("status", ImportContext.MSG.NO_INIT);
	    	return;
	    }
		
	    if (command != null) { 
	    	if (ImportContext.ABORT_CMD.equals(command)) { 
	    		mImporter.runCommand(request, null);
	    		
	    	} else if (mImporter.isBusy()) { 
	    		message = ImportContext.MSG.CMD_RUNNING;
	    		
	    	} else if (ImportContext.FULL_IMPORT_CMD.equals(command) || 
	    			ImportContext.DELTA_IMPORT_CMD.equals(command) ||
	    			ImportContext.IMPORT_CMD.equals(command)) { 
	    		
	    		UpdateProcessorChain chain = mImporter.getSearchCore().getUpdateProcessingChain(
	    				params.get(UpdateParams.UPDATE_CHAIN));
	    		
	    		UpdateProcessor processor = chain.createProcessor(req, rsp);
	    		ImportWriter writer = getImportWriter(processor, req);
	    		
	    		// Asynchronous request for normal mode
	            if (request.getContentStream() == null && !request.isSyncMode())
	            	mImporter.runAsync(request, writer);
	            else 
	            	mImporter.runCommand(request, writer);
	            
	    	} else if (ImportContext.RELOAD_CONF_CMD.equals(command)) { 
	    		message = ImportContext.MSG.CONFIG_NOT_RELOADED;
	    	}
	    }
	    
		rsp.add("status", mImporter.isBusy() ? "busy" : "idle");
		rsp.add("importResponse", message);
		rsp.add("statusMessages", mImporter.getStatusMessages());
  	}
  	
  	private ImportWriter getImportWriter(UpdateProcessor processor, 
  			ISearchRequest req) throws ErrorException { 
  		return new ImportWriterImpl(processor, req);
  	}
  	
    @SuppressWarnings("unused")
	private Map<String, Object> getParamsMap(Params params) throws ErrorException {
    	Map<String, Object> result = new HashMap<String, Object>();
    	
        Iterator<String> names = params.getParameterNamesIterator();
        while (names.hasNext()) {
        	String s = names.next();
        	String[] val = params.getParams(s);
        	if (val == null || val.length < 1)
        		continue;
        	
        	if (val.length == 1)
        		result.put(s, val[0]);
        	else
        		result.put(s, Arrays.asList(val));
        }
        
        return result;
	}
  	
}
