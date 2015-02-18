package org.javenstudio.lightning.response;

import java.io.Writer;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedListPlugin;
import org.javenstudio.lightning.request.Request;

/**
 * Implementations of <code>QueryResponseWriter</code> are used to 
 * format responses to query requests.
 *
 * Different <code>QueryResponseWriter</code>s are registered with 
 * the <code>Core</code>.
 * One way to register a QueryResponseWriter with the core is through 
 * the <code>config.xml</code> file.
 * <p>
 * Example <code>config.xml</code> entry to register a 
 * <code>QueryResponseWriter</code> implementation to
 * handle all queries with a writer type of "simple":
 * <p>
 * <code>
 *    &lt;queryResponseWriter name="simple" class="foo.SimpleResponseWriter" /&gt;
 * </code>
 * <p>
 * A single instance of any registered QueryResponseWriter is created
 * via the default constructor and is reused for all relevant queries.
 *
 */
public interface ResponseWriter extends NamedListPlugin {
	
	public static String CONTENT_TYPE_XML_UTF8 = "application/xml; charset=UTF-8";
	public static String CONTENT_TYPE_TEXT_UTF8 = "text/plain; charset=UTF-8";
	public static String CONTENT_TYPE_TEXT_ASCII = "text/plain; charset=US-ASCII";

	/**
	 * Write a Response, this method must be thread save.
	 *
	 * <p>
	 * Information about the request (in particular: formating options) may be 
	 * obtained from <code>req</code> but the dominant source of information 
	 * should be <code>rsp</code>.
	 * <p>
	 * There are no mandatory actions that write must perform.
	 * An empty write implementation would fulfill
	 * all interface obligations.
	 * </p> 
	 */
	public void write(Writer writer, Request request, Response response) 
			throws ErrorException;

	/** 
	 * Return the applicable Content Type for a request, this method 
	 * must be thread safe.
	 *
	 * <p>
	 * QueryResponseWriter's must implement this method to return a valid 
	 * HTTP Content-Type header for the request, that will logically 
	 * correspond with the output produced by the write method.
	 * </p>
	 * @return a Content-Type string, which may not be null.
	 */
	public String getContentType(Request request, Response response) 
			throws ErrorException;
  
	/** 
	 * <code>init</code> will be called just once, immediately after creation.
	 * <p>The args are user-level initialization parameters that
	 * may be specified when declaring a response writer in
	 * config.xml
	 */
	public void init(NamedList<?> args) throws ErrorException;
	
}
