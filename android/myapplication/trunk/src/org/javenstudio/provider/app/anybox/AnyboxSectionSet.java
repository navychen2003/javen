package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.android.app.FilterType;
import org.javenstudio.android.app.SortType;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.ISectionSet;

public class AnyboxSectionSet implements ISectionSet {
	private static final Logger LOG = Logger.getLogger(AnyboxSectionSet.class);

	private AnyboxSection[] mSections = null;
	private AnyboxSection.SortData[] mSorts = null;
	private String mSorted = null;
	
	private int mTotalCount = 0;
	private int mSectionFrom = 0;
	private int mSectionCount = 0;
	
	private long mRequestTime = 0;
	private long mReloadId = 0;
	
	private AnyboxSectionSet() {}
	
	public AnyboxSection[] getSections() { return mSections; }
	public AnyboxSection.SortData[] getSorts() { return mSorts; }
	public String getSorted() { return mSorted; }
	
	public int getTotalCount() { return mTotalCount; }
	public int getSectionFrom() { return mSectionFrom; }
	public int getSectionCount() { return mSectionCount; }
	
	public long getRequestTime() { return mRequestTime; }
	public long getReloadId() { return mReloadId; }
	
	static AnyboxSectionSet loadSectionSet(AnyboxLibrary library, 
			ISectionFolder parent, AnyboxData data, long reloadId) throws IOException {
		if (library == null || parent == null || data == null) 
			return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadSectionSet: data=" + data);
		
		AnyboxSection[] sections = AnyboxSection.loadSections(
				library, parent, data.get("sections"));
		AnyboxSection.SortData[] sorts = AnyboxSection.loadSorts(
				library, parent, data.getArray("sorts"));
		
		if (sections == null || sections.length == 0)
			return null;
		
		int totalCount = data.getInt("total_count", 0);
		int sectionFrom = data.getInt("section_from", 0);
		int sectionCount = data.getInt("section_count", 0);
		
		String sorted = null;
		if (sorts != null) {
			for (AnyboxSection.SortData sort : sorts) {
				if (sort == null) continue;
				String sortedVal = sort.getSorted();
				if (sortedVal != null && sortedVal.length() > 0)
					sorted = sortedVal.toLowerCase();
			}
		}
		
		boolean parentIsLibrary = parent == library;
		if (sorted != null) {
			if (sorted.startsWith("name.")) {
				if (sorted.endsWith(".desc"))
					sortSectionsByNameDesc(sections, parentIsLibrary);
				else
					sortSectionsByNameAsc(sections, parentIsLibrary);
			} else if (sorted.startsWith("update.")) {
				if (sorted.endsWith(".desc"))
					sortSectionsByUpdateDesc(sections, parentIsLibrary);
				else
					sortSectionsByUpdateAsc(sections, parentIsLibrary);
			} else if (sorted.startsWith("size.")) {
				if (sorted.endsWith(".desc"))
					sortSectionsBySizeDesc(sections, parentIsLibrary);
				else
					sortSectionsBySizeAsc(sections, parentIsLibrary);
			} else if (sorted.startsWith("type.")) {
				if (sorted.endsWith(".desc"))
					sortSectionsByTypeDesc(sections, parentIsLibrary);
				else
					sortSectionsByTypeAsc(sections, parentIsLibrary);
			} else {
				sortSectionsByNameAsc(sections, parentIsLibrary);
			}
		} else {
			sortSectionsByNameAsc(sections, parentIsLibrary);
		}
		
		AnyboxSectionSet set = new AnyboxSectionSet();
		set.mSections = sections;
		set.mSorts = sorts;
		set.mSorted = sorted;
		set.mTotalCount = totalCount;
		set.mSectionFrom = sectionFrom;
		set.mSectionCount = sectionCount;
		set.mRequestTime = System.currentTimeMillis();
		set.mReloadId = reloadId;
		
		return set;
	}
	
	public static String getSortTypeString(SortType.Type sort) {
		if (sort == null) return null;
		switch (sort) {
		case NAME_ASC:
			return "name.asc";
		case NAME_DESC:
			return "name.desc";
		case MODIFIED_ASC:
			return "update.asc";
		case MODIFIED_DESC:
			return "update.desc";
		case SIZE_ASC:
			return "size.asc";
		case SIZE_DESC:
			return "size.desc";
		case TYPE_ASC:
			return "type.asc";
		case TYPE_DESC:
			return "type.desc";
		}
		return null;
	}
	
	public static String getFilterTypeString(FilterType.Type filter) {
		if (filter == null) return null;
		switch (filter) {
		case ALL:
			return "";
		case IMAGE:
			return "image/";
		case AUDIO:
			return "audio/";
		case VIDEO:
			return "video/";
		}
		return null;
	}
	
