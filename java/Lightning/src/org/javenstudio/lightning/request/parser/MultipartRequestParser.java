package org.javenstudio.lightning.request.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.ContentStreamBase;
import org.javenstudio.falcon.util.MultiMapParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.fileupload.FileItem;
import org.javenstudio.lightning.fileupload.FileUploadException;
import org.javenstudio.lightning.fileupload.disk.DiskFileItemFactory;
import org.javenstudio.lightning.fileupload.servlet.ServletFileUpload;
import org.javenstudio.lightning.request.HttpHelper;
import org.javenstudio.lightning.request.RequestInput;
import org.javenstudio.lightning.request.RequestParser;

/**
 * Extract Multipart streams
 */
public class MultipartRequestParser implements RequestParser {
	private static final Logger LOG = Logger.getLogger(MultipartRequestParser.class);

    public static final boolean isMultipartContent(RequestInput request) {
    	return ServletFileUpload.isServletMultipartContent(request);
    }
	
	private final long mUploadLimitKB;
	
	public MultipartRequestParser(long limit) { 
		mUploadLimitKB = limit;
	}
	
	@Override
	public Params parseParamsAndFillStreams(RequestInput input,
			List<ContentStream> streams) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("parseParamsAndFillStreams: input=" + input);
		
	    if (!ServletFileUpload.isServletMultipartContent(input)) {
	    	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    			"Not multipart content! " + input.getContentType() );
	    }
		
	    MultiMapParams params = HttpHelper.parseQueryString(input.getQueryString());
	    
	    try {
		    // Create a factory for disk-based file items
		    DiskFileItemFactory factory = new DiskFileItemFactory();
	
		    // Set factory constraints
		    // TODO - configure factory.setSizeThreshold(yourMaxMemorySize);
		    // TODO - configure factory.setRepository(yourTempDirectory);
	
		    // Create a new file upload handler
		    ServletFileUpload upload = new ServletFileUpload(factory);
		    upload.setSizeMax( ((long) mUploadLimitKB) * 1024L );
	
		    // Parse the request
		    List<FileItem> items = upload.parseRequest(input);
		    Iterator<FileItem> iter = items.iterator();
		    while (iter.hasNext()) {
		        FileItem item = (FileItem) iter.next();
	
		        // If its a form field, put it in our parameter map
		        if (item.isFormField()) {
		          params.addParam( 
		            item.getFieldName(), 
		            item.getString());
		        }
		        // Add the stream
		        else { 
		          streams.add(new FileItemContentStream(item));
		        }
		    }
	    } catch (FileUploadException ex) { 
	    	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    }
	    
		return params;
	}

	static class FileItemContentStream extends ContentStreamBase { 
		private final FileItem mItem;
  
		public FileItemContentStream(FileItem file) {
			this.mItem = file;
			this.contentType = file.getContentType();
			this.name = file.getName();
			this.sourceInfo = file.getFieldName();
			this.size = file.getSize();
		}
    
		@Override
		public File getFile() {
			return mItem.getFile();
		}
		
		@Override
		public InputStream getStream() throws IOException {
			return mItem.getInputStream();
		}
		
		@Override
		public void close() { 
			mItem.delete();
			super.close();
		}
	}
	
}
