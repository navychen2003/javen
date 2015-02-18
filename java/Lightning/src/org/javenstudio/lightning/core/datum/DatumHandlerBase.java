package org.javenstudio.lightning.core.datum;

import java.util.Comparator;
import java.util.TreeSet;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.ISectionGroup;
import org.javenstudio.falcon.datum.ISectionRoot;
import org.javenstudio.falcon.datum.ISectionSet;
import org.javenstudio.falcon.datum.ISectionSort;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.datum.SectionQuery;
import org.javenstudio.falcon.datum.cache.MemCache;
import org.javenstudio.falcon.setting.cluster.ILibraryInfo;
import org.javenstudio.falcon.setting.cluster.IStorageInfo;
import org.javenstudio.falcon.setting.cluster.StorageManager;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.util.FsUtils;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.core.CoreHandlerBase;

public abstract class DatumHandlerBase extends CoreHandlerBase {
	private static final Logger LOG = Logger.getLogger(DatumHandlerBase.class);
	
	private final DatumCore mCore;
	
	public DatumHandlerBase(DatumCore core) { 
		if (core == null) throw new NullPointerException();
		mCore = core;
	}
	
	public DatumCore getCore() { return mCore; }
	public MemCache getCache() { return getCore().getCache(); }
	
	public DataManager getManager(IUser user) throws ErrorException { 
		//return getCore().getManager(userKey); 
		return user != null ? user.getDataManager() : null;
	}
	
	public StorageManager getStorageManager(IUser user) {
		if (user != null) 
			return user.getUserManager().getStorageManager(user.getUserKey());
		return null;
	}
	
	public IData getData(IMember user, String key, 
			IData.Access access, String accesskey) throws ErrorException { 
		return SectionHelper.getData(user, key, access, accesskey);
	}
	
