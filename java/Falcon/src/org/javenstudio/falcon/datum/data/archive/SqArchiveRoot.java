package org.javenstudio.falcon.datum.data.archive;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.datum.data.FileSource;
import org.javenstudio.falcon.datum.data.FileStorer;
import org.javenstudio.falcon.datum.data.NameData;
import org.javenstudio.falcon.datum.data.SqFileSource;
import org.javenstudio.falcon.datum.data.SqLibrary;
import org.javenstudio.falcon.datum.data.SqRoot;
import org.javenstudio.falcon.datum.data.SqRootDir;
import org.javenstudio.falcon.datum.data.SqRootFile;

public final class SqArchiveRoot extends SqRoot {
	//private static final Logger LOG = Logger.getLogger(SqArchiveRoot.class);
	
	//public static final String ARCHIVE_NAME = "Archive";
	
	//public static SqArchiveRoot create(SqLibrary library) 
	//		throws ErrorException { 
	//	return create(library, ARCHIVE_NAME);
	//}
	
	public static SqArchiveRoot create(SqLibrary library, String name) 
			throws ErrorException { 
		String rootKey = SectionHelper.newArchiveRootKey(
				name + "@" + System.currentTimeMillis());
		
		String rootPathKey = SectionHelper.newFileKey(
				name + "@" + System.currentTimeMillis(), true);
		
		return new SqArchiveRoot(library, name, rootKey, rootPathKey);
	}
	
	private String mRootName = null;
	
	public SqArchiveRoot(SqLibrary library, String name, 
			String key, String pathKey) {
		super(library, name, key, pathKey);
		mRootName = name;
		onCreated();
	}

	@Override
	public String getContentType() { 
		return "application/x-archive-root";
	}
	
	@Override
	public SqRootDir newRootDir(NameData nameData) { 
		return new SqArchiveDir(this, nameData);
	}
	
	@Override
	public SqRootFile newRootFile(NameData nameData) { 
		return SqArchiveFile.create(this, nameData);
	}

	@Override
	public FileStorer newStorer() {
		return new FileStorer() {
				private final SqFileSource mSource = 
						new SqFileSource(SqArchiveRoot.this);
				@Override
				public FileSource getSource() {
					return mSource;
				}
			};
	}

	@Override
	public String getName() {
		return mRootName; 
	}
	
	@Override
	public boolean setName(String name) { 
		if (name != null && name.length() > 0 && !name.equals(mRootName)) { 
			mRootName = name;
			return true;
		}
		return false;
	}
	
	@Override
	public boolean canMove() { 
		return false;
	}
	
	@Override
	public boolean canDelete() { 
		return true;
	}
	
	@Override
	public boolean canWrite() { 
		return true;
	}
	
	@Override
	public boolean canCopy() { 
		return false;
	}
	
	@Override
	public boolean supportOperation(IData.Operation op) { 
		if (op != null) { 
			switch (op) { 
			case DELETE: return true;
			case UPLOAD: return true;
			case NEWFOLDER: return true;
			case MOVE: return true;
			case COPY: return true;
			default: return false;
			}
		}
		return false;
	}
	
}
