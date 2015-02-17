package org.apache.james.mime4j.field.address;

import java.util.ArrayList;

/**
 * The abstract base for classes that represent RFC2822 addresses.
 * This includes groups and mailboxes.
 * 
 * Currently, no public methods are introduced on this class.
 * 
 * 
 */
public abstract class Address {

	/**
	 * Adds any mailboxes represented by this address
	 * into the given ArrayList. Note that this method
	 * has default (package) access, so a doAddMailboxesTo
	 * method is needed to allow the behavior to be
	 * overridden by subclasses.
	 */
	final void addMailboxesTo(ArrayList<Address> results) {
		doAddMailboxesTo(results);
	}
	
	/**
	 * Adds any mailboxes represented by this address
	 * into the given ArrayList. Must be overridden by
	 * concrete subclasses.
	 */
	protected abstract void doAddMailboxesTo(ArrayList<Address> results);

}