package org.javenstudio.android.entitydb.content;

import org.javenstudio.android.entitydb.TAccount;
import org.javenstudio.cocoka.database.SQLiteContentIterable;
import org.javenstudio.common.util.EntityCursor;

public class AccountIterable extends SQLiteContentIterable<AccountData, TAccount> {

	public AccountIterable(EntityCursor<TAccount> cursor) { 
		super(cursor); 
	}
	
	@Override 
	protected AccountData newContent(TAccount entity) { 
		return new AccountDataImpl(entity); 
	}
	
}
