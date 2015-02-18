package org.javenstudio.lightning.util;

import java.net.URI;

import org.javenstudio.lightning.http.HttpHelper;
import org.javenstudio.lightning.http.IHttpResult;

public class SimpleHttp extends SimpleShell {

	public static void main(String[] args) throws Exception {
		//Configuration conf = loadConf().getConf();
		String location = args != null && args.length > 0 ? args[0] : 
			"http://s.cn.bing.net/az/hprichbg/rb/QingdaoNightScenery_ZH-CN12266199615_1366x768.jpg";
		IHttpResult result = HttpHelper.fetchURL(URI.create(location));
		System.out.println(">>>> " + result);
	}
	
}
