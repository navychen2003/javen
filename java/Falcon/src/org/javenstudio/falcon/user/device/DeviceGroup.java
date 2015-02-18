package org.javenstudio.falcon.user.device;

import java.util.HashSet;
import java.util.Set;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.setting.SettingCategory;
import org.javenstudio.falcon.setting.SettingConf;
import org.javenstudio.falcon.setting.SettingGroup;
import org.javenstudio.falcon.setting.SettingTable;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.IParams;

public final class DeviceGroup extends SettingConf 
		implements SettingGroup.UpdateHandler, SettingTable.SettingRowFactory {
	private static final Logger LOG = Logger.getLogger(DeviceGroup.class);

	private final Set<SettingGroup> mGroups = new HashSet<SettingGroup>();
	private final DeviceType mType;
	
	DeviceGroup(DeviceManager setting, DeviceType type) 
			throws ErrorException { 
		super(setting);
		if (type == null) throw new NullPointerException();
		mType = type;
		initSettings(setting);
	}
	
	private void initSettings(DeviceManager setting) throws ErrorException { 
		SettingCategory category = setting.createCategory(DEVICE_NAME);
		category.setTitle("Client Device");
		
		if (true) {
			SettingGroup group = getType().initDeviceSetting(category, this);
			mGroups.add(group);
		}
	}
	
	public DeviceManager getManager() { 
		return (DeviceManager)getSettingManager(); 
	}
	
	public DeviceType getType() { return mType; }
	
	synchronized final Device addDevice(Device dev) throws ErrorException { 
		if (dev == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("addDevice: device=" + dev);
		
		DeviceTable.addDevice(getManager(), dev);
		return dev;
	}
	
	public synchronized Device getDevice(String key) throws ErrorException {
		if (key == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("getDevice: key=" + key);
		
		return DeviceTable.getDevice(this, key);
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("close: type=" + getType().getName());
	}
	
	@Override
	public Device createRow(IDatabase.Result res) throws ErrorException {
		if (res == null) return null;
		
		String key = SettingTable.getAttrString(res, SettingTable.KEY_QUALIFIER);
		String name = SettingTable.getAttrString(res, SettingTable.NAME_QUALIFIER);
		String ver = SettingTable.getAttrString(res, SettingTable.VERSION_QUALIFIER);
		String category = SettingTable.getAttrString(res, SettingTable.CATEGORY_QUALIFIER);
		
		if (key == null || key.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Wrong device key");
		}
		
		if (name == null || ver == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Wrong device name and version");
		}
		
		if (category == null || !category.equals(getType().getName())) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Wrong device type: " + category);
		}
		
		Device dev = getType().newDevice(key, name, ver);
		if (dev != null) 
			dev.getFields(res);
		
		if (LOG.isDebugEnabled())
			LOG.debug("createRow: device=" + dev);
		
		return dev;
	}
	
	private static final String DEVICE_NAME = "device";
	
	@Override
	protected final void loadSetting(String categoryName, String groupName, 
			String name, Object val) throws ErrorException { 
		if (categoryName.equals(DEVICE_NAME)) { 
			if (groupName.equals(getType().getName())) { 
				getType().loadDeviceSetting(name, val);
			}
		}
	}
	
	@Override
	public synchronized final boolean updateSetting(SettingGroup group, 
			Object input) throws ErrorException {
		if (group == null || input == null)
			throw new NullPointerException();
		
		getManager().getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
		try {
			if (!mGroups.contains(group)) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Update failed with wrong handler");
			}
			
			if (!(input instanceof IParams)) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Update failed with wrong input");
			}
			
			IParams params = (IParams)input;
			if (getType().getName().equals(group.getName())) {
				return getType().updateDeviceSetting(group, params);
			
			} else { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Setting group update not implements");
			}
		} finally { 
			getManager().getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
}
