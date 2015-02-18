package org.javenstudio.android.mail;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.telephony.TelephonyManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.javenstudio.mail.OutputBody;
import org.javenstudio.mail.Preference;
import org.javenstudio.mail.internet.MimeUtility;
import org.javenstudio.mail.transport.Transport;
import org.javenstudio.android.mail.controller.BinaryTempFileBody;
import org.javenstudio.cocoka.android.ResourceContext;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.storage.Storage;
import org.javenstudio.common.util.Logger;

public abstract class Preferences extends Preference {
	private static Logger LOG = Logger.getLogger(Preferences.class);
	
	// Preferences file
    public static final String PREFERENCES_FILE = "MyMail.Main";

    // Preferences field names
    //private static final String ACCOUNT_UUIDS = "accountUuids";
    //private static final String DEFAULT_ACCOUNT_UUID = "defaultAccountUuid";
    //private static final String ENABLE_DEBUG_LOGGING = "enableDebugLogging";
    //private static final String ENABLE_SENSITIVE_LOGGING = "enableSensitiveLogging";
    //private static final String ENABLE_EXCHANGE_LOGGING = "enableExchangeLogging";
    //private static final String ENABLE_EXCHANGE_FILE_LOGGING = "enableExchangeFileLogging";
    private static final String DEVICE_UID = "deviceUID";
    //private static final String ONE_TIME_INITIALIZATION_PROGRESS = "oneTimeInitializationProgress";
    private static final String SHOW_UNREAD_COUNT_ALL = "showUnreadCountAll";
    private static final String SHOW_ALL_MAILBOXES_COMBINED = "showAllMailboxesCombined";
    private static final String SHOW_ONLY_UNREAD_COMBINED = "showOnlyUnreadCombined";

    private static Preferences sPreferences;
    private static String sPreferencesClassName = null; 

    private final Context mContext; 
    private final SharedPreferences mSharedPreferences;
    private final Preferences.BuildInfo mBuildInfo; 

    protected Preferences(final Context context, String preferenceFile) {
    	if (preferenceFile == null || preferenceFile.length() == 0) 
    		preferenceFile = PREFERENCES_FILE; 
    	
    	mContext = context; 
        mSharedPreferences = context.getSharedPreferences(preferenceFile, Context.MODE_PRIVATE);
        
        mBuildInfo = new Preferences.BuildInfo() { 
				public String getPackageName() { return context.getPackageName(); }
		    	public String getVersionRelease() { return Build.VERSION.RELEASE; }
		    	public String getVersionCodename() { return Build.VERSION.CODENAME; }
		    	public String getBuildModel() { return Build.MODEL; }
		    	public String getBuildID() { return Build.ID; }
		    	public String getBuildManufacturer() { return Build.MANUFACTURER; }
			};
    }
	
    /**
     * TODO need to think about what happens if this gets GCed along with the
     * Activity that initialized it. Do we lose ability to read Preferences in
     * further Activities? Maybe this should be stored in the Application
     * context.
     */
    public static synchronized Preferences getPreferences() {
        if (sPreferences == null) {
        	String className = sPreferencesClassName; 
        	sPreferences = instantiatePreferences(className); 
        	Preference.setPreference(sPreferences);
        }
        return sPreferences;
    }
    
    public static synchronized void setPreferencesClassName(String className) { 
    	if (className != null && className.length() > 0 && sPreferencesClassName == null) 
    		sPreferencesClassName = className; 
    }
    
    /**
     * Static named constructor.  It should be overrode by extending class.
     * Because this method will be called through reflection, it can not be protected.
     */
    public static Preferences newInstance() {
        throw new RuntimeException("Preferences.newInstance: Unknown class");
    }

    private static Preferences instantiatePreferences(String className) {
        Object o = null;
        try {
            Class<?> c = Class.forName(className);
            // and invoke "newInstance" class method and instantiate sender object.
            java.lang.reflect.Method m = c.getMethod("newInstance");
            o = m.invoke(null);
        } catch (Exception e) {
            LOG.error(String.format(
            	"exception invoking %s.newInstance() method", className), e);
            throw new RuntimeException("can not instantiate Preferences object for " + className);
        }
        if (!(o instanceof Preferences)) {
            throw new RuntimeException(className + " create incompatible object");
        }
        return (Preferences) o;
    }
    
