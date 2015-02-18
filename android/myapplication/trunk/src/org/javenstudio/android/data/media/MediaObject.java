package org.javenstudio.android.data.media;

import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.data.DataException;
import org.javenstudio.android.data.DataObject;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.cocoka.data.IMediaObject;
import org.javenstudio.cocoka.data.MediaHelper;

public abstract class MediaObject extends DataObject 
		implements IMediaObject {
    
    public MediaObject(DataPath path, long version) { 
    	super(path, version);
    }
    
    public int getSupportedOperations() {
        return 0;
    }
    
    public boolean supportOperation(FileOperation.Operation op) {
    	return false;
    }
    
    public boolean delete() throws DataException {
        throw new UnsupportedOperationException();
    }

    public void rotate(int degrees) {
        throw new UnsupportedOperationException();
    }
    
    public static long nextVersionNumber() {
    	return MediaHelper.nextVersionNumber();
    }
    
}
