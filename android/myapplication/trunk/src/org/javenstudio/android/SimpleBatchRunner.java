package org.javenstudio.android;

import java.util.LinkedList;

import org.javenstudio.common.util.Logger;

public class SimpleBatchRunner {
	private static final Logger LOG = Logger.getLogger(SimpleBatchRunner.class);

	public static abstract class Command implements Runnable { 
		public String getName() { return getClass().getName(); }
		public boolean isRunnableNow() { return true; }
	}
	
	private final LinkedList<Command> mCommands = new LinkedList<Command>();
	
	public synchronized void put(Command cmd) { 
		if (cmd != null) 
			mCommands.add(cmd);
	}
	
	public synchronized void clear() { 
		mCommands.clear();
	}
	
	public synchronized void runAll() { 
		while (mCommands.size() > 0) { 
			Command cmd = mCommands.removeFirst();
			if (cmd == null) continue;
			
			try {
				if (cmd.isRunnableNow()) {
					if (LOG.isDebugEnabled())
						LOG.debug("running " + cmd.getName());
					
					cmd.run();
				}
			} catch (Throwable ex) { 
				if (LOG.isWarnEnabled())
					LOG.warn("run " + cmd.getName() + " error: " + ex.toString(), ex);
			}
		}
	}
	
}
