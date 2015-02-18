package org.javenstudio.raptor.paxos.server.auth;

import java.util.Enumeration;
import java.util.HashMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.paxos.server.PaxosServer;

public class ProviderRegistry {
    private static final Logger LOG = Logger.getLogger(ProviderRegistry.class);

    private static boolean initialized = false;
    private static HashMap<String, AuthenticationProvider> authenticationProviders =
        new HashMap<String, AuthenticationProvider>();

    public static void initialize() {
        synchronized (ProviderRegistry.class) {
            if (initialized)
                return;
            IPAuthenticationProvider ipp = new IPAuthenticationProvider();
            DigestAuthenticationProvider digp = new DigestAuthenticationProvider();
            authenticationProviders.put(ipp.getScheme(), ipp);
            authenticationProviders.put(digp.getScheme(), digp);
            Enumeration<Object> en = System.getProperties().keys();
            while (en.hasMoreElements()) {
                String k = (String) en.nextElement();
                if (k.startsWith("paxos.authProvider.")) {
                    String className = System.getProperty(k);
                    try {
                        Class<?> c = PaxosServer.class.getClassLoader()
                                .loadClass(className);
                        AuthenticationProvider ap = (AuthenticationProvider) c
                                .newInstance();
                        authenticationProviders.put(ap.getScheme(), ap);
                    } catch (Exception e) {
                        LOG.warn("Problems loading " + className,e);
                    }
                }
            }
            initialized = true;
        }
    }

    public static AuthenticationProvider getProvider(String scheme) {
        if(!initialized)
            initialize();
        return authenticationProviders.get(scheme);
    }

    public static String listProviders() {
        StringBuilder sb = new StringBuilder();
        for(String s: authenticationProviders.keySet()) {
        sb.append(s + " ");
}
        return sb.toString();
    }
}

