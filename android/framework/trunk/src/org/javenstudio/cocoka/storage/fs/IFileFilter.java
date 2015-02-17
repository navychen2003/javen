package org.javenstudio.cocoka.storage.fs;

public interface IFileFilter {
	
    /**
     * Checks if an File entry should be included or not.
     * 
     * @param file entry to be checked for inclusion. May be <code>null</code>.
     * @return <code>true</code> if the file is to be included, <code>false</code> otherwise
     */
    public boolean accept(String path);
    
}
