package org.javenstudio.lightning.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingManager;
import org.javenstudio.falcon.util.IOUtils;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.SimpleXMLParser;
import org.javenstudio.falcon.util.SimpleXMLWriter;

public class CoreAdminSetting extends SettingManager {
	private static final Logger LOG = Logger.getLogger(CoreAdminSetting.class);
	
	private final CoreContainers mContainers;
	private final GlobalSetting mGlobal;
	
	public CoreAdminSetting(CoreContainers containers) throws ErrorException { 
		if (containers == null) throw new NullPointerException();
		mContainers = containers;
		mGlobal = new GlobalSetting(this);
	}
	
	public CoreContainers getContainers() { return mContainers; }
	public GlobalSetting getGlobal() { return mGlobal; }
	
	private File getSettingXml() throws ErrorException { 
		File confDir = new File(getContainers().getHomeDir(), "conf");
		if (!confDir.exists())
			confDir.mkdirs();
		
		if (!confDir.exists()) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"confDir not exists");
		}
		
		return new File(confDir, "setting.xml");
	}
	
	@Override
	protected void saveSettingsConf(NamedList<Object> items) 
			throws ErrorException { 
		File settingXml = getSettingXml();
		
		if (LOG.isDebugEnabled())
			LOG.debug("saveSetting: save to " + settingXml);
		
		try { 
			OutputStreamWriter writer = new OutputStreamWriter(
					new FileOutputStream(settingXml), "UTF-8");
			SimpleXMLWriter xmlwriter = new SimpleXMLWriter(
					writer, "setting", true);
			
			if (LOG.isDebugEnabled())
				LOG.debug("saveSetting: " + items);
			
			xmlwriter.write(items);
			xmlwriter.close();
			
			writer.flush();
			writer.close();
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	@Override
	protected NamedList<Object> loadSettingsConf() 
			throws ErrorException { 
		File settingXml = getSettingXml();
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadSetting: loading " + settingXml);
		
		InputStream in = null;
		try { 
			in = new FileInputStream(settingXml);
			SimpleXMLParser parser = new SimpleXMLParser("setting");
			
			NamedList<Object> items = parser.parse(in, "UTF-8");
			
			if (LOG.isDebugEnabled())
				LOG.debug("loadSetting: " + items);
			
			return items;
		} catch (FileNotFoundException ex) {
			return null;
		} finally { 
			IOUtils.closeQuietly(in);
		}
	}
	
}
