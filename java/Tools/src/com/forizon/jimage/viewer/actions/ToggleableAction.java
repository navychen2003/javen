package com.forizon.jimage.viewer.actions;

import javax.swing.Action;

public interface ToggleableAction extends Action {
    public void setSelected(boolean selected);
    public boolean getSelected();
}

