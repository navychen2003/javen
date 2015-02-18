package org.javenstudio.falcon.datum;

import java.util.ArrayList;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.data.SqHelper;
import org.javenstudio.falcon.datum.data.SqLibrary;
import org.javenstudio.falcon.setting.SettingConf;
import org.javenstudio.falcon.util.FsUtils;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class DataConf {

	public static final String STATUS_ENABLED = "enabled";
	
	public static class SectionInfo { 
		public String id;
		public String key;
		public String className;
		public String name;
		public String contentType;
		public long createdTime;
		public long modifiedTime;
		public long indexedTime;
		public long optimizeTime;
		public boolean isFolder;
		public int totalFolderCount;
		public int totalFileCount;
		public long totalFileLength;
	}
	
	public static class LibraryInfo { 
		public String id;
		public String key;
		public String storeUri;
		public String className;
		public String name;
		public String hostname;
		public String contentType;
		public int maxEntries;
		public long createdTime;
		public long modifiedTime;
		public long indexedTime;
		public long optimizeTime;
		public boolean canRead;
		public boolean canWrite;
		public boolean canMove;
		public boolean canDelete;
		public boolean canCopy;
		public boolean isDefault;
		public SectionInfo[] sectionList;
	}
	
	public static void loadLibrary(DataManager manager, 
			NamedList<Object> items) throws ErrorException { 
		LibraryInfo[] infos = toLibraryInfos(items);
		loadLibrary(manager, infos);
	}
	
	public static void loadLibrary(DataManager manager, 
			LibraryInfo[] infos) throws ErrorException {
		for (int i=0; infos != null && i < infos.length; i++) { 
			LibraryInfo libraryInfo = infos[i];
			if (libraryInfo == null) continue;
			
			String className = libraryInfo.className;
			if (className == null) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Library className is null");
			}
			
			if (className.equals(SqLibrary.class.getName())) {
				SqHelper.addLibrary(manager, libraryInfo);
				
			//} else if (className.equals(FsLibrary.class.getName())) { 
			//	FsHelper.addLibrary(manager, libraryInfo);
				
			} else {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Unknown class: " + className);
			}
		}
	}
	
	public static String[] toLibraryIds(NamedList<Object> items) { 
		if (items == null) return null;
		ArrayList<String> infos = new ArrayList<String>();
		
		for (int i=0; i < items.size(); i++) { 
			//String name = items.getName(i);
			Object value = items.getVal(i);
			
			if (value != null && value instanceof NamedList) { 
				@SuppressWarnings("unchecked")
				NamedList<Object> item = (NamedList<Object>)value;
				
				String id = SettingConf.getString(item, "id");
				String status = SettingConf.getString(item, "status");
				
				if (id != null && id.length() > 0 && status != null) {
					if (STATUS_ENABLED.equalsIgnoreCase(status))
						infos.add(id);
				}
			}
		}
		
		return infos.toArray(new String[infos.size()]);
	}
	
	public static LibraryInfo[] toLibraryInfos(NamedList<Object> items) { 
		if (items == null) return null;
		ArrayList<LibraryInfo> infos = new ArrayList<LibraryInfo>();
		
		for (int i=0; i < items.size(); i++) { 
			//String name = items.getName(i);
			Object value = items.getVal(i);
			
			if (value != null && value instanceof NamedList) { 
				@SuppressWarnings("unchecked")
				NamedList<Object> item = (NamedList<Object>)value;
				
				LibraryInfo libraryInfo = toLibraryInfo(item);
				if (libraryInfo != null)
					infos.add(libraryInfo);
			}
		}
		
		return infos.toArray(new LibraryInfo[infos.size()]);
	}
	
	public static LibraryInfo toLibraryInfo(NamedList<Object> item) { 
		if (item == null) return null;
		LibraryInfo libraryInfo = new LibraryInfo();
		
		libraryInfo.id = SettingConf.getString(item, "id");
		libraryInfo.key = SettingConf.getString(item, "key");
		libraryInfo.storeUri = SettingConf.getString(item, "storeUri");
		libraryInfo.className = SettingConf.getString(item, "class");
		libraryInfo.name = SettingConf.getString(item, "name");
		libraryInfo.hostname = SettingConf.getString(item, "hostname");
		libraryInfo.contentType = SettingConf.getString(item, "contentType");
		libraryInfo.maxEntries = SettingConf.getInt(item, "maxEntries");
		libraryInfo.createdTime = SettingConf.getLong(item, "createdTime");
		libraryInfo.modifiedTime = SettingConf.getLong(item, "modifiedTime");
		libraryInfo.indexedTime = SettingConf.getLong(item, "indexedTime");
		libraryInfo.optimizeTime = SettingConf.getLong(item, "optimizeTime");
		libraryInfo.canRead = SettingConf.getBool(item, "canRead");
		libraryInfo.canWrite = SettingConf.getBool(item, "canWrite");
		libraryInfo.canMove = SettingConf.getBool(item, "canMove");
		libraryInfo.canDelete = SettingConf.getBool(item, "canDelete");
		libraryInfo.canCopy = SettingConf.getBool(item, "canCopy");
		libraryInfo.isDefault = SettingConf.getBool(item, "isDefault");
		libraryInfo.sectionList = toSectionInfos(item.get("sectionList"));
		
		return libraryInfo;
	}
	
	static SectionInfo[] toSectionInfos(Object sectionListVal) { 
		ArrayList<SectionInfo> infos = new ArrayList<SectionInfo>();
		
		if (sectionListVal != null && sectionListVal instanceof NamedList) { 
			@SuppressWarnings("unchecked")
			NamedList<Object> sectionListItem = (NamedList<Object>)sectionListVal;
			
			for (int j=0; j < sectionListItem.size(); j++) { 
				Object val = sectionListItem.getVal(j);
				
				if (val != null && val instanceof NamedList) { 
					@SuppressWarnings("unchecked")
					NamedList<Object> sectionItem = (NamedList<Object>)val;
					
					SectionInfo sectionInfo = toSectionInfo(sectionItem);
					if (sectionInfo != null) 
						infos.add(sectionInfo);
				}
			}
		}
		
		return infos.toArray(new SectionInfo[infos.size()]);
	}
	
	static SectionInfo toSectionInfo(NamedList<Object> item) { 
		if (item == null) return null;
		SectionInfo sectionInfo = new SectionInfo();
		
		sectionInfo.id = SettingConf.getString(item, "id");
		sectionInfo.key = SettingConf.getString(item, "key");
		sectionInfo.className = SettingConf.getString(item, "class");
		sectionInfo.name = SettingConf.getString(item, "name");
		sectionInfo.contentType = SettingConf.getString(item, "contentType");
		sectionInfo.createdTime = SettingConf.getLong(item, "createdTime");
		sectionInfo.modifiedTime = SettingConf.getLong(item, "modifiedTime");
		sectionInfo.indexedTime = SettingConf.getLong(item, "indexedTime");
		sectionInfo.optimizeTime = SettingConf.getLong(item, "optimizeTime");
		sectionInfo.isFolder = SettingConf.getBool(item, "isFolder");
		sectionInfo.totalFolderCount = SettingConf.getInt(item, "totalFolderCount");
		sectionInfo.totalFileCount = SettingConf.getInt(item, "totalFileCount");
		sectionInfo.totalFileLength = SettingConf.getLong(item, "totalFileLength");
		
		return sectionInfo;
	}
	
	public static NamedList<Object> toSimpleNamedList(ILibrary[] libraries) 
			throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		for (int i=0; libraries != null && i < libraries.length; i++) { 
			ILibrary library = libraries[i];
			NamedList<Object> libraryInfo = getSimpleLibraryInfo(library);
			if (library != null && libraryInfo != null) 
				items.add(library.getContentKey(), libraryInfo);
		}
		
		return items;
	}
	
	public static NamedList<Object> toNamedList(ILibrary[] libraries) 
			throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		for (int i=0; libraries != null && i < libraries.length; i++) { 
			ILibrary library = libraries[i];
			NamedList<Object> libraryInfo = getLibraryInfo(library);
			if (library != null && libraryInfo != null) 
				items.add(library.getContentKey(), libraryInfo);
		}
		
		return items;
	}
	
	public static NamedList<Object> toNamedList(ILibrary library) 
			throws ErrorException { 
		return getLibraryInfo(library);
	}
	
	public static NamedList<Object> getSimpleLibraryInfo(ILibrary item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		String storeUri = FsUtils.normalizeUri(item.getStoreFs().getUri().toString());
		
		info.add("id", item.getContentId());
		info.add("key", item.getContentKey());
		info.add("storeUri", storeUri);
		info.add("status", STATUS_ENABLED);
		
		return info;
	}
	
	public static NamedList<Object> getLibraryInfo(ILibrary item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		String storeUri = FsUtils.normalizeUri(item.getStoreFs().getUri().toString());
		
		info.add("class", item.getClass().getName());
		info.add("id", item.getContentId());
		info.add("key", item.getContentKey());
		info.add("storeUri", storeUri);
		info.add("name", item.getName());
		info.add("hostname", item.getHostName());
		info.add("contentType", item.getContentType());
		info.add("maxEntries", item.getMaxEntries());
		info.add("createdTime", item.getCreatedTime());
		info.add("modifiedTime", item.getModifiedTime());
		info.add("indexedTime", item.getIndexedTime());
		info.add("optimizedTime", item.getOptimizedTime());
		info.add("canRead", item.canRead());
		info.add("canWrite", item.canWrite());
		info.add("canMove", item.canMove());
		info.add("canDelete", item.canDelete());
		info.add("canCopy", item.canCopy());
		info.add("isDefault", item.isDefault());
		
		NamedList<Object> items = new NamedMap<Object>();
		int count = item.getSectionCount();
		
		for (int i=0; i < count; i++) { 
			ISectionRoot section = item.getSectionAt(i);
			NamedList<Object> sectionInfo = getSectionInfo(section);
			if (section != null && sectionInfo != null) 
				items.add(section.getContentKey(), sectionInfo);
		}
		
		info.add("sectionList", items);
		
		return info;
	}
	
	public static NamedList<Object> getSectionInfo(ISectionRoot item) 
			throws ErrorException {
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("class", item.getClass().getName());
		info.add("id", item.getContentId());
		info.add("key", item.getContentKey());
		info.add("name", item.getName());
		info.add("isFolder", item.isFolder());
		info.add("contentType", item.getContentType());
		info.add("createdTime", item.getCreatedTime());
		info.add("modifiedTime", item.getModifiedTime());
		info.add("indexedTime", item.getIndexedTime());
		info.add("optimizeTime", item.getOptimizedTime());
		info.add("totalFolderCount", item.getTotalFolderCount());
		info.add("totalFileCount", item.getTotalFileCount());
		info.add("totalFileLength", item.getTotalFileLength());
		info.add("status", STATUS_ENABLED);
		
		return info;
	}

}
