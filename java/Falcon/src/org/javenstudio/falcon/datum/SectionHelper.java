package org.javenstudio.falcon.datum;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.profile.MemberManager;
import org.javenstudio.falcon.util.IdentityUtils;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.Path;

public class SectionHelper {
	private static final Logger LOG = Logger.getLogger(SectionHelper.class);

	public static IFolderInfo[] listLocalRoots(Configuration conf) 
			throws ErrorException { 
		return LocalFolderInfo.listLocalRoots(conf);
	}
	
	public static IFolderInfo getLocalFolder(String key) { 
		return LocalFolderInfo.getFolder(key);
	}
	
	public static IFolderInfo[] listRoots(DataManager manager) { 
		if (manager == null) return null;
		
		ILibrary[] libraries = manager.getLibraries();
		ArrayList<IFolderInfo> list = new ArrayList<IFolderInfo>();
		
		for (int i=0; libraries != null && i < libraries.length; i++) { 
			ILibrary library = libraries[i];
			if (library != null) {
				IFolderInfo folder = library.getFolderInfo();
				if (folder != null)
					list.add(folder);
			}
		}
		
		return list.toArray(new IFolderInfo[list.size()]);
	}
	
	public static class CacheSection { 
		private final WeakReference<ISection> mSectionRef;
		private final int mCacheVersion;
		
		public CacheSection(ISection data, int version) { 
			mSectionRef = new WeakReference<ISection>(data);
			mCacheVersion = version;
		}
		
		public ISection getSection() { return mSectionRef.get(); }
		public int getVersion() { return mCacheVersion; }
	}
	
	private static final Map<String, CacheSection> mCaches = 
			new HashMap<String, CacheSection>();
	
	public static void putCache(String sectionId, ISection section, int version) { 
		if (sectionId == null || section == null) 
			return;
		
		synchronized (mCaches) { 
			if (LOG.isDebugEnabled()) {
				LOG.debug("putCache: key=" + sectionId + " data=" + section 
						+ " version=" + version);
			}
			
			mCaches.put(sectionId, new CacheSection(section, version));
		}
	}
	
	public static ISection getCache(String sectionId, int version) { 
		if (sectionId == null) return null;
		
		synchronized (mCaches) { 
			CacheSection cache = mCaches.get(sectionId);
			if (cache != null && cache.getVersion() == version) {
				ISection section = cache.getSection();
				if (section != null) {
					if (LOG.isDebugEnabled()) { 
						LOG.debug("getCache: key=" + sectionId + " data=" + section 
								+ " version=" + version);
					}
					return section;
				}
			}
			
			mCaches.remove(sectionId);
			return null;
		}
	}
	
	public static IData[] getData(IMember member, String[] keys, 
			IData.Access access, String accesskey) throws ErrorException { 
		if (keys == null) return null;
		
		ArrayList<IData> list = new ArrayList<IData>();
		for (String key : keys) { 
			IData data = getData(member, key, access, accesskey);
			if (data != null)
				list.add(data);
		}
		
		return list.toArray(new IData[list.size()]);
	}
	
	@SuppressWarnings("unused")
	public static IData getData(IMember member, String key, 
			IData.Access access, String accesskey) throws ErrorException { 
		if (key == null) return null;
		
		boolean isDefault = false;
		IUser user = null;
		String userKey = null;
		String libraryKey = key;
		String rootKey = null;
		String fileKey = null;
		
		if (key != null && key.indexOf('@') > 0) { 
			String username = key.toLowerCase();
			
			user = UserHelper.getLocalUserByName(username);
			if (user != null) {
				userKey = user.getUserKey();
				isDefault = true;
			}
		}
		
		if (!isDefault && key.length() != 8) {
			String[] keys = SectionHelper.splitKeys(key);
			if (keys != null) { 
				if (keys.length == 1) { 
					libraryKey = keys[0];
				} else if (keys.length == 4) { 
					userKey = keys[0];
					libraryKey = keys[1];
					rootKey = keys[2];
					fileKey = keys[3];
				}
			}
		}
		
		if (user == null) user = UserHelper.getLocalUserByKey(userKey);
		DataManager manager = user != null ? user.getDataManager() : null; 
		if (manager == null) return null;
		
		ILibrary library = (isDefault) ? manager.getDefaultLibrary() : 
			manager.getLibrary(libraryKey);
		
		IData data = null;
		if (library != null) {
			if (fileKey == null || fileKey.equals("00000000"))
				data = library;
			else 
				data = library.getSection(key);
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("getData: key=" + key + ", access=" + access 
					+ ", data=" + data);
		}
		
		checkAccess(member, data, access, accesskey);
		
		return data;
	}
	
	public static String newName(String name) { 
		return name != null && name.length() > 0 ? 
				(new Path(name)).getName() : "";
	}
	
	public static void checkKey(String key) { 
		if (key == null || key.length() != 8) 
			throw new IllegalArgumentException("Key(" + key + ") length must be 8");
	}
	
	public static final char RECYCLE_CHAR = '0';
	public static final char SHARE_CHAR = '1';
	public static final char UPLOAD_CHAR = '2';
	public static final char ARCHIVE_CHAR = '3';
	
	public static String newRecycleRootKey(String path) throws ErrorException { 
		return newRootKey(path, RECYCLE_CHAR);
	}
	
	public static String newShareRootKey(String path) throws ErrorException { 
		return newRootKey(path, SHARE_CHAR);
	}
	
	public static String newUploadRootKey(String path) throws ErrorException { 
		return newRootKey(path, UPLOAD_CHAR);
	}
	
