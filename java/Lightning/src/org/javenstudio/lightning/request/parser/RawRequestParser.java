package org.javenstudio.lightning.request.parser;

import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.request.RequestInput;
import org.javenstudio.lightning.request.RequestParser;

/**
 * The raw parser just uses the params directly
 */
public class RawRequestParser implements RequestParser {
	private static final Logger LOG = Logger.getLogger(RawRequestParser.class);

	@Override
	public Params parseParamsAndFillStreams(RequestInput input,
			List<ContentStream> streams) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("parseParamsAndFillStreams: input=" + input);
		
	    // The javadocs for HttpServletRequest are clear that req.getReader() should take
	    // care of any character encoding issues.  BUT, there are problems while running on
	    // some servlet containers: including Tomcat 5 and resin.
	    //
	    // Rather than return req.getReader(), this uses the default ContentStreamBase method
	    // that checks for charset definitions in the ContentType.
	    
	    streams.add(new RequestContentStream(input));
	    return input.getQueryStringAsParams();
	}

}
