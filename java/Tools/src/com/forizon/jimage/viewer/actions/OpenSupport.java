package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.JImageView;
import com.forizon.jimage.viewer.context.Context;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class OpenSupport extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public OpenSupport(Context aContext) {
        super(aContext);
     }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Desktop.getDesktop().browse(new URI(context.getProperties().getProperty(JImageView.APPLICATION_SUPPORT)));
        }
        catch (IOException ex) {
            context.getReporter().report(ex);
        }
        catch (URISyntaxException ex) {
            context.getReporter().report(ex);
        }
    }
}

