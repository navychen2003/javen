package org.javenstudio.lightning.core.datum;

import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.datum.StoreInfo;
import org.javenstudio.falcon.datum.data.SqHelper;
import org.javenstudio.falcon.datum.data.SqLibrary;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class DatumLibraryHandler extends DatumHandlerBase {

	public static RequestHandler createHandler(DatumCore core) { 
		return new DatumLibraryHandler(core);
	}
	
	public DatumLibraryHandler(DatumCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		
		String action = trim(req.getParam("action"));
		String id = trim(req.getParam("id"));
		String type = trim(req.getParam("type"));
		String name = trim(req.getParam("name"));
		String storeUri = trim(req.getParam("store"));
		String[] paths = req.getParams("path");
		int maxEntries = parseInt(req.getParam("max_entries"));
		
		NamedList<Object> info = null;
		
		if (action == null || action.length() == 0) 
			action = "info";
		
		if (type == null || type.length() == 0) 
			type = "file";
		
		if (action.equalsIgnoreCase("add")) { 
			ILibrary library = register(user, name, type, storeUri, paths, maxEntries);
			info = getLibraryInfo(library, 0, true);
			
		} else if (action.equalsIgnoreCase("edit")) { 
			ILibrary library = modify(user, id, name, type, paths);
			info = getLibraryInfo(library, 0, true);
			
		} else if (action.equalsIgnoreCase("delete")) { 
			ILibrary library = unregister(user, id);
			info = getLibraryInfo(library, 0, true);
			
		} else { 
			ILibrary library = user.getDataManager().getLibrary(id);
			info = getLibraryInfo(library, 0, true);
		}
		
		if (info == null)
			info = new NamedMap<Object>();
		
		rsp.add("action", action);
		rsp.add("type", type);
		rsp.add("library", info);
	}

	private ILibrary unregister(IMember user, String id) throws ErrorException { 
		if (user == null || id == null) return null;
		
		DataManager manager = getManager(user);
		ILibrary[] libraries = manager.getLibraries();
		ILibrary found = null;
		
		for (int i=0; libraries != null && i < libraries.length; i++) { 
			ILibrary library = libraries[i];
			if (library == null) continue;
			
			if (id.equals(library.getContentId())) { 
				if (found != null) { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Library: " + library.getName() + " duplicated with id: " + id);
				}
				found = library;
			}
		}
		
		if (found == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Library: " + id + " not found");
		}
		
		ILibrary removed = manager.removeLibrary(found.getContentKey());
		if (removed == null || removed != found) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Library: " + id + " removed is wrong");
		}
		
		MessageHelper.notifySys(user.getUserKey(), 
				Strings.get(user.getPreference().getLanguage(), "Remove library: ") + 
				removed.getName());
		
		return removed;
	}
	
	private ILibrary register(IMember user, String name, 
			String type, String storeUri, String[] paths, int maxEntries) 
			throws ErrorException { 
		if (user == null || name == null || type == null)
			return null;
		
		DataManager manager = getManager(user);
		ILibrary[] libraries = manager.getLibraries();
		
		for (int i=0; libraries != null && i < libraries.length; i++) { 
			ILibrary library = libraries[i];
			if (library != null && name.equals(library.getName())) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Library: " + library.getName() + " already existed");
			}
		}
		
		if (storeUri == null || storeUri.length() == 0) { 
			StoreInfo[] stores = getCore().getStoreInfos();
			for (int i=0; stores != null && i < stores.length; i++) { 
				StoreInfo storeInfo = stores[i];
				if (storeInfo != null) { 
					storeUri = storeInfo.getScheme();
					break;
				}
			}
		}
		
		ILibrary library = null;
		
		if ("photo".equals(type)) {
			library = SqHelper.registerPhoto(user, manager, name, storeUri, 
					paths, maxEntries);
			
		} else if ("music".equals(type)) {
			library = SqHelper.registerMusic(user, manager, name, storeUri, 
					paths, maxEntries);
			
		} else if ("video".equals(type)) {
			library = SqHelper.registerVideo(user, manager, name, storeUri, 
					paths, maxEntries);
			
		} else if ("file".equals(type)) {
			library = SqHelper.registerFile(user, manager, name, storeUri, 
					paths, maxEntries);
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Library type: " + type + " not supported");
		}
		
		if (library != null) {
			MessageHelper.notifySys(user.getUserKey(), 
					Strings.get(user.getPreference().getLanguage(), "Add library: ") + 
					library.getName());
		}
		
		return library;
	}
	
	private ILibrary modify(IMember user, String id, String name, 
			String type, String[] paths) throws ErrorException { 
		if (user == null || id == null || name == null)
			return null;
		
		DataManager manager = getManager(user);
		ILibrary[] libraries = manager.getLibraries();
		ILibrary found = null;
		
		for (int i=0; libraries != null && i < libraries.length; i++) { 
			ILibrary library = libraries[i];
			if (library == null) continue;
			
			if (!id.equals(library.getContentId()) && name.equals(library.getName())) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Library: " + library.getName() + " already existed");
			}
			
			if (id.equals(library.getContentId())) { 
				if (found != null) { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Library: " + library.getName() + " duplicated with id: " + id);
				}
				found = library;
			}
		}
		
		if (found == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Library: " + id + " not found");
		}
		
		if (found instanceof SqLibrary) {
			SqHelper.modifyLibrary(user, (SqLibrary)found, name, type, paths);
			
			if (name != null && !name.equals(found.getName())) {
				MessageHelper.notifySys(user.getUserKey(), Strings.format(
						Strings.get(user.getPreference().getLanguage(), "Modify library: \"%1$s\" to \"%2$s\""), 
						found.getName(), name));
			}
			
			return found;
		}
		
		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
				"Library: " + found + " cannot modified");
	}
	
}