    public final Context getContext() { 
    	return mContext; 
    }
    
    public final ResourceContext getResourceContext() { 
    	return ResourceHelper.getResourceContext(); 
    }
    
    public static interface BuildInfo { 
    	public String getPackageName(); 
    	public String getVersionRelease(); 
    	public String getVersionCodename(); 
    	public String getBuildModel(); 
    	public String getBuildID(); 
    	public String getBuildManufacturer(); 
    }
    
    public BuildInfo getBuildInfo() { 
    	return mBuildInfo; 
    }
    
    public InputStream openInputStream(String uri) throws IOException, FileNotFoundException { 
		// try to open the file
        Uri fileUri = Uri.parse(uri);
        return getContext().getContentResolver().openInputStream(fileUri);
    }
    
    public String getNetworkOperatorName() { 
    	TelephonyManager tm =
                (TelephonyManager)getContext().getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getNetworkOperatorName(); 
    }
    
    public abstract String getTemporaryDirectory(); 
    public abstract String getContentProviderClassName(); 
    public abstract String getControllerClassName(); 
    
	public abstract String getStoragePath(String name);
	public abstract Storage getStorage(String name) throws IOException;
    
    public int getTemporaryIOBufferSize() { 
    	return 1024 * 4; // default buffer size
    }
    
    public long getMessagingConnectTimeout() { 
    	return 3 * 60 * 1000;
    }
    
    public long getMessagingCommandTimeout() { 
    	return 10 * 60 * 1000;
    }
    
    @Override
    public MimeUtility.OutputBodyFactory getMimeOutputBodyFactory() { 
		return new MimeUtility.OutputBodyFactory() {
				@Override
				public OutputBody createDefaultOutputBody() {
					return new BinaryTempFileBody();
				}
			};
	}
    
    /**
     * Generate a new "device UID".  This is local to Email app only, to prevent possibility
     * of correlation with any other user activities in any other apps.
     * @return a persistent, unique ID
     */
    @Override
    public synchronized String getDeviceUID() {
         String result = mSharedPreferences.getString(DEVICE_UID, null);
         if (result == null) {
             result = UUID.randomUUID().toString();
             mSharedPreferences.edit().putString(DEVICE_UID, result).commit();
         }
         return result;
    }
    
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
    @Override
    public String getImapIdValues(String userName, String host, String capabilities) {
    	return null; 
    }
    
    @Override
    public String makeCommonImapId() { 
    	//TelephonyManager tm =
        //        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String networkOperator = Preferences.getPreferences().getNetworkOperatorName();
        if (networkOperator == null) networkOperator = "";

        Preferences.BuildInfo info = Preferences.getPreferences().getBuildInfo(); 
        String imapId = makeImapId(info.getPackageName(), info.getVersionRelease(), 
        		info.getVersionCodename(), info.getBuildModel(), info.getBuildID(), info.getBuildManufacturer(), 
        		networkOperator); 
        
        //sImapId = makeCommonImapId(context.getPackageName(), Build.VERSION.RELEASE,
        //        Build.VERSION.CODENAME, Build.MODEL, Build.ID, Build.MANUFACTURER,
        //        networkOperator);
        
        return imapId;
	}
    
    @Override
    public Transport.SocketFactory getSecureSocketFactory(boolean insecure) { 
    	return SocketHelper.getSSLSocketFactory(insecure);
    }
    
    @Override
	public Transport.SocketFactory getSocketFactory() { 
    	return SocketHelper.getSocketFactory();
    }
    
    public boolean getShowUnreadCountAll() {
        return mSharedPreferences.getBoolean(SHOW_UNREAD_COUNT_ALL, false);
    }
    
    public boolean getShowAllMailboxesCombined() {
        return mSharedPreferences.getBoolean(SHOW_ALL_MAILBOXES_COMBINED, false);
    }
    
    public boolean getShowOnlyUnreadCombined() {
        return mSharedPreferences.getBoolean(SHOW_ONLY_UNREAD_COMBINED, false);
    }
    
}
