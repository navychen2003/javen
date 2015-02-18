package org.javenstudio.mail;

import java.util.regex.Pattern;

import org.javenstudio.mail.internet.MimeUtility;
import org.javenstudio.mail.transport.Transport;

public abstract class Preference {

	private static Preference sInstance = null;
	
	public static synchronized void setPreference(Preference instance) { 
		if (sInstance != null) 
			throw new RuntimeException("Preference already set");
		if (instance == null) 
			throw new RuntimeException("Preference input null");
		if (instance != sInstance) 
			sInstance = instance;
	}
	
	public static synchronized Preference getPreference() { 
		if (sInstance == null) 
			throw new RuntimeException("Preference not set"); 
		return sInstance;
	}
	
	public Preference() {}
	
	public int getBodySaneMaxLines() { 
		return Constants.FETCH_BODY_SANE_SUGGESTED_SIZE / 76;
	}
	
	public int getBodySaneSize() { 
		return Constants.FETCH_BODY_SANE_SUGGESTED_SIZE;
	}
	
	public abstract Transport.SocketFactory getSecureSocketFactory(boolean insecure); 
	public abstract Transport.SocketFactory getSocketFactory(); 
	
	public MimeUtility.OutputBodyFactory getMimeOutputBodyFactory() { 
		return null;
	}
	
	/**
	 * The first section is global to all IMAP connections, and generates the fixed
     * values in any IMAP ID message
	 */
	public abstract String makeCommonImapId();
	
	/**
     * Returns additional key/value pairs for the IMAP ID string.
     *
     * Vendor function:
     *  Select: GET_IMAP_ID
     *  Params: GET_IMAP_ID_USER (String)
     *          GET_IMAP_ID_HOST (String)
     *          GET_IMAP_ID_CAPABILITIES (String)
     *  Result: GET_IMAP_ID (String)
     *
     * @param userName the server that is being contacted (e.g. "imap.server.com")
     * @param host the server that is being contacted (e.g. "imap.server.com")
     * @param capabilities reported capabilities, if known.  null is OK
     * @return zero or more key/value pairs, quoted and delimited by spaces.  If there is
     * nothing to add, return null.
     */
    public String getImapIdValues(String userName, String host, String capabilities) {
    	return null; 
    }
	
    /**
     * Generate a new "device UID".  This is local to Email app only, to prevent possibility
     * of correlation with any other user activities in any other apps.
     * @return a persistent, unique ID
     */
    public String getDeviceUID() { 
    	return null;
    }
    
    /**
     * Helper function that actually builds the static part of the IMAP ID string.  This is
     * separated from getImapId for testability.  There is no escaping or encoding in IMAP ID so
     * any rogue chars must be filtered here.
     *
     * @param packageName context.getPackageName()
     * @param version Build.VERSION.RELEASE
     * @param codeName Build.VERSION.CODENAME
     * @param model Build.MODEL
     * @param id Build.ID
     * @param vendor Build.MANUFACTURER
     * @param networkOperator TelephonyManager.getNetworkOperatorName()
     * @return the static (never changes) portion of the IMAP ID
     */
    public static String makeImapId(String packageName, String version,
            String codeName, String model, String id, String vendor, String networkOperator) {

        // Before building up IMAP ID string, pre-filter the input strings for "legal" chars
        // This is using a fairly arbitrary char set intended to pass through most reasonable
        // version, model, and vendor strings: a-z A-Z 0-9 - _ + = ; : . , / <space>
        // The most important thing is *not* to pass parens, quotes, or CRLF, which would break
        // the format of the IMAP ID list.
        Pattern p = Pattern.compile("[^a-zA-Z0-9-_\\+=;:\\.,/ ]");
        packageName = p.matcher(packageName).replaceAll("");
        version = p.matcher(version).replaceAll("");
        codeName = p.matcher(codeName).replaceAll("");
        model = p.matcher(model).replaceAll("");
        id = p.matcher(id).replaceAll("");
        vendor = p.matcher(vendor).replaceAll("");
        networkOperator = p.matcher(networkOperator).replaceAll("");

        // "name" "com.android.email"
        StringBuffer sb = new StringBuffer("\"name\" \"");
        sb.append(packageName);
        sb.append("\"");

        // "os" "android"
        sb.append(" \"os\" \"android\"");

        // "os-version" "version; build-id"
        sb.append(" \"os-version\" \"");
        if (version.length() > 0) {
            sb.append(version);
        } else {
            // default to "1.0"
            sb.append("1.0");
        }
        // add the build ID or build #
        if (id.length() > 0) {
            sb.append("; ");
            sb.append(id);
        }
        sb.append("\"");

        // "vendor" "the vendor"
        if (vendor.length() > 0) {
            sb.append(" \"vendor\" \"");
            sb.append(vendor);
            sb.append("\"");
        }

        // "x-android-device-model" the device model (on release builds only)
        if ("REL".equals(codeName)) {
            if (model.length() > 0) {
                sb.append(" \"x-android-device-model\" \"");
                sb.append(model);
                sb.append("\"");
            }
        }

        // "x-android-mobile-net-operator" "name of network operator"
        if (networkOperator.length() > 0) {
            sb.append(" \"x-android-mobile-net-operator\" \"");
            sb.append(networkOperator);
            sb.append("\"");
        }

        return sb.toString();
    }
    
}
