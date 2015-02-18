package org.javenstudio.lightning;

import java.lang.annotation.Annotation;

import org.javenstudio.raptor.VersionAnnotation;
import org.javenstudio.raptor.util.VersionInfo;

public class Constants {

	public static final String VERSION = "0.1.0";
	public static final String RVERSION = "5100";
	public static final String RDATE = "20140601";
	
	public static final String SPECIFICATION_VERSION = VERSION + "." + RVERSION;
	public static final String IMPLEMENTS_VERSION = SPECIFICATION_VERSION + " " + RDATE;
	
	public static final String HTTP_USER_AGENT_NAME = "Lightning";
	public static final String HTTP_USER_AGENT = HTTP_USER_AGENT_NAME + " " + IMPLEMENTS_VERSION;
	
	public static final String APPSERVER_NAME = "jetty";
	public static final String PROJECT = "lightning";
	public static final String PROJECT_BASE = "org.javenstudio." + PROJECT;
	public static final String[] PROJECT_PACKAGES = {"", "context.", 
		"core.", "core.datum.", "core.search.", "core.service", "core.user", 
		"handler.", "handler.admin.", "request.", "request.parser.", "response.", "response.writer.", 
		"servlet.", "util.", "velocity."};
	
	public static final String DEFAULT_CORE_NAME = "core1";
	
	@SuppressWarnings("unused")
	private static final String DEFAULT_LIGHTNING_XML = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
			"<lightning persistent=\"false\">\n" +
			"  <cores adminPath=\"/admin/cores\" defaultCoreName=\"" + DEFAULT_CORE_NAME + "\">\n" +
			"    <indexcore name=\""+ DEFAULT_CORE_NAME + "\" shard=\"${shard:}\" instanceDir=\"core1\" />\n" +
			"  </cores>\n" +
			"  <services></services>\n" +
			"</lightning>";
	
	public static final String CORE_PROPERTIES_FILENAME = "core.properties";
	
	public static final String LIGHTNING_XML_FILENAME = "lightning.xml";
	public static final String SAMPLE_XML_FILENAME = "lightning-sample.xml";
	public static final String CONFIG_XML_FILENAME = "config.xml";
	
	public static final String DEFAULT_HOST_CONTEXT = "lihgtning";
	public static final String DEFAULT_HOST_PORT = "8000";
	
	static { 
		VersionInfo.setVersion(new VersionAnnotation() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return null;
			}

			@Override
			public String version() {
				return VERSION;
			}

			@Override
			public String user() {
				return "root";
			}

			@Override
			public String date() {
				return RDATE;
			}

			@Override
			public String url() {
				return null;
			}

			@Override
			public String revision() {
				return RVERSION;
			}

			@Override
			public String javacversion() {
				return null;
			}
		});
	}
	
	public static String getVersion() { return VersionInfo.getVersion(); }
	public static String getRevision() { return VersionInfo.getRevision(); }
	public static String getDate() { return VersionInfo.getDate(); }
	
}
