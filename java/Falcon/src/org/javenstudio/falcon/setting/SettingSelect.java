package org.javenstudio.falcon.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class SettingSelect extends Setting {

	public static class Option { 
		private final String mValue;
		private String mTitle;
		
		public Option(String value) { 
			this(value, null);
		}
		
		public Option(String value, String title) { 
			if (value == null) throw new NullPointerException();
			mValue = value;
			mTitle = title;
		}
		
		public String getValue() { return mValue; }
		public String getTitle() { return mTitle; }
		public void setTitle(String text) { mTitle = text; }
	}
	
	private List<Option> mOptions = new ArrayList<Option>();
	private Comparator<Option> mSorter = null;
	
	public SettingSelect(SettingManager manager, String name) { 
		this(manager, name, null);
	}
	
	public SettingSelect(SettingManager manager, String name, String value) { 
		super(manager, name, value);
	}

	@Override
	public String getType() {
		return Setting.SELECT_TYPE;
	}
	
	public void addOption(String value) { 
		addOption(value, null);
	}
	
	public void addOption(String value, String title) { 
		if (value == null) return;
		
		synchronized (mOptions) { 
			for (Option item : mOptions) { 
				if (value.equals(item.getValue()))
					return;
			}
			
			mOptions.add(new Option(value, title));
		}
	}
	
	public void clearOptions() { 
		synchronized (mOptions) { 
			mOptions.clear();
		}
	}
	
	public void setSorter(Comparator<Option> sorter) { 
		mSorter = sorter;
	}
	
	public Option[] getOptions() { 
		return getOptions(mSorter);
	}
	
	public Option[] getOptions(Comparator<Option> sorter) { 
		synchronized (mOptions) { 
			Option[] result = mOptions.toArray(new Option[mOptions.size()]);
			
			if (sorter != null) 
				Arrays.sort(result, sorter);
			
			return result;
		}
	}
	
	@Override
	protected void toNamedList(NamedList<Object> result) { 
		super.toNamedList(result);
		
		if (getManager().isSaveAll()) {
			ArrayList<Object> list = new ArrayList<Object>();
			Option[] options = getOptions();
			
			for (int i=0; options != null && i < options.length; i++) { 
				Option option = options[i];
				if (option == null) continue;
				
				NamedList<Object> item = new NamedMap<Object>();
				item.add(Setting.VALUE_NAME, Setting.toString(option.getValue()));
				item.add(Setting.TITLE_NAME, Setting.toString(option.getTitle()));
				
				list.add(item);
			}
			
			result.add(Setting.OPTIONS_NAME, list.toArray(new Object[list.size()]));
		}
	}
	
}
