package org.javenstudio.falcon.datum.data;

import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.permission.FsAction;
import org.javenstudio.raptor.fs.permission.FsPermission;

public class FileFilter {

	public boolean accept(FileStatus status) { 
		if (status != null && !status.isHidden()) { 
			FsPermission p = status.getPermission();
			if (p != null) { 
				FsAction ua = p.getUserAction();
				FsAction ga = p.getGroupAction();
				FsAction oa = p.getOtherAction();
				
				if (ua != null && ua.implies(FsAction.READ))
					return true;
				
				if (ga != null && ga.implies(FsAction.READ))
					return true;
				
				if (oa != null && oa.implies(FsAction.READ))
					return true;
			}
		}
		
		return false;
	}
	
}
