package org.javenstudio.falcon.datum.data;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.util.MapFileFormat;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.BytesWritable;
import org.javenstudio.raptor.io.MapFile;
import org.javenstudio.raptor.io.Text;

public abstract class FileLoader extends FileIO {
	private static final Logger LOG = Logger.getLogger(FileLoader.class);

	private MapFile.Reader mNameReader = null;
	private Map<Integer,MapFile.Reader> mFileReaders = null;
	
	public FileLoader() {}
	
	public abstract SqRoot getRoot();
	
	public SqLibrary getLibrary() { return getRoot().getLibrary(); }
	public DataManager getManager() { return getLibrary().getManager(); }
	
	public Path getStorePath() throws ErrorException { 
		return FileStorer.getStorePath(getLibrary(), getRoot().getContentKey());
	}
	
	private Object fileLock() { return getLibrary().getLock(); }
	private Object nameLock() { return getRoot().getLock(); }
	
	private MapFile.Reader getNameReader() 
			throws IOException, ErrorException { 
		synchronized (nameLock()) {
			if (mNameReader != null) return mNameReader;
			
			Configuration conf = getManager().getConfiguration();
			FileSystem fs = getLibrary().getStoreFs();
			Path storePath = getStorePath();
			
			if (true) {
				Path path = new Path(storePath, //FileStorer.NAME_NAME +
						"." + getRoot().getContentKey());
				
				if (LOG.isDebugEnabled())
					LOG.debug("getNameReader: name mapFile: " + path);
				
				MapFileFormat<Text,NameData> format = 
						new MapFileFormat<Text,NameData>(
								conf, fs, Text.class, NameData.class);
				
				MapFile.Reader reader = format.getMapReader(path);
				mNameReader = reader;
			}
			
			return mNameReader;
		}
	}
	
	private MapFile.Reader getFileReader(int index) 
			throws IOException, ErrorException { 
		if (index <= 0) return null;
		
		synchronized (fileLock()) {
			if (mFileReaders == null) 
				mFileReaders = new HashMap<Integer,MapFile.Reader>();
			
			MapFile.Reader reader = mFileReaders.get(index);
			if (reader != null) return reader;
			
			Configuration conf = getManager().getConfiguration();
			FileSystem fs = getLibrary().getStoreFs();
			Path storePath = getStorePath();
			
			if (true) {
				Path path = new Path(storePath, FileStorer.FILE_NAME + index);
				
				if (LOG.isDebugEnabled())
					LOG.debug("getFileReader: file mapFile: " + path);
				
				MapFileFormat<Text,FileData> format = 
						new MapFileFormat<Text,FileData>(
								conf, fs, Text.class, FileData.class);
				
				reader = format.getMapReader(path);
				mFileReaders.put(index, reader);
			}
			
			return reader;
		}
	}
	
	@Override
	public void close() throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("close");
		
		synchronized (fileLock()) {
			try {
				Map<Integer,MapFile.Reader> fileReaders = mFileReaders;
				if (fileReaders != null) {
					for (MapFile.Reader reader : fileReaders.values()) {
						reader.close();
					}
				}
				
				mFileReaders = null;
			} catch (IOException e) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		}
		
		synchronized (nameLock()) {
			try {
				MapFile.Reader nameReader = mNameReader;
				if (nameReader != null) 
					nameReader.close();
				
				mNameReader = null;
			} catch (IOException e) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		}
		
