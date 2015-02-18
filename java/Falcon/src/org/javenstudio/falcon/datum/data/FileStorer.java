package org.javenstudio.falcon.datum.data;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.MimeTypes;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.IImageSource;
import org.javenstudio.falcon.datum.MetadataLoader;
import org.javenstudio.falcon.datum.util.BytesBufferPool;
import org.javenstudio.falcon.datum.util.ImageCache;
import org.javenstudio.falcon.datum.util.MapFileFormat;
import org.javenstudio.falcon.datum.util.RecordWriter;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.MapFile;
import org.javenstudio.raptor.io.Text;

public abstract class FileStorer extends FileIO {
	private static final Logger LOG = Logger.getLogger(FileStorer.class);

	public static final String FILE_NAME = ".z";
	
	public static final int FILE_MAXCOUNT = 100000;
	public static final int FILE_MAXLEN = 96 * 1024 * 1024;
	
	private interface INameWriter { 
		public void write(Text key, NameData value) throws IOException, ErrorException;
		public void close() throws IOException, ErrorException;
	}
	
	private interface IFileWriter { 
		public void write(Text key, FileData value) throws IOException, ErrorException;
		public void close() throws IOException, ErrorException;
	}
	
	@SuppressWarnings("unused")
	private class NameDataWriter implements INameWriter { 
		private final RecordWriter<Text,NameData> mNameWriter;
		private final Map<Text,NameData> mNameMap;
		private int mTotalCount = 0;
		private long mTotalSize = 0;
		
		public NameDataWriter(RecordWriter<Text,NameData> writer) { 
			mNameWriter = writer;
			mNameMap = new TreeMap<Text,NameData>(
				new Comparator<Text>() {
					@Override
					public int compare(Text o1, Text o2) { 
						return o1.compareTo(o2);
					}
				});
		}
		
		@Override
		public void write(Text key, NameData value) throws IOException { 
			if (key == null || value == null) return;
			mNameMap.put(key, value);
			mTotalCount ++;
			mTotalSize += value.getBufferSize();
		}
		
		@Override
		public void close() throws IOException { 
			for (Map.Entry<Text, NameData> entry : mNameMap.entrySet()) { 
				Text key = entry.getKey();
				NameData value = entry.getValue();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("close: write NameData: key=" + key 
							+ " data=" + value);
				}
				
				mNameWriter.write(key, value);
			}
			mNameWriter.close();
		}
		
