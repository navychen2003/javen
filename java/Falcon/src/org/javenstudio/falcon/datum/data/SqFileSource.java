package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.io.InputStream;

public class SqFileSource extends FileSource {

	public static final long MAPFILE_MAXLEN = 2 * 1024 * 1024;
	public static final long METAFILE_MAXLEN = 8 * 1024 * 1024;
	
	private final SqRoot mRoot;
	
	public SqFileSource(SqRoot root) { 
		if (root == null) throw new NullPointerException();
		mRoot = root;
	}
	
	@Override
	public InputStream getInputStream(Item item) throws IOException {
		if (item == null) return null;
		if (item instanceof StreamItem) { 
			StreamItem streamItem = (StreamItem)item;
			return streamItem.getStream().getStream();
		} else if (item instanceof SqFileItem) { 
			SqFileItem fileItem = (SqFileItem)item;
			return fileItem.getRootFile().open();
		}
		return null;
	}

	@Override
	public SqRoot getRoot() {
		return mRoot;
	}

	@Override
	public boolean isStoreAllToMapFile(FileSource.Item file) { 
		return false; //file.getLength() < MAPFILE_MAXLEN;
	}
	
	@Override
	public boolean isStoreAllToMetaFile(FileSource.Item file) {
		return file.getAttrs().getLength() < METAFILE_MAXLEN;
	}

}
