package org.javenstudio.raptor.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;


public class Request {
    // ------------------------------------------------------- Static Variables


    /**
     * The set of attribute names that are special for request dispatchers.
     */
    protected static final String specials[] = { 
    
      Globals.INCLUDE_REQUEST_URI_ATTR, Globals.INCLUDE_CONTEXT_PATH_ATTR,
      Globals.INCLUDE_SERVLET_PATH_ATTR, Globals.INCLUDE_PATH_INFO_ATTR,
      Globals.INCLUDE_QUERY_STRING_ATTR, Globals.FORWARD_REQUEST_URI_ATTR, 
      Globals.FORWARD_CONTEXT_PATH_ATTR, Globals.FORWARD_SERVLET_PATH_ATTR, 
      Globals.FORWARD_PATH_INFO_ATTR, Globals.FORWARD_QUERY_STRING_ATTR 
    };


    /**
     * The current dispatcher type.
     */
    protected Object dispatcherType = null;


    /**
     * The request parameters for this request.  This is initialized from the
     * wrapped request, but updates are allowed.
     */
    protected Map<String, String[]> parameters = null;


    /**
     * Have the parameters for this request already been parsed?
     */
    private boolean parsedParams = false;


    /**
     * The path information for this request.
     */
    protected String pathInfo = null;


    /**
     * The query parameters for the current request.
     */
    private String queryParamString = null;


    /**
     * The query string for this request.
     */
    protected String queryString = null;


    /**
     * The current request dispatcher path.
     */
    protected Object requestDispatcherPath = null;


    /**
     * The request URI for this request.
     */
    protected String requestURI = null;

    protected String _scheme = null; 

    protected String _encoding = null; 


    /**
      * The default behavior of this method is to return getScheme()
     * on the wrapped request object.
     */
    public String getScheme() {
    	return _scheme == null ? "http" : _scheme;
    }

    public void setScheme(String scheme) {
    	this._scheme = scheme;
    }


    /**
     * Special attributes.
     */
    protected Object[] specialAttributes = new Object[specials.length];


    // ------------------------------------------------- ServletRequest Methods

    private Request request = null;  // parent request


    /**
     * Return the wrapped request object.
     */
    public Request getRequest() {
        return this.request;
    }


    public Request() {
    }

    public Request(Request parent) {
        this.request = parent; 
    }


    /**
     * Override the <code>getAttribute()</code> method of the wrapped request.
     *
     * @param name Name of the attribute to retrieve
     */
    public Object getAttribute(String name) {

        if (name.equals(Globals.DISPATCHER_TYPE_ATTR)) {
            return dispatcherType;
        } else if (name.equals(Globals.DISPATCHER_REQUEST_PATH_ATTR)) {
            if ( requestDispatcherPath != null ){
                return requestDispatcherPath.toString();
            } else {
                return null;   
            }
        }

        int pos = getSpecial(name);
        if (pos == -1) {
            if (getRequest() == null) 
                return null; 
            return getRequest().getAttribute(name);
        } else {
            if ((specialAttributes[pos] == null) 
                && (specialAttributes[5] == null) && (pos >= 5)) {
                // If it's a forward special attribute, and null, it means this
                // is an include, so we check the wrapped request since 
                // the request could have been forwarded before the include
                if (getRequest() == null) 
                    return null; 
                return getRequest().getAttribute(name);
            } else {
                return specialAttributes[pos];
            }
        }

    }


    /**
     * Override the <code>getAttributeNames()</code> method of the wrapped
     * request.
     */
    @SuppressWarnings("rawtypes")
	public Enumeration getAttributeNames() {
        return (new AttributeNamesEnumerator());
    }


    /**
     * Override the <code>removeAttribute()</code> method of the
     * wrapped request.
     *
     * @param name Name of the attribute to remove
     */
    public void removeAttribute(String name) {

        if (!removeSpecial(name)) {
            if (getRequest() != null) 
                getRequest().removeAttribute(name);
        }

    }


    /**
     * Override the <code>setAttribute()</code> method of the
     * wrapped request.
     *
     * @param name Name of the attribute to set
     * @param value Value of the attribute to set
     */
    public void setAttribute(String name, Object value) {

        if (name.equals(Globals.DISPATCHER_TYPE_ATTR)) {
            dispatcherType = value;
            return;
        } else if (name.equals(Globals.DISPATCHER_REQUEST_PATH_ATTR)) {
            requestDispatcherPath = value;
            return;
        }

        if (!setSpecial(name, value)) {
            if (getRequest() != null) 
                getRequest().setAttribute(name, value);
        }

    }



