package org.javenstudio.lightning.core.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.core.CoreContainer;
import org.javenstudio.lightning.core.CoreDescriptor;

public class UserDescriptor extends CoreDescriptor {

	public UserDescriptor(CoreContainer cores, String name, 
			String instanceDir) throws ErrorException {
		super(cores, name, instanceDir);
	}
	
	public UserDescriptor(CoreDescriptor descr) {
		super(descr);
	}
	
}
