package org.javenstudio.falcon.util.job;

import java.util.Map;

// A Job is like a Callable, but it has an addition JobContext parameter.
public interface Job<T> {
    public T run(JobContext jc);
    public String getName();
    public String getUser();
    public String getMessage();
    public Map<String, String> getStatusMessages();
    public void onCancel();
}
