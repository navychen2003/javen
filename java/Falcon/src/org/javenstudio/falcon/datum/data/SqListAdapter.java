package org.javenstudio.falcon.datum.data;

import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.ISectionCollector;

public abstract class SqListAdapter {

	private SqSectionList mSectionList = null;
	
	protected abstract SqSection[] loadSections() throws ErrorException;
	
	public SqSectionList getSectionList(ISectionCollector collector) 
			throws ErrorException { 
		if (mSectionList == null) { 
			SqSection[] sections = loadSections();
			if (sections == null) sections = new SqSection[0];
			
			Arrays.sort(sections, new Comparator<SqSection>() {
					@Override
					public int compare(SqSection o1, SqSection o2) {
						boolean isfolder1 = o1.isFolder();
						boolean isfolder2 = o2.isFolder();
						if (isfolder1 != isfolder2) { 
							if (isfolder1) return -1;
							return 1;
						}
						String name1 = o1.getName();
						String name2 = o2.getName();
						return name1.compareToIgnoreCase(name2);
					}
				});
			
			mSectionList = new SqSectionList(sections);
		}
		
		if (collector != null) { 
			SqSectionList list = mSectionList;
			if (list != null) { 
				SqSection[] sections = list.getSections();
				if (sections != null) { 
					for (SqSection section : sections) { 
						if (section != null)
							collector.addSection(section);
					}
				}
			}
		}
		
		return mSectionList;
	}
	
	public void close() { 
		SqSectionList sections = mSectionList;
		mSectionList = null;
		
		for (int i=0; sections != null && i < sections.getSectionCount(); i++) { 
			SqSection section = sections.getSectionAt(i);
			if (section != null) 
				section.close();
		}
	}
	
}
