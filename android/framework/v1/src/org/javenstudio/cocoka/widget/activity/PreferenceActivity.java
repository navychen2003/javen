package org.javenstudio.cocoka.widget.activity;

import java.util.ArrayList;
import java.util.List;

import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;

public class PreferenceActivity extends android.preference.PreferenceActivity {

	private final List<PreferenceField> mFields = new ArrayList<PreferenceField>(); 
	
	protected abstract class PreferenceField implements Preference.OnPreferenceChangeListener { 
		private boolean mRequired = false; 
		
		public PreferenceField() { 
			synchronized (mFields) { 
				mFields.add(this); 
			}
		}
		
		public abstract Preference getPreference(); 
		public abstract String getValue(); 
		
		public final void setRequired(boolean required) { 
			mRequired = required; 
		}
		
		public final boolean isRequired() { 
			return mRequired; 
		}
	}
	
	protected final PreferenceField[] getPreferenceFields(boolean empty, boolean required) { 
		synchronized (mFields) { 
			ArrayList<PreferenceField> fields = new ArrayList<PreferenceField>(); 
			
			for (PreferenceField field : mFields) { 
				boolean emptyOk = !empty || isPreferenceFieldEmpty(field); 
				boolean requiredOk = !required || field.isRequired(); 
				
				if (emptyOk && requiredOk) 
					fields.add(field); 
			}
			
			if (fields.size() > 0) 
				return fields.toArray(new PreferenceField[0]); 
			
			return null; 
		}
	}
	
	protected boolean isPreferenceFieldEmpty(PreferenceField field) { 
		if (field != null) { 
			String value = field.getValue(); 
			if (value != null && value.length() > 0) 
				return false; 
		}
		return true; 
	}
	
	protected class EditTextField extends PreferenceField { 
    	private final EditTextPreference mEditText; 
    	
    	public EditTextField(CharSequence preferenceKey) { 
    		this(preferenceKey, null, null); 
    	}
    	
    	public EditTextField(CharSequence preferenceKey, String value) { 
    		this(preferenceKey, value, value); 
    	}
    	
    	public EditTextField(CharSequence preferenceKey, String value, String summary) { 
    		mEditText = (EditTextPreference) findPreference(preferenceKey); 
    		mEditText.setOnPreferenceChangeListener(this); 
    		
    		if (summary == null) 
    			summary = value; 
    		
    		setSummary(normalizeSummary(summary)); 
    		setValue(normalizeValue(value)); 
    	}
    	
    	@Override 
    	public final Preference getPreference() { 
    		return getEditTextPreference(); 
    	}
    	
    	public final EditTextPreference getEditTextPreference() { 
    		return mEditText; 
    	}
    	
    	@Override 
    	public final String getValue() { 
    		return getText(); 
    	}
    	
    	public final String getText() { 
    		return mEditText.getText(); 
    	}
    	
    	@Override 
    	public boolean onPreferenceChange(Preference preference, Object newValue) { 
    		final String value = newValue.toString();
    		
    		changeValue(value); 
    		
    		return false;
    	}
    	
    	public void changeValue(String value) { 
    		setSummary(normalizeSummary(value));
    		setValue(normalizeValue(value));
    		
    		onValueChanged(value); 
    	}
    	
    	protected void setSummary(String summary) { 
    		if (summary != null) 
    			mEditText.setSummary(summary); 
    	}
    	
    	protected void setValue(String value) { 
    		mEditText.setText(value); 
    	}
    	
    	public String normalizeSummary(String text) { 
    		return text; 
    	}
    	
    	public String normalizeValue(String value) { 
    		return value; 
    	}
    	
    	public void onValueChanged(String value) { 
    		// ignore
    	}
    }
    
	protected class PasswordField extends EditTextField { 
    	public PasswordField(CharSequence preferenceKey) { 
    		this(preferenceKey, null, null); 
    	}
    	
    	public PasswordField(CharSequence preferenceKey, String value) { 
    		this(preferenceKey, value, value); 
    	}
    	
    	public PasswordField(CharSequence preferenceKey, String value, String summary) { 
    		super(preferenceKey, value, summary); 
    	}
    	
    	public String normalizeSummary(String text) { 
    		if (text != null && text.length() > 0) { 
    			StringBuilder sbuf = new StringBuilder(); 
    			for (int i=0; i < text.length(); i++) { 
    				sbuf.append('*'); 
    			}
    			text = sbuf.toString(); 
    		}
    		return text; 
    	}
	}
	
