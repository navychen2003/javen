package org.apache.james.mime4j.field.address;

import java.util.ArrayList;

/**
 * A named group of zero or more mailboxes.  
 *
 * 
 */
public class Group extends Address {
	private String name;
	private MailboxList mailboxList;
	
	/**
	 * @param name The group name.
	 * @param mailboxes The mailboxes in this group.
	 */
	public Group(String name, MailboxList mailboxes) {
		this.name = name;
		this.mailboxList = mailboxes;
	}

	/**
	 * Returns the group name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the mailboxes in this group.
	 */
	public MailboxList getMailboxes() {
		return mailboxList;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(name);
		buf.append(":");
		for (int i = 0; i < mailboxList.size(); i++) {
			buf.append(mailboxList.get(i).toString());
			if (i + 1 < mailboxList.size())
				buf.append(",");
		}
		buf.append(";");
		return buf.toString();
	}

	protected void doAddMailboxesTo(ArrayList<Address> results) {
		for (int i = 0; i < mailboxList.size(); i++)
			results.add(mailboxList.get(i));
	}
}