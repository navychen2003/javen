package org.javenstudio.provider.account;

import org.javenstudio.provider.ProviderActionTabBase;

public class AccountActionTab extends ProviderActionTabBase {
	//private static final Logger LOG = Logger.getLogger(AccountActionTab.class);

	public AccountActionTab(AccountInfoItem item, String name) { 
		this(item, name, 0);
	}
	
	public AccountActionTab(AccountInfoItem item, String name, int iconRes) { 
		super(item, name, iconRes);
	}
	
}
