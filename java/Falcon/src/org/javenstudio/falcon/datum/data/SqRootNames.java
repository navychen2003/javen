package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.raptor.io.Text;

public final class SqRootNames {
	//private static final Logger LOG = Logger.getLogger(SqRootNames.class);

	public static interface Creator { 
		public NameData create() throws ErrorException;
	}
	
	public static interface Collector { 
		public void add(Text key, NameData data) throws ErrorException;
	}
	
	private Text mRootKey = null;
	
	private final Map<Text,NameData> mNames = 
		new TreeMap<Text,NameData>(
			new Comparator<Text>() {
				@Override
				public int compare(Text o1, Text o2) { 
					return o1.compareTo(o2);
				}
			});
	
	public SqRootNames() {}
	
	public static SqRootNames loadNames(SqRoot root) 
			throws ErrorException { 
		SqRootNames names = new SqRootNames();
		names.reloadNames(root);
		return names;
	}
	
	private void updatePaths(Text key, Text parentKey, Text path) { 
		if (key == null || parentKey == null || path == null) 
			return;
		
		NameData data = mNames.get(key);
		if (data == null) return;
		
		data.getAttrs().setParentKey(parentKey);
		data.getAttrs().setPath(path);
		if (!data.isDirectory()) return;
		
		Text dirkey = data.getKey();
		Text[] fileKeys = data.getFileKeys();
		
		String paths = path.toString();
		if (paths != null && paths.length() > 0) { 
			if (!paths.endsWith("/")) paths += "/";
			paths = paths + data.getAttrs().getName();
		} else { 
			paths = "/";
		}
		
		Text dirpath = new Text(paths);
		
		for (int i=0; fileKeys != null && i < fileKeys.length; i++) { 
			Text fileKey = fileKeys[i];
			updatePaths(fileKey, dirkey, dirpath);
		}
	}
	
