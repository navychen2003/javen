package org.javenstudio.falcon.datum.data.upload;

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

public class SqUploadRoot extends SqRoot {
	//private static final Logger LOG = Logger.getLogger(SqUploadRoot.class);

	public static final String UPLOAD_NAME = "Upload Files";
	
	public static boolean isDefaultUpload(SqRoot root) { 
		return root != null && root instanceof SqUploadRoot && 
				UPLOAD_NAME.equals(root.getName());
	}
	
	public static SqUploadRoot create(SqLibrary library) 
			throws ErrorException { 
		return create(library, UPLOAD_NAME);
	}
	
	private static SqUploadRoot create(SqLibrary library, String name) 
			throws ErrorException { 
		String rootKey = SectionHelper.newUploadRootKey(
				name + "@" + System.currentTimeMillis());
		
		String rootPathKey = SectionHelper.newFileKey(
				name + "@" + System.currentTimeMillis(), true);
		
		return new SqUploadRoot(library, name, rootKey, rootPathKey);
	}
	
	public SqUploadRoot(SqLibrary library, String name, 
			String key, String pathKey) {
		super(library, name, key, pathKey);
		onCreated();
	}

	@Override
	public String getContentType() { 
		return "application/x-upload-root";
	}
	
	@Override
	public SqRootDir newRootDir(NameData data) { 
		return new SqUploadDir(this, data);
	}

	@Override
	public SqRootFile newRootFile(NameData data) { 
		return SqUploadFile.create(this, data);
	}

	@Override
	public FileStorer newStorer() {
		return new FileStorer() {
				private final SqFileSource mSource = 
						new SqFileSource(SqUploadRoot.this);
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
