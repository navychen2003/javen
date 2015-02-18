package org.javenstudio.lightning.core.tray;

import java.awt.Desktop;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.util.Shell;
import org.javenstudio.raptor.util.Strings;

public class TrayHelper {
	private static final Logger LOG = Logger.getLogger(TrayHelper.class);

	public static final String VERSION = "0.1.0";
	public static final String WEBSITE = "http://www.anybox.org";
	public static final String HELPURL = "http://www.anybox.org";
	public static final String COPYRIGHT = "Copyright (c) 2009-2014 Javen-Studio";
	public static final String STATUS = "Javen-Studio (c) 2009-2014";
	public static final String AUTHOR = "Naven Chan";
	
	public static final String ABOUT_TITLE = "Anybox Host";
	public static final String ABOUT_TEXT = "Copyright (C) 2013 Javen-Studio\n\n     This program comes with ABSOLUTELY NO WARRANTY.\n\n     This program is free software: you can redistribute it and/or modify\n     it under the terms of the GNU General Public License as published by\n     the Free Software Foundation, either version 3 of the License, or\n     (at your option) any later version.";
	
	public static void initTray(Configuration conf, String tooltip) throws ErrorException {
		if (LOG.isDebugEnabled()) LOG.debug("initTray: tooltip=" + tooltip);
		
		try {
			if (!SystemTray.isSupported() || !conf.getBoolean("lightning.systemtray.enabled", true)) 
				return;
			
			final PopupMenu popup = new PopupMenu();
			final TrayIcon trayIcon = Options.getTrayIcon();
			final SystemTray tray = SystemTray.getSystemTray();
			
			MenuItem aboutItem = new MenuItem(Strings.get("About"));
			MenuItem openItem = new MenuItem(Strings.get("Open"));
			//MenuItem jfmItem = new MenuItem(Strings.get("File Manager"));
			MenuItem exitItem = new MenuItem(Strings.get("Exit"));
			
			aboutItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						about_actionPerformed(e);
					}
				});
			
			exitItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						exit_actionPerformed(e);
					}
				});
			
			openItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						open_actionPerformed(e);
					}
				});
			
			popup.add(aboutItem);
			popup.addSeparator();
			popup.add(openItem);
			popup.addSeparator();
			popup.add(exitItem);
			
			trayIcon.setPopupMenu(popup);
			trayIcon.setToolTip(tooltip);
			
			tray.add(trayIcon);
		} catch (Throwable e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	private static void about_actionPerformed(ActionEvent event) {
	    TrayAbout dlg = new TrayAbout(); 
	    dlg.setLocationRelativeTo(null); 
	    dlg.setVisible(true);
	}
	
	private static void exit_actionPerformed(ActionEvent event) {
		try {
			Shell.execCommand(System.getProperty("lightning.stopcommand.file"));
		} catch (Throwable e) {
			if (LOG.isErrorEnabled())
				LOG.error("exit_actionPerformed: error: " + e, e);
		}
	}
	
	private static void open_actionPerformed(ActionEvent event) {
		try {
			Desktop.getDesktop().browse(new URI(System.getProperty("lightning.website.uri")));
		} catch (Throwable e) {
			if (LOG.isErrorEnabled())
				LOG.error("open_actionPerformed: error: " + e, e);
		}
	}
	
}