		public int getTotalCount() { return mTotalCount; }
		public long getTotalSize() { return mTotalSize; }
	}
	
	private class FileDataWriter implements IFileWriter { 
		private final RecordWriter<Text,FileData> mFileWriter;
		private final Map<Text,FileData> mFileMap;
		private final int mWriterIndex;
		private int mTotalCount = 0;
		private long mTotalSize = 0;
		
		public FileDataWriter(RecordWriter<Text,FileData> writer, int idx) { 
			mFileWriter = writer;
			mWriterIndex = idx;
			mFileMap = new TreeMap<Text,FileData>(
				new Comparator<Text>() {
					@Override
					public int compare(Text o1, Text o2) { 
						return o1.compareTo(o2);
					}
				});
		}
		
		@Override
		public void write(Text key, FileData value) throws IOException { 
			if (key == null || value == null) return;
			value.getAttrs().setFileIndex(mWriterIndex);
			mFileMap.put(key, value);
			mTotalCount ++;
			mTotalSize += value.getBufferSize();
		}
		
		@Override
		public void close() throws IOException { 
			for (Map.Entry<Text, FileData> entry : mFileMap.entrySet()) { 
				Text key = entry.getKey();
				FileData value = entry.getValue();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("close: write FileData: key=" + key 
							+ " data=" + value);
				}
				
				mFileWriter.write(key, value);
			}
			mFileWriter.close();
		}
		
		public int getTotalCount() { return mTotalCount; }
		public long getTotalSize() { return mTotalSize; }
	}
	
	private class FileDataWriters implements IFileWriter { 
		private final FileSystem mFs;
		private final MapFileFormat<Text,FileData> mFormat;
		private final Path mStorePath;
		private FileDataWriter mWriter = null;
		private int mWriterIndex = 0;
		
		public FileDataWriters(MapFileFormat<Text,FileData> format, 
				FileSystem fs, Path storePath) { 
			mFormat = format;
			mStorePath = storePath;
			mFs = fs;
		}
		
		private Map<Text,FileData> loadFileDatas(Path path) throws IOException { 
			final Map<Text,FileData> map = new TreeMap<Text,FileData>(
				new Comparator<Text>() {
					@Override
					public int compare(Text o1, Text o2) { 
						return o1.compareTo(o2);
					}
				});
			
			MapFile.Reader reader = mFormat.getMapReader(path);
			while (reader != null) { 
				Text key = new Text();
				FileData data = new FileData();
				
				if (reader.next(key, data) == false) 
					break;
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("loadFileDatas: key=" + key + " attrs=" + data.getAttrs());
				}
				
				map.put(key, data);
			}
			
			return map;
		}
		
		@Override
		public synchronized void write(Text key, FileData value) 
				throws IOException, ErrorException { 
			if (mWriter == null) { 
				while (true) {
					mWriterIndex ++;
					Path path = new Path(mStorePath, FileStorer.FILE_NAME + mWriterIndex);
					
					Map<Text,FileData> map = null;
					if (mFs.exists(path)) { 
						Path dataPath = new Path(path, "data");
						try {
							FileStatus status = mFs.getFileStatus(dataPath);
							if (status != null) {
								if (status.getLen() >= FILE_MAXLEN) continue;
								map = loadFileDatas(path);
							}
						} catch (FileNotFoundException ignore) { 
						}
					}
					
					if (LOG.isDebugEnabled())
						LOG.debug("getFileWriter: file mapFile: " + path);
					
					MapFileFormat<Text,FileData> format = mFormat;
					
					RecordWriter<Text,FileData> writer = 
							format.getRecordWriter(path, null);
					
					mWriter = new FileDataWriter(writer, mWriterIndex);
					
					if (map != null) { 
						for (Map.Entry<Text, FileData> entry : map.entrySet()) { 
							Text entryKey = entry.getKey();
							FileData entryVal = entry.getValue();
							mWriter.write(entryKey, entryVal);
						}
					}
					break;
				}
			}
			
			mWriter.write(key, value);
			
			if (mWriter.getTotalCount() >= FILE_MAXCOUNT || 
				mWriter.getTotalSize() >= FILE_MAXLEN) {
				mWriter.close();
				mWriter = null;
			}
		}
		
		@Override
		public synchronized void close() throws IOException { 
			if (mWriter != null)
				mWriter.close();
			
			mWriter = null;
			mWriterIndex = 0;
		}
	}
	
	private MapFileFormat<Text,NameData> mNameFormat = null;
	private NameDataWriter mNameWriter = null;
	
	private MapFileFormat<Text,FileData> mFileFormat = null;
	private FileDataWriters mFileWriters = null;
	
	public abstract FileSource getSource();
	
	public SqRoot getRoot() { return getSource().getRoot(); }
	public SqLibrary getLibrary() { return getRoot().getLibrary(); }
	public DataManager getManager() { return getLibrary().getManager(); }
	
	public Path getStorePath() throws ErrorException { 
		return getStorePath(getLibrary(), getRoot().getContentKey());
	}
	
	public static Path getStorePath(SqLibrary library, String rootKey) 
			throws ErrorException { 
		Path libraryPath = library.getManager().getLibraryPath(library);
		Path storePath = libraryPath; //new Path(libraryPath, rootKey);
		return storePath;
	}
	
	private Object fileLock() { return getLibrary().getLock(); }
	private Object nameLock() { return getRoot().getLock(); }
	
	private NameDataWriter getNameWriter() throws IOException, ErrorException { 
		synchronized (nameLock()) {
			if (mNameFormat != null && mNameWriter != null)
				return mNameWriter;
			
			Configuration conf = getManager().getConfiguration();
			FileSystem fs = getLibrary().getStoreFs();
	
			Path storePath = getStorePath();
			fs.mkdirs(storePath);
			
			if (true) {
				Path path = new Path(storePath, //FileStorer.NAME_NAME +
						"." + getRoot().getContentKey());
				
				if (LOG.isDebugEnabled())
					LOG.debug("getNameWriter: name mapFile: " + path);
				
				MapFileFormat<Text,NameData> format = 
						new MapFileFormat<Text,NameData>(
								conf, fs, Text.class, NameData.class);
				
				RecordWriter<Text,NameData> writer = 
						format.getRecordWriter(path, null);
				
				mNameFormat = format;
				mNameWriter = new NameDataWriter(writer);
			}
			
			return mNameWriter;
		}
	}
	
	private FileDataWriters getFileWriters() throws IOException, ErrorException { 
		synchronized (fileLock()) {
			if (mFileFormat != null && mFileWriters != null)
				return mFileWriters;
			
			Configuration conf = getManager().getConfiguration();
			FileSystem fs = getLibrary().getStoreFs();
	
			Path storePath = getStorePath();
			fs.mkdirs(storePath);
			
			if (true) {
				//Path path = new Path(storePath, FILE_NAME);
				
				//if (LOG.isDebugEnabled())
				//	LOG.debug("getFileWriter: file mapFile: " + path);
				
				MapFileFormat<Text,FileData> format = 
						new MapFileFormat<Text,FileData>(
								conf, fs, Text.class, FileData.class);
				
				//RecordWriter<Text,FileData> writer = 
				//		format.getRecordWriter(path, null);
				
				mFileFormat = format;
				mFileWriters = new FileDataWriters(format, fs, storePath);
			}
			
			return mFileWriters;
		}
	}
	
	private FileDataWriter createFileWriter(int index) 
			throws IOException, ErrorException { 
		synchronized (fileLock()) {
			Configuration conf = getManager().getConfiguration();
			FileSystem fs = getLibrary().getStoreFs();
	
			Path storePath = getStorePath();
			fs.mkdirs(storePath);
			
			//if (true) {
				Path path = new Path(storePath, FILE_NAME + index);
				
				if (LOG.isDebugEnabled())
					LOG.debug("createFileWriter: file mapFile: " + path);
				
				MapFileFormat<Text,FileData> format = 
						new MapFileFormat<Text,FileData>(
								conf, fs, Text.class, FileData.class);
				
				RecordWriter<Text,FileData> writer = 
						format.getRecordWriter(path, null);
				
				return new FileDataWriter(writer, index);
			//}
		}
	}
	
	public void closeFiles() throws IOException, ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("closeFiles");
		
		synchronized (fileLock()) {
			FileDataWriters fileWriters = mFileWriters;
			if (fileWriters != null)
				fileWriters.close();
			
			mFileFormat = null;
			mFileWriters = null;
			
			getLibrary().reset();
		}
	}
	
	public void closeNames() throws IOException, ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("closeNames");
		
		synchronized (nameLock()) {
			NameDataWriter nameWriter = mNameWriter;
			if (nameWriter != null) 
				nameWriter.close();
			
			mNameFormat = null;
			mNameWriter = null;
		}
	}
	
	@Override
	public void close() throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("close");
		
		try {
			closeFiles();
			closeNames();
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
		
		super.close();
	}
	
	public void storeName(Map<Text,NameData> nameMap) 
			throws IOException, ErrorException { 
		if (nameMap == null) return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("storeName: root=" + getRoot().getContentKey() 
					+ " nameMap.size=" + nameMap.size());
		}
		
		synchronized (nameLock()) {
			for (Map.Entry<Text, NameData> entry : nameMap.entrySet()) { 
				Text key = entry.getKey();
				NameData data = entry.getValue();
				
				storeName(key, data);
			}
		}
	}
	
	public void storeName(Text key, NameData data) 
			throws IOException, ErrorException { 
		if (key == null || data == null) return;
		
		NameDataWriter writer = getNameWriter();
		if (writer == null) return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("storeName: key=" + key + " attrs=" + data.getAttrs());
		}
		
		writer.write(key, data);
	}
	
	public void storeName(FileSource.Item item) 
			throws IOException, ErrorException { 
		if (item == null) return;
		storeName(item, item.getAttrs().getFileIndex());
	}
	
	public void storeName(FileSource.Item item, int fileidx) 
			throws IOException, ErrorException { 
		synchronized (nameLock()) {
			doStoreName(item, fileidx);
		}
	}
	
	private void doStoreName(FileSource.Item item, int fileidx) 
			throws IOException, ErrorException { 
		if (item == null) return;
		
		NameDataWriter writer = getNameWriter();
		if (writer == null) return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("storeName: key=" + item.getKey() 
					+ " attrs=" + item.getAttrs());
		}
		
		Text itemKey = new Text(item.getKey());
		
		if (item instanceof FileSource.DirItem) { 
			FileSource.DirItem dir = (FileSource.DirItem)item;
			
			ArrayList<Text> list = new ArrayList<Text>();
			for (int i=0; i < dir.getItemCount(); i++) { 
				FileSource.Item subItem = dir.getItemAt(i);
				if (subItem != null) 
					list.add(new Text(subItem.getKey()));
			}
			
			Text[] fileKeys = list.toArray(new Text[list.size()]);
			
			NameData data = new NameData(itemKey, item.getAttrs(), fileKeys);
			data.getAttrs().setFileIndex(fileidx);
			
			writer.write(itemKey, data);
		} else {
			NameData data = new NameData(itemKey, item.getAttrs(), null);
			data.getAttrs().setFileIndex(fileidx);
			
			writer.write(itemKey, data);
		}
	}
	
	public void storeFile(int fileIndex, Map<Text,FileData> fileMap) 
			throws IOException, ErrorException { 
		if (fileMap == null) return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("storeFile: root=" + getRoot().getContentKey() 
					+ " fileMap.size=" + fileMap.size());
		}
		
		synchronized (fileLock()) {
			FileDataWriter writer = createFileWriter(fileIndex);
			
			try {
				for (Map.Entry<Text, FileData> entry : fileMap.entrySet()) { 
					Text key = entry.getKey();
					FileData data = entry.getValue();
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("storeFile: key=" + key + " attrs=" + data.getAttrs());
					}
					
					writer.write(key, data);
				}
			} finally { 
				writer.close();
			}
		}
	}
	
	public void storeFile(Text key, FileData data) 
			throws IOException, ErrorException { 
		if (key == null || data == null) 
			return;
		
		synchronized (fileLock()) {
			FileDataWriters writer = getFileWriters();
			if (writer == null) return;
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("storeFile: key=" + key + " attrs=" + data.getAttrs());
			}
			
			writer.write(key, data);
		}
	}
	
	public boolean removeFsName() throws IOException, ErrorException { 
		synchronized (nameLock()) {
			if (getRoot().getTotalFileCount() > 0 || 
				getRoot().getTotalFolderCount() > 0 || 
				getRoot().getSubCount() > 0) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Root: " + getRoot() + " is not empty, cannot delete.");
			}
			
			Path path = new Path(getStorePath(), //FileStorer.NAME_NAME +
					"." + getRoot().getContentKey());
			
			Path indexPath = new Path(path, "index");
			Path dataPath = new Path(path, "data");
			
			FileSystem fs = getLibrary().getStoreFs();
			if (!fs.exists(path)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("removeFsFile: namePath=" + path 
							+ " not existed");
				}
				
				return true;
			}
			
			boolean res1 = fs.delete(indexPath, true);
			
			if (LOG.isInfoEnabled()) {
				LOG.info("removeFsFile: indexPath=" + indexPath 
						+ " result=" + res1);
			}
			
			boolean res2 = fs.delete(dataPath, true);
			
			if (LOG.isInfoEnabled()) {
				LOG.info("removeFsFile: dataPath=" + dataPath 
						+ " result=" + res2);
			}
			
			if (res1 || res2) {
				boolean res = fs.delete(path, true);
				
				if (LOG.isInfoEnabled()) {
					LOG.info("removeFsFile: namePath=" + path 
							+ " result=" + res);
				}
				
				return res;
			}
			
			return false;
		}
	}
	
	public boolean removeFsFile(String fileKey) 
			throws IOException, ErrorException { 
		if (fileKey == null || fileKey.length() == 0)
			return false;
		
		synchronized (fileLock()) {
			Path storePath = new Path(getStorePath(), ""+fileKey.charAt(0));
			Path filePath = new Path(storePath, fileKey + ".file");
			Path dataPath = new Path(storePath, fileKey + ".dat");
			
			FileSystem fs = getLibrary().getStoreFs();
			
			boolean res1 = fs.delete(filePath, false);
			
			if (LOG.isInfoEnabled()) {
				LOG.info("removeFsFile: key=" + fileKey + " filePath=" + filePath 
						+ " result=" + res1);
			}
			
			boolean res2 = fs.delete(dataPath, false);
			
			if (LOG.isInfoEnabled()) {
				LOG.info("removeFsFile: key=" + fileKey + " dataPath=" + dataPath 
						+ " result=" + res2);
			}
			
			return res1 || res2;
		}
	}
	
	public boolean storeFsFile(String fileKey, FileData fileData) 
			throws IOException, ErrorException { 
		if (fileKey == null || fileKey.length() == 0 || fileData == null)
			return false;
		
		if (!fileKey.equals(fileData.getFileKey().toString())) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Wrong file key: " + fileKey + " must equals to " + fileData.getFileKey());
		}
		
		synchronized (fileLock()) {
			Path storePath = new Path(getStorePath(), ""+fileKey.charAt(0));
			Path filePath = new Path(storePath, fileKey + ".file");
			//Path dataPath = new Path(storePath, fileKey + ".dat");
			
			if (LOG.isDebugEnabled()) 
				LOG.debug("storeFsFile: key=" + fileKey + " filePath=" + filePath);
			
			FileSystem fs = getLibrary().getStoreFs();
			
			DataOutputStream outputMeta = new DataOutputStream(fs.create(filePath));
			boolean result = false;
			
			try { 
				fileData.write(outputMeta);
				
				result = true;
			} finally { 
				if (outputMeta != null)
					outputMeta.close();
			}
			
			return result;
		}
	}
	
	protected void readFileMetaTags(final FileSource.Item file, 
			final FileMetaCollector collector) throws IOException, ErrorException { 
		if (file == null || collector == null) return;
		
		if (file.loadMetadata(collector))
			return;
		
		try { 
			MetadataLoader loader = getManager().getCore().getMetadataLoader();
			if (loader != null) {
				final Map<String,Object> metaInfos = new TreeMap<String,Object>();
				
				MetadataLoader.InputFile infile = 
					new MetadataLoader.InputFile() {
						@Override
						public File getFile() { 
							return file.getFile();
						}
						@Override
						public InputStream openFile() throws IOException {
							return getSource().getInputStream(file);
						}
						@Override
						public String getName() {
							return file.getAttrs().getName().toString();
						}
						@Override
						public long getLength() {
							return file.getAttrs().getLength();
						}
						@Override
						public String getExtension() {
							return file.getAttrs().getExtension().toString();
						}
					};
				
				MetadataLoader.TagCollector tagcltr = 
					new MetadataLoader.TagCollector() {
						@Override
						protected void addTag(String tagName, Object tagValue) {
							metaInfos.put(tagName, tagValue);
						}
						@Override
						public void addAttachment(String name, String mimeType, byte[] data) { 
							collector.addScreenshot(name, mimeType, data);
						}
					};
				
				loader.loadMetadatas(infile, tagcltr);
				
				for (Map.Entry<String, Object> entry : metaInfos.entrySet()) { 
					String name = entry.getKey();
					Object val = entry.getValue();
					
					if (name != null && val != null) 
						collector.addMetaTag(name, val);
				}
			}
		} catch (IOException e) {
			if (LOG.isWarnEnabled())
				LOG.warn("readFileMetaTags: " + file.getAttrs() + " error: " + e);
			
		}
	}
	
	protected void readScreenshots(final FileSource.Item file, 
			FileMetaCollector collector) throws IOException, ErrorException { 
		if (file == null || collector == null) return;
		
		String contentType = MimeTypes.getContentTypeByExtension(
				file.getAttrs().getExtension().toString());
		
		if (contentType != null && contentType.startsWith("image/")) {
			readImageScreenshots(getSource(), file, collector);
		}
	}
	
	static void readImageScreenshots(final FileSource source, 
			final FileSource.Item file, FileMetaCollector collector) 
			throws IOException, ErrorException { 
		ImageCache imageCache = null;
		try {
			imageCache = new ImageCache(
				new ImageCache.ImageData() {
					@Override
					public void putCache(String key, byte[] data) {
						//getLibrary().putImageData(key, data);
					}
					@Override
					public BytesBufferPool.BytesBuffer getCache(String key) {
						return null; //getLibrary().getImageData(key);
					}
					@Override
					public InputStream openImage(int size) throws IOException {
						return source.getInputStream(file);
					}
					@Override
					public long getModifiedTime() {
						return file.getAttrs().getModifiedTime();
					}
					@Override
					public String getKey() {
						return file.getKey();
					}
					@Override
					public String getName() {
						return file.getAttrs().getName().toString();
					}
					@Override
					public String getExtension() {
						return file.getAttrs().getExtension().toString();
					}
					@Override
					public long getContentLength() {
						return file.getAttrs().getLength();
					}
				});
			
			file.getAttrs().setWidth(imageCache.getWidth());
			file.getAttrs().setHeight(imageCache.getHeight());
			
			ArrayList<FileScreenshot> list = new ArrayList<FileScreenshot>();
			
			if (list.size() == 0) { 
				FileScreenshot shot4k = createScreenshot(file, imageCache, 
						"4k", FileScreenshot.SIZE_4K);
				if (shot4k != null) list.add(shot4k);
			}
			
			if (list.size() == 0) { 
				FileScreenshot shothd = createScreenshot(file, imageCache, 
						"hd", FileScreenshot.SIZE_FHD);
				if (shothd != null) list.add(shothd);
			}
			
			if (list.size() == 0) { 
				FileScreenshot shothd = createScreenshot(file, imageCache, 
						"hd", FileScreenshot.SIZE_HD);
				if (shothd != null) list.add(shothd);
			}
			
			if (list.size() == 0) { 
				FileScreenshot shotsd = createScreenshot(file, imageCache, 
						"sd", FileScreenshot.SIZE_SD);
				if (shotsd != null) list.add(shotsd);
			}
			
			for (FileScreenshot shot : list) { 
				collector.addScreenshot(shot);
			}
			
			//return list.toArray(new FileScreenshot[list.size()]);
		} finally { 
			if (imageCache != null)
				imageCache.close();
		}
	}
	
	static FileScreenshot createScreenshot(FileSource.Item file, 
			ImageCache imageCache, String name, final int screenSize) 
			throws IOException {
		if (file.getAttrs().getLength() < 512 * 1024) return null;
		
		int width = imageCache.getWidth();
		int height = imageCache.getHeight();
		
		FileScreenshot result = null;
		//int screenSize = FileScreenshot.DEFAULT_SIZE;
		
		if (width > screenSize || height > screenSize) { 
			IImageSource.Param param = new IImageSource.Param() {
					@Override
					public int getParamWidth() {
						return screenSize;
					}
					@Override
					public int getParamHeight() {
						return screenSize;
					}
					@Override
					public boolean getParamTrim() {
						return false;
					}
				};
			
			IImageSource.Bitmap bitmap = imageCache.getBitmap(param);
			InputStream stream = bitmap != null ? bitmap.openBitmap() : null;
			
			try { 
				if (bitmap != null && stream != null) { 
					int size = stream.available();
					if (size > 0 && size < 1024 * 1024 && size < file.getAttrs().getLength() / 2) { 
						byte[] buffer = new byte[size];
						stream.read(buffer);
						
						String mimeType = bitmap.getMimeType();
						int shotWidth = bitmap.getWidth();
						int shotHeight = bitmap.getHeight();
						
						if (mimeType == null || mimeType.length() == 0) { 
							String format = imageCache.getFormat();
							if (format == null || format.length() == 0)
								format = "jpg";
							
							mimeType = "image/" + format;
						}
						
						result = new FileScreenshot(name, mimeType, 
								shotWidth, shotHeight, buffer);
					}
				}
			} finally { 
				if (stream != null) 
					stream.close();
			}
		}
		
		return result;
	}
	
	@SuppressWarnings("unused")
	private FileData createFileData(final FileSource.Item file, 
			final Text key, final byte[] buffer) 
			throws IOException, ErrorException { 
		FileMetaTag[] metatags = null; 
		FileMetaInfo[] metainfos = null;
		FileScreenshot[] screenshots = null;
		
		if (true) { 
			FileMetaCollector collector = new FileMetaCollector(file);
			readFileMetaTags(file, collector);
			readScreenshots(file, collector);
			
			metatags = collector.getMetaTags();
			metainfos = collector.getMetaInfos();
			screenshots = collector.getScreenshots();
		}
		
		if (metatags == null)
			metatags = new FileMetaTag[0];
		
		if (metainfos == null)
			metainfos = new FileMetaInfo[0];
		
		if (screenshots == null)
			screenshots = new FileScreenshot[0];
		
		FileData data = new FileData(key, file.getAttrs(), 
				metatags, metainfos, screenshots, buffer);
		
		//data.getAttrs().setName(name);
		//data.getAttrs().setExtension(extName);
		//data.getAttrs().setModifiedTime(file.getModificationTime());
		//data.getAttrs().setLength(file.getLength());
		
		return data;
	}
	
	public FileData storeFile(final FileSource.Item file, 
			final FileData fdata) throws IOException, ErrorException { 
		synchronized (fileLock()) { 
			return doStoreFile(file, fdata);
		}
	}
	
	private FileData doStoreFile(final FileSource.Item file, 
			final FileData fdata) throws IOException, ErrorException { 
		if (file == null || file.isFolder()) return null;
		if (fdata == null) return storeFile(file);
		
		Text key = new Text(file.getKey());
		FileData result = null;
		
		if (getSource().isStoreAllToMapFile(file)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("storeFile: key=" + key + " attrs=" + file.getAttrs());
			}
			
			DigestInputStream input = openDigestInputStream(getSource().getInputStream(file));
			try { 
				int size = input.available();
				if (size > 0) { 
					byte[] buffer = new byte[size];
					input.read(buffer);
					
					FileData data = new FileData(key, 
							fdata.getAttrs(), 
							fdata.getMetaTags(), fdata.getMetaInfos(), 
							fdata.getScreenshots(), 
							buffer);
					
					//data.getAttrs().setName(name);
					//data.getAttrs().setExtension(extName);
					//data.getAttrs().setModifiedTime(file.getModificationTime());
					//data.getAttrs().setLength(file.getLength());
					data.getAttrs().setChecksum(getDigestChecksum(input));
					
					FileDataWriters writer = getFileWriters();
					if (writer != null) {
						writer.write(key, data);
						result = data;
					}
				}
				
				//file.setOptimizeFlag(NameData.OPTIMIZE_MAPPED);
			} finally { 
				if (input != null) 
					input.close();
			}
		} else { 
			String fileid = key.toString();
			
			FileSystem fs = getLibrary().getStoreFs();
			Path storePath = new Path(getStorePath(), ""+fileid.charAt(0));
			
			Path filePath = new Path(storePath, fileid + ".dat");
			Path metaPath = new Path(storePath, fileid + ".file");
			
			if (fs.exists(filePath) || fs.exists(metaPath)) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"File: " + file.getAttrs().getName() + " already exists");
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("storeFile: key=" + key 
						+ " attrs=" + file.getAttrs() 
						+ " storePath=" + filePath);
			}
			
			fs.mkdirs(storePath);
			
			DigestInputStream input = openDigestInputStream(getSource().getInputStream(file)); 
			OutputStream outputData = null; //fs.create(filePath);
			DataOutputStream outputMeta = new DataOutputStream(fs.create(metaPath));
			
			try { 
				boolean storeAllToMetaFile = getSource().isStoreAllToMetaFile(file);
				byte[] dataBuf = new byte[0];
				
				if (storeAllToMetaFile) { 
					int size = input.available();
					if (size > 0) { 
						byte[] buffer = new byte[size];
						input.read(buffer);
						dataBuf = buffer;
					}
				} else {
					outputData = fs.create(filePath);
					
					byte[] buffer = new byte[40960];
					long left = input.available();
					while (left > 0) { 
						int size = input.read(buffer);
						if (size < 0) break;
						if (size > 0) 
							outputData.write(buffer, 0, size);
					}
				}
				
				FileData data = new FileData(key, 
						fdata.getAttrs(), 
						fdata.getMetaTags(), fdata.getMetaInfos(), 
						fdata.getScreenshots(), 
						dataBuf);
				
				//data.getAttrs().setName(name);
				//data.getAttrs().setExtension(extName);
				//data.getAttrs().setModifiedTime(file.getModificationTime());
				//data.getAttrs().setLength(file.getLength());
				data.getAttrs().setChecksum(getDigestChecksum(input));
				
				//if (isStoreMetaToMapFile(file)) {
				//	RecordWriter<Text,FileData> writer = getFileWriter();
				//	if (writer != null) 
				//		writer.write(key, data);
				//}
				
				data.write(outputMeta);
				
				result = data;
			} finally { 
				if (outputMeta != null)
					outputMeta.close();
				if (outputData != null)
					outputData.close();
				if (input != null) 
					input.close();
			}
		}
		
		return result;
	}
	
	public FileData storeFile(final FileSource.Item file) 
			throws IOException, ErrorException { 
		synchronized (fileLock()) { 
			return doStoreFile(file);
		}
	}
	
	private FileData doStoreFile(final FileSource.Item file) 
			throws IOException, ErrorException { 
		if (file == null || file.isFolder()) return null;
		
		Text key = new Text(file.getKey());
		
		FileMetaTag[] metatags = null; 
		FileMetaInfo[] metainfos = null;
		FileScreenshot[] screenshots = null;
		FileData result = null;
		
		if (true) { 
			FileMetaCollector collector = new FileMetaCollector(file);
			readFileMetaTags(file, collector);
			readScreenshots(file, collector);
			
			metatags = collector.getMetaTags();
			metainfos = collector.getMetaInfos();
			screenshots = collector.getScreenshots();
		}
		
		if (metatags == null)
			metatags = new FileMetaTag[0];
		
		if (metainfos == null)
			metainfos = new FileMetaInfo[0];
		
		if (screenshots == null)
			screenshots = new FileScreenshot[0];
		
		file.getAttrs().setPosterCount(screenshots.length);
		
		if (getSource().isStoreAllToMapFile(file)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("storeFile: key=" + key 
						+ " attrs=" + file.getAttrs() 
						+ " metatagCount=" + metatags.length 
						+ " metainfoCount=" + metainfos.length 
						+ " screenCount=" + screenshots.length);
			}
			
			DigestInputStream input = openDigestInputStream(getSource().getInputStream(file));
			try { 
				int size = input.available();
				if (size > 0) { 
					byte[] buffer = new byte[size];
					input.read(buffer);
					
					FileData data = new FileData(key, file.getAttrs(), 
							metatags, metainfos, screenshots, buffer);
					
					//data.getAttrs().setName(name);
					//data.getAttrs().setExtension(extName);
					//data.getAttrs().setModifiedTime(file.getModificationTime());
					//data.getAttrs().setLength(file.getLength());
					data.getAttrs().setChecksum(getDigestChecksum(input));
					
					FileDataWriters writer = getFileWriters();
					if (writer != null) {
						writer.write(key, data);
						result = data;
					}
				}
				
				//file.setOptimizeFlag(NameData.OPTIMIZE_MAPPED);
			} finally { 
				if (input != null) 
					input.close();
			}
		} else { 
			String fileid = key.toString();
			
			FileSystem fs = getLibrary().getStoreFs();
			Path storePath = new Path(getStorePath(), ""+fileid.charAt(0));
			
			Path filePath = new Path(storePath, fileid + ".dat");
			Path metaPath = new Path(storePath, fileid + ".file");
			
			if (fs.exists(filePath) || fs.exists(metaPath)) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"File: " + file.getAttrs().getName() + " already exists");
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("storeFile: key=" + key 
						+ " attrs=" + file.getAttrs() 
						+ " metatagCount=" + metatags.length 
						+ " metainfoCount=" + metainfos.length 
						+ " screenCount=" + screenshots.length 
						+ " storePath=" + filePath);
			}
			
			fs.mkdirs(storePath);
			
			DigestInputStream input = openDigestInputStream(getSource().getInputStream(file)); 
			OutputStream outputData = null; //fs.create(filePath);
			DataOutputStream outputMeta = new DataOutputStream(fs.create(metaPath));
			
			try { 
				boolean storeAllToMetaFile = getSource().isStoreAllToMetaFile(file);
				byte[] dataBuf = new byte[0];
				
				if (storeAllToMetaFile) { 
					int size = input.available();
					if (size > 0) { 
						byte[] buffer = new byte[size];
						input.read(buffer);
						dataBuf = buffer;
					}
				} else {
					outputData = fs.create(filePath);
					
					byte[] buffer = new byte[40960];
					long left = input.available();
					while (left > 0) { 
						int size = input.read(buffer);
						if (size < 0) break;
						if (size > 0) 
							outputData.write(buffer, 0, size);
					}
				}
				
				FileData data = new FileData(key, file.getAttrs(), 
						metatags, metainfos, screenshots, dataBuf);
				
				//data.getAttrs().setName(name);
				//data.getAttrs().setExtension(extName);
				//data.getAttrs().setModifiedTime(file.getModificationTime());
				//data.getAttrs().setLength(file.getLength());
				data.getAttrs().setChecksum(getDigestChecksum(input));
				
				//if (isStoreMetaToMapFile(file)) {
				//	RecordWriter<Text,FileData> writer = getFileWriter();
				//	if (writer != null) 
				//		writer.write(key, data);
				//}
				
				data.write(outputMeta);
				
				result = data;
			} finally { 
				if (outputMeta != null)
					outputMeta.close();
				if (outputData != null)
					outputData.close();
				if (input != null) 
					input.close();
			}
		}
		
		return result;
	}
	
}
