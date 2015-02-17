package org.javenstudio.common.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.FileOutputStream; 
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException; 
import java.util.Map;
import java.util.HashMap;

@SuppressWarnings({"unused"})
public class SimpleHttp {

  public final static String CONTENT_ENCODING = "Content-Encoding";
  public final static String CONTENT_LANGUAGE = "Content-Language";
  public final static String CONTENT_LENGTH = "Content-Length";
  public final static String CONTENT_LOCATION = "Content-Location";
  public final static String CONTENT_DISPOSITION = "Content-Disposition";
  public final static String CONTENT_MD5 = "Content-MD5";
  public final static String CONTENT_TYPE = "Content-Type";
  public final static String LAST_MODIFIED = "Last-Modified";
  public final static String LOCATION = "Location";
  
  public static final int BUFFER_SIZE = 8 * 1024;
  
  private static final byte[] EMPTY_CONTENT = new byte[0];
 
  /** The proxy hostname. */ 
  protected String proxyHost = null;

  /** The proxy port. */
  protected int proxyPort = 8080; 

  /** Indicates if a proxy is used */
  protected boolean useProxy = false;

  /** The network timeout in millisecond */
  protected int timeout = 1 * 60 * 1000;

  /** The length limit for downloaded content, in bytes. */
  protected int maxContent = 5 * 1024 * 1024; 

  /** The number of times a thread will delay when trying to fetch a page. */
  protected int maxDelays = 3;

  /**
   * The maximum number of threads that should be allowed
   * to access a host at one time.
   */
  protected int maxThreadsPerHost = 1; 

  /**
   * The number of seconds the fetcher will delay between
   * successive requests to the same server.
   */
  protected long serverDelay = 1000;

  /** The Hawk 'User-Agent' request header */
  protected String userAgent = getAgentString(
                        "CrawlCVS", null, "Crawl",
                    "http://www.crawl.com",
                    "agent@crawl.com");

    
  /**
   * Maps from host to a Long naming the time it should be unblocked.
   * The Long is zero while the host is in use, then set to now+wait when
   * a request finishes.  This way only one thread at a time accesses a
   * host.
   */
  //private static HashMap BLOCKED_ADDR_TO_TIME = new HashMap();
  
  /**
   * Maps a host to the number of threads accessing that host.
   */
  //private static HashMap THREADS_PER_HOST_COUNT = new HashMap();
  
  /**
   * Queue of blocked hosts.  This contains all of the non-zero entries
   * from BLOCKED_ADDR_TO_TIME, ordered by increasing time.
   */
  //private static LinkedList BLOCKED_ADDR_QUEUE = new LinkedList();
  
  /** Do we block by IP addresses or by hostnames? */
  //private boolean byIP = true;
 
  /** Do we use HTTP/1.1? */
  protected boolean useHttp11 = false;
  
  /** Skip page if Crawl-Delay longer than this value. */
  protected long maxCrawlDelay = -1L;

  /** Plugin should handle host blocking internally. */
  protected boolean checkBlocking = true;
  
  /** Plugin should handle robot rules checking internally. */
  protected boolean checkRobots = true;
  
  private URI uri = null;
  private byte[] content = null;
  private int code = 0;
  private Map<String, String> headers = new HashMap<String, String>(); 
  
  public SimpleHttp() {
	  
  }
  
  public void reset() {
    this.uri = null; 
    this.content = null; 
    this.code = 0; 
    this.headers.clear(); 
  }
  
  public void saveTo(String filename) throws IOException {
    if (filename == null || content == null) 
      return; 
      
    FileOutputStream fos = new FileOutputStream(filename); 
    fos.write(content); 
    fos.flush(); 
    fos.close(); 
  }
  
  public void fetch(String uri) throws IOException {
    try {
    	fetch(new URI(uri)); 
    } catch (URISyntaxException e) {
      throw new IOException(e.toString()); 
    }
  }
  
