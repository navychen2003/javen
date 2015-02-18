package org.javenstudio.common.indexdb;

import java.io.IOException;

public class MergeAbortedException extends IOException {
	private static final long serialVersionUID = 1L;
	
	public MergeAbortedException() {
		super("merge is aborted");
    }
	
    public MergeAbortedException(String message) {
      super(message);
    }
    
}