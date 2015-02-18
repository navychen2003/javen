package org.javenstudio.falcon.datum;

public abstract class DataObject implements IData {
	//private static final Logger LOG = Logger.getLogger(DataObject.class);

	//@Override
	//public String getContentType() { 
	//	return MimeType.TYPE_APPLICATION.getType();
	//}
	
	@Override
	public long getModifiedTime() { 
		return 0;
	}
	
}
