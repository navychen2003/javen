package org.javenstudio.provider.people;

import org.javenstudio.provider.people.contact.ContactFactory;
import org.javenstudio.provider.people.contact.ContactProvider;

public abstract class BaseContactProvider extends ContactProvider {

	public BaseContactProvider(String name, int iconRes, 
			ContactFactory factory) { 
		super(name, iconRes, factory);
	}
	
}
