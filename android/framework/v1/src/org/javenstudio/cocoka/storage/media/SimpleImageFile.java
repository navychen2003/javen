package org.javenstudio.cocoka.storage.media;

import org.javenstudio.cocoka.storage.FileCache;
import org.javenstudio.cocoka.storage.FileCacheFactory;
import org.javenstudio.cocoka.storage.Storage;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.BitmapCacheFile;
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
	public BitmapCacheFile getBitmapFile() { 
		return null; 
	}
	
}