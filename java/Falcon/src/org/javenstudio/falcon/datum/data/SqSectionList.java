package org.javenstudio.falcon.datum.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.falcon.datum.ISectionQuery;

final class SqSectionList {

	public abstract class SortList { 
		private final SqSort mSort;
		private SqSection[] mSectionsAsc = null;
		private SqSection[] mSectionsDesc = null;
		
		public SortList(String name, String title) { 
			mSort = new SqSort(name, title);
		}
		
		public SqSort getSort() { return mSort; }
		public String getSortName() { return mSort.getName(); }
		
		public synchronized SqSection[] getAsc() { 
			if (mSectionsAsc == null) 
				mSectionsAsc = initList(getAscComparator());
			return mSectionsAsc; 
		}
		
		public synchronized SqSection[] getDesc() { 
			if (mSectionsDesc == null) 
				mSectionsDesc = initList(getDescComparator());
			return mSectionsDesc; 
		}
		
		protected abstract Comparator<SqSection> getAscComparator();
		protected abstract Comparator<SqSection> getDescComparator();
	}
	
	final class NameSortList extends SortList { 
		public NameSortList() { 
			super("name", "Name");
		}

		@Override
		protected Comparator<SqSection> getAscComparator() {
			return new Comparator<SqSection>() {
					@Override
					public int compare(SqSection o1, SqSection o2) {
						boolean d1 = o1.isFolder();
						boolean d2 = o2.isFolder();
						return d1 != d2 ? (d1 ? -1 : 1) : 
							(o1.getName().compareTo(o2.getName()));
					}
				};
		}

		@Override
		protected Comparator<SqSection> getDescComparator() {
			return new Comparator<SqSection>() {
					@Override
					public int compare(SqSection o1, SqSection o2) {
						boolean d1 = o1.isFolder();
						boolean d2 = o2.isFolder();
						return d1 != d2 ? (d1 ? -1 : 1) : 
							(o2.getName().compareTo(o1.getName()));
					}
				};
		}
	}
	
	final class TypeSortList extends SortList { 
		public TypeSortList() { 
			super("type", "Type");
		}

		@Override
		protected Comparator<SqSection> getAscComparator() {
			return new Comparator<SqSection>() {
					@Override
					public int compare(SqSection o1, SqSection o2) {
						boolean d1 = o1.isFolder();
						boolean d2 = o2.isFolder();
						
						int t = o1.getContentType().compareTo(o2.getContentType());
						
						return d1 != d2 ? (d1 ? -1 : 1) : 
							(t != 0 ? t : o1.getName().compareTo(o2.getName()));
					}
				};
		}

		@Override
		protected Comparator<SqSection> getDescComparator() {
			return new Comparator<SqSection>() {
					@Override
					public int compare(SqSection o1, SqSection o2) {
						boolean d1 = o1.isFolder();
						boolean d2 = o2.isFolder();
						
						int t = o2.getContentType().compareTo(o1.getContentType());
						
						return d1 != d2 ? (d1 ? -1 : 1) : 
							(t != 0 ? t : o2.getName().compareTo(o1.getName()));
					}
				};
		}
	}
	
	final class UpdateSortList extends SortList { 
		public UpdateSortList() { 
			super("update", "Date Updated");
		}

		@Override
		protected Comparator<SqSection> getAscComparator() {
			return new Comparator<SqSection>() {
					@Override
					public int compare(SqSection o1, SqSection o2) {
						boolean d1 = o1.isFolder();
						boolean d2 = o2.isFolder();
						
						long date1 = o1.getModifiedTime();
						long date2 = o2.getModifiedTime();
						
						return d1 != d2 ? (d1 ? -1 : 1) : 
							(date1 > date2 ? 1 : (date1 < date2 ? -1 : 0));
					}
				};
		}

		@Override
		protected Comparator<SqSection> getDescComparator() {
			return new Comparator<SqSection>() {
					@Override
					public int compare(SqSection o1, SqSection o2) {
						boolean d1 = o1.isFolder();
						boolean d2 = o2.isFolder();
						
						long date1 = o1.getModifiedTime();
						long date2 = o2.getModifiedTime();
						
						return d1 != d2 ? (d1 ? -1 : 1) : 
							(date1 > date2 ? -1 : (date1 < date2 ? 1 : 0));
					}
				};
		}
	}
	
	final class SizeSortList extends SortList { 
		public SizeSortList() { 
			super("size", "Size");
		}

		@Override
		protected Comparator<SqSection> getAscComparator() {
			return new Comparator<SqSection>() {
					@Override
					public int compare(SqSection o1, SqSection o2) {
						boolean d1 = o1.isFolder();
						boolean d2 = o2.isFolder();
						
						long len1 = o1.getContentLength();
						long len2 = o2.getContentLength();
						
						return d1 != d2 ? (d1 ? -1 : 1) : 
							(len1 > len2 ? 1 : (len1 < len2 ? -1 : 0));
					}
				};
		}