	public static String newArchiveRootKey(String path) throws ErrorException { 
		return newRootKey(path, ARCHIVE_CHAR);
	}
	
	private static String newRootKey(String path, char last) throws ErrorException { 
		return IdentityUtils.newChecksumKey(path, 7) + last;
	}
	
	public static final char DIR_CHAR = '0';
	public static final char FILE_CHAR = '1';
	
	public static String newFileKey(String path, boolean isDir) throws ErrorException { 
		return IdentityUtils.newChecksumKey(path, 7) + (isDir ? DIR_CHAR : FILE_CHAR);
	}
	
	public static boolean isDirectoryKey(String key) { 
		if (key != null && key.length() == 8) 
			return key.charAt(key.length()-1) == DIR_CHAR;
		else
			return false;
	}
	
	public static final char LIBRARY_CHAR = '0';
	public static final char DATABASE_CHAR = '1';
	
	public static String newLibraryKey(String name) throws ErrorException { 
		return IdentityUtils.newChecksumKey(name, 7) + LIBRARY_CHAR;
	}
	
	public static String newDatabaseKey(String name) throws ErrorException { 
		return IdentityUtils.newChecksumKey(name, 7) + DATABASE_CHAR;
	}
	
	public static String newFolderKey(String name) throws ErrorException { 
		return IdentityUtils.newKey(name, 8);
	}
	
	public static String newSectionKey(String name) throws ErrorException { 
		return IdentityUtils.newKey(name, 8);
	}
	
	public static String newContentId(String key) { 
		return IdentityUtils.toIdentity(key, 32);
	}
	
	public static String[] splitKeys(String id) throws ErrorException { 
		if (id != null) { 
			switch (id.length()) { 
			case 8: 
				return new String[]{ id };
			case 32: 
				return new String[] { 
						id.substring(0, 8), id.substring(8, 16), 
						id.substring(16, 24), id.substring(24, 32)
					};
			}
		}
		//throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
		//		"Illegal keys: " + id);
		return null;
	}
	
	public static boolean isShareFile(String id) throws ErrorException { 
		String[] ids = splitKeys(id);
		if (ids != null && ids.length == 4) { 
			String rootKey = ids[2];
			if (rootKey != null && rootKey.length() == 8) { 
				char last = rootKey.charAt(7);
				return last == SHARE_CHAR;
			}
		}
		return false;
	}
	
	public static void checkAccess(IMember user, IData data, 
			IData.Access access, String accesskey) throws ErrorException { 
		if (data == null) return;
		
		if (user != null) { 
			if (user.getUserKey().equals(data.getManager().getUserKey()))
				return;
		}
		
		String id = data.getContentId();
		String[] ids = splitKeys(id);
		
		if (ids != null && ids.length == 4) { 
			String userKey = ids[0];
			String rootKey = ids[2];
			if (rootKey != null && rootKey.length() == 8) { 
				if (data instanceof ISection) {
					ISection section = (ISection)data;
					
					char last = rootKey.charAt(7);
					if (last == SHARE_CHAR) { // public files
						String akey = section.getAccessKey();
						boolean accesskeyOkay = false;
						if (accesskey != null && akey != null && akey.length() > 0) {
							if (akey.equals(accesskey)) {
								accesskeyOkay = true;
							}
						}
						
						if (access != null) {
							switch (access) {
							case DETAILS:
							case THUMB:
							case STREAM:
								// support access details, thumb and stream
								return;
							case INFO:
							case DOWNLOAD:
							case LIST:
								// support read access
								if (accesskeyOkay) return;
								break;
							case UPDATE:
							case INDEX:
								// not supported
								break;
							default:
								// not supported
								break;
							}
						}
					}
				}
				
				if (user != null && user.getUserKey().equals(userKey))
					return;
			}
		}
		
		if (user != null) {
			IUser usr = data.getManager().getUser();
			if (usr != null && usr instanceof IGroup) { 
				IGroup group = (IGroup)usr;
				
				MemberManager mm = group.getMemberManager();
				if (mm != null) { 
					mm.loadMembers(false);
					
					MemberManager.GroupMember gm = mm.getMember(user.getUserKey());
					if (gm != null) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("checkAccess: user=" + user + " group=" + group 
									+ " member=" + gm + " data=" + data);
						}
						
						return;
					}
				}
			}
		}
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Access denied: no permission for " + id + " with " 
					+ IData.Util.stringOfAccess(access) + " access");
	}
	
	public static void checkPermission(IUser user, IData data, 
			IData.Action op) throws ErrorException { 
		if (data == null) return;
		
		String id = data.getContentId();
		String owner = data.getOwner();
		
		if (user != null) {
			if (user.getUserKey().equals(data.getManager().getUserKey()))
				return;
			
			if (owner != null && owner.equals(user.getUserName()))
				return;
			
			IUser usr = data.getManager().getUser();
			if (usr != null && usr instanceof IGroup) { 
				IGroup group = (IGroup)usr;
				
				MemberManager mm = group.getMemberManager();
				if (mm != null) { 
					mm.loadMembers(false);
					
					MemberManager.GroupMember gm = mm.getMember(user.getUserKey());
					if (gm != null) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("checkPermission: user=" + user + " group=" + group 
									+ " member=" + gm + " data=" + data);
						}
						
						String role = gm.getRole();
						if (MemberManager.ROLE_OWNER.equals(role) || MemberManager.ROLE_MANAGER.equals(role))
							return;
					}
				}
			}
		}
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Access denied: no permission for " + id);
	}
	
}