    // --------------------------------------------- HttpServletRequest Methods


    /**
     * Override the <code>getParameter()</code> method of the wrapped request.
     *
     * @param name Name of the requested parameter
     */
    public String getParameter(String name) {

	parseParameters();

        Object value = parameters.get(name);
        if (value == null)
            return (null);
        else if (value instanceof String[])
            return (((String[]) value)[0]);
        else if (value instanceof String)
            return ((String) value);
        else
            return (value.toString());

    }


    /**
     * Override the <code>getParameterMap()</code> method of the
     * wrapped request.
     */
    public Map<String, String[]> getParameterMap() {

	parseParameters();
        return (parameters);
    }


    /**
     * Override the <code>getParameterNames()</code> method of the
     * wrapped request.
     */
    @SuppressWarnings("rawtypes")
	public Enumeration getParameterNames() {
    	parseParameters();
        return (new Enumerator(parameters.keySet()));
    }


    /**
     * Override the <code>getParameterValues()</code> method of the
     * wrapped request.
     *
     * @param name Name of the requested parameter
     */
    public String[] getParameterValues(String name) {

	parseParameters();
        Object value = parameters.get(name);
        if (value == null)
            return ((String[]) null);
        else if (value instanceof String[])
            return ((String[]) value);
        else if (value instanceof String) {
            String values[] = new String[1];
            values[0] = (String) value;
            return (values);
        } else {
            String values[] = new String[1];
            values[0] = value.toString();
            return (values);
        }

    }


    /**
     * Override the <code>getPathInfo()</code> method of the wrapped request.
     */
    public String getPathInfo() {

        return (this.pathInfo);

    }


    /**
     * Override the <code>getQueryString()</code> method of the wrapped
     * request.
     */
    public String getQueryString() {

        return (this.queryString);

    }


    /**
     * Override the <code>getRequestURI()</code> method of the wrapped
     * request.
     */
    public String getRequestURI() {

        return (this.requestURI);

    }


    /**
     * Override the <code>getRequestURL()</code> method of the wrapped
     * request.
     */
    public StringBuffer getRequestURL() {

        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = 80; //getServerPort();
        if (port < 0)
            port = 80; // Work around java.net.URL bug

        url.append(scheme);
        url.append("://");
        url.append("localhost"); //getServerName());
        if ((scheme.equals("http") && (port != 80))
            || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());

        return (url);

    }


    /**
     * Perform a shallow copy of the specified Map, and return the result.
     *
     * @param orig Origin Map to be copied
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	Map copyMap(Map orig) {

        if (orig == null)
            return (new HashMap());
        HashMap dest = new HashMap();
        Iterator keys = orig.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            dest.put(key, orig.get(key));
        }
        return (dest);

    }


    /**
     * Set the path information for this request.
     *
     * @param pathInfo The new path info
     */
    void setPathInfo(String pathInfo) {

        this.pathInfo = pathInfo;

    }


    /**
     * Set the query string for this request.
     *
     * @param queryString The new query string
     */
    void setQueryString(String queryString) {

        this.queryString = queryString;

    }



    /**
     * Parses the parameters of this request.
     *
     * If parameters are present in both the query string and the request
     * content, they are merged.
     */
    @SuppressWarnings("unchecked")
	void parseParameters() {

    	if (parsedParams) 
    		return;

        parameters = new HashMap<String, String[]>();
        if (getRequest() != null) 
            parameters = copyMap(getRequest().getParameterMap());
        mergeParameters();
        parsedParams = true;
    }


