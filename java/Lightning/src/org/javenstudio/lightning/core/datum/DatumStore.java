package org.javenstudio.lightning.core.datum;

import java.io.File;
import java.util.ArrayList;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataConf;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.datum.IDatabaseStore;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.datum.ILibraryStore;
import org.javenstudio.falcon.util.FsUtils;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.lightning.core.CoreStore;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;

public class DatumStore implements ILibraryStore, IDatabaseStore {

	private final DatumCore mCore;
	
	public DatumStore(DatumCore core) { 
		if (core == null) throw new NullPointerException();
		mCore = core;
	}
	
	public DatumCore getCore() { return mCore; }
	
	public final CoreStore getUserStore() { 
		return getCore().getDescriptor().getContainer().getContainers().getUserStore(); 
	}
	
	private File getLibraryDirFile(String userId, String libraryId) 
			throws ErrorException { 
		if (libraryId == null || libraryId.length() == 0)
			throw new NullPointerException("LibraryId is null or empty");
		
		File userDir = getUserStore().getUserDirFile(userId);
		if (!userDir.exists() && !userDir.mkdir()) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"userDir: " + userDir + " create failed");
		}
		
		File libDir = new File(userDir, libraryId);
		if (!libDir.exists() && !libDir.mkdir()) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"libraryDir: " + libDir + " create failed");
		}
		
		return libDir;
	}
	
	private Path getLibraryDirPath(String userId, String libraryId) 
			throws ErrorException { 
		if (libraryId == null || libraryId.length() == 0)
			throw new NullPointerException("LibraryId is null or empty");
		
		return new Path(getUserStore().getUserDirPath(userId), libraryId);
	}
	
	private Path getLibraryXmlPath(String userId, String libraryId) 
			throws ErrorException { 
		return new Path(getLibraryDirPath(userId, libraryId), "library.xml");
	}
	
	private File getLibraryXmlFile(String userId, String libraryId) 
			throws ErrorException { 
		return new File(getLibraryDirFile(userId, libraryId), "library.xml");
	}
	
	private Path getLibraryListXmlPath(String userId) throws ErrorException { 
		return new Path(getUserStore().getUserDirPath(userId), "librarylist.xml");
	}
	
	private File getLibraryListXmlFile(String userId) throws ErrorException { 
		return new File(getUserStore().getUserDirFile(userId), "librarylist.xml");
	}
	
	private Path getDatabaseDirPath(String userId) throws ErrorException { 
		if (userId == null || userId.length() == 0)
			return new Path(getUserStore().getDataDirPath(), ".db");
		else
			return new Path(getUserStore().getUserDirPath(userId), ".db");
	}
	
	private File getDatabaseDirFile(String userId) throws ErrorException { 
		if (userId == null || userId.length() == 0)
			return new File(getUserStore().getDataDirFile(), ".db");
		else
			return new File(getUserStore().getUserDirFile(userId), ".db");
	}
	
	@Override
	public FileSystem getDatabaseFs() throws ErrorException { 
		return getUserStore().getStoreFs(null);
	}
	
	@Override
	public Path getDatabasePath(IDatabase.Manager manager) 
			throws ErrorException { 
		FileSystem fs = getDatabaseFs();
		boolean isLocal = FsUtils.isLocalFs(fs);
		
		String userId = manager.getUserKey();
		
		if (isLocal) { 
			File dir = getDatabaseDirFile(userId);
			return new Path(dir.getAbsolutePath());
			
		} else { 
			return getDatabaseDirPath(userId);
		}
	}
	
	@Override
	public Path getLibraryPath(DataManager manager, ILibrary library) 
			throws ErrorException { 
		FileSystem fs = library.getStoreFs();
		boolean isLocal = FsUtils.isLocalFs(fs);
		
		String userId = manager.getUserKey();
		String libraryId = library.getContentKey();
		
		if (isLocal) { 
			File dir = getLibraryDirFile(userId, libraryId);
			return new Path(dir.getAbsolutePath());
			
		} else { 
			return getLibraryDirPath(userId, libraryId);
		}
	}
	
	@Override
	public void loadLibraryList(DataManager manager) throws ErrorException {
		DataConf.LibraryInfo[] libraryInfos = null;
		if (libraryInfos == null) {
			FileSystem fs = getUserStore().getStoreFs(null);
			NamedList<Object> items = null;
			
			if (fs != null && !FsUtils.isLocalFs(fs)) { 
				Path librarylistXml = getLibraryListXmlPath(manager.getUserKey());
				items = CoreStore.readXml(fs, librarylistXml, "librarylist");
				
			} else {
				File librarylistXml = getLibraryListXmlFile(manager.getUserKey());
				items = CoreStore.readXml(librarylistXml, "librarylist");
			}
			
			libraryInfos = DataConf.toLibraryInfos(items);
		}
		
		if (libraryInfos != null) { 
			ArrayList<DataConf.LibraryInfo> infos = new ArrayList<DataConf.LibraryInfo>();
			
			for (int i=0; i < libraryInfos.length; i++) { 
				DataConf.LibraryInfo libraryInfo = libraryInfos[i];
				if (libraryInfo == null) continue;
				
				String libraryId = libraryInfo.key;
				String userId = manager.getUserKey(); 
				String storeUri = libraryInfo.storeUri;
				
				if (userId == null) userId = "root";
				if (storeUri == null) storeUri = "file:///";
				if (libraryId == null || libraryId.length() == 0)
					continue;
				
				if (storeUri.startsWith("file:")) { 
					File libraryXml = getLibraryXmlFile(userId, libraryId);
					NamedList<Object> item = CoreStore.readXml(libraryXml, "library");
					
					DataConf.LibraryInfo info = DataConf.toLibraryInfo(item);
					if (info != null) infos.add(info);
					
				} else if (storeUri.startsWith("dfs:")) { 
					Path path = getLibraryXmlPath(userId, libraryId);
					
					FileSystem fs = manager.getCore().getStoreFs(storeUri);
					NamedList<Object> item = CoreStore.readXml(fs, path, "library");
					
					DataConf.LibraryInfo info = DataConf.toLibraryInfo(item);
					if (info != null) infos.add(info);
					
				} else { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Store system: " + storeUri + " not supported");
				}
			}
			
			DataConf.loadLibrary(manager, 
					infos.toArray(new DataConf.LibraryInfo[infos.size()]));
		}
	}
	
	@Override
	public void saveLibraryList(DataManager manager, ILibrary[] libraries) 
			throws ErrorException {
		if (libraries != null) { 
			for (ILibrary library : libraries) { 
				if (library == null) continue;
				
				FileSystem fs = library.getStoreFs();
				boolean isLocal = FsUtils.isLocalFs(fs);
				
				String userId = manager.getUserKey(); 
				String libraryId = library.getContentKey();
				NamedList<Object> items = DataConf.toNamedList(library);
				
				if (isLocal) {
					File libraryxml = getLibraryXmlFile(userId, libraryId);
					CoreStore.writeXml(libraryxml, items, "library");
					
				} else { 
					Path path = getLibraryXmlPath(userId, libraryId);
					CoreStore.writeXml(fs, path, items, "library");
				}
			}
		}
		
		FileSystem fs = getUserStore().getStoreFs(null);
		NamedList<Object> items = DataConf.toSimpleNamedList(libraries);
		
		if (fs != null && !FsUtils.isLocalFs(fs)) { 
			Path librarylistXml = getLibraryListXmlPath(manager.getUserKey());
			CoreStore.writeXml(fs, librarylistXml, items, "librarylist");
			
		} else {
			File librarylistXml = getLibraryListXmlFile(manager.getUserKey());
			CoreStore.writeXml(librarylistXml, items, "librarylist");
		}
	}
	
}
