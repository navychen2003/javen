package org.apache.james.mime4j.field.address;

import java.util.ArrayList;

/**
 * An immutable, random-access list of Strings (that 
 * are supposedly domain names or domain literals).
 *
 */
@SuppressWarnings("unchecked")
public class DomainList {
	private ArrayList<String> domains;
	
	/**
	 * @param domains An ArrayList that contains only String objects. 
	 * @param dontCopy true iff it is not possible for the domains ArrayList to be modified by someone else.
	 */
	public DomainList(ArrayList<String> domains, boolean dontCopy) {
		if (domains != null)
			this.domains = (dontCopy ? domains : (ArrayList<String>) domains.clone());
		else
			this.domains = new ArrayList<String>(0);
	}
	
	/**
	 * The number of elements in this list.
	 */
	public int size() {
		return domains.size();
	}

	/**
	 * Gets the domain name or domain literal at the
	 * specified index.
	 * @throws IndexOutOfBoundsException If index is &lt; 0 or &gt;= size().
	 */
	public String get(int index) {
		if (0 > index || size() <= index)
			throw new IndexOutOfBoundsException();
		return (String) domains.get(index);
	}

	/**
	 * Returns the list of domains formatted as a route
	 * string (not including the trailing ':'). 
	 */
	public String toRouteString() {
		StringBuffer out = new StringBuffer();
		for (int i = 0; i < domains.size(); i++) {
			out.append("@");
			out.append(get(i));
			if (i + 1 < domains.size())
				out.append(",");
		}
		return out.toString();
	}
}