  public void fetch(URI uri) throws IOException {
    this.uri = uri; 
    
    if (!"http".equals(uri.getScheme()))
    	throw new IOException("Not an HTTP url:" + uri);
	
	URL url = uri.toURL(); 
	String path = "".equals(url.getFile()) ? "/" : url.getFile();
	
	// some servers will redirect a request with a host line like
	// "Host: <hostname>:80" to "http://<hpstname>/<orig_path>"- they
	// don't want the :80...
	
	String host = url.getHost();
	int port;
	String portString;
	if (url.getPort() == -1) {
	  port= 80;
	  portString= "";
	} else {
	  port= url.getPort();
	  portString= ":" + port;
	}
	Socket socket = null;
	
	try {
	  socket = new Socket();                    // create the socket
	  socket.setSoTimeout(getTimeout());
	
	  // connect
	  String sockHost = useProxy() ? getProxyHost() : host;
	  int sockPort = useProxy() ? getProxyPort() : port;
	  InetSocketAddress sockAddr= new InetSocketAddress(sockHost, sockPort);
	  socket.connect(sockAddr, getTimeout());
	
	  // make request
	  OutputStream req = socket.getOutputStream();
	
	  StringBuilder reqStr = new StringBuilder("GET ");
	  if (useProxy()) {
	  	reqStr.append(url.getProtocol()+"://"+host+portString+path);
	  } else {
	  	reqStr.append(path);
	  }
	
	  reqStr.append(" HTTP/1.0\r\n");
	
	  reqStr.append("Host: ");
	  reqStr.append(host);
	  reqStr.append(portString);
	  reqStr.append("\r\n");
	
	  reqStr.append("Accept-Encoding: x-gzip, gzip\r\n");
	
	  String userAgent = getUserAgent();
	  if ((userAgent == null) || (userAgent.length() == 0)) {
	    //log("User-agent is not set!"); 
	  } else {
	    reqStr.append("User-Agent: ");
	    reqStr.append(userAgent);
	    reqStr.append("\r\n");
	  }
	
	  reqStr.append("\r\n");
	  byte[] reqBytes= reqStr.toString().getBytes();
	
	  req.write(reqBytes);
	  req.flush();
	    
	  PushbackInputStream in =                  // process response
	    new PushbackInputStream(
	      new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE), 
	      BUFFER_SIZE) ;
	
	  StringBuilder line = new StringBuilder();
	
	  boolean haveSeenNonContinueStatus= false;
	  while (!haveSeenNonContinueStatus) {
	    // parse status code line
	    this.code = parseStatusLine(in, line); 
	    // parse headers
	    parseHeaders(in, line);
	    haveSeenNonContinueStatus= code != 100; // 100 is "Continue"
	  }
	
	  readPlainContent(in);
	
	  String contentEncoding = getHeader(CONTENT_ENCODING);
	  if ("gzip".equals(contentEncoding) || "x-gzip".equals(contentEncoding)) {
	    //content = http.processGzipEncoded(content, url);
	    //log("sip not supported."); 
	  } else {
	    //log("fetched " + content.length + " bytes from " + url);
	  }

    } finally {
      if (socket != null)
        socket.close();
    }
  }
  
  private void readPlainContent(InputStream in) 
    throws IOException, IOException {

    int contentLength = Integer.MAX_VALUE;    // get content length
	String contentLengthString = headers.get(CONTENT_LENGTH);
	if (contentLengthString != null) {
	  contentLengthString = contentLengthString.trim();
	  try {
	    contentLength = Integer.parseInt(contentLengthString);
	  } catch (NumberFormatException e) {
	    throw new IOException("bad content length: "+contentLengthString);
	  }
	}
	if (getMaxContent() >= 0
	  && contentLength > getMaxContent())   // limit download size
	  contentLength  = getMaxContent();
	
	ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
	byte[] bytes = new byte[BUFFER_SIZE];
	int length = 0;                           // read content
    for (int i = in.read(bytes); i != -1; i = in.read(bytes)) {
      out.write(bytes, 0, i);
      length += i;
      if (length >= contentLength)
        break;
    }
    content = out.toByteArray();
  }

  private void readChunkedContent(PushbackInputStream in, StringBuilder line) 
    	throws IOException, IOException {
    boolean doneChunks = false;
    int contentBytesRead = 0;
    byte[] bytes = new byte[BUFFER_SIZE];
    ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);

    while (!doneChunks) {
      readLine(in, line, false);
      String chunkLenStr;

      int pos= line.indexOf(";");
	  if (pos < 0) {
	    chunkLenStr= line.toString();
	  } else {
	    chunkLenStr= line.substring(0, pos);
	  }
	  chunkLenStr= chunkLenStr.trim();
	  int chunkLen;
	  try {
	    chunkLen= Integer.parseInt(chunkLenStr, 16);
	  } catch (NumberFormatException e){ 
	    throw new IOException("bad chunk length: "+line.toString());
	  }
	
	  if (chunkLen == 0) {
	    doneChunks= true;
	    break;
	  }
	
	  if ( (contentBytesRead + chunkLen) > getMaxContent() )
	    chunkLen= getMaxContent() - contentBytesRead;
	
	  // read one chunk
	  int chunkBytesRead= 0;
	  while (chunkBytesRead < chunkLen) {
	
	    int toRead= (chunkLen - chunkBytesRead) < BUFFER_SIZE ?
	                (chunkLen - chunkBytesRead) : BUFFER_SIZE;
	    int len= in.read(bytes, 0, toRead);
	
	    if (len == -1) 
	      throw new IOException("chunk eof after " + contentBytesRead
	                                  + " bytes in successful chunks"
	                                  + " and " + chunkBytesRead 
	                                  + " in current chunk");
	
	    // DANGER!!! Will printed GZIPed stuff right to your
	    // terminal!
	
	    out.write(bytes, 0, len);
	    chunkBytesRead+= len;  
	  }
	
	  readLine(in, line, false);

    }

    if (!doneChunks) {
      if (contentBytesRead != getMaxContent()) 
    	throw new IOException("chunk eof: !doneChunk && didn't max out");
      return;
    }

    content = out.toByteArray();
    parseHeaders(in, line);
  }

  private int parseStatusLine(PushbackInputStream in, StringBuilder line)
    throws IOException {
    readLine(in, line, false);

    int codeStart = line.indexOf(" ");
    int codeEnd = line.indexOf(" ", codeStart+1);

	// handle lines with no plaintext result code, ie:
	// "HTTP/1.1 200" vs "HTTP/1.1 200 OK"
	if (codeEnd == -1) 
	  codeEnd= line.length();

	int code;
	try {
	  code= Integer.parseInt(line.substring(codeStart+1, codeEnd));
	} catch (NumberFormatException e) {
	  throw new IOException("bad status line '" + line 
	                          + "': " + e.getMessage());
    }

    return code;
  }

  private void processHeaderLine(StringBuilder line)
    throws IOException {

    int colonIndex = line.indexOf(":");       // key is up to colon
	if (colonIndex == -1) {
	  int i;
	  for (i= 0; i < line.length(); i++)
	    if (!Character.isWhitespace(line.charAt(i)))
	      break;
	  if (i == line.length())
	    return;
	  throw new IOException("No colon in header:" + line);
	}
	String key = line.substring(0, colonIndex);
	
	int valueStart = colonIndex+1;            // skip whitespace
	while (valueStart < line.length()) {
	  int c = line.charAt(valueStart);
	  if (c != ' ' && c != '\t')
        break;
      valueStart++;
    }
    String value = line.substring(valueStart);
    headers.put(key, value);
  }

  // Adds headers to our headers Metadata
  private void parseHeaders(PushbackInputStream in, StringBuilder line)
    throws IOException, IOException {

    while (readLine(in, line, true) != 0) {

      // handle HTTP responses with missing blank line after headers
	  int pos;
	  if ( ((pos= line.indexOf("<!DOCTYPE")) != -1) ||
	       ((pos= line.indexOf("<HTML")) != -1) ||
	       ((pos= line.indexOf("<html")) != -1) ) {
	
	    in.unread(line.substring(pos).getBytes(getCharacterEncoding()));
	    line.setLength(pos);
	
	    try {
	        //TODO: (CM) We don't know the header names here
	        //since we're just handling them generically. It would
	        //be nice to provide some sort of mapping function here
	        //for the returned header names to the standard metadata
	        //names in the ParseData class
	      processHeaderLine(line);
	    } catch (Exception e) {
	      // fixme:
          e.printStackTrace();
        }
        return;
      }

      processHeaderLine(line);
    }
  }

  private static int readLine(PushbackInputStream in, StringBuilder line,
                      boolean allowContinuedLine)
    throws IOException {
    line.setLength(0);
    for (int c = in.read(); c != -1; c = in.read()) {
      switch (c) {
        case '\r':
      if (peek(in) == '\n') {
        in.read();
      }
    case '\n': 
      if (line.length() > 0) {
        // at EOL -- check for continued line if the current
        // (possibly continued) line wasn't blank
        if (allowContinuedLine) 
          switch (peek(in)) {
            case ' ' : case '\t':                   // line is continued
              in.read();
              continue;
          }
      }
      return line.length();      // else complete
        default :
          line.append((char)c);
      }
    }
    throw new EOFException();
  }

  private static int peek(PushbackInputStream in) throws IOException {
    int value = in.read();
    in.unread(value);
    return value;
  }
  
  public static String getCharacterEncoding() {
    String encoding = System.getProperty("file.encoding"); 
    if (encoding == null || encoding.length() == 0) 
    	encoding = System.getProperty("sun.jnu.encoding"); 
    if (encoding == null || encoding.length() == 0) 
    	encoding = "UTF-8"; 
    return encoding; 
  }

  public URI getUri() {
    return uri; 
  }

  public int getCode() {
    return code;
  }

  public String getHeader(String name) {
    return headers.get(name);
  }
  
  public Map<String, String> getHeaders() {
    return headers;
  }

  public byte[] getContent() {
    return content;
  }

  public String getProxyHost() {
    return proxyHost;
  }

  public int getProxyPort() {
    return proxyPort;
  }

  public boolean useProxy() {
    return useProxy;
  }

  public int getTimeout() {
    return timeout;
  }
  
  public int getMaxContent() {
    return maxContent;
  }

  public int getMaxDelays() {
    return maxDelays;
  }
  
  public String getUserAgent() {
    return userAgent;
  }
  
  private static String getAgentString(String agentName,
                                       String agentVersion,
                                       String agentDesc,
                                       String agentURL,
                                       String agentEmail) {
    
    if ( (agentName == null) || (agentName.trim().length() == 0) ) {
      //log("No User-Agent string set (http.agent.name)!");
    }

    StringBuilder buf= new StringBuilder();
	
	buf.append(agentName);
	if (agentVersion != null) {
	  buf.append("/");
	  buf.append(agentVersion);
	}
	if ( ((agentDesc != null) && (agentDesc.length() != 0))
	  || ((agentEmail != null) && (agentEmail.length() != 0))
	  || ((agentURL != null) && (agentURL.length() != 0)) ) {
	  buf.append(" (");
	  
	  if ((agentDesc != null) && (agentDesc.length() != 0)) {
	    buf.append(agentDesc);
	    if ( (agentURL != null) || (agentEmail != null) )
	      buf.append("; ");
	  }
	  
	  if ((agentURL != null) && (agentURL.length() != 0)) {
	    buf.append(agentURL);
	    if (agentEmail != null)
	      buf.append("; ");
	  }
	  
	  if ((agentEmail != null) && (agentEmail.length() != 0))
	    buf.append(agentEmail);
	  
	  buf.append(")");
    }
    return buf.toString();
  }
}
