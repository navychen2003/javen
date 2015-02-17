package org.javenstudio.cocoka.storage;

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.BitmapFile;
import org.javenstudio.cocoka.util.ImageFile;
import org.javenstudio.cocoka.util.MimeType;

public class SimpleImageFile extends BaseMediaFile implements ImageFile {

	private final FileCacheFactory mFactory; 
	
	public SimpleImageFile(Storage store, IFile file) {
		super(store, file); 
		
		mFactory = new FileCacheFactory() {
				@Override
				public FileCache create() {
					return new FileCache(SimpleImageFile.this);
				}
			};
	}
	
	@Override 
	public MimeType getMimeType() {
		return MimeType.TYPE_AUDIO; 
	}
	
	@Override 
	protected FileCacheFactory getCacheFactory() { 
		return mFactory; 
	}
	
	@Override 
	public BitmapFile getBitmapFile() { 
		return null; 
	}
	
}