package org.javenstudio.cocoka.net.http;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import org.javenstudio.common.util.Logger;

public class HttpHelper {
	private static Logger LOG = Logger.getLogger(HttpHelper.class);

	public static String fetchHtml(String location) {
		HttpClient client = SimpleHttpClient.newInstance("Android"); 
		
		HttpGet request = new HttpGet(location);
		
		try {
			HttpResponse response = client.execute(request); 
			
			int statusCode = response.getStatusLine().getStatusCode(); 
			if (statusCode == HttpStatus.SC_OK) {
				return EntityUtils.toString(response.getEntity()); 
				
			} else
				LOG.warn("fetchHtml error: "+statusCode);
			
		} catch (IOException e) {
			LOG.error("fetchHtml error", e);
		}
		
		return null; 
	}
	
	public static byte[] fetchBytes(String location) {
		HttpClient client = SimpleHttpClient.newInstance("Android"); 
		
		HttpGet request = new HttpGet(location);
		
		try {
			HttpResponse response = client.execute(request); 
			
			int statusCode = response.getStatusLine().getStatusCode(); 
			if (statusCode == HttpStatus.SC_OK) {
				return EntityUtils.toByteArray(response.getEntity()); 
				
			} else
				LOG.warn("fetchBytes error: "+statusCode);
			
		} catch (IOException e) {
			LOG.error("fetchBytes error", e);
		}
		
		return null; 
	}
	
}