    /**
     * Save query parameters for this request.
     *
     * @param queryString The query string containing parameters for this
     *                    request
     */
    public void setQueryParams(String queryString) {
        this.queryParamString = queryString;
        this.parsedParams = false; 
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Is this attribute name one of the special ones that is added only for
     * included servlets?
     *
     * @param name Attribute name to be tested
     */
    protected boolean isSpecial(String name) {

        for (int i = 0; i < specials.length; i++) {
            if (specials[i].equals(name))
                return (true);
        }
        return (false);

    }


    /**
     * Get a special attribute.
     *
     * @return the special attribute pos, or -1 if it is not a special 
     *         attribute
     */
    protected int getSpecial(String name) {
        for (int i = 0; i < specials.length; i++) {
            if (specials[i].equals(name)) {
                return (i);
            }
        }
        return (-1);
    }


    /**
     * Set a special attribute.
     * 
     * @return true if the attribute was a special attribute, false otherwise
     */
    protected boolean setSpecial(String name, Object value) {
        for (int i = 0; i < specials.length; i++) {
            if (specials[i].equals(name)) {
                specialAttributes[i] = value;
                return (true);
            }
        }
        return (false);
    }


    /**
     * Remove a special attribute.
     * 
     * @return true if the attribute was a special attribute, false otherwise
     */
    protected boolean removeSpecial(String name) {
        for (int i = 0; i < specials.length; i++) {
            if (specials[i].equals(name)) {
                specialAttributes[i] = null;
                return (true);
            }
        }
        return (false);
    }


    /**
     * Merge the two sets of parameter values into a single String array.
     *
     * @param values1 First set of values
     * @param values2 Second set of values
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	protected String[] mergeValues(Object values1, Object values2) {
        ArrayList results = new ArrayList();

        if (values1 == null)
            ;
        else if (values1 instanceof String)
            results.add(values1);
        else if (values1 instanceof String[]) {
            String values[] = (String[]) values1;
            for (int i = 0; i < values.length; i++)
                results.add(values[i]);
        } else
            results.add(values1.toString());

        if (values2 == null)
            ;
        else if (values2 instanceof String)
            results.add(values2);
        else if (values2 instanceof String[]) {
            String values[] = (String[]) values2;
            for (int i = 0; i < values.length; i++)
                results.add(values[i]);
        } else
            results.add(values2.toString());

        String values[] = new String[results.size()];
        return ((String[]) results.toArray(values));

    }


    // ------------------------------------------------------ Private Methods

    /**
      * The default behavior of this method is to return getCharacterEncoding()
     * on the wrapped request object.
     */

    public String getCharacterEncoding() {
	return _encoding;
    }

    public void setCharacterEncoding(String encoding) {
	this._encoding = encoding;
    }


    /**
     * Merge the parameters from the saved query parameter string (if any), and
     * the parameters already present on this request (if any), such that the
     * parameter values from the query string show up first if there are
     * duplicate parameter names.
     */
    @SuppressWarnings("rawtypes")
	private void mergeParameters() {
        if ((queryParamString == null) || (queryParamString.length() < 1))
            return;

        HashMap<String, String[]> queryParameters = new HashMap<String, String[]>();
        String encoding = getCharacterEncoding();
        if (encoding == null)
            encoding = "ISO-8859-1";
        try {
            RequestUtil.parseParameters
                (queryParameters, queryParamString, encoding);
        } catch (Exception e) {
            ;
        }
        Iterator keys = parameters.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value = queryParameters.get(key);
            if (value == null) {
                queryParameters.put(key, parameters.get(key));
                continue;
            }
            queryParameters.put
                (key, mergeValues(value, parameters.get(key)));
        }
        parameters = queryParameters;

    }


    // ----------------------------------- AttributeNamesEnumerator Inner Class


    /**
     * Utility class used to expose the special attributes as being available
     * as request attributes.
     */
    @SuppressWarnings("rawtypes")
	protected class AttributeNamesEnumerator implements Enumeration {

        protected int pos = -1;
        protected int last = -1;
        protected Enumeration parentEnumeration = null;
        protected String next = null;

        public AttributeNamesEnumerator() {
            if (getRequest() == null) return; 
            parentEnumeration = getRequest().getAttributeNames();
            for (int i = 0; i < specialAttributes.length; i++) {
                if (getAttribute(specials[i]) != null) {
                    last = i;
                }
            }
        }

        public boolean hasMoreElements() {
            return ((pos != last) || (next != null) 
                    || ((next = findNext()) != null));
        }

        public Object nextElement() {
            if (pos != last) {
                for (int i = pos + 1; i <= last; i++) {
                    if (getAttribute(specials[i]) != null) {
                        pos = i;
                        return (specials[i]);
                    }
                }
            }
            String result = next;
            if (next != null) {
                next = findNext();
            } else {
                throw new NoSuchElementException();
            }
            return result;
        }

        protected String findNext() {
            String result = null;
            while ((result == null) && (parentEnumeration.hasMoreElements())) {
                String current = (String) parentEnumeration.nextElement();
                if (!isSpecial(current)) {
                    result = current;
                }
            }
            return result;
        }

    }


  /** For debugging. */
  @SuppressWarnings("rawtypes")
  public static void main(String[] args) throws Exception {
    String usage = "Usage: Request <url query>";

    if (args.length == 0) {
      System.err.println(usage);
      System.exit(-1);
    }

    Request r = new Request(); 
    r.setQueryParams(args[0]); 
    r.setCharacterEncoding(StringUtils.getCharacterEncoding()); 

    Enumeration e = r.getParameterNames(); 
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement(); 
      String val = (String) r.getParameter(key); 
      System.out.println(key + " = " + val); 
    }
  }

}