	public IData getCacheData(IMember user, String key, 
			IData.Access access, String accesskey) throws ErrorException { 
		if (key == null) return null;
		
		Object obj = getCache().get(key);
		if (obj != null && obj instanceof IData) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getCacheData: return cached data, key=" 
						+ key + " data=" + obj);
			}
			
			IData data = (IData)obj;
			SectionHelper.checkAccess(user, data, access, accesskey);
			
			return data;
		}
		
		IData data = getData(user, key, access, accesskey);
		if (data != null && isRequestCache()) { 
			if (LOG.isDebugEnabled()) {
				LOG.debug("getCacheData: cache data, key=" 
						+ key + " data=" + obj);
			}
			
			getCache().put(key, data);
		}
		
		return data;
	}
	
	static String getPermissions(IData data) { 
		if (data == null) return "-----";
		return (data.canRead() ? "r" : "-")
				+ (data.canWrite() ? "w" : "-") 
				+ (data.canDelete() ? "d" : "-") 
				+ (data.canMove() ? "m" : "-")
				+ (data.canCopy() ? "c" : "-");
	}
	
	static String getOperations(IData data) { 
		if (data == null) return "------";
		return (data.supportOperation(IData.Operation.DELETE) ? "d" : "-")
				+ (data.supportOperation(IData.Operation.MOVE) ? "m" : "-")
				+ (data.supportOperation(IData.Operation.COPY) ? "c" : "-")
				+ (data.supportOperation(IData.Operation.UPLOAD) ? "u" : "-")
				+ (data.supportOperation(IData.Operation.NEWFOLDER) ? "n" : "-")
				+ (data.supportOperation(IData.Operation.EMPTY) ? "e" : "-");
	}
	
	static NamedList<Object> getLibraryInfo(IStorageInfo storage, ILibraryInfo item, 
			int subfilecount, boolean includeSections) throws ErrorException { 
		if (storage == null || item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("id", toString(item.getContentId()));
		info.add("name", toString(item.getName()));
		info.add("hostname", toString(item.getHostName()));
		info.add("type", toString(item.getContentType()));
		info.add("poster", toString(item.getPoster()));
		info.add("background", toString(item.getBackground()));
		info.add("ctime", item.getCreatedTime());
		info.add("mtime", item.getModifiedTime());
		info.add("itime", item.getIndexedTime());
		info.add("subcount", item.getSubCount());
		info.add("sublen", item.getSubLen());
		
		return info;
	}
	
	static NamedList<Object> getLibraryInfo(ILibrary item, 
			int subfilecount, boolean includeSections) throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		int subcount = item.getTotalFileCount() + item.getTotalFolderCount();
		long sublen = item.getTotalFileLength();
		
		info.add("id", toString(item.getContentId()));
		info.add("name", toString(item.getName()));
		info.add("hostname", toString(item.getHostName()));
		info.add("type", toString(item.getContentType()));
		info.add("poster", toString(getPoster(item)));
		info.add("background", toString(getBackground(item)));
		info.add("ctime", item.getCreatedTime());
		info.add("mtime", item.getModifiedTime());
		info.add("itime", item.getIndexedTime());
		info.add("subcount", subcount);
		info.add("sublen", sublen);
		
		if (includeSections) { 
			info.add("sections", getSectionInfos(
					item.getSections(new SectionQuery(0, 0)), subfilecount));
			
		} else if (subfilecount > 0) {
			ISectionSet set = item.getSections(new SectionQuery(0, 0));
			NamedList<Object> subitems = getSectionSubItemsInfo(set, subfilecount);
			
			if (subitems != null) 
				info.add("sections", subitems);
		}
		
		return info;
	}
	
	static NamedList<Object> getSectionInfos(ISectionSet sections, 
			int subfilecount) throws ErrorException {
		NamedList<Object> items = new NamedMap<Object>();
		
		for (int i=0; sections != null && i < sections.getSectionCount(); i++) { 
			ISection section = sections.getSectionAt(i);
			NamedList<Object> info = getSectionInfo(section, subfilecount);
			if (section != null && info != null) 
				items.add(section.getContentId(), info);
		}
		
		return items;
	}
	
	static NamedList<Object> getSectionInfos(ISection[] sections) 
			throws ErrorException {
		NamedList<Object> items = new NamedMap<Object>();
		
		for (int i=0; sections != null && i < sections.length; i++) { 
			ISection section = sections[i];
			NamedList<Object> info = getSectionInfo(section);
			if (section != null && info != null) 
				items.add(section.getContentId(), info);
		}
		
		return items;
	}
	
	public static NamedList<Object> getSectionInfo(ISection item) 
			throws ErrorException {
		return getSectionInfo(item, 0);
	}
	
	public static NamedList<Object> getSectionInfo(ISection item, 
			int subfilecount) throws ErrorException {
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		String libname = item.getLibrary().getName();
		String path = item.getParentPath();
		String extension = item.getExtension();
		
		int subcount = 0;
		long sublen = 0;
		if (item instanceof ISectionRoot) { 
			ISectionRoot root = (ISectionRoot)item;
			subcount = root.getTotalFileCount() + root.getTotalFolderCount();
			sublen = root.getTotalFileLength();
		} else { 
			subcount = item.getSubCount();
			sublen = item.getSubLength();
		}
		
		path = "/" + libname + path;
		
		info.add("id", toString(item.getContentId()));
		info.add("name", toString(item.getName()));
		info.add("isfolder", item.isFolder());
		info.add("type", toString(item.getContentType()));
		info.add("extname", toString(extension));
		info.add("path", toString(FsUtils.normalizePath(path)));
		info.add("poster", toString(getPoster(item)));
		info.add("background", toString(getBackground(item)));
		info.add("owner", toString(item.getOwner()));
		info.add("checksum", toString(item.getChecksum()));
		//info.add("ctime", item.getCreatedTime());
		info.add("mtime", item.getModifiedTime());
		//info.add("itime", item.getIndexedTime());
		info.add("length", item.getContentLength());
		info.add("width", item.getWidth());
		info.add("height", item.getHeight());
		info.add("timelen", item.getDuration());
		info.add("subcount", subcount);
		info.add("sublen", sublen);
		
		if (subfilecount > 0 && subcount > 0 && item.isFolder()) {
			NamedList<Object> subitems = getSectionSubItemsInfo(
					new ISection[]{item}, subfilecount);
			
			if (subitems != null) 
				info.add("subitems", subitems);
		}
		
		return info;
	}
	
	static NamedList<Object> getSectionSubItemsInfo(ISection[] sections, 
			int subfilecount) throws ErrorException {
		if (sections == null || subfilecount <= 0) return null;
		
		if (subfilecount > 0 && sections.length > 0) {
			ISection[] subset = getSectionSubItems(sections, subfilecount);
			if (subset == null) return null;
			
			NamedList<Object> subitems = new NamedMap<Object>();
			for (ISection subitem : subset) {
				if (subitem == null) continue;
				NamedList<Object> subinfo = getSectionSubItemInfo(subitem);
				if (subinfo != null) {
					subitems.add(subitem.getContentId(), subinfo);
					
					if (subitems.size() >= subfilecount)
						break;
				}
			}
			
			return subitems;
		}
		
		return null;
	}
	
	static NamedList<Object> getSectionSubItemsInfo(ISectionSet set, 
			int subfilecount) throws ErrorException {
		if (set == null || subfilecount <= 0) return null;
		
		if (subfilecount > 0 && set.getSectionCount() > 0) {
			ISection[] subset = getSectionSubItems(set, subfilecount);
			if (subset == null) return null;
			
			NamedList<Object> subitems = new NamedMap<Object>();
			for (ISection subitem : subset) {
				if (subitem == null) continue;
				NamedList<Object> subinfo = getSectionSubItemInfo(subitem);
				if (subinfo != null) {
					subitems.add(subitem.getContentId(), subinfo);
					
					if (subitems.size() >= subfilecount)
						break;
				}
			}
			
			return subitems;
		}
		
		return null;
	}
	
	static NamedList<Object> getSectionSubItemInfo(ISection subitem) 
			throws ErrorException {
		if (subitem == null) return null;
		NamedList<Object> subinfo = new NamedMap<Object>();
		
		subinfo.add("id", toString(subitem.getContentId()));
		subinfo.add("name", toString(subitem.getName()));
		subinfo.add("isfolder", subitem.isFolder());
		subinfo.add("type", toString(subitem.getContentType()));
		subinfo.add("extname", toString(subitem.getExtension()));
		subinfo.add("poster", toString(getPoster(subitem)));
		subinfo.add("background", toString(getBackground(subitem)));
		subinfo.add("owner", toString(subitem.getOwner()));
		subinfo.add("checksum", toString(subitem.getChecksum()));
		//subinfo.add("ctime", item.getCreatedTime());
		subinfo.add("mtime", subitem.getModifiedTime());
		//subinfo.add("itime", item.getIndexedTime());
		subinfo.add("length", subitem.getContentLength());
		subinfo.add("width", subitem.getWidth());
		subinfo.add("height", subitem.getHeight());
		subinfo.add("timelen", subitem.getDuration());
		
		return subinfo;
	}
	
	static ISection[] getSectionSubItems(ISection[] sections, 
			int subfilecount) throws ErrorException {
		if (sections == null || subfilecount <= 0) return null;
		
		TreeSet<ISection> subset = new TreeSet<ISection>(
			new Comparator<ISection>() {
				@Override
				public int compare(ISection o1, ISection o2) {
					long tm1 = o1.getModifiedTime();
					long tm2 = o2.getModifiedTime();
					if (tm1 > tm2) return -1;
					else if (tm1 < tm2) return 1;
					return o1.getName().compareTo(o2.getName());
				}
			});
		
		for (ISection section : sections) {
			if (section == null || !section.isFolder()) continue;
			ISectionSet set = section.getSubSections(new SectionQuery(0, section.getSubCount()));
			if (set != null) {
				for (int i=0; i < set.getSectionCount(); i++) {
					ISection subitem = set.getSectionAt(i);
					if (subitem != null && !subitem.isFolder()) {
						String subtype = subitem.getContentType();
						String[] subposters = subitem.getPosters();
						
						if ((subposters != null && subposters.length > 0) || 
							(subtype != null && subtype.startsWith("image/"))) {
							subset.add(subitem);
							
							if (subset.size() > subfilecount)
								subset.pollLast();
						}
					}
				}
			}
		}
		
		return subset.toArray(new ISection[subset.size()]);
	}
	
	static ISection[] getSectionSubItems(ISectionSet sections, 
			int subfilecount) throws ErrorException {
		if (sections == null || subfilecount <= 0) return null;
		
		TreeSet<ISection> subset = new TreeSet<ISection>(
			new Comparator<ISection>() {
				@Override
				public int compare(ISection o1, ISection o2) {
					long tm1 = o1.getModifiedTime();
					long tm2 = o2.getModifiedTime();
					if (tm1 > tm2) return -1;
					else if (tm1 < tm2) return 1;
					return o1.getName().compareTo(o2.getName());
				}
			});
		
		for (int k=0; k < sections.getSectionCount(); k++) {
			ISection section = sections.getSectionAt(k);
			if (section == null || !section.isFolder()) continue;
			
			ISectionSet set = section.getSubSections(new SectionQuery(0, section.getSubCount()));
			if (set != null) {
				for (int i=0; i < set.getSectionCount(); i++) {
					ISection subitem = set.getSectionAt(i);
					if (subitem != null && !subitem.isFolder()) {
						String subtype = subitem.getContentType();
						String[] subposters = subitem.getPosters();
						
						if ((subposters != null && subposters.length > 0) || 
							(subtype != null && subtype.startsWith("image/"))) {
							subset.add(subitem);
							
							if (subset.size() > subfilecount)
								subset.pollLast();
						}
					}
				}
			}
		}
		
		return subset.toArray(new ISection[subset.size()]);
	}
	
	static String getPoster(ILibrary item) throws ErrorException {
		if (item == null) return null;
		
		String[] keys = item.getPosters();
		if (keys != null && keys.length > 0)
			return keys[0];
		
		return null;
	}
	
	static String getBackground(ILibrary item) throws ErrorException {
		if (item == null) return null;
		
		String[] keys = item.getBackgrounds();
		if (keys != null && keys.length > 0)
			return keys[0];
		
		return null;
	}
	
	static String getPoster(ISection item) throws ErrorException {
		if (item == null) return null;
		
		String[] keys = item.getPosters();
		if (keys != null && keys.length > 0)
			return keys[0];
		
		return null;
	}
	
	static String getBackground(ISection item) throws ErrorException {
		if (item == null) return null;
		
		String[] keys = item.getBackgrounds();
		if (keys != null && keys.length > 0)
			return keys[0];
		
		return null;
	}
	
	static NamedList<Object> getGroupInfo(ISectionGroup item) 
			throws ErrorException {
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("name", toString(item.getName()));
		info.add("title", toString(item.getTitle()));
		
		return info;
	}
	
	static NamedList<Object> getSortInfo(ISectionSort item) 
			throws ErrorException {
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("name", toString(item.getName()));
		info.add("title", toString(item.getTitle()));
		
		return info;
	}
	
}
