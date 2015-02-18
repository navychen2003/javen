package org.javenstudio.lightning.core.user;

import java.io.InputStream;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.lightning.core.CoreAdminConfig;
import org.javenstudio.lightning.core.CoreConfig;

public class UserConfig extends CoreConfig {

	public UserConfig(CoreAdminConfig conf, ContextLoader loader, 
			String name, InputStream is) throws ErrorException {
		super(conf, loader, name, is);
	}

}
