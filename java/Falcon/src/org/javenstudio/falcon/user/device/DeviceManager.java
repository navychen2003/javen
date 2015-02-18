package org.javenstudio.falcon.user.device;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingManager;
import org.javenstudio.falcon.user.Member;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.NamedList;

public class DeviceManager extends SettingManager {
	private static final Logger LOG = Logger.getLogger(DeviceManager.class);

	private final Member mUser;
	private final IDeviceStore mStore;
	
	private final Map<String,DeviceType> mDeviceTypes = 
			new HashMap<String,DeviceType>();;
	
	private final Map<String, DeviceGroup> mDeviceGroups = 
			new HashMap<String, DeviceGroup>();
	
	private volatile boolean mLoaded = false;
	private volatile boolean mClosed = false;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return DeviceManager.this.getUser().getLock();
			}
			@Override
			public String getName() {
				return "DeviceManager(" + DeviceManager.this.getUser().getUserName() + ")";
			}
		};
	
	public DeviceManager(Member user, IDeviceStore store) 
			throws ErrorException { 
		if (user == null || store == null) throw new NullPointerException();
		mUser = user;
		mStore = store;
		loadDevices(false);
	}
	
	private void initDeviceTypes() throws ErrorException { 
		addDeviceType(new WebDeviceType(this));
		addDeviceType(new AndroidDeviceType(this));
		addDeviceType(new IOSDeviceType(this));
		addDeviceType(new WinDeviceType(this));
		addDeviceType(new HostDeviceType(this));
	}
	
	public static boolean hasDeviceType(String apptype) {
		if (apptype == null) return false;
		
		if (apptype.equals(WebDeviceType.DEV_WEB)) return true;
		if (apptype.equals(AndroidDeviceType.DEV_ANDROID)) return true;
		if (apptype.equals(IOSDeviceType.DEV_IOS)) return true;
		if (apptype.equals(WinDeviceType.DEV_WIN)) return true;
		if (apptype.equals(HostDeviceType.DEV_HOST)) return true;
		
		return false;
	}
	
	private void addDeviceType(DeviceType type) throws ErrorException { 
		if (type == null) return;
		synchronized (mDeviceTypes) {
			final String name = type.getName();
			if (mDeviceTypes.containsKey(name) || mDeviceGroups.containsKey(name)) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"DeviceType: " + type + " already exists");
			}
			mDeviceTypes.put(name, type);
			mDeviceGroups.put(name, new DeviceGroup(this, type));
		}
	}
	
	public DeviceType getDeviceType(String type) { 
		if (type == null) return null;
		synchronized (mDeviceTypes) {
			return mDeviceTypes.get(type);
		}
	}
	
	public Member getUser() { return mUser; }
	public IDeviceStore getStore() { return mStore; }
	public ILockable.Lock getLock() { return mLock; }
	
	public synchronized Device addDevice(Device dev) 
			throws ErrorException { 
		if (dev == null) return null;
		
		synchronized (mUser) { 
			synchronized (mDeviceGroups) { 
				String type = dev.getType().getName();
				DeviceGroup list = mDeviceGroups.get(type);
				if (list != null) 
					return list.addDevice(dev);
				
				return null;
			}
		}
	}

	public synchronized DeviceGroup getDeviceGroup(String type) {
		if (type == null) return null;
		
		synchronized (mUser) { 
			synchronized (mDeviceGroups) { 
				return mDeviceGroups.get(type);
			}
		}
	}
	
	synchronized void addDeviceGroup(DeviceGroup conf) 
			throws ErrorException { 
		if (conf == null) return;
		
		synchronized (mUser) { 
			synchronized (mDeviceGroups) { 
				String type = conf.getType().getName();
				if (mDeviceGroups.containsKey(type)) { 
					DeviceGroup existed = mDeviceGroups.get(type);
					if (existed != conf) { 
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"DeviceGroup of type: " + type + " already exists");
					}
					return;
				}
				mDeviceGroups.put(type, conf);
				
				if (LOG.isDebugEnabled())
					LOG.debug("addDeviceGroup: group=" + conf);
			}
		}
	}
	
	@Override
	public boolean isSaveAll() { return false; }
	
	@Override
	protected void saveSettingsConf(NamedList<Object> items) 
			throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("saveSettingsConf: user=" + mUser);
		
		synchronized (mUser) {
			synchronized (mDeviceGroups) { 
				getStore().saveDeviceSetting(this, items);
			}
		}
	}
	
	@Override
	protected synchronized NamedList<Object> loadSettingsConf() 
			throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("loadSettingsConf: user=" + mUser);
		
		synchronized (mUser) {
			synchronized (mDeviceGroups) { 
				return getStore().loadDeviceSetting(this);
			}
		}
	}
	
	public synchronized void saveDevices() throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("saveDevices: user=" + mUser);
		
		getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
		try {
			synchronized (mUser) {
				synchronized (mDeviceGroups) { 
					DeviceTable.saveDevices(this);
					saveSettings();
				}
			}
		} finally { 
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	public synchronized void loadDevices(boolean force) 
			throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("loadDevices: user=" + mUser);
		
		getLock().lock(ILockable.Type.READ, null);
		try {
			synchronized (mUser) {
				synchronized (mDeviceGroups) { 
					if (mLoaded && force == false)
						return;
					
					clear();
					initDeviceTypes();
					loadSettings();
					
					mLoaded = true;
				}
			}
		} finally { 
			getLock().unlock(ILockable.Type.READ);
		}
	}
	
	@Override
	protected synchronized void clear() { 
		super.clear();
		mDeviceTypes.clear();
		mDeviceGroups.clear();
		mLoaded = false;
	}
	
	public boolean isClosed() { return mClosed; }
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		mClosed = true;
		
		try {
			getLock().lock(ILockable.Type.READ, null);
			try {
				DeviceGroup[] groups = mDeviceGroups.values().toArray(
						new DeviceGroup[mDeviceGroups.size()]);
				
				if (groups != null) { 
					for (DeviceGroup group : groups) { 
						if (group != null) group.close();
					}
				}
				
				getUser().removeDeviceManager();
				clear();
			} finally { 
				getLock().unlock(ILockable.Type.READ);
			}
		} catch (ErrorException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("close: error: " + e, e);
		}
	}
	
}