	private static int compareRootFolder(AnyboxSection lhs, AnyboxSection rhs, 
			boolean parentIsLibrary) {
		int res = 0;
		if (parentIsLibrary && lhs.isFolder() && rhs.isFolder()) {
			String ltype = lhs.getType();
			String rtype = rhs.getType();
			if (ltype == null || rtype == null) {
				if (ltype == null) res = 1;
				else res = -1;
			} else if (!ltype.equalsIgnoreCase(rtype)) {
				int ltypeid = 0;
				if (ltype.endsWith("x-recycle-root")) ltypeid = 1;
				else if (ltype.endsWith("x-share-root")) ltypeid = 2;
				else if (ltype.endsWith("x-upload-root")) ltypeid = 3;
				
				int rtypeid = 0;
				if (rtype.endsWith("x-recycle-root")) rtypeid = 1;
				else if (rtype.endsWith("x-share-root")) rtypeid = 2;
				else if (rtype.endsWith("x-upload-root")) rtypeid = 3;
				
				if (ltypeid == 1) { 
					res = -1;
				} else if (ltypeid == 2) {
					if (rtypeid == 1) res = 1;
					else res = -1;
				} else if (ltypeid == 3) {
					if (rtypeid == 1) res = 1;
					else if (rtypeid == 2) res = 1;
					else res = -1;
				} else if (rtypeid == 1) {
					res = 1;
				} else if (rtypeid == 2) {
					if (ltypeid == 1) res = -1;
					else res = 1;
				} else if (rtypeid == 3) {
					if (ltypeid == 1) res = -1;
					else if (ltypeid == 2) res = -1;
					else res = 1;
				}
			}
			//if (LOG.isDebugEnabled()) 
			//	LOG.debug("compareFolder: lhs=" + lhs + " rhs=" + rhs + " res=" + res);
		}
		return res;
	}
	
