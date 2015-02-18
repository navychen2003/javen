package com.forizon.jimage.viewer.context;

import com.forizon.jimage.viewer.actions.ActionsBuilder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FullScreenMouseListener extends MouseAdapter {
    final Context context;
    public FullScreenMouseListener(Context aContext) {
        context = aContext;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            context.getActions().get(ActionsBuilder.VIEW_MODE_GROUP + "-FullScreen")
                    .actionPerformed(null);
        }
    }
}
