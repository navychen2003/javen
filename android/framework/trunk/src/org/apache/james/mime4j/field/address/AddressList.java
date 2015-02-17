package org.apache.james.mime4j.field.address;

import java.io.StringReader;
import java.util.ArrayList;

import org.apache.james.mime4j.field.address.parser.AddressListParser;
import org.apache.james.mime4j.field.address.parser.ParseException;

/**
 * An immutable, random-access list of Address objects.
 *
 */
@SuppressWarnings("unchecked")
public class AddressList {
	
	private ArrayList<Address> addresses;

	/**
	 * @param addresses An ArrayList that contains only Address objects. 
	 * @param dontCopy true iff it is not possible for the addresses ArrayList to be modified by someone else.
	 */
	public AddressList(ArrayList<Address> addresses, boolean dontCopy) {
		if (addresses != null)
			this.addresses = (dontCopy ? addresses : (ArrayList<Address>) addresses.clone());
		else
			this.addresses = new ArrayList<Address>(0);
	}

	/**
	 * The number of elements in this list.
	 */
	public int size() {
		return addresses.size();
	}

	/**
	 * Gets an address. 
	 */
	public Address get(int index) {
		if (0 > index || size() <= index)
			throw new IndexOutOfBoundsException();
		return (Address) addresses.get(index);
	}

	/**
	 * Returns a flat list of all mailboxes represented
	 * in this address list. Use this if you don't care
	 * about grouping. 
	 */
	public MailboxList flatten() {
		// in the common case, all addresses are mailboxes
		boolean groupDetected = false;
		for (int i = 0; i < size(); i++) {
			if (!(get(i) instanceof Mailbox)) {
				groupDetected = true;
				break;
			}
		}
		
		if (!groupDetected)
			return new MailboxList(addresses, true);
		
		ArrayList<Address> results = new ArrayList<Address>();
		for (int i = 0; i < size(); i++) {
			Address addr = get(i);
			addr.addMailboxesTo(results);
		}
		
		// copy-on-construct this time, because subclasses
		// could have held onto a reference to the results
		return new MailboxList(results, false);
	}
	
	/**
	 * Dumps a representation of this address list to
	 * stdout, for debugging purposes.
	 */
	public void print() {
		for (int i = 0; i < size(); i++) {
			Address addr = get(i);
			System.out.println(addr.toString());
		}
	}



	/**
	 * Parse the address list string, such as the value 
	 * of a From, To, Cc, Bcc, Sender, or Reply-To
	 * header.
	 * 
	 * The string MUST be unfolded already.
	 */
	public static AddressList parse(String rawAddressList) throws ParseException {
		AddressListParser parser = new AddressListParser(new StringReader(rawAddressList));
		return Builder.getInstance().buildAddressList(parser.parse());
	}
	
	/**
	 * Test console.
	 */
	public static void main(String[] args) throws Exception {
		java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
		while (true) {
			try {
				System.out.print("> ");
				String line = reader.readLine();
				if (line.length() == 0 || line.toLowerCase().equals("exit") || line.toLowerCase().equals("quit")) {
					System.out.println("Goodbye.");
					return;
				}
				AddressList list = parse(line);
				list.print();
			}
			catch(Exception e) {
				e.printStackTrace();
				Thread.sleep(300);
			}
		}
	}
}