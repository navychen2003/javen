package org.javenstudio.falcon.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

/**
 * Three concrete implementations for ContentStream 
 * - one for File/URL/String
 * 
 * @since 1.2
 */
public abstract class ContentStreamBase implements ContentStream
{
  public static final String DEFAULT_CHARSET = "utf-8";
  
  protected String name;
  protected String sourceInfo;
  protected String contentType;
  protected Long size;
  
  //---------------------------------------------------------------------
  //---------------------------------------------------------------------
  
  public static String getCharsetFromContentType( String contentType )
  {
    if( contentType != null ) {
      int idx = contentType.toLowerCase(Locale.ROOT).indexOf( "charset=" );
      if( idx > 0 ) {
        return contentType.substring( idx + "charset=".length() ).trim();
      }
    }
    return null;
  }
  
  //------------------------------------------------------------------------
  //------------------------------------------------------------------------
  
  /**
   * Construct a <code>ContentStream</code> from a <code>URL</code>
   * 
   * This uses a <code>URLConnection</code> to get the content stream
   * @see  URLConnection
   */
  public static class URLStream extends ContentStreamBase
  {
    private final URL url;
    
    public URLStream( URL url ) {
      this.url = url; 
      sourceInfo = "url";
    }

    @Override
    public InputStream getStream() throws IOException {
      URLConnection conn = this.url.openConnection();
      
      contentType = conn.getContentType();
      name = url.toExternalForm();
      size = new Long( conn.getContentLength() );
      return conn.getInputStream();
    }
  }
  
  /**
   * Construct a <code>ContentStream</code> from a <code>File</code>
   */
  public static class FileStream extends ContentStreamBase
  {
    private final File file;
    
    public FileStream( File f ) {
      file = f; 
      
      contentType = null; // ??
      name = file.getName();
      size = file.length();
      sourceInfo = file.toURI().toString();
    }

    @Override
    public String getContentType() {
      if(contentType==null) {
        InputStream stream = null;
        try {
          stream = new FileInputStream(file);
          char first = (char)stream.read();
          if(first == '<') {
            return "application/xml";
          }
          if(first == '{') {
            return "application/json";
          }
        } catch(Exception ex) {
        } finally {
          if (stream != null) try {
            stream.close();
          } catch (IOException ioe) {}
        }
      }
      return contentType;
    }

    @Override
    public InputStream getStream() throws IOException {
      return new FileInputStream( file );
    }
    
    @Override
    public File getFile() {
    	return file;
    }
  }
  

  /**
   * Construct a <code>ContentStream</code> from a <code>String</code>
   */
  public static class StringStream extends ContentStreamBase
  {
    private final String str;
    
    public StringStream( String str ) {
      this.str = str; 
      
      contentType = null;
      name = null;
      size = new Long( str.length() );
      sourceInfo = "string";
    }

    @Override
    public String getContentType() {
      if(contentType==null && str.length() > 0) {
        char first = str.charAt(0);
        if(first == '<') {
          return "application/xml";
        }
        if(first == '{') {
          return "application/json";
        }
        // find a comma? for CSV?
      }
      return contentType;
    }

    @Override
    public InputStream getStream() throws IOException {
      return new ByteArrayInputStream( str.getBytes(DEFAULT_CHARSET) );
    }

    /**
     * If an charset is defined (by the contentType) use that, otherwise 
     * use a StringReader
     */
    @Override
    public Reader getReader() throws IOException {
      String charset = getCharsetFromContentType( contentType );
      return charset == null 
        ? new StringReader( str )
        : new InputStreamReader( getStream(), charset );
    }
  }

  /**
   * Base reader implementation.  If the contentType declares a 
   * charset use it, otherwise use "utf-8".
   */
  public Reader getReader() throws IOException {
    String charset = getCharsetFromContentType( getContentType() );
    return charset == null 
      ? new InputStreamReader( getStream(), DEFAULT_CHARSET )
      : new InputStreamReader( getStream(), charset );
  }

  //------------------------------------------------------------------
  // Getters / Setters for overrideable attributes
  //------------------------------------------------------------------

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public File getFile() {
	  return null;
  }
  
  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public String getSourceInfo() {
    return sourceInfo;
  }

  public void setSourceInfo(String sourceInfo) {
    this.sourceInfo = sourceInfo;
  }
  
  public void close() {}
}
