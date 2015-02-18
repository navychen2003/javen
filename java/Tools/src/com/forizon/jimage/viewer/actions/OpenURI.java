package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import com.forizon.jimage.viewer.imagelist.wrapspi.WrapException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

public class OpenURI extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public OpenURI(Context aContext) {
        super(aContext);
    }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_U);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control U"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/internet.png")));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String uri = JOptionPane.showInputDialog(context.getWindow(), context.getLanguageSupport().localizeString(this.getClass().getName() + ".message"), "http://");
        if (uri != null) {
            try {
                context.getImageContext().setImage(new URI(uri));
            } catch (URISyntaxException ex) {
                context.getReporter().report(ex);
            } catch (WrapException ex) {
                context.getReporter().report(ex);
            }
        }
    }
}

