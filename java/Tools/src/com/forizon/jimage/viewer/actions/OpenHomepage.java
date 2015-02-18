package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.JImageView;
import com.forizon.jimage.viewer.context.Context;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.net.URI;

public class OpenHomepage extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public OpenHomepage(Context aContext) {
        super(aContext);
     }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Desktop.getDesktop().browse(new URI(context.getProperties().getProperty(JImageView.APPLICATION_HOMEPAGE)));
        } catch (Exception ex) {
            context.getReporter().report(ex);
        }
    }
}

