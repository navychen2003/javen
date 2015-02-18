package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import com.forizon.jimage.viewer.imagelist.ImageIdentity;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class Delete extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public Delete(Context aContext) {
        super(aContext);
    }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_DELETE);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("DELETE"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/edit-delete.png")));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int selection = JOptionPane.showConfirmDialog(context.getWindow(),
          context.getLanguageSupport().localizeString(this.getClass().getName()+".message"));
        ImageIdentity imageIdentity = context.getImageContext().getImageIdentity();
        if (selection == JOptionPane.YES_OPTION
          && imageIdentity instanceof ImageIdentity
          && ((ImageIdentity)imageIdentity).getWrappedType().isAssignableFrom(File.class)) {
            File file = ((ImageIdentity<File>)imageIdentity).getWrapped();
            if (file != null && file.canWrite()) {
                try {
                    context.getImageContext().getImageIdentityListModel().remove(imageIdentity);
                    file.delete();
                } catch (Exception ex) {
                    context.getReporter().report(ex);
                }
            }
        }
    }
    
}