		super.close();
	}
	
	public void loadNameDatas(NameData.Collector collector) 
			throws IOException, ErrorException { 
		if (collector == null) return;
		
		synchronized (nameLock()) {
			try {
				MapFile.Reader nameReader = getNameReader();
				if (nameReader != null)
					nameReader.reset();
				
				while (nameReader != null) { 
					Text key = new Text();
					NameData data = new NameData();
					
					if (nameReader.next(key, data) == false) 
						break;
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("loadNameDatas: root=" + getRoot().getContentKey() 
								+ " key=" + key + " attrs=" + data.getAttrs());
					}
					
					collector.addNameData(key, data);
					
					String pathKey = key.toString();
					getRoot().putCacheNameData(pathKey, data);
				}
			} catch (FileNotFoundException ex) { 
				if (LOG.isDebugEnabled())
					LOG.debug("loadNameDatas: failed: " + ex);
			}
		}
	}
	
	public Map<Text,NameData> loadNameDatas() 
			throws IOException, ErrorException { 
		final Map<Text,NameData> map = new TreeMap<Text,NameData>(
			new Comparator<Text>() {
				@Override
				public int compare(Text o1, Text o2) { 
					return o1.compareTo(o2);
				}
			});
		
		loadNameDatas(new NameData.Collector() {
				@Override
				public void addNameData(Text key, NameData data) {
					if (key != null && data != null) 
						map.put(key, data);
				}
			});
		
		return map;
	}
	
	public NameData loadNameData(String pathKey) 
			throws IOException, ErrorException { 
		if (pathKey == null) return null;
		
		synchronized (nameLock()) {
			try {
				NameData dataRef = getRoot().getCacheNameData(pathKey);
				if (dataRef != null)
					return dataRef;
				
				MapFile.Reader nameReader = getNameReader();
				if (nameReader != null) { 
					Text key = new Text(pathKey);
					NameData val = new NameData();
					NameData data = (NameData)nameReader.get(key, val);
					
					if (data != null) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("loadNameData: root=" + getRoot().getContentKey() 
									+ " key=" + key + " attrs=" + data.getAttrs());
						}
						
						getRoot().putCacheNameData(pathKey, data);
					}
					
					return data;
				}
			} catch (FileNotFoundException ex) { 
				if (LOG.isDebugEnabled())
					LOG.debug("loadNameData: failed: " + ex);
			}
		}
		
		return null;
	}
	
	public void loadFileDatas(int index, FileData.Collector collector) 
			throws IOException, ErrorException { 
		if (collector == null) return;
		
		synchronized (fileLock()) {
			try {
				MapFile.Reader fileReader = getFileReader(index);
				if (fileReader != null) 
					fileReader.reset();
				
				while (fileReader != null) { 
					Text key = new Text();
					FileData data = new FileData();
					
					if (fileReader.next(key, data) == false) 
						break;
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("loadFileDatas: key=" + key + " attrs=" + data.getAttrs());
					}
					
					collector.addFileData(key, data);
					
					String pathKey = key.toString();
					getRoot().putCacheFileData(pathKey, data);
				}
			} catch (FileNotFoundException ex) { 
				if (LOG.isDebugEnabled())
					LOG.debug("loadFileDatas: failed: " + ex);
			}
		}
	}
	
	public Map<Text,FileData> loadFileDatas(int index) 
			throws IOException, ErrorException { 
		final Map<Text,FileData> map = new TreeMap<Text,FileData>(
			new Comparator<Text>() {
				@Override
				public int compare(Text o1, Text o2) { 
					return o1.compareTo(o2);
				}
			});
		
		loadFileDatas(index, new FileData.Collector() {
				@Override
				public void addFileData(Text key, FileData data) {
					if (key != null && data != null)
						map.put(key, data);
				}
			});
		
		return map;
	}
	
	public FileData loadFileData(int index, String pathKey) 
			throws IOException, ErrorException { 
		if (pathKey == null) return null;
		if (pathKey.endsWith("0")) return null;
		
		synchronized (fileLock()) {
			try {
				FileData dataRef = getRoot().getCacheFileData(pathKey);
				if (dataRef != null)
					return dataRef;
				
				MapFile.Reader fileReader = getFileReader(index);
				if (fileReader != null) { 
					Text key = new Text(pathKey);
					FileData val = new FileData();
					FileData data = (FileData)fileReader.get(key, val);
					
					if (data != null) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("loadFileData: key=" + key + " attrs=" + data.getAttrs());
						}
						
						getRoot().putCacheFileData(pathKey, data);
						return data;
					}
				}
			} catch (FileNotFoundException ex) { 
				if (LOG.isDebugEnabled())
					LOG.debug("loadFileData: failed: " + ex);
			}
		}
		
		return openFsFileData(pathKey);
	}
	
	public InputStream openFile(int index, String fileKey) 
			throws ErrorException { 
		if (fileKey == null) return null;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("openFile: key=" + fileKey + " index=" + index);
		
		try {
			final FileData data = loadFileData(index, fileKey);
			if (data != null) { 
				if (LOG.isDebugEnabled()) 
					LOG.debug("openFile: key=" + fileKey + " attrs=" + data.getAttrs());
				
				BytesWritable bytes = data.getFileData();
				if (bytes != null) { 
					byte[] buffer = bytes.getBytes();
					int size = bytes.getLength();
					
					if (LOG.isDebugEnabled()) { 
						LOG.debug("openFile: key=" + fileKey + " buffer=" 
								+ (buffer!=null?(""+buffer.length):"null") + " size=" + size);
					}
					
					if (buffer != null && buffer.length > 0 && size > 0) { 
						return new ByteArrayInputStream(buffer, 0, size) { 
								@SuppressWarnings("unused")
								private final FileData mData = data;
							};
					}
				}
				
				if (data.getAttrs().getLength() > 0) 
					return openFsFile(fileKey);
			}
			
			return openFsFile(fileKey);
		} catch (FileNotFoundException ex) {
			if (LOG.isDebugEnabled())
				LOG.debug("openFile: failed: " + ex);
			
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
		
		return null;
	}
	
	protected InputStream openFsFile(String fileKey) 
			throws IOException, ErrorException { 
		synchronized (fileLock()) {
			Path storePath = new Path(getStorePath(), ""+fileKey.charAt(0));
			Path filePath = new Path(storePath, fileKey + ".dat");
			
			if (LOG.isDebugEnabled()) 
				LOG.debug("openFsFile: key=" + fileKey + " filePath=" + filePath);
			
			try {
				FileSystem fs = getLibrary().getStoreFs();
				return fs.open(filePath);
			} catch (FileNotFoundException ex) {
				if (LOG.isDebugEnabled())
					LOG.debug("openFsFile: failed: " + ex);
			}
		}
		
		return null;
	}
	
	protected FileData openFsFileData(String fileKey) 
			throws IOException, ErrorException { 
		synchronized (fileLock()) {
			Path storePath = new Path(getStorePath(), ""+fileKey.charAt(0));
			Path filePath = new Path(storePath, fileKey + ".file");
			
			if (LOG.isDebugEnabled()) 
				LOG.debug("openFsFileData: key=" + fileKey + " filePath=" + filePath);
			
			InputStream stream = null;
			try {
				FileSystem fs = getLibrary().getStoreFs();
				stream = fs.open(filePath);
				if (stream != null) {
					DataInputStream input = new DataInputStream(stream);
					FileData data = new FileData();
					data.readFields(input);
					return data;
				}
			} catch (FileNotFoundException ex) {
				if (LOG.isDebugEnabled())
					LOG.debug("openFsFileData: failed: " + ex);
				
			} finally { 
				try { 
					if (stream != null) stream.close();
				} catch (Throwable e) { 
					if (LOG.isDebugEnabled())
						LOG.debug("openFsFileData: error: " + e, e);
				}
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("openFsFileData: null error: filePath=" + filePath);
		}
		
		return null;
	}
	
}
