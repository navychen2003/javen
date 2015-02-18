package org.javenstudio.lightning.core.datum;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.core.CoreContainer;
import org.javenstudio.lightning.core.CoreDescriptor;

public class DatumDescriptor extends CoreDescriptor {

	public DatumDescriptor(CoreContainer cores, String name, 
			String instanceDir) throws ErrorException {
		super(cores, name, instanceDir);
	}
	
	public DatumDescriptor(CoreDescriptor descr) {
		super(descr);
	}
	
}