	public synchronized void storeNames(FileStorer storer) 
			throws ErrorException { 
		if (storer == null) throw new NullPointerException();
		
		try {
			updatePaths(mRootKey, new Text(), new Text());
			storer.storeName(mNames);
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public void reloadNames(SqRoot root) throws ErrorException { 
		FileLoader loader = root.newLoader();
		try {
			reloadNames(loader, root.getPathKey(), root.getName());
		} finally { 
			loader.close();
		}
	}
	
	public synchronized void reloadNames(FileLoader loader, 
			String rootkey, String rootname) throws ErrorException { 
		if (loader == null) throw new NullPointerException();
		
		if (rootkey == null || rootkey.length() == 0 || 
			rootname == null || rootname.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Root key or name is empty");
		}
		
		try {
			mNames.clear();
			
			loader.loadNameDatas(new NameData.Collector() {
					@Override
					public void addNameData(Text key, NameData data) {
						if (key != null && data != null)
							mNames.put(key, data);
					}
				});
			
			Text rootKey = new Text(rootkey);
			
			if (!mNames.containsKey(rootKey)) { 
				//NameData rootData = NameData.newDir(rootKey, null, null, rootname, 
				//		System.currentTimeMillis(), 0);
				
				NameData rootData = new NameData(rootKey , null, null);
				rootData.getAttrs().setName(rootname);
				rootData.getAttrs().setModifiedTime(System.currentTimeMillis());
				
				mNames.put(rootData.getKey(), rootData);
				rootKey = rootData.getKey();
			}
			
			mRootKey = rootKey;
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public synchronized Text getRootKey() { 
		return mRootKey;
	}
	
	public synchronized int size() { 
		return mNames.size();
	}
	
	public synchronized boolean contains(final Text key) { 
		return key != null && mNames.containsKey(key);
	}
	
	public synchronized NameData remove(final Text key) { 
		return key != null ? mNames.remove(key) : null;
	}
	
	public synchronized void getNameDatas(Collector collector) 
			throws ErrorException { 
		if (collector == null) return;
		
		for (Map.Entry<Text, NameData> entry : mNames.entrySet()) { 
			Text key = entry.getKey();
			NameData data = entry.getValue();
			
			collector.add(key, data);
		}
	}
	
	public NameData getNameData(final Text key) throws ErrorException { 
		return getNameData(key, null);
	}
	
	public synchronized NameData getNameData(final Text key, 
			Creator ctor) throws ErrorException { 
		if (key == null) return null;
		NameData data = mNames.get(key);
		if (data == null && ctor != null) { 
			data = ctor.create();
			if (data != null) { 
				if (!key.equals(data.getKey())) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"New NameData key: " + data.getKey() + " not equals to " + key);
				}
				mNames.put(key, data);
			}
		}
		return data;
	}
	
	public synchronized void getNameDatasIn(NameData dir, 
			Collector collector) throws ErrorException { 
		if (dir == null || collector == null) return;
		if (!dir.isDirectory()) return;
		
		Text[] fileKeys = dir.getFileKeys();
		
		for (int i=0; fileKeys != null && i < fileKeys.length; i++) { 
			Text fileKey = fileKeys[i];
			if (fileKey == null) continue;
			
			NameData fileData = getNameData(fileKey, null);
			if (fileData != null) { 
				collector.add(fileKey, fileData);
				getNameDatasIn(fileData, collector);
			}
		}
	}
	
	public synchronized boolean existsIn(NameData dir, 
			Text key) throws ErrorException { 
		if (dir == null || key == null) return false;
		if (key.getLength() == 0 || !dir.isDirectory()) return false;
		if (key.equals(dir.getKey())) return true;
		
		Text[] fileKeys = dir.getFileKeys();
		
		for (int i=0; fileKeys != null && i < fileKeys.length; i++) { 
			Text fileKey = fileKeys[i];
			if (fileKey == null) continue;
			if (fileKey.equals(key)) return true;
			
			NameData fileData = getNameData(fileKey, null);
			if (fileData != null && existsIn(fileData, key)) 
				return true;
		}
		
		return false;
	}
	
	public synchronized boolean existsFileName(NameData data, String name) 
			throws ErrorException { 
		if (data == null || !data.isDirectory()) return false;
		if (name == null || name.length() == 0) return false;
		
		Text[] fileKeys = data.getFileKeys();
		Text nameTxt = new Text(name);
		
		for (int i=0; fileKeys != null && i < fileKeys.length; i++) { 
			Text fileKey = fileKeys[i];
			if (fileKey == null) continue;
			
			NameData fileData = getNameData(fileKey, null);
			if (fileData != null) { 
				if (nameTxt.equals(fileData.getAttrs().getName()))
					return true;
			}
		}
		
		return false;
	}
	
	public synchronized String newFileName(NameData data, String name) 
			throws ErrorException { 
		if (name == null || name.length() == 0) 
			name = "Untitled";
		if (data == null || !data.isDirectory()) 
			return name;
		
		Text[] fileKeys = data.getFileKeys();
		Text nameTxt = new Text(name);
		String title = name;
		int index = 1;
		
		for (int i=0; fileKeys != null && i < fileKeys.length; i++) { 
			Text fileKey = fileKeys[i];
			if (fileKey == null) continue;
			
			NameData fileData = getNameData(fileKey, null);
			if (fileData != null) { 
				if (nameTxt.equals(fileData.getAttrs().getName())) {
					index ++;
					name = title + " (" + index + ")";
					nameTxt = new Text(name);
				}
			}
		}
		
		return name;
	}
	
	public synchronized boolean addFileKey(NameData data, Text key) { 
		if (data == null || !data.isDirectory()) return false;
		if (key == null || key.getLength() == 0) return false;
		
		Text[] fileKeys = data.getFileKeys();
		Set<Text> list = new HashSet<Text>();
		
		for (int i=0; fileKeys != null && i < fileKeys.length; i++) { 
			Text fileKey = fileKeys[i];
			if (fileKey != null && fileKey.getLength() > 0)
				list.add(fileKey);
		}
		
		list.add(key);
		
		Text[] newKeys = list.toArray(new Text[list.size()]);
		data.setFileKeys(newKeys);
		
		return true;
	}
	
	public synchronized boolean removeFileKey(NameData data, Text key) { 
		if (data == null || !data.isDirectory()) return false;
		if (key == null || key.getLength() == 0) return false;
		
		Text[] fileKeys = data.getFileKeys();
		Set<Text> list = new HashSet<Text>();
		boolean found = false;
		
		for (int i=0; fileKeys != null && i < fileKeys.length; i++) { 
			Text fileKey = fileKeys[i];
			if (fileKey != null && fileKey.getLength() > 0) {
				if (fileKey.equals(key)) { 
					found = true;
					continue;
				}
				list.add(fileKey);
			}
		}
		
		if (found) {
			Text[] newKeys = list.toArray(new Text[list.size()]);
			data.setFileKeys(newKeys);
			
			return true;
		}
		
		return false;
	}
	
	public synchronized int removeEmpty(final Set<Text> dirKeys) 
			throws ErrorException { 
		if (dirKeys == null || dirKeys.size() == 0)
			return 0;
		
		Text[] keys = dirKeys.toArray(new Text[dirKeys.size()]);
		int count = 0;
		
		for (Text key : keys) { 
			if (key == null) continue;
			
			NameData data = getNameData(key);
			if (data == null || !data.isDirectory())
				continue;
			
			Text[] fileKeys = data.getFileKeys();
			int fileCount = 0;
			
			if (fileKeys != null && fileKeys.length > 0) {
				for (Text fileKey : fileKeys) { 
					if (fileKey == null || !contains(fileKey))
						continue;
					if (dirKeys.contains(fileKey))
						continue;
					fileCount ++;
				}
			}
			
			if (fileCount == 0) {
				remove(key);
				count ++;
			}
		}
		
		return count;
	}
	
	public synchronized long getFileLength(final Text key) 
			throws ErrorException { 
		if (key == null) return 0;
		
		NameData data = getNameData(key);
		if (data == null) return 0;
		
		if (!data.isDirectory())
			return data.getAttrs().getLength();
		
		Text[] fileKeys = data.getFileKeys();
		long totalLen = 0;
		
		if (fileKeys != null) { 
			for (Text fileKey : fileKeys) { 
				long fileLen = getFileLength(fileKey);
				totalLen += fileLen;
			}
		}
		
		return totalLen;
	}
	
	public synchronized boolean setPoster(NameData data, 
			Text[] posters, Text[] backgrounds, boolean append) { 
		if (data == null) return false;
		
		FilePoster poster = data.getFilePoster();
		if (poster == null) { 
			if (posters != null || backgrounds != null) {
				poster = new FilePoster(0, posters, backgrounds);
				data.setFilePoster(poster);;
			}
			return true;
			
		} else if (append == false) { 
			if (posters != null || backgrounds != null) {
				poster.setPosters(posters);
				poster.setBackgrounds(backgrounds);
			} else { 
				if (poster.getFlag() != 0) { 
					poster.setPosters(null);
					poster.setBackgrounds(null);
				} else 
					data.setFilePoster(null);
			}
			return true;
		}
		
		poster.setPosters(mergeKeys(poster.getPosters(), posters));
		poster.setBackgrounds(mergeKeys(poster.getBackgrounds(), backgrounds));
		
		return true;
	}
	
	private static Text[] mergeKeys(Text[] list1, Text[] list2) { 
		Set<Text> list = new HashSet<Text>();
		
		for (int i=0; list1 != null && i < list1.length; i++) { 
			Text key = list1[i];
			if (key != null && key.getLength() > 0)
				list.add(key);
		}
		
		for (int i=0; list2 != null && i < list2.length; i++) { 
			Text key = list2[i];
			if (key != null && key.getLength() > 0)
				list.add(key);
		}
		
		return list.toArray(new Text[list.size()]);
	}
	
}
