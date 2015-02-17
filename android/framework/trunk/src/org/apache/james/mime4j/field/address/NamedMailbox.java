package org.apache.james.mime4j.field.address;

/**
 * A Mailbox that has a name/description.
 *
 * 
 */
public class NamedMailbox extends Mailbox {
	private String name;

	/**
	 * @see Mailbox#Mailbox(String, String)
	 */
	public NamedMailbox(String name, String localPart, String domain) {
		super(localPart, domain);
		this.name = name;
	}

	/**
	 * @see Mailbox#Mailbox(DomainList, String, String)
	 */
	public NamedMailbox(String name, DomainList route, String localPart, String domain) {
		super(route, localPart, domain);
		this.name = name;
	}
	
	/**
	 * Creates a named mailbox based on an unnamed mailbox. 
	 */
	public NamedMailbox(String name, Mailbox baseMailbox) {
		super(baseMailbox.getRoute(), baseMailbox.getLocalPart(), baseMailbox.getDomain());
		this.name = name;
	}

	/**
	 * Returns the name of the mailbox. 
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Same features (or problems) as Mailbox.getAddressString(boolean),
	 * only more so.
	 * 
	 * @see Mailbox#getAddressString(boolean) 
	 */
	public String getAddressString(boolean includeRoute) {
		return (name == null ? "" : name + " ") + super.getAddressString(includeRoute);
	}
}