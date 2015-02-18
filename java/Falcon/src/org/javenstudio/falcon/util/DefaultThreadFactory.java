package org.javenstudio.falcon.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultThreadFactory implements ThreadFactory {
	
    private static final AtomicInteger sPoolNumber = new AtomicInteger(1);
    
    private final ThreadGroup mGroup;
    private final AtomicInteger mThreadNumber = new AtomicInteger(1);
    private final String mPrefix;

    public DefaultThreadFactory(String namePrefix) {
        SecurityManager s = System.getSecurityManager();
        mGroup = (s != null)? s.getThreadGroup() :
                             Thread.currentThread().getThreadGroup();
        mPrefix = namePrefix + "-" + sPoolNumber.getAndIncrement() + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(mGroup, r,
                              mPrefix + mThreadNumber.getAndIncrement(),
                              0);

        t.setDaemon(false);
        
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        
        return t;
    }
    
}
