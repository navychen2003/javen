package org.javenstudio.provider.library.list;

public class LibraryListFactory {

	public LibraryListDataSets createLibraryListDataSets(LibraryListProvider provider) {
		return new LibraryListDataSets(new LibraryListCursorFactory());
	}
	
	public LibraryListBinder createLibraryListBinder(LibraryListProvider provider) {
		return new LibraryListBinder(provider);
	}
	
}
