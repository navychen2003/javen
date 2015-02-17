package org.javenstudio.cocoka.storage;

import java.io.IOException; 
import java.io.InputStream;
import java.util.ArrayList;

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.storage.fs.IFileSystem;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.util.MimeTypes;
import org.javenstudio.common.util.Logger;

public class MediaStorageProvider implements StorageProvider {
	private static Logger LOG = Logger.getLogger(MediaStorageProvider.class);

	protected final Storage mStorage; 
	
	public MediaStorageProvider(Storage store) {
		mStorage = store; 
		if (mStorage == null) 
    		throw new NullPointerException("Storage is null");
	} 
	
	@Override
	public final Storage getStorage() { return mStorage; }
	
	public boolean existsFile(String filename) {
		return mStorage.existsFile(filename); 
	}
	
	public IFile[] listFsFiles() throws IOException { 
		return listFsFiles((String)null); 
	}
	
	public IFile[] listFsFiles(String filepath) throws IOException { 
		//if (filepath == null || filepath.length() == 0) 
		//	return null; 
				
		IFile path = mStorage.newFsFileByName(filepath); 
		return listFsFiles(path);
	}
	
	public IFile[] listFsFiles(IFile path) throws IOException { 
		return mStorage.listFiles(path); 
	}
	
	public StorageFile[] listFiles() throws IOException {
		return listFiles(null); 
	}
	
	public StorageFile[] listFiles(String filepath) throws IOException {
		//if (filepath == null || filepath.length() == 0) 
		//	return null; 
		
		IFile path = mStorage.newFsFileByName(filepath); 
		IFile[] files = mStorage.listFiles(path); 
	
		ArrayList<StorageFile> storageFiles = new ArrayList<StorageFile>(); 
		
		for (int i=0; files != null && i < files.length; i++) {
			IFile file = files[i]; 
			if (file == null || file.isDirectory()) 
				continue; 
			
			String filename = file.getPath(); 
			String extension = null; 
			
			int pos = filename.lastIndexOf('.'); 
			if (pos > 0) extension = filename.substring(pos+1); 
			
			MimeType type = MimeTypes.getExtensionMimeType(extension); 
			StorageFile storageFile = null; 
			
			if (type == MimeType.TYPE_IMAGE) {
				storageFile = loadImageFile(filename); 
			} else if (type == MimeType.TYPE_IMAGE) {
				storageFile = loadAudioFile(filename); 
			}

			if (storageFile == null) { 
				if (LOG.isWarnEnabled())
					LOG.warn("MediaCache: file type not supported: "+filename); 
				
				continue; 
			}
			
			storageFiles.add(storageFile); 
		}
		
		return storageFiles.toArray(new StorageFile[storageFiles.size()]); 
	}
	
	public StorageFile loadFile(MimeType type, String filename) throws IOException {
		StorageFile file = mStorage.getFile(type, filename); 
		
		if (file == null) 
			throw new IOException("could not get file: "+filename);
		
		return file; 
	}
	
	public StorageFile loadAudioFile(String filename) throws IOException {
		return loadFile(MimeType.TYPE_AUDIO, filename); 
	}
	
	public StorageFile loadImageFile(String filename) throws IOException {
		return loadFile(MimeType.TYPE_IMAGE, filename); 
	}
	
	public StorageFile createTempAudioFile(String extensionName) throws IOException {
		return createAudioFile(null, extensionName); 
	}
	
	public StorageFile createTempAudioFile(InputStream is, 
			String extensionName) throws IOException {
		return createAudioFile(is, null, extensionName); 
	}
	
	public StorageFile createTempImageFile(String extensionName) throws IOException {
		return createImageFile(null, extensionName); 
	}
	
	public StorageFile createTempImageFile(InputStream is, 
			String extensionName) throws IOException {
		return createImageFile(is, null, extensionName); 
	}
	
	public StorageFile createTempImageFile(BitmapRef bitmap, 
			String extensionName) throws IOException {
		return createImageFile(bitmap, null, extensionName); 
	}
	
	public StorageFile createAudioFile(String fileName, 
			String extensionName) throws IOException {
		return createAudioFile((InputStream)null, fileName, extensionName); 
	}
	
	public StorageFile createAudioFile(InputStream is, String fileName, 
			String extensionName) throws IOException {
		if (extensionName == null || extensionName.length() == 0) 
			extensionName = "amr"; 
		
		return createFile(is, MimeType.TYPE_AUDIO, fileName, extensionName); 
	}
	
	public StorageFile createImageFile(String fileName, 
			String extensionName) throws IOException {
		return createImageFile((InputStream)null, fileName, extensionName); 
	}
	
	public StorageFile createImageFile(InputStream is, String fileName, 
			String extensionName) throws IOException {
		if (extensionName == null || extensionName.length() == 0) 
			extensionName = "jpg"; 
		
		return createFile(is, MimeType.TYPE_IMAGE, fileName, extensionName); 
	}
	
	public TempStorageFile createTempFile(MimeType type, String fileName, 
			String extensionName) throws IOException {
		if (extensionName == null || extensionName.length() == 0) 
			extensionName = "dat"; 
		
		TempStorageFile file = mStorage.createTempFile(type, fileName, extensionName); 
		
		if (file == null) 
			throw new IOException("could not create new "+extensionName+" temp file");
		
		return file; 
	}
	
