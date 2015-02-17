package org.javenstudio.cocoka.widget.setting;

public interface OnDependencyChangeListener {
    /**
     * Called when this preference has changed in a way that dependents should
     * care to change their state.
     * 
     * @param disablesDependent Whether the dependent should be disabled.
     */
    void onDependencyChanged(Setting dependency, boolean disablesDependent);
}
