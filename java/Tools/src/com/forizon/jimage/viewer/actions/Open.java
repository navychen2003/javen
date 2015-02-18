package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import com.forizon.jimage.viewer.file.ImageFileFilter;
import com.forizon.jimage.viewer.imagelist.ImageIdentity;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class Open extends AbstractAction {
	private static final long serialVersionUID = 1L;
	
	protected JFileChooser chooser;

    public Open(Context aContext) {
        super(aContext);
    }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_O);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control O"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/document-open.png")));
    }

	@Override
    public void actionPerformed(ActionEvent e) {
        if (chooser == null) {
            chooser = new JFileChooser();
            chooser.setFileFilter(ImageFileFilter.getInstanceAllowDirectory());
        }
        ImageIdentity imageIdentity = context.getImageContext().getLastImageIdentity();
        if (imageIdentity != null
          && ((ImageIdentity)imageIdentity).getWrappedType().isAssignableFrom(File.class)) {
            chooser.setSelectedFile(((ImageIdentity<File>)imageIdentity).getWrapped());
        }
        if (chooser.showOpenDialog(context.getWindow()) == JFileChooser.APPROVE_OPTION) {
            try {
                context.getImageContext().setImage(chooser.getSelectedFile());
            } catch (Exception ex) {
                context.getReporter().report(ex);
            }
        }
    }
}
