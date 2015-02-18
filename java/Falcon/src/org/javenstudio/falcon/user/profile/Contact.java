package org.javenstudio.falcon.user.profile;

import java.util.HashMap;
import java.util.Map;

public class Contact extends Address {

	public static final String TYPE_FAMILY = "family";
	public static final String TYPE_FRIEND = "friend";
	
	public static final String ADDRESS_DEFAULT = "default";
	public static final String ADDRESS_HOME = "home";
	public static final String ADDRESS_COMPANY = "company";
	
	private final Map<String, Address> mAddrs = new HashMap<String, Address>();
	
	public Contact(String key) { 
		super(key);
	}
	
	@Override
	public void clear() { 
		super.clear();
		
		synchronized (mAddrs) { 
			mAddrs.clear();
		}
	}
	
	public void addAll(Contact addr) { 
		if (addr == null || addr == this) return;
		super.addAll(addr, false);
		
		for (Address item : addr.mAddrs.values()) { 
			addAddress(item);
		}
	}
	
	public Address addAddress(Address addr) { 
		if (addr == null) return null;
		if (addr == this) return this;
		
		synchronized (mAddrs) { 
			String key = addr.getKey();
			
			Address item = mAddrs.get(key);
			if (item != null) { 
				item.addAll(addr, false);
				return item;
			}
			
			mAddrs.put(key, addr);
			return addr;
		}
	}
	
	public Address removeAddress(String name) { 
		if (name == null) return null;
		
		synchronized (mAddrs) { 
			return mAddrs.remove(name);
		}
	}
	
	public Address getAddress(String name) { 
		if (name == null) return null;
		
		synchronized (mAddrs) { 
			return mAddrs.get(name);
		}
	}
	
	public String[] getAddressNames() { 
		synchronized (mAddrs) { 
			return mAddrs.keySet().toArray(new String[mAddrs.size()]);
		}
	}
	
	public Address[] getAddresses() { 
		synchronized (mAddrs) { 
			return mAddrs.values().toArray(new Address[mAddrs.size()]);
		}
	}
	
	public void clearAddresses() { 
		synchronized (mAddrs) { 
			mAddrs.clear();
		}
	}
	
	public int getAddressSize() { 
		synchronized (mAddrs) { 
			return mAddrs.size();
		}
	}
	
}
