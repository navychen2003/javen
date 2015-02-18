package org.javenstudio.android.mail;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;
import android.content.res.XmlResourceParser;

import org.javenstudio.mail.sender.SenderInfo;

public class SenderInfoHelper {

	public static void registerSenders(int resourceId) {
        try {
            XmlResourceParser xml = Preferences.getPreferences().getResourceContext().getXml(resourceId);
            int xmlEventType;
            // walk through senders.xml file.
            while ((xmlEventType = xml.next()) != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG && "sender".equals(xml.getName())) {
                    String scheme = xml.getAttributeValue(null, "scheme");
                    if (scheme != null && scheme.length() > 0) {
                    	String className = xml.getAttributeValue(null, "class");
                    	
                    	SenderInfo result = new SenderInfo(scheme, className); 
                    	SenderInfo.registerSender(result);
                    }
                }
            }
        } catch (XmlPullParserException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }
    }
	
}
