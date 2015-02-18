package org.javenstudio.lightning.handler.system;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.Locale;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.util.FsUtils;
import org.javenstudio.falcon.util.IOUtils;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.Constants;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

/**
 * This handler returns system info
 * 
 */
public final class SystemInfoHandler extends AdminHandlerBase {
	private static Logger LOG = Logger.getLogger(SystemInfoHandler.class);

	private final Core mCore;
	
	// on some platforms, resolving canonical hostname can cause the thread
	// to block for several seconds if nameservices aren't available
	// so resolve this once per handler instance 
	//(ie: not static, so core reload will refresh)
	private final String mHostName;

	public SystemInfoHandler(Core core) {
		mCore = core;
		mHostName = FsUtils.getHostName();
		
		if (core == null) 
			throw new NullPointerException();
	}

	public final Core getCore() { return mCore; }
	
	@Override
	public final void handleRequestBody(Request req, Response rsp) 
			throws ErrorException {
		checkAuth(req, IUserClient.Op.ACCESS);
		
		Core core = getCore();
		rsp.add("core", getCoreInfo(core));
		rsp.add("indexdb", getVersionInfo());
		rsp.add("jvm", getJvmInfo());
		rsp.add("system", getSystemInfo());
		//rsp.setHttpCaching(false);
	}
	
	/** Get core info */
	protected NamedMap<Object> getCoreInfo(Core core) throws ErrorException {
		NamedMap<Object> info = new NamedMap<Object>();
		if (core != null) {
			info.add("host", mHostName);
			info.add("now", new Date());
			info.add("start", new Date(core.getStartTime()));
	
			NamedMap<Object> dirs = getDirectoryInfo(core);
			info.add("directory", dirs);
			
			core.getCoreInfo(info);
		}
		
		return info;
	}
  
	protected NamedMap<Object> getDirectoryInfo(Core core) throws ErrorException {
		NamedMap<Object> dirs = new NamedMap<Object>();
		if (core != null) {
			dirs.add("cwd" , new File(System.getProperty("user.dir")).getAbsolutePath());
			dirs.add("instance", new File(core.getContextLoader().getInstanceDir()).getAbsolutePath());
			dirs.add("data", new File(core.getDataDir()).getAbsolutePath());
			
			core.getDirectoryInfo(dirs);
		}
		
		return dirs;
	}
	
	/** Get system info */
	protected NamedMap<Object> getSystemInfo() {
		NamedMap<Object> info = new NamedMap<Object>();
    
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		info.add("name", os.getName());
		info.add("version", os.getVersion());
		info.add("arch", os.getArch());
		info.add("systemLoadAverage", os.getSystemLoadAverage());

		// com.sun.management.OperatingSystemMXBean
		addGetterIfAvaliable(os, "committedVirtualMemorySize", info);
		addGetterIfAvaliable(os, "freePhysicalMemorySize", info);
		addGetterIfAvaliable(os, "freeSwapSpaceSize", info);
		addGetterIfAvaliable(os, "processCpuTime", info);
		addGetterIfAvaliable(os, "totalPhysicalMemorySize", info);
		addGetterIfAvaliable(os, "totalSwapSpaceSize", info);

		// com.sun.management.UnixOperatingSystemMXBean
		addGetterIfAvaliable(os, "openFileDescriptorCount", info );
		addGetterIfAvaliable(os, "maxFileDescriptorCount", info );

		try { 
			if(!os.getName().toLowerCase(Locale.ROOT).startsWith("windows")) {
				// Try some command line things
				info.add("uname", execute("uname -a"));
				info.add("uptime", execute("uptime"));
			}
		} catch(Throwable ex) {
			if (LOG.isWarnEnabled())
				LOG.warn(ex.toString(), ex);
		}
		
		return info;
	}
  
	/**
	 * Try to run a getter function.  This is useful because java 1.6 has a few extra
	 * useful functions on the <code>OperatingSystemMXBean</code>
	 * 
	 * If you are running a sun jvm, there are nice functions in:
	 * UnixOperatingSystemMXBean and com.sun.management.OperatingSystemMXBean
	 * 
	 * it is package protected so it can be tested...
	 */
	protected void addGetterIfAvaliable(Object obj, String getter, NamedList<Object> info) {
		// This is a 1.6 function, so lets do a little magic to *try* to make it work
		try {
			String n = Character.toUpperCase(getter.charAt(0)) + getter.substring(1);
			Method m = obj.getClass().getMethod("get" + n);
			m.setAccessible(true);
			
			Object v = m.invoke(obj, (Object[])null);
			if (v != null) 
				info.add(getter, v);
			
		} catch (Exception ex) { 
			// don't worry, this only works for 1.6
		}
	}
  
	/**
	 * Utility function to execute a function
	 */
	public static String execute(String cmd) {
		DataInputStream in = null;
		Process process = null;
    
		try {
			process = Runtime.getRuntime().exec(cmd);
			in = new DataInputStream( process.getInputStream() );
			// use default charset from locale here, because the command 
			// invoked also uses the default locale:
			return IOUtils.toString(new InputStreamReader(in, Charset.defaultCharset()));
			
		} catch (Exception ex) {
			// ignore - log.warn("Error executing command", ex);
			return "(error executing: " + cmd + ")";
			
		} finally {
			if (process != null) {
				IOUtils.closeQuietly(process.getOutputStream());
				IOUtils.closeQuietly(process.getInputStream());
				IOUtils.closeQuietly(process.getErrorStream());
			}
		}
	}
  
