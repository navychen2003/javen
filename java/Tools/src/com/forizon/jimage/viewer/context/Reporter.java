package com.forizon.jimage.viewer.context;

import java.util.logging.Level;

public interface Reporter {

    void report(Level level, String e);

    void report(Throwable e);

}

