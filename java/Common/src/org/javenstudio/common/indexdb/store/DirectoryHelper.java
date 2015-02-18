package org.javenstudio.common.indexdb.store;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;

public class DirectoryHelper {

	public static void deleteFilesIgnoringExceptions(IDirectory dir, String... files) {
		for (String name : files) {
			try {
				dir.deleteFile(name);
			} catch (IOException ignored) {
				// ignore
			}
		}
	}
	
}