	/**
	 * Get JVM Info - including memory info
	 */
	protected NamedMap<Object> getJvmInfo() {
		NamedMap<Object> jvm = new NamedMap<Object>();

	    final String javaVersion = System.getProperty("java.specification.version", "unknown"); 
	    final String javaVendor = System.getProperty("java.specification.vendor", "unknown"); 
	    final String javaName = System.getProperty("java.specification.name", "unknown"); 
	    final String jreVersion = System.getProperty("java.version", "unknown");
	    final String jreVendor = System.getProperty("java.vendor", "unknown");
	    final String vmVersion = System.getProperty("java.vm.version", "unknown"); 
	    final String vmVendor = System.getProperty("java.vm.vendor", "unknown"); 
	    final String vmName = System.getProperty("java.vm.name", "unknown"); 
    
	    // Summary Info
	    jvm.add("version", jreVersion + " " + vmVersion);
	    jvm.add("name", jreVendor + " " + vmName );
	    
	    // details
	    NamedMap<Object> java = new NamedMap<Object>();
	    java.add("vendor", javaVendor);
	    java.add("name", javaName);
	    java.add("version", javaVersion);
	    jvm.add("spec", java);
	    
	    NamedMap<Object> jre = new NamedMap<Object>();
	    jre.add("vendor", jreVendor);
	    jre.add("version", jreVersion);
	    jvm.add("jre", jre);
	    
	    NamedMap<Object> vm = new NamedMap<Object>();
	    vm.add("vendor", vmVendor);
	    vm.add("name", vmName);
	    vm.add("version", vmVersion);
	    jvm.add("vm", vm);
	    
		Runtime runtime = Runtime.getRuntime();
		jvm.add("processors", runtime.availableProcessors());
    
		// not thread safe, but could be thread local
		DecimalFormat df = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.ROOT));

		NamedMap<Object> mem = new NamedMap<Object>();
		NamedMap<Object> raw = new NamedMap<Object>();
		
		long free = runtime.freeMemory();
		long max = runtime.maxMemory();
		long total = runtime.totalMemory();
		long used = total - free;
		double percentUsed = ((double)(used)/(double)max)*100;
		
		raw.add("free",  free);
		mem.add("free",  humanReadableUnits(free, df));
		raw.add("total", total);
		mem.add("total", humanReadableUnits(total, df));
		raw.add("max",   max);
		mem.add("max",   humanReadableUnits(max, df));
		raw.add("used",  used);
		mem.add("used",  humanReadableUnits(used, df) + 
				" (%" + df.format(percentUsed) + ")");
		raw.add("used%", percentUsed);

		mem.add("raw", raw);
		jvm.add("memory", mem);

		// JMX properties -- probably should be moved to a different handler
		NamedMap<Object> jmx = new NamedMap<Object>();
		try {
			RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
			jmx.add("bootclasspath", mx.getBootClassPath());
			jmx.add("classpath", mx.getClassPath());

			// the input arguments passed to the Java virtual machine
			// which does not include the arguments to the main method.
			jmx.add("commandLineArgs", mx.getInputArguments());

			jmx.add("startTime", new Date(mx.getStartTime()));
			jmx.add("upTimeMS",  mx.getUptime());

		} catch (Exception e) {
			if (LOG.isWarnEnabled())
				LOG.warn("Error getting JMX properties", e);
		}
		
		jvm.add("jmx", jmx);
		
		return jvm;
	}
  
	protected NamedMap<Object> getVersionInfo() {
		NamedMap<Object> info = new NamedMap<Object>();

		Package lightningPackage = org.javenstudio.lightning.Constants.class.getPackage();
		if (lightningPackage != null) {
			String specVersion = lightningPackage.getSpecificationVersion();
			String implVersion = lightningPackage.getImplementationVersion();
			
			if (specVersion == null || implVersion == null) { 
				specVersion = Constants.SPECIFICATION_VERSION;
				implVersion = Constants.IMPLEMENTS_VERSION;
			}
			
			info.add("lightning-spec-version", specVersion);
			info.add("lightning-impl-version", implVersion);
		}
  
		Package indexdbPackage = org.javenstudio.hornet.Constants.class.getPackage();
		if (indexdbPackage != null) {
			String specVersion = indexdbPackage.getSpecificationVersion();
			String implVersion = indexdbPackage.getImplementationVersion();
			
			if (specVersion == null || implVersion == null) { 
				specVersion = Constants.SPECIFICATION_VERSION;
				implVersion = Constants.IMPLEMENTS_VERSION;
			}
			
			info.add("indexdb-spec-version", specVersion);
			info.add("indexdb-impl-version", implVersion);
		}
		
		return info;
	}
  
	private static final long ONE_KB = 1024;
	private static final long ONE_MB = ONE_KB * ONE_KB;
	private static final long ONE_GB = ONE_KB * ONE_MB;

	/**
	 * Return good default units based on byte size.
	 */
	private static String humanReadableUnits(long bytes, DecimalFormat df) {
		String newSizeAndUnits;

		if (bytes / ONE_GB > 0) {
			newSizeAndUnits = String.valueOf(df.format((float)bytes / ONE_GB)) + " GB";
		} else if (bytes / ONE_MB > 0) {
			newSizeAndUnits = String.valueOf(df.format((float)bytes / ONE_MB)) + " MB";
		} else if (bytes / ONE_KB > 0) {
			newSizeAndUnits = String.valueOf(df.format((float)bytes / ONE_KB)) + " KB";
		} else {
			newSizeAndUnits = String.valueOf(bytes) + " bytes";
		}

		return newSizeAndUnits;
	}
  
}
