package com.forizon.jimage.viewer.context;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultReporter implements Reporter {
    Context context;
    Logger logger;

    public DefaultReporter(Context context, Logger logger) {
        this.context = context;
        this.logger = logger;
    }

    public void report(Level level, String e) {
        logger.log(level, e);
    }

    public void report(Throwable e) {
        Level level = null;
        if (e instanceof Exception && !(e instanceof RuntimeException)) {
            level = Level.SEVERE;
        } else if (e instanceof RuntimeException) {
            level = Level.WARNING;
        } else {
            level = Level.INFO;
        }
        logger.log(level, e.getClass().getSimpleName(), e);
    }
}

