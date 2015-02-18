package org.javenstudio.raptor.bigdb.util;

import java.io.IOException;
import java.net.URL;

/**
 * Create a Jetty embedded server to answer http requests. The primary goal
 * is to serve up status information for the server.
 * There are three contexts:
 *   "/stacks/" -> points to stack trace
 *   "/static/" -> points to common static files (src/bigdb-webapps/static)
 *   "/" -> the jsp server code from (src/bigdb-webapps/<name>)
 */
public class InfoServer /*extends HttpServer*/ {
  /**
   * Create a status server on the given port.
   * The jsp scripts are taken from src/bigdb-webapps/<code>name<code>.
   * @param name The name of the server
   * @param bindAddress address to bind to
   * @param port The port to use on the server
   * @param findPort whether the server should start at the given port and
   * increment by 1 until it finds a free port.
   * @throws IOException e
   */
  public InfoServer(String name, String bindAddress, int port, boolean findPort)
  throws IOException {
    //super(name, bindAddress, port, findPort);
    //webServer.addHandler(new ContextHandlerCollection());
  }

  /*
  protected void addDefaultApps(ContextHandlerCollection parent, String appDir)
  throws IOException {
    super.addDefaultApps(parent, appDir);
    // Must be same as up in hadoop.
    final String logsContextPath = "/logs";
    // Now, put my logs in place of hadoops... disable old one first.
    Context oldLogsContext = null;
    for (Map.Entry<Context, Boolean> e : defaultContexts.entrySet()) {
      if (e.getKey().getContextPath().equals(logsContextPath)) {
        oldLogsContext = e.getKey();
        break;
      }
    }
    if (oldLogsContext != null) {
      this.defaultContexts.put(oldLogsContext, Boolean.FALSE);
    }
    // Now do my logs.
    // set up the context for "/logs/" if "hadoop.log.dir" property is defined.
    String logDir = System.getProperty("bigdb.log.dir");
    if (logDir != null) {
      Context logContext = new Context(parent, "/logs");
      logContext.setResourceBase(logDir);
      logContext.addServlet(DefaultServlet.class, "/");
      defaultContexts.put(logContext, true);
    }
  }*/

  /**
   * Get the pathname to the <code>path</code> files.
   * @return the pathname as a URL
   */
  protected String getWebAppsPath() throws IOException {
    // Hack: webapps is not a unique enough element to find in CLASSPATH
    // We'll more than likely find the hadoop webapps dir.  So, instead
    // look for the 'master' webapp in the webapps subdir.  That should
    // get us the bigdb context.  Presumption is that place where the
    // master webapp resides is where we want this InfoServer picking up
    // web applications.
    final String master = "master";
    String p = getWebAppDir(master);
    // Now strip master + the separator off the end of our context
    return p.substring(0, p.length() - (master.length() + 1/* The separator*/));
  }

  private static String getWebAppsPath(final String path)
  throws IOException {
    URL url = InfoServer.class.getClassLoader().getResource(path);
    if (url == null)
      throw new IOException("bigdb-webapps not found in CLASSPATH: " + path);
    return url.toString();
  }

  public void setAttribute(String name, Object val) {}
  
  public void start() {}
  public void stop() {}
  
  /**
   * Get the path for this web app
   * @param webappName web app
   * @return path
   * @throws IOException e
   */
  public static String getWebAppDir(final String webappName)
  throws IOException {
    String webappDir;
    webappDir = getWebAppsPath("bigdb-webapps/" + webappName);
    return webappDir;
  }
}

