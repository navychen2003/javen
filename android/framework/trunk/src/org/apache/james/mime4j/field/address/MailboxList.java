package org.apache.james.mime4j.field.address;

import java.util.ArrayList;

/**
 * An immutable, random-access list of Mailbox objects.
 *
 */
@SuppressWarnings("unchecked")
public class MailboxList {

	private ArrayList<Address> mailboxes;
	
	/**
	 * @param mailboxes An ArrayList that contains only Mailbox objects. 
	 * @param dontCopy true iff it is not possible for the mailboxes ArrayList to be modified by someone else.
	 */
	public MailboxList(ArrayList<Address> mailboxes, boolean dontCopy) {
		if (mailboxes != null)
			this.mailboxes = (dontCopy ? mailboxes : (ArrayList<Address>) mailboxes.clone());
		else
			this.mailboxes = new ArrayList<Address>(0);
	}
	
	/**
	 * The number of elements in this list.
	 */
	public int size() {
		return mailboxes.size();
	}
	
	/**
	 * Gets an address. 
	 */
	public Mailbox get(int index) {
		if (0 > index || size() <= index)
			throw new IndexOutOfBoundsException();
		return (Mailbox) mailboxes.get(index);
	}
	
	/**
	 * Dumps a representation of this mailbox list to
	 * stdout, for debugging purposes.
	 */
	public void print() {
		for (int i = 0; i < size(); i++) {
			Mailbox mailbox = get(i);
			System.out.println(mailbox.toString());
		}
	}

}