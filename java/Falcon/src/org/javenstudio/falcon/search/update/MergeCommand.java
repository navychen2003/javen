package org.javenstudio.falcon.search.update;

import org.javenstudio.hornet.index.segment.DirectoryReader;
import org.javenstudio.falcon.search.ISearchRequest;

/**
 * A merge indexes command encapsulated in an object.
 *
 * @since 1.4
 */
public class MergeCommand extends UpdateCommand {
	
	private DirectoryReader[] mReaders;

	public MergeCommand(ISearchRequest req, DirectoryReader[] readers) {
		super(req);
		mReaders = readers;
	}

	@Override
	public String getName() {
		return "mergeIndexes";
	}

	public DirectoryReader[] getReaders() { 
		return mReaders;
	}
	
	@Override
	protected void toString(StringBuilder sb) {
		if (mReaders != null && mReaders.length > 0) {
			sb.append("readers={");
			sb.append(mReaders[0].getDirectory());
			for (int i = 1; i < mReaders.length; i++) {
				sb.append(",").append(mReaders[i].getDirectory());
			}
			sb.append("}");
		}
	}
	
}
