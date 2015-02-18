package org.javenstudio.provider.people.contact;

public class ContactFactory {

	public ContactDataSets createContactDataSets(ContactProvider p) { 
		return new ContactDataSets(new ContactCursorFactory());
	}
	
	public ContactBinder createContactBinder(ContactProvider p) { 
		return new ContactBinder(p);
	}
	
}
