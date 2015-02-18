package org.javenstudio.lightning.core.datum;

import org.javenstudio.common.util.MimeTypes;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class DatumValueHandler extends DatumHandlerBase {

	public static RequestHandler createHandler(DatumCore core) { 
		return new DatumValueHandler(core);
	}
	
	public DatumValueHandler(DatumCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		@SuppressWarnings("unused")
		String userKey = UserHelper.checkUserKey(req, IUserClient.Op.ACCESS);
		String name = trim(req.getParam("name"));
		if (name == null) name = "";
		
		NamedMap<Object> values = null;
		
		if (name.equals("file.extension")) { 
			String mimeType = trim(req.getParam("mimetype"));
			values = getExtensionList(mimeType);
		}
		
		if (values == null)
			values = new NamedMap<Object>();
		
		rsp.add("name", name);
		rsp.add("values", values);
	}
	
	private NamedMap<Object> getExtensionList(String mimeType) {
		NamedMap<Object> info = new NamedMap<Object>();
		
		if (mimeType != null && mimeType.length() > 0) {
			MimeTypes.MimeTypeInfo typeInfo = MimeTypes.getMimeTypeInfo(mimeType);
			addExtension(info, typeInfo);
			
		} else { 
			MimeTypes.MimeTypeInfo[] typeInfos = MimeTypes.getMimeTypeInfos();
			if (typeInfos != null) { 
				for (MimeTypes.MimeTypeInfo typeInfo : typeInfos) { 
					addExtension(info, typeInfo);
				}
			}
		}
		
		return info;
	}
	
	private void addExtension(NamedMap<Object> info, MimeTypes.MimeTypeInfo typeInfo) { 
		if (info == null || typeInfo == null) return;
		
		MimeTypes.FileTypeInfo[] fileInfos = typeInfo.getFileTypes();
		if (fileInfos == null) return;
		
		for (MimeTypes.FileTypeInfo fileInfo : fileInfos) { 
			if (fileInfo == null) continue;
			String value = fileInfo.getExtensionName();
			String title = fileInfo.getContentType();
			if (value != null && title != null) 
				info.add(value, title);
		}
	}
	
}
