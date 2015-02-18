package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import com.forizon.jimage.viewer.imagelist.ImageException;
import com.forizon.jimage.viewer.imagelist.ImageIdentity;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class SaveAs extends AbstractAction {
	private static final long serialVersionUID = 1L;
	
	protected JFileChooser chooser;
	
    public SaveAs(Context aContext) {
        super(aContext);
    }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_V);
        //putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control S"));
        //putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/document-open.png")));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public void actionPerformed(ActionEvent e) {
        if (chooser == null) {
            buildJFileChooser();
        }
		ImageIdentity imageIdentity = context.getImageContext().getImageIdentity();
        if (imageIdentity instanceof ImageIdentity
          && ((ImageIdentity)imageIdentity).getWrappedType().isAssignableFrom(File.class)) {
            chooser.setSelectedFile(((ImageIdentity<File>)imageIdentity).getWrapped());
        }
        if (chooser.showSaveDialog(context.getWindow()) == JFileChooser.APPROVE_OPTION) {
            try {
                Image image = imageIdentity.getImage();
                if (image instanceof RenderedImage) {
                    ImageIO.write((RenderedImage)image,
                                  chooser.getFileFilter().getDescription(),
                                  chooser.getSelectedFile());
                }
            } catch (IOException ex) {
                context.getReporter().report(ex);
            } catch (ImageException ex) {
                context.getReporter().report(ex);
            }
        }
    }

    protected void buildJFileChooser() {
        chooser = new JFileChooser();
        TreeMap<String, Object> map = new TreeMap<String, Object>();
        String[] types = ImageIO.getWriterFormatNames();
        String normalized;
        for (String type: types) {
            normalized = type.toLowerCase();
            if (!map.containsKey(normalized)) {
                map.put(normalized, null);
                chooser.addChoosableFileFilter(new FileNameExtensionFilter(normalized, normalized));
            }
        }
    }
}