    protected class ListField extends PreferenceField { 
    	private final ListPreference mList; 
    	
    	public ListField(CharSequence preferenceKey) { 
    		this(preferenceKey, null, null); 
    	}
    	
    	public ListField(CharSequence preferenceKey, String value) { 
    		this(preferenceKey, value, null); 
    	}
    	
    	public ListField(CharSequence preferenceKey, String value, String summary) { 
    		mList = (ListPreference) findPreference(preferenceKey); 
    		mList.setOnPreferenceChangeListener(this); 
    		
    		if (summary != null) 
    			setSummary(summary); 
    		else 
    			setSummary(normalizeSummary(value)); 
    		
    		if (value != null) 
    			setValue(normalizeValue(value)); 
    	}
    	
    	@Override 
    	public final Preference getPreference() { 
    		return getListPreference(); 
    	}
    	
    	public final ListPreference getListPreference() { 
    		return mList; 
    	}
    	
    	@Override 
    	public final String getValue() { 
    		return mList.getValue(); 
    	}
    	
    	@Override 
    	public boolean onPreferenceChange(Preference preference, Object newValue) { 
    		final String value = newValue.toString();
    		
    		changeValue(value); 
    		
    		return false;
    	}
    	
    	public void changeValue(String value) { 
    		setSummary(normalizeSummary(value));
    		setValue(normalizeValue(value));
    		
    		onValueChanged(value); 
    	}
    	
    	protected void setSummary(String summary) { 
    		if (summary != null) 
    			mList.setSummary(summary); 
    	}
    	
    	protected void setValue(String value) { 
    		mList.setValue(value); 
    	}
    	
    	public String normalizeSummary(String value) { 
    		int index = mList.findIndexOfValue(value);
    		CharSequence[] entries = mList.getEntries(); 
    		
    		String summary = entries != null && index >= 0 && index < entries.length ? 
    				entries[index].toString() : null; 
    		
    		return summary; 
    	}
    	
    	public String normalizeValue(String value) { 
    		return value; 
    	}
    	
    	public void onValueChanged(String value) { 
    		// ignore
    	}
    }
    
    protected class CheckBoxField extends PreferenceField { 
    	private final CheckBoxPreference mCheckbox; 
    	
    	public CheckBoxField(CharSequence preferenceKey) { 
    		this(preferenceKey, false, null); 
    	}
    	
    	public CheckBoxField(CharSequence preferenceKey, boolean value) { 
    		this(preferenceKey, value, null); 
    	}
    	
    	public CheckBoxField(CharSequence preferenceKey, boolean value, String summary) { 
    		mCheckbox = (CheckBoxPreference) findPreference(preferenceKey); 
    		mCheckbox.setOnPreferenceChangeListener(this); 
    		
    		if (summary != null) 
    			setSummary(summary); 
    		else 
    			setSummary(normalizeSummary(value)); 
    		
    		setValue(normalizeValue(value)); 
    	}
    	
    	@Override 
    	public final Preference getPreference() { 
    		return getCheckBoxPreference(); 
    	}
    	
    	public final CheckBoxPreference getCheckBoxPreference() { 
    		return mCheckbox; 
    	}
    	
    	@Override 
    	public final String getValue() { 
    		return Boolean.toString(getChecked()); 
    	}
    	
    	public final boolean getChecked() { 
    		return mCheckbox.isChecked(); 
    	}
    	
    	@Override 
    	public boolean onPreferenceChange(Preference preference, Object newValue) { 
    		final boolean value = Boolean.valueOf(newValue.toString());
    		
    		changeValue(value); 
    		
    		return false;
    	}
    	
    	public void changeValue(boolean value) { 
    		setSummary(normalizeSummary(value));
    		setValue(normalizeValue(value));
    		
    		onValueChanged(value); 
    	}
    	
    	protected void setSummary(String summary) { 
    		if (summary != null) 
    			mCheckbox.setSummary(summary); 
    	}
    	
    	protected void setValue(boolean value) { 
    		mCheckbox.setChecked(value); 
    	}
    	
    	public String normalizeSummary(boolean value) { 
    		String summary = null; 
    		
    		return summary; 
    	}
    	
    	public boolean normalizeValue(boolean value) { 
    		return value; 
    	}
    	
    	public void onValueChanged(boolean value) { 
    		// ignore
    	}
    }
	
}
