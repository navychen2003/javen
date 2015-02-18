package org.javenstudio.cocoka.widget.setting;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Intent;
import android.util.AttributeSet;

import org.javenstudio.cocoka.widget.Constants;
import org.javenstudio.common.util.Log;

public class SettingInflater extends GenericInflater<Setting, SettingGroup> {
	private static final String INTENT_TAG_NAME = "intent";

	public SettingInflater(SettingManager manager) {
		super(manager); 
	}
	
    @Override
    protected boolean onCreateCustomFromTag(XmlPullParser parser, Setting parentSetting,
            AttributeSet attrs) throws XmlPullParserException {
        final String tag = parser.getName();
        
        if (tag.equals(INTENT_TAG_NAME)) {
            Intent intent = null;
            
            try {
                intent = Intent.parseIntent(getContext().getResources(), parser, attrs);
            } catch (IOException e) {
                Log.w(Constants.getTag(), "Could not parse Intent.", e);
            }
            
            if (intent != null) {
                parentSetting.setIntent(intent);
            }
            
            return true;
        }
        
        return false;
    }

    @Override
    protected SettingGroup onMergeRoots(SettingGroup givenRoot, boolean attachToGivenRoot,
            SettingGroup xmlRoot) {
        // If we were given a Settings, use it as the root (ignoring the root
        // Settings from the XML file).
        if (givenRoot == null) {
            xmlRoot.onAttachedToHierarchy();
            return xmlRoot;
        } else {
            return givenRoot;
        }
    }
	
}