		@Override
		protected Comparator<SqSection> getDescComparator() {
			return new Comparator<SqSection>() {
					@Override
					public int compare(SqSection o1, SqSection o2) {
						boolean d1 = o1.isFolder();
						boolean d2 = o2.isFolder();
						
						long len1 = o1.getContentLength();
						long len2 = o2.getContentLength();
						
						return d1 != d2 ? (d1 ? -1 : 1) : 
							(len1 > len2 ? -1 : (len1 < len2 ? 1 : 0));
					}
				};
		}
	}
	
	private final Map<String, SortList> mSortMap;
	private final List<SqSort> mSortList;
	private final SqGroup[] mGroups;
	private final SqSection[] mSections;
	
	public SqSectionList(SqSection[] sections) { 
		mSortMap = new HashMap<String, SortList>();
		mSortList = new ArrayList<SqSort>();
		mGroups = new SqGroup[] { new SqGroup("all", "All") };
		mSections = sections;
		
		addSortList(new UpdateSortList()); 
		addSortList(new SizeSortList()); 
		addSortList(new TypeSortList()); 
		addSortList(new NameSortList()); 
	}
	
	public SqGroup[] getGroups() { return mGroups; }
	
	public void addSortList(SortList sortList) { 
		if (sortList == null) return;
		
		synchronized (mSortMap) { 
			String key = sortList.getSortName();
			if (mSortMap.containsKey(key))
				throw new IllegalArgumentException("Sort: " + key + " already added");
			
			mSortMap.put(sortList.getSortName(), sortList);
			mSortList.add(sortList.getSort());
		}
	}
	
	public SortList getSortList(String sort) { 
		if (sort != null && sort.length() > 0) { 
			int pos = sort.indexOf('.');
			if (pos > 0) sort = sort.substring(0, pos);
			
			synchronized (mSortMap) { 
				return mSortMap.get(sort);
			}
		}
		
		return null;
	}
	
	public SqSort[] getSorts() { 
		synchronized (mSortMap) {
			return mSortList.toArray(new SqSort[mSortList.size()]);
		}
	}
	
	public int getSectionCount() { 
		return mSections != null ? mSections.length : 0;
	}
	
	public SqSection getSectionAt(int index) { 
		return mSections != null && index >= 0 && index < mSections.length ? 
				mSections[index] : null;
	}
	
	private SqSection[] initList(Comparator<SqSection> comp) { 
		if (mSections != null && comp != null) { 
			SqSection[] result = new SqSection[mSections.length];
			System.arraycopy(mSections, 0, result, 0, mSections.length);
			Arrays.sort(result, comp);
			return result;
		}
		return mSections; 
	}
	
	public SqSection[] getSections() { 
		return mSections;
	}
	
	public SqSectionSet getSectionSet(ISectionQuery query) { 
		if (query != null) { 
			return getSectionSet(query.getFilter(), query.getSortParam(), 
					(int)query.getResultStart(), 
					(int)query.getResultCount());
		}
		return null;
	}
	
	public SqSectionSet getSectionSet(ISectionQuery.Filter filter, 
			String sort, int start, int count) { 
		boolean asc = true;
		SortList sortList = null;
		int totalCount = 0;
		int offset = 0;
		
		SqSection[] sections = getSections();
		if (sections != null) 
			totalCount = sections.length;
		
		if (sort == null || sort.length() == 0) 
			sort = "name";
		
		if (sort != null) { 
			if (sort.indexOf(".asc") > 0)
				asc = true;
			else if (sort.indexOf(".desc") > 0)
				asc = false;
			
			sortList = getSortList(sort);
			if (sortList != null && (!sortList.getSortName().equals("name") || !asc)) 
				sections = asc ? sortList.getAsc() : sortList.getDesc();
			else
				sort = "name.asc";
		}
		
		if (sections == null || sections.length == 0) 
			return null;
		
		if (filter != null) {
			ArrayList<SqSection> list = new ArrayList<SqSection>();
			for (SqSection section : sections) {
				if (section != null && filter.acceptSection(section))
					list.add(section);
			}
			sections = list.toArray(new SqSection[list.size()]);
		}
		
		if (sections == null || sections.length == 0) 
			return null;
		
		totalCount = sections.length;
		
		if (start <= 0) start = 0;
		if (count > sections.length - start || count <= 0) 
			count = sections.length - start;
		
		//if (start == 0 && count == sections.length) 
		//	return sections;
		
		return new SqSectionSet(getGroups(), getSorts(), 
				sections, sort, start, count, totalCount, offset);
	}
	
}
