package org.javenstudio.falcon.user.auth;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.util.MapFileFormat;
import org.javenstudio.falcon.datum.util.RecordWriter;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.MapFile;
import org.javenstudio.raptor.io.Text;

abstract class BufferWriter<T extends Buffer> {
	private static final Logger LOG = Logger.getLogger(BufferWriter.class);

	private final AuthStore mStore;
	private final MapFileFormat<Text,T> mFormat;
	//private final RecordWriter<Text,T> mWriter;
	
	private final Map<Text,T> mDataMap;
	private final FileSystem mFs;
	private final String mDirName;
	private final String mName;
	
	private volatile boolean mChanged = false;
	
	public BufferWriter(AuthStore store, Class<? extends Buffer> clazz, 
			String dirname, String name) throws ErrorException { 
		if (store == null || clazz == null) throw new NullPointerException();
		if (name == null || name.length() == 0) 
			throw new IllegalArgumentException("name is empty");
		
		mStore = store;
		try {
			Configuration conf = store.getStore().getConfiguration();
			FileSystem fs = store.getStore().getAuthStoreFs();
			
			mFs = fs;
			mName = name;
			mDirName = dirname;
			
			Path path = getStorePath(false); //getStore().getUserDBPath(name);
			//fs.mkdirs(path);
			
			if (LOG.isDebugEnabled())
				LOG.debug("UserWriter: path: " + path);
			
			MapFileFormat<Text,T> format = 
					new MapFileFormat<Text,T>(
							conf, fs, Text.class, clazz);
			
			//RecordWriter<Text,T> writer = 
			//		format.getRecordWriter(path, null);
			
			final Map<Text,T> map = new TreeMap<Text,T>(
				new Comparator<Text>() {
					@Override
					public int compare(Text o1, Text o2) { 
						return o1.compareTo(o2);
					}
				});
			
			if (fs.exists(new Path(path, "data"))) {
				MapFile.Reader reader = format.getMapReader(path);
				if (reader != null) { 
					reader.reset();
					
					while (true) { 
						Text key = new Text();
						T data = newBuffer();
						
						if (reader.next(key, data) == false) 
							break;
						
						if (LOG.isDebugEnabled()) 
							LOG.debug("UserWriter: key=" + key + " data=" + data);
						
						map.put(key, data);
					}
				}
			}
			
			mFormat = format;
			//mWriter = writer;
			mDataMap = map;
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public AuthStore getStore() { return mStore; }
	public String getDirName() { return mDirName; }
	
	private Path getStorePath(boolean forWrite) throws IOException, ErrorException { 
		//Configuration conf = getStore().getConfiguration();
		FileSystem fs = mFs; //getStore().getUserDBFs();
		
		Path path = getStore().getStore().getAuthStorePath(mDirName, mName, forWrite);
		if (forWrite && !fs.exists(path)) fs.mkdirs(path);
		
		return path;
	}
	
	protected abstract T newBuffer() throws ErrorException;
	
	public synchronized T get(Text key) throws ErrorException { 
		return key != null ? mDataMap.get(key) : null;
	}
	
	public synchronized T remove(Text key) throws ErrorException { 
		if (key != null) { 
			T data = mDataMap.remove(key);
			if (data != null) mChanged = true;
			return data;
		}
		return null;
	}
	
	public synchronized void write(Text key, T data) 
			throws ErrorException { 
		if (key != null && data != null) {
			mDataMap.put(key, data);
			mChanged = true;
		}
	}
	
	public synchronized void close() throws ErrorException { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("close: path=" + mDirName + "/" + mName);
		
		if (!mChanged) return;
		try {
			if (mDataMap.size() > 0) { 
				Path path = getStorePath(true);
				
				RecordWriter<Text,T> writer = 
						mFormat.getRecordWriter(path, null);
				
				for (Map.Entry<Text, T> entry : mDataMap.entrySet()) { 
					Text key = entry.getKey();
					T value = entry.getValue();
					
					if (LOG.isDebugEnabled()) 
						LOG.debug("close: write: key=" + key + " data=" + value);
					
					writer.write(key, value);
				}
				writer.close();
				
			} else { 
				Path path = getStorePath(false);
				Path dataFile = new Path(path, "data");
				Path indexFile = new Path(path, "index");
				
				if (LOG.isDebugEnabled()) 
					LOG.debug("close: remove: path=" + dataFile);
				
				mFs.delete(dataFile, false);
				mFs.delete(indexFile, false);
			}
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
}
