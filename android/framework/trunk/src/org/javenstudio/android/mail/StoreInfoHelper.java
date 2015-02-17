package org.javenstudio.android.mail;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;
import android.content.res.XmlResourceParser;

import org.javenstudio.mail.store.StoreInfo;

public class StoreInfoHelper {

	public static void registerStores(int resourceId) {
        try {
            XmlResourceParser xml = Preferences.getPreferences().getResourceContext().getXml(resourceId);
            int xmlEventType;
            // walk through stores.xml file.
            while ((xmlEventType = xml.next()) != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG && "store".equals(xml.getName())) {
                    String xmlScheme = xml.getAttributeValue(null, "scheme");
                    String className = xml.getAttributeValue(null, "class");
                    if (xmlScheme != null && xmlScheme.length() > 0) {
                        StoreInfo result = new StoreInfo(xmlScheme, className);
                        result.setPushSupported(xml.getAttributeBooleanValue(
                                null, "push", false));
                        result.setVisibleLimitDefault(xml.getAttributeIntValue(
                                null, "visibleLimitDefault", Constants.VISIBLE_LIMIT_DEFAULT));
                        result.setVisibleLimitIncrement(xml.getAttributeIntValue(
                                null, "visibleLimitIncrement", Constants.VISIBLE_LIMIT_INCREMENT));
                        result.setAccountInstanceLimit(xml.getAttributeIntValue(
                                null, "accountInstanceLimit", -1));
                        StoreInfo.registerStore(result);
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