	private static void sortSectionsByNameDesc(AnyboxSection[] sections, 
			final boolean parentIsLibrary) {
		if (sections == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("sortSectionsByNameDesc: parentIsLibrary=" + parentIsLibrary);
		
		Arrays.sort(sections, new Comparator<AnyboxSection>() {
				@Override
				public int compare(AnyboxSection lhs, AnyboxSection rhs) {
					if (lhs.isFolder() != rhs.isFolder()) {
						if (lhs.isFolder()) return -1;
						else return 1;
					}
					int folderRes = compareRootFolder(lhs, rhs, parentIsLibrary);
					if (folderRes != 0) return folderRes;
					String lname = lhs.getName();
					String rname = rhs.getName();
					if (lname == null || rname == null) {
						if (lname == null) return 1;
						else return -1;
					}
					int res = lname.compareToIgnoreCase(rname);
					return res > 0 ? -1 : (res < 0 ? 1 : 0);
				}
			});
	}
	
	private static void sortSectionsByNameAsc(AnyboxSection[] sections, 
			final boolean parentIsLibrary) {
		if (sections == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("sortSectionsByNameAsc: parentIsLibrary=" + parentIsLibrary);
		
		Arrays.sort(sections, new Comparator<AnyboxSection>() {
				@Override
				public int compare(AnyboxSection lhs, AnyboxSection rhs) {
					if (lhs.isFolder() != rhs.isFolder()) {
						if (lhs.isFolder()) return -1;
						else return 1;
					}
					int folderRes = compareRootFolder(lhs, rhs, parentIsLibrary);
					if (folderRes != 0) return folderRes;
					String lname = lhs.getName();
					String rname = rhs.getName();
					if (lname == null || rname == null) {
						if (lname == null) return 1;
						else return -1;
					}
					int res = lname.compareToIgnoreCase(rname);
					return res > 0 ? 1 : (res < 0 ? -1 : 0);
				}
			});
	}
	
	private static void sortSectionsByUpdateDesc(AnyboxSection[] sections, 
			final boolean parentIsLibrary) {
		if (sections == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("sortSectionsByUpdateDesc: parentIsLibrary=" + parentIsLibrary);
		
		Arrays.sort(sections, new Comparator<AnyboxSection>() {
				@Override
				public int compare(AnyboxSection lhs, AnyboxSection rhs) {
					if (lhs.isFolder() != rhs.isFolder()) {
						if (lhs.isFolder()) return -1;
						else return 1;
					}
					int folderRes = compareRootFolder(lhs, rhs, parentIsLibrary);
					if (folderRes != 0) return folderRes;
					long ltm = lhs.getModifiedTime();
					long rtm = rhs.getModifiedTime();
					if (ltm > rtm) return -1;
					else if (ltm < rtm) return 1;
					else return 0;
				}
			});
	}
	
	private static void sortSectionsByUpdateAsc(AnyboxSection[] sections, 
			final boolean parentIsLibrary) {
		if (sections == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("sortSectionsByUpdateAsc: parentIsLibrary=" + parentIsLibrary);
		
		Arrays.sort(sections, new Comparator<AnyboxSection>() {
				@Override
				public int compare(AnyboxSection lhs, AnyboxSection rhs) {
					if (lhs.isFolder() != rhs.isFolder()) {
						if (lhs.isFolder()) return -1;
						else return 1;
					}
					int folderRes = compareRootFolder(lhs, rhs, parentIsLibrary);
					if (folderRes != 0) return folderRes;
					long ltm = lhs.getModifiedTime();
					long rtm = rhs.getModifiedTime();
					if (ltm > rtm) return 1;
					else if (ltm < rtm) return -1;
					else return 0;
				}
			});
	}
	
	private static void sortSectionsBySizeDesc(AnyboxSection[] sections, 
			final boolean parentIsLibrary) {
		if (sections == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("sortSectionsBySizeDesc: parentIsLibrary=" + parentIsLibrary);
		
		Arrays.sort(sections, new Comparator<AnyboxSection>() {
				@Override
				public int compare(AnyboxSection lhs, AnyboxSection rhs) {
					if (lhs.isFolder() != rhs.isFolder()) {
						if (lhs.isFolder()) return -1;
						else return 1;
					}
					int folderRes = compareRootFolder(lhs, rhs, parentIsLibrary);
					if (folderRes != 0) return folderRes;
					long llen = lhs.isFolder() ? lhs.getSubLength() : lhs.getLength();
					long rlen = rhs.isFolder() ? rhs.getSubLength() : rhs.getLength();
					if (llen > rlen) return -1;
					else if (llen < rlen) return 1;
					else return 0;
				}
			});
	}
	
	private static void sortSectionsBySizeAsc(AnyboxSection[] sections, 
			final boolean parentIsLibrary) {
		if (sections == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("sortSectionsBySizeAsc: parentIsLibrary=" + parentIsLibrary);
		
		Arrays.sort(sections, new Comparator<AnyboxSection>() {
				@Override
				public int compare(AnyboxSection lhs, AnyboxSection rhs) {
					if (lhs.isFolder() != rhs.isFolder()) {
						if (lhs.isFolder()) return -1;
						else return 1;
					}
					int folderRes = compareRootFolder(lhs, rhs, parentIsLibrary);
					if (folderRes != 0) return folderRes;
					long llen = lhs.isFolder() ? lhs.getSubLength() : lhs.getLength();
					long rlen = rhs.isFolder() ? rhs.getSubLength() : rhs.getLength();
					if (llen > rlen) return 1;
					else if (llen < rlen) return -1;
					else return 0;
				}
			});
	}
	
	private static void sortSectionsByTypeDesc(AnyboxSection[] sections, 
			final boolean parentIsLibrary) {
		if (sections == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("sortSectionsByTypeDesc: parentIsLibrary=" + parentIsLibrary);
		
		Arrays.sort(sections, new Comparator<AnyboxSection>() {
				@Override
				public int compare(AnyboxSection lhs, AnyboxSection rhs) {
					if (lhs.isFolder() != rhs.isFolder()) {
						if (lhs.isFolder()) return -1;
						else return 1;
					}
					int folderRes = compareRootFolder(lhs, rhs, parentIsLibrary);
					if (folderRes != 0) return folderRes;
					String ltype = lhs.getType();
					String rtype = rhs.getType();
					if (ltype == null || rtype == null) {
						if (ltype == null) return 1;
						else return -1;
					}
					int res = ltype.compareToIgnoreCase(rtype);
					return res > 0 ? -1 : (res < 0 ? 1 : 0);
				}
			});
	}
	
	private static void sortSectionsByTypeAsc(AnyboxSection[] sections, 
			final boolean parentIsLibrary) {
		if (sections == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("sortSectionsByTypeAsc: parentIsLibrary=" + parentIsLibrary);
		
		Arrays.sort(sections, new Comparator<AnyboxSection>() {
				@Override
				public int compare(AnyboxSection lhs, AnyboxSection rhs) {
					if (lhs.isFolder() != rhs.isFolder()) {
						if (lhs.isFolder()) return -1;
						else return 1;
					}
					int folderRes = compareRootFolder(lhs, rhs, parentIsLibrary);
					if (folderRes != 0) return folderRes;
					String ltype = lhs.getType();
					String rtype = rhs.getType();
					if (ltype == null || rtype == null) {
						if (ltype == null) return 1;
						else return -1;
					}
					int res = ltype.compareToIgnoreCase(rtype);
					return res > 0 ? 1 : (res < 0 ? -1 : 0);
				}
			});
	}
	
}
