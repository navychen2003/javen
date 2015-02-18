package org.javenstudio.falcon.datum.data.recycle;

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

public class SqRecycleRoot extends SqRoot {
	//private static final Logger LOG = Logger.getLogger(SqRecycleRoot.class);

	public static final String RECYCLE_NAME = "Recycle Bin";
	
	public static SqRecycleRoot create(SqLibrary library) 
			throws ErrorException { 
		return create(library, RECYCLE_NAME);
	}
	
	private static SqRecycleRoot create(SqLibrary library, String name) 
			throws ErrorException { 
		String rootKey = SectionHelper.newRecycleRootKey(
				name + "@" + System.currentTimeMillis());
		
		String rootPathKey = SectionHelper.newFileKey(
				name + "@" + System.currentTimeMillis(), true);
		
		return new SqRecycleRoot(library, name, rootKey, rootPathKey);
	}
	
	public SqRecycleRoot(SqLibrary library, String name, 
			String key, String pathKey) {
		super(library, name, key, pathKey);
		onCreated();
	}

	@Override
	public String getContentType() { 
		return "application/x-recycle-root";
	}
	
	@Override
	public SqRootDir newRootDir(NameData data) { 
		return new SqRecycleDir(this, data);
	}
	
	@Override
	public SqRootFile newRootFile(NameData data) { 
		return SqRecycleFile.create(this, data);
	}
	
	@Override
	public FileStorer newStorer() {
		return new FileStorer() {
				private final SqFileSource mSource = 
						new SqFileSource(SqRecycleRoot.this);
				@Override
				public FileSource getSource() {
					return mSource;
				}
			};
	}
	
	@Override
	public boolean canMove() { 
		return false;
	}
	
	@Override
	public boolean canDelete() { 
		return false;
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
			case MOVE: return true;
			case COPY: return false;
			case UPLOAD: return false;
			case NEWFOLDER: return false;
			case EMPTY: return true;
			}
		}
		return false;
	}
	
}
