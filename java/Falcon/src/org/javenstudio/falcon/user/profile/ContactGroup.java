package org.javenstudio.falcon.user.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingConf;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class ContactGroup {
	private static final Logger LOG = Logger.getLogger(ContactGroup.class);

	private final Map<String, Contact> mContacts = new HashMap<String, Contact>();
	private final ContactManager mManager;
	private final String mType;
	
	public ContactGroup(ContactManager manager, String type) { 
		if (manager == null || type == null) throw new NullPointerException();
		mManager = manager;
		mType = type;
	}

	public ContactManager getManager() { return mManager; }
	public String getContactType() { return mType; }
	
	public Contact addContact(Contact contact) { 
		if (contact == null) return null;
		
		synchronized (mContacts) { 
			String key = contact.getKey();
			
			if (LOG.isDebugEnabled())
				LOG.debug("addContact: contact=" + contact);
			
			Contact item = mContacts.get(key);
			if (item != null) { 
				item.addAll(contact);
				return item;
			}
			
			mContacts.put(key, contact);
			return contact;
		}
	}
	
	public Contact removeContact(String key) { 
		if (key == null) return null;
		
		synchronized (mContacts) { 
			Contact contact = mContacts.remove(key);
			
			if (LOG.isDebugEnabled())
				LOG.debug("removeContact: contact=" + contact);
			
			return contact;
		}
	}
	
	public Contact getContact(String key) { 
		if (key == null) return null;
		
		synchronized (mContacts) { 
			return mContacts.get(key);
		}
	}
	
	public String[] getContactKeys() { 
		synchronized (mContacts) { 
			return mContacts.keySet().toArray(new String[mContacts.size()]);
		}
	}
	
	public Contact[] getContacts() { 
		synchronized (mContacts) { 
			return mContacts.values().toArray(new Contact[mContacts.size()]);
		}
	}
	
	public void clearContacts() { 
		synchronized (mContacts) { 
			if (LOG.isDebugEnabled())
				LOG.debug("clearContacts");
			
			mContacts.clear();
		}
	}
	
	public int getContactCount() { 
		synchronized (mContacts) { 
			return mContacts.size();
		}
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("close: type=" + getContactType());
		
		clearContacts();
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{type=" + mType + "}";
	}
	
	static void loadContactGroup(ContactManager manager, 
			NamedList<Object> item) throws ErrorException { 
		if (manager == null || item == null) return;
		
		String type = SettingConf.getString(item, "type");
		Contact[] contacts = loadContacts(item.get("contacts"));
		
		if (type == null || type.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"ContactGroup type: " + type + " is wrong");
		}
		
		if (contacts != null && contacts.length > 0) { 
			for (Contact contact : contacts) { 
				manager.addContact(contact, type);
			}
		}
	}
	
	static NamedList<Object> toNamedList(ContactGroup item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("type", item.getContactType());
		info.add("contacts", toNamedLists(item.getContacts()));
		
		return info;
	}
	
	private static Contact[] loadContacts(Object listVal) 
			throws ErrorException { 
		ArrayList<Contact> list = new ArrayList<Contact>();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("loadContacts: class=" + (listVal!=null?listVal.getClass():null) 
					+ " list=" + listVal);
		}
		
		if (listVal != null && listVal instanceof List) { 
			List<?> listItem = (List<?>)listVal;
			
			for (int j=0; j < listItem.size(); j++) { 
				Object val = listItem.get(j);
				
				if (LOG.isDebugEnabled())
					LOG.debug("loadContacts: listItem=" + val);
				
				if (val != null && val instanceof NamedList) { 
					@SuppressWarnings("unchecked")
					NamedList<Object> item = (NamedList<Object>)val;
					
					Contact contact = loadContact(item);
					if (contact != null)
						list.add(contact);
				}
			}
		}
		
		return list.toArray(new Contact[list.size()]);
	}
	
	@SuppressWarnings("unchecked")
	private static NamedList<Object>[] toNamedLists(Contact[] contacts) 
			throws ErrorException { 
		ArrayList<NamedList<?>> items = new ArrayList<NamedList<?>>();
		
		for (int i=0; contacts != null && i < contacts.length; i++) { 
			Contact contact = contacts[i];
			NamedList<Object> contactInfo = toNamedList(contact);
			if (contact != null && contactInfo != null) 
				items.add(contactInfo);
		}
		
		return items.toArray(new NamedList[items.size()]);
	}
	
	@SuppressWarnings("unchecked")
	private static Contact loadContact(NamedList<Object> item) 
			throws ErrorException { 
		if (item == null) return null;
		
		String key = SettingConf.getString(item, "key");
		Address def = Address.loadAddress((NamedList<Object>)item.get("default"));
		Address[] addrs = Address.loadAddresses(item.get("addresses"));
		
		if (key == null || key.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Contact key: " + key + " is wrong");
		}
		
		if (def == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Contact default address is wrong");
		}
		
		Contact contact = new Contact(key);
		contact.addAll(def, false);
		
		if (addrs != null && addrs.length > 0) { 
			for (Address addr : addrs) { 
				contact.addAddress(addr);
			}
		}
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadContact: contact=" + contact);
		
		return contact;
	}
	
	private static NamedList<Object> toNamedList(Contact item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("key", item.getKey());
		info.add("default", Address.toNamedList((Address)item));
		info.add("addresses", Address.toNamedLists(item.getAddresses()));
		
		return info;
	}
	
}
