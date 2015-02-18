package org.javenstudio.lightning.http.util;

import org.apache.http.impl.client.DefaultHttpClient;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.Params;

/**
 * The default http client configurer. If the behaviour needs to be customized a
 * new HttpCilentConfigurer can be set by calling
 * {@link HttpUtil#setConfigurer(HttpClientConfigurer)}
 */
@SuppressWarnings("deprecation")
public class HttpConfigurer {
  
  protected void configure(DefaultHttpClient httpClient, Params config) throws ErrorException {
    
    if (config.get(HttpUtil.PROP_MAX_CONNECTIONS) != null) {
      HttpUtil.setMaxConnections(httpClient,
          config.getInt(HttpUtil.PROP_MAX_CONNECTIONS));
    }
    
    if (config.get(HttpUtil.PROP_MAX_CONNECTIONS_PER_HOST) != null) {
      HttpUtil.setMaxConnectionsPerHost(httpClient,
          config.getInt(HttpUtil.PROP_MAX_CONNECTIONS_PER_HOST));
    }
    
    if (config.get(HttpUtil.PROP_CONNECTION_TIMEOUT) != null) {
      HttpUtil.setConnectionTimeout(httpClient,
          config.getInt(HttpUtil.PROP_CONNECTION_TIMEOUT));
    }
    
    if (config.get(HttpUtil.PROP_SO_TIMEOUT) != null) {
      HttpUtil.setSoTimeout(httpClient,
          config.getInt(HttpUtil.PROP_SO_TIMEOUT));
    }
    
    if (config.get(HttpUtil.PROP_USE_RETRY) != null) {
      HttpUtil.setUseRetry(httpClient,
          config.getBool(HttpUtil.PROP_USE_RETRY));
    }
    
    if (config.get(HttpUtil.PROP_FOLLOW_REDIRECTS) != null) {
      HttpUtil.setFollowRedirects(httpClient,
          config.getBool(HttpUtil.PROP_FOLLOW_REDIRECTS));
    }
    
    final String basicAuthUser = config
        .get(HttpUtil.PROP_BASIC_AUTH_USER);
    final String basicAuthPass = config
        .get(HttpUtil.PROP_BASIC_AUTH_PASS);
    HttpUtil.setBasicAuth(httpClient, basicAuthUser, basicAuthPass);
    
    if (config.get(HttpUtil.PROP_ALLOW_COMPRESSION) != null) {
      HttpUtil.setAllowCompression(httpClient,
          config.getBool(HttpUtil.PROP_ALLOW_COMPRESSION));
    }
  }
}
