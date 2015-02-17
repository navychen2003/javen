package org.javenstudio.cocoka.storage;

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.AudioFile;
import org.javenstudio.cocoka.util.MimeType;

public class SimpleAudioFile extends BaseMediaFile implements AudioFile {

	private final FileCacheFactory mFactory; 
	
	public SimpleAudioFile(Storage store, IFile file) {
		super(store, file); 
		
		mFactory = new FileCacheFactory() {
				@Override
				public FileCache create() {
					return new FileCache(SimpleAudioFile.this);
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
	
}
