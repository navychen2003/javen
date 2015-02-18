package org.javenstudio.lightning.handler.system;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Locale;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class ThreadDumpHandler extends AdminHandlerBase {

	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		checkAuth(req, IUserClient.Op.ACCESS);
		
		NamedMap<Object> system = new NamedMap<Object>();
	    rsp.add("system", system);

	    ThreadMXBean tmbean = ManagementFactory.getThreadMXBean();
	    
	    // Thread Count
	    NamedMap<Object> nl = new NamedMap<Object>();
	    nl.add("current", tmbean.getThreadCount());
	    nl.add("peak", tmbean.getPeakThreadCount());
	    nl.add("daemon", tmbean.getDaemonThreadCount());
	    system.add("threadCount", nl);
	    
	    // Deadlocks
	    ThreadInfo[] tinfos;
	    long[] tids = tmbean.findMonitorDeadlockedThreads();
	    if (tids != null) {
	    	tinfos = tmbean.getThreadInfo(tids, Integer.MAX_VALUE);
	    	NamedList<NamedMap<Object>> lst = new NamedList<NamedMap<Object>>();
	    	for (ThreadInfo ti : tinfos) {
	    		if (ti != null) 
	    			lst.add("thread", getThreadInfo(ti, tmbean));
	    	}
	    	system.add("deadlocks", lst);
	    }
	    
	    // Now show all the threads....
	    tids = tmbean.getAllThreadIds();
	    tinfos = tmbean.getThreadInfo(tids, Integer.MAX_VALUE);
	    NamedList<NamedMap<Object>> lst = new NamedList<NamedMap<Object>>();
	    for (ThreadInfo ti : tinfos) {
	    	if (ti != null) 
	    		lst.add("thread", getThreadInfo(ti, tmbean));
	    }
	    system.add("threadDump", lst);
	    //rsp.setHttpCaching(false);
	}

	private static NamedMap<Object> getThreadInfo(ThreadInfo ti, ThreadMXBean tmbean) {
		NamedMap<Object> info = new NamedMap<Object>();
		long tid = ti.getThreadId();

		info.add("id", tid);
		info.add("name", ti.getThreadName());
		info.add("state", ti.getThreadState().toString());
    
		if (ti.getLockName() != null) 
			info.add("lock", ti.getLockName());
		
		if (ti.isSuspended()) 
			info.add("suspended", true);
		
		if (ti.isInNative()) 
			info.add("native", true);
		
		if (tmbean.isThreadCpuTimeSupported()) {
			info.add("cpuTime", formatNanos(tmbean.getThreadCpuTime(tid)));
			info.add("userTime", formatNanos(tmbean.getThreadUserTime(tid)));
		}

		if (ti.getLockOwnerName() != null) {
			NamedMap<Object> owner = new NamedMap<Object>();
			owner.add("name", ti.getLockOwnerName());
			owner.add("id", ti.getLockOwnerId());
		}
    
		// Add the stack trace
		int i = 0;
		String[] trace = new String[ti.getStackTrace().length];
		for (StackTraceElement ste : ti.getStackTrace()) {
			trace[i++] = ste.toString();
		}
		info.add("stackTrace", trace);
		
		return info;
	}
  
	private static String formatNanos(long ns) {
		return String.format(Locale.ROOT, "%.4fms", ns / (double) 1000000);
	}
	
}
