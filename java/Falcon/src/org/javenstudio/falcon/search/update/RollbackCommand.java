package org.javenstudio.falcon.search.update;

import org.javenstudio.falcon.search.ISearchRequest;

/**
 *
 * @since 1.4
 */
public class RollbackCommand extends UpdateCommand {

	public RollbackCommand(ISearchRequest req) {
		super(req);
	}

	@Override
	public String getName() {
		return "rollback";
	}

}
