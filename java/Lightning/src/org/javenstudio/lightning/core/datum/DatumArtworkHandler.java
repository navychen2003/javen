package org.javenstudio.lightning.core.datum;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.ISectionRoot;
import org.javenstudio.falcon.datum.data.FileStream;
import org.javenstudio.falcon.datum.data.SqLibrary;
import org.javenstudio.falcon.datum.data.archive.SqArchiveRoot;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.http.HttpHelper;
import org.javenstudio.lightning.http.IHttpResult;
import org.javenstudio.lightning.http.SimpleFileStream;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class DatumArtworkHandler extends DatumHandlerBase {

	public static RequestHandler createHandler(DatumCore core) { 
		return new DatumArtworkHandler(core);
	}
	
	public DatumArtworkHandler(DatumCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		String action = trim(req.getParam("action"));
		
		if (action == null || action.length() == 0)
			action = "list";
		
		rsp.add("action", action);
		
		if (action.equalsIgnoreCase("download")) {
			handleDownload(req, rsp, user);
		} else if (action.equalsIgnoreCase("list")) { 
			handleList(req, rsp, user);
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	private void handleDownload(Request req, Response rsp, 
			IMember me) throws ErrorException {
		String username = trim(req.getParam("username"));
		String rootname = trim(req.getParam("rootname"));
		String imgurl = trim(req.getParam("url"));
		
		IUser usr = me;
		if (username != null && username.length() > 0) { 
			IUser u = UserHelper.getLocalUserByName(username);
			if (u != null) usr = u;
		}
		
		ISectionRoot[] roots = getArtworkRoot(usr, rootname);
		if (roots == null || roots.length == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"There is no library found, please add one first.");
		}
		
		ISectionRoot root = roots[0];
		//ILibrary library = root.getLibrary();
		
		if (imgurl == null || imgurl.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Artwork image URL is empty");
		}
		
		final URI url;
		try {
			url = new URI(imgurl);
		} catch (Throwable e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
		}
		
		IHttpResult result = HttpHelper.fetchURL(url);
		if (result == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Http result is null");
		}
		
		SimpleFileStream fstream = SimpleFileStream.create(result, 
				getCore().getMetadataLoader());
		
		if (fstream == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"File stream is null");
		}
		
		int count = DatumUploadHandler.actionSave(me, usr, root, 
				new FileStream[]{fstream});
		
		if (count <= 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No artwork image downloaded");
		}
	}
	
	private void handleList(Request req, Response rsp, 
			IMember user) throws ErrorException {
		String username = trim(req.getParam("username"));
		String rootname = trim(req.getParam("rootname"));
		String path = trim(req.getParam("path"));
		String prefix = trim(req.getParam("prefix"));
		String suffix = trim(req.getParam("suffix"));
		
		IUser usr = user;
		if (username != null && username.length() > 0) { 
			IUser u = UserHelper.getLocalUserByName(username);
			if (u != null) usr = u;
		}
		
		ISectionRoot[] roots = getArtworkRoot(usr, rootname);
		if (roots == null || roots.length == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"There is no library found, please add one first.");
		}
		
		ISectionRoot root = roots[0];
		ILibrary library = root.getLibrary();
		ISection[] sections = findArtwork(roots, path, prefix, suffix);
		
		NamedList<Object> items = new NamedMap<Object>();
		if (sections != null) { 
			for (int i=0; i < sections.length; i++) { 
				ISection section = sections[i];
				NamedList<Object> info = getSectionInfo(section);
				if (section != null && info != null) 
					items.add(section.getContentId(), info);
			}
		}
		
		rsp.add("section_id", root.getContentId());
		rsp.add("section_name", root.getName());
		rsp.add("section_type", root.getContentType());
		rsp.add("section_perms", getPermissions(root));
		rsp.add("section_ops", getOperations(root));
		
		rsp.add("library_id", library.getContentId());
		rsp.add("library_name", library.getName());
		rsp.add("library_type", library.getContentType());
		
		rsp.add("artworks", items);
	}
	
	private ISectionRoot[] getArtworkRoot(IUser user, 
			String rootName) throws ErrorException { 
		if (user == null) throw new NullPointerException();
		if (rootName == null || rootName.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Artwork root name is empty");
		}
		
		DataManager manager = getManager(user);
		if (manager != null) { 
			ArrayList<ISectionRoot> list = new ArrayList<ISectionRoot>();
			
			ILibrary[] libraries = manager.getLibraries();
			ILibrary artworklib = null;
			
			for (int i=0; libraries != null && i < libraries.length; i++) { 
				ILibrary library = libraries[i];
				if (library == null) continue;
				if (artworklib == null || library.isDefault()) 
					artworklib = library;
				
				for (int j=0; j < library.getSectionCount(); j++) { 
					ISectionRoot root = library.getSectionAt(j);
					if (root == null) continue;
					
					if (rootName.equalsIgnoreCase(root.getName())) 
						list.add(root);
				}
			}
			
			if (list.size() == 0 && artworklib != null) {
				if (artworklib instanceof SqLibrary) { 
					SqLibrary library = (SqLibrary)artworklib;
					SqArchiveRoot root = SqArchiveRoot.create(library, rootName);
					library.addRoot(root);
					
					manager.saveLibraryList();
					list.add(root);
				}
			}
			
			return list.toArray(new ISectionRoot[list.size()]);
		}
		
		return null;
	}
	
	private ISection[] findArtwork(final ISectionRoot[] roots, final String path, 
			final String prefix, final String suffix) throws ErrorException { 
		if (roots == null || roots.length == 0) return null;
		
		final ArrayList<ISection> list = new ArrayList<ISection>();
		
		final String prefixStr = prefix != null ? prefix.toLowerCase() : null;
		final String suffixStr = suffix != null ? suffix.toLowerCase() : null;
		
		for (ISectionRoot root : roots) {
			if (root == null) continue;
			
			root.listSection(new ISection.Collector() {
					@Override
					public void addSection(ISection section) throws ErrorException {
						if (section != null && !section.isFolder()) {
							String name = section.getName();
							if (name != null) name = name.toLowerCase();
							
							boolean prefixOk = true;
							boolean suffixOk = true;
							
							if (prefixStr != null && prefixStr.length() > 0) { 
								if (name.startsWith(prefixStr) == false)
									prefixOk = false;
							}
							
							if (suffixStr != null && suffixStr.length() > 0) { 
								if (name.endsWith(suffixStr) == false)
									suffixOk = false;
							}
							
							if (prefixOk && suffixOk)
								list.add(section);
						}
					}
				});
		}
		
		ISection[] result = list.toArray(new ISection[list.size()]);
		
		Arrays.sort(result, new Comparator<ISection>() {
				@Override
				public int compare(ISection o1, ISection o2) {
					long date1 = o1.getModifiedTime();
					long date2 = o2.getModifiedTime();
					return date1 > date2 ? -1 : (date1 < date2 ? 1 : 0);
				}
				
			});
		
		return result;
	}
	
}
