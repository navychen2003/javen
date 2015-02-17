package org.javenstudio.android.mail.controller;

import java.io.Serializable;
import java.net.URI;

import android.content.res.XmlResourceParser;

import org.javenstudio.android.mail.Preferences;
import org.javenstudio.common.util.Logger;

public class MailProvider {
	private static Logger LOG = Logger.getLogger(MailProvider.class);

	private final int mResourceId; 
	
	public MailProvider(int resourceId) { 
		mResourceId = resourceId; 
	}
	
	/**
     * Search the list of known Email providers looking for one that matches the user's email
     * domain.  We check for vendor supplied values first, then we look in providers_product.xml,
     * and finally by the entries in platform providers.xml.  This provides a nominal override
     * capability.
     *
     * A match is defined as any provider entry for which the "domain" attribute matches.
     *
     * @param domain The domain portion of the user's email address
     * @return suitable Provider definition, or null if no match found
     */
    public Provider findProviderForDomain(String domain) {
    	return findProviderForDomain(domain, mResourceId); 
    }
	
    /**
     * Search a single resource containing known Email provider definitions.
     *
     * @param domain The domain portion of the user's email address
     * @param resourceId Id of the provider resource to scan
     * @return suitable Provider definition, or null if no match found
     */
    public static Provider findProviderForDomain(String domain, int resourceId) {
        try {
            XmlResourceParser xml = Preferences.getPreferences().getResourceContext().getXml(resourceId);
            int xmlEventType;
            Provider provider = null;
            while ((xmlEventType = xml.next()) != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG
                        && "provider".equals(xml.getName())
                        && domain.equalsIgnoreCase(getXmlAttribute(xml, "domain"))) {
                    provider = new Provider();
                    provider.id = getXmlAttribute(xml, "id");
                    provider.label = getXmlAttribute(xml, "label");
                    provider.domain = getXmlAttribute(xml, "domain");
                    provider.note = getXmlAttribute(xml, "note");
                }
                else if (xmlEventType == XmlResourceParser.START_TAG
                        && "incoming".equals(xml.getName())
                        && provider != null) {
                    provider.incomingUriTemplate = new URI(getXmlAttribute(xml, "uri"));
                    provider.incomingUsernameTemplate = getXmlAttribute(xml, "username");
                }
                else if (xmlEventType == XmlResourceParser.START_TAG
                        && "outgoing".equals(xml.getName())
                        && provider != null) {
                    provider.outgoingUriTemplate = new URI(getXmlAttribute(xml, "uri"));
                    provider.outgoingUsernameTemplate = getXmlAttribute(xml, "username");
                }
                else if (xmlEventType == XmlResourceParser.END_TAG
                        && "provider".equals(xml.getName())
                        && provider != null) {
                    return provider;
                }
            }
        }
        catch (Exception e) {
        	LOG.error("Error while trying to load provider settings.", e);
        }
        return null;
    }

    /**
     * Attempts to get the given attribute as a String resource first, and if it fails
     * returns the attribute as a simple String value.
     * @param xml
     * @param name
     * @return the requested resource
     */
    private static String getXmlAttribute(XmlResourceParser xml, String name) {
        int resId = xml.getAttributeResourceValue(null, name, 0);
        if (resId == 0) {
            return xml.getAttributeValue(null, name);
        }
        else {
            return Preferences.getPreferences().getResourceContext().getString(resId);
        }
    }

    public static class Provider implements Serializable {
        private static final long serialVersionUID = 8511656164616538989L;

        public String id;
        public String label;
        public String domain;
        public URI incomingUriTemplate;
        public String incomingUsernameTemplate;
        public URI outgoingUriTemplate;
        public String outgoingUsernameTemplate;
        public String note;
    }
	
}