	public StorageFile createFile(MimeType type, String fileName, 
			String extensionName) throws IOException {
		return createFile(null, type, fileName, extensionName); 
	}
	
	public StorageFile createFile(InputStream is, MimeType type, 
			String fileName, String extensionName) throws IOException {
		if (extensionName == null || extensionName.length() == 0) 
			extensionName = "dat"; 
		
		StorageFile file = mStorage.createFile(type, fileName, extensionName); 
		
		if (file == null) 
			throw new IOException("could not create new "+extensionName+" file");
		
		if (is != null) {
			IFile fsfile = file.getFile(); 
			Storage store = file.getStorage(); 
			IFileSystem fs = store.getFileSystem(); 
			
			FileHelper.saveFile(fs, fsfile, is, store.getWriteBufferSize()); 
		}
		
		return file; 
	}
	
	public StorageFile createImageFile(BitmapRef bitmap, String fileName, 
			String extensionName) throws IOException {
		if (bitmap == null) return null; 
		
		if (extensionName == null || extensionName.length() == 0) 
			extensionName = "jpg"; 
		
		StorageFile file = mStorage.createFile(MimeType.TYPE_IMAGE, fileName, extensionName); 
		
		if (file == null) 
			throw new IOException("could not create new "+extensionName+" file");
		
		IFile fsfile = file.getFile(); 
		Storage store = file.getStorage(); 
		IFileSystem fs = store.getFileSystem(); 
		
		FileHelper.saveBitmap(fs, fsfile, bitmap, store.getWriteBufferSize()); 
		
		return file; 
	}
	
	public StorageFile openImageFile(String filepath) throws IOException {
		StorageFile file = mStorage.getFileByPath(MimeType.TYPE_IMAGE, filepath); 
		
		if (file == null) 
			throw new IOException("could not open "+filepath+" file");
		
		return file; 
	}
	
	public StorageFile openAudioFile(String filepath) throws IOException {
		StorageFile file = mStorage.getFileByPath(MimeType.TYPE_AUDIO, filepath); 
		
		if (file == null) 
			throw new IOException("could not open "+filepath+" file");
		
		return file; 
	}
	
	public StorageFile openFile(String filepath) throws IOException {
		StorageFile file = mStorage.getFileByPath(MimeType.TYPE_APPLICATION, filepath); 
		
		if (file == null) 
			throw new IOException("could not open "+filepath+" file");
		
		return file; 
	}
	
	/*
	private static final String[] CURSOR_COLS = new String[] {
		MediaStore.Audio.Media._ID, 
		MediaStore.Audio.Media.DISPLAY_NAME,
		MediaStore.Audio.Media.TITLE, 
		MediaStore.Audio.Media.DURATION,
		MediaStore.Audio.Media.ARTIST, 
		MediaStore.Audio.Media.ALBUM,
		MediaStore.Audio.Media.YEAR, 
		MediaStore.Audio.Media.MIME_TYPE,
		MediaStore.Audio.Media.SIZE, 
		MediaStore.Audio.Media.DATA };
	
	public static MediaInfo getMediaInfo(final Context context, final String filePath) {
		if (context == null || filePath == null || filePath.length() == 0) 
			return null; 
		
		Uri Media_URI = null; 
		String where = null; 
		String selectionArgs[] = null; 
		
		if (filePath.startsWith("content://media/")) {
			Media_URI = Uri.parse(filePath); 
			where = null; 
			selectionArgs = null; 
			
		} else {
			Media_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
			where = MediaStore.MediaColumns.DATA + "=?";
			selectionArgs = new String[] { filePath };

		}
		
		return getMediaInfo(context, Media_URI, where, selectionArgs); 
	}
	
	public static MediaInfo getMediaInfo(final Context context, final Uri uri) {
		return getMediaInfo(context, uri, null, null); 
	}
	
	public static MediaInfo getMediaInfo(final Context context, 
			final Uri Media_URI, String where, String[] selectionArgs) {
		if (context == null || Media_URI == null) 
			return null; 
		
		Cursor cursor = context.getContentResolver().query(Media_URI,
				CURSOR_COLS, where, selectionArgs, null);

		if (cursor == null || cursor.getCount() == 0) {
			return null; 

		} else {
			cursor.moveToFirst();
			MediaInfo info = getMediaInfoFromCursor(cursor);
			return info; 
		}
	}
	
	public static MediaInfo getMediaInfoFromCursor(Cursor cursor) {
		if (cursor == null) return null; 
		
		MediaInfo info = new MediaInfo(); 
		
		info.mFileName = cursor.getString(1);
		info.mMediaName = cursor.getString(2); 
		info.mDuration = cursor.getInt(3); 
		info.mMediaArtist = cursor.getString(4); 
		info.mMediaAlbum = cursor.getString(5); 
		info.mMediaYear = cursor.getString(6); 
		info.mFileType = cursor.getString(7); 
		info.mMediaSize = cursor.getInt(8); 
		info.mFilePath = cursor.getString(9); 
		
		return info; 
	}
	*/
}
