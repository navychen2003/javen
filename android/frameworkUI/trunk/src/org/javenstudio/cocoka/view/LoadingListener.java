package org.javenstudio.cocoka.view;

public interface LoadingListener {
    public void onLoadingStarted();
    /**
     * Called when loading is complete or no further progress can be made.
     *
     * @param loadingFailed true if data source cannot provide requested data
     */
    public void onLoadingFinished(boolean loadingFailed);
}
