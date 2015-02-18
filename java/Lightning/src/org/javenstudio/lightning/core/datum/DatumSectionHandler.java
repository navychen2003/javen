package org.javenstudio.lightning.core.datum;

import java.util.ArrayList;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.ISectionGroup;
import org.javenstudio.falcon.datum.ISectionQuery;
import org.javenstudio.falcon.datum.ISectionRoot;
import org.javenstudio.falcon.datum.ISectionSet;
import org.javenstudio.falcon.datum.ISectionSort;
import org.javenstudio.falcon.datum.SectionQuery;
import org.javenstudio.falcon.datum.index.IndexSearcher;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class DatumSectionHandler extends DatumHandlerBase {
	
	public static RequestHandler createHandler(DatumCore core) { 
		return new DatumSectionHandler(core);
	}
	
	public DatumSectionHandler(DatumCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(final Request req, final Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		
		String accesskey = trim(req.getParam("accesskey"));
		String key = trim(req.getParam("id"));
		String querystr = trim(req.getParam("q"));
		String sfrom = trim(req.getParam("from"));
		String scount = trim(req.getParam("count"));
		String sbyfolder = trim(req.getParam("byfolder"));
		String ssubfilecount = trim(req.getParam("subfilecount"));
		
		final boolean byfolder;
		if (sbyfolder != null && sbyfolder.equalsIgnoreCase("false"))
			byfolder = false;
		else
			byfolder = true;
		
		boolean isfolder = true;
		int totalcount = 0; 
		int subcount = 0;
		long sublength = 0;
		int sectioncount = parseInt(scount);
		int sectionfrom = parseInt(sfrom);
		int subfilecount = parseInt(ssubfilecount);
		
		if (sectionfrom < 0) 
			sectionfrom = 0;
		
		if (sectioncount <= 0)
			sectioncount = 20;
		
		NamedList<Object> items = new NamedMap<Object>();
		ArrayList<Object> groups = new ArrayList<Object>();
		ArrayList<Object> sorts = new ArrayList<Object>();
		
		ISectionSet sections = null;
		
		String id = "";
		String name = "";
		String hostname = "";
		String contentType = "";
		String parentId = "";
		String parentName = "";
		String parentType = "";
		String rootId = "";
		String rootName = "";
		String rootType = "";
		String libraryId = "";
		String libraryName = "";
		String libraryType = "";
		String permissions = "";
		String operations = "";
		String extension = "";
		String poster = "";
		String background = "";
		String userId = "";
		String userName = "";
		String userType = "";
		String userTitle = "";
		String owner = "";
		
		final String query = querystr != null ? querystr : "";
		
		final String filtertype = trim(req.getParam("filtertype"));
		final ISectionQuery.Filter finalFilter;
		
		if (filtertype != null && filtertype.length() > 0) {
			ISectionQuery.Filter filter = new ISectionQuery.Filter() {
					@Override
					public boolean acceptSection(ISection section) {
						if (section != null) {
							if (section.isFolder()) return true;
							String type = section.getContentType();
							if (type != null && type.startsWith(filtertype))
								return true;
						}
						return false;
					}
				};
			finalFilter = filter;
		} else
			finalFilter = null;
		
		SectionQuery squery = new SectionQuery(sectionfrom, sectioncount) {
				@Override
				public String getQuery() { 
					return query;
				}
				@Override
				public String getParam(String name) {
					try {
						return trim(req.getParam(name));
					} catch (Throwable e) { 
						return null;
					}
				}
				@Override
				public ISectionQuery.Filter getFilter() {
					return finalFilter;
				}
				@Override
				public boolean isByFolder() { 
					return byfolder;
				}
			};
		
		if (key != null && !key.equalsIgnoreCase("all")) {
			if (key.equalsIgnoreCase("me@")) key = user.getUserName() + '@';
			IData data = getData(user, key, IData.Access.LIST, accesskey);
			
			if (data != null && data instanceof ILibrary) {
				ILibrary library = (ILibrary)data;
				IUser usr = library.getManager().getUser();
				
				isfolder = true;
				id = library.getContentId();
				name = library.getName();
				contentType = library.getContentType();
				permissions = getPermissions(data);
				operations = getOperations(data);
				
				userId = usr.getUserKey();
				userName = usr.getUserName();
				userType = usr instanceof IGroup ? IUser.GROUP : IUser.USER;
				userTitle = usr.getPreference().getNickName();
				owner = library.getOwner();
				libraryId = library.getContentId();
				libraryName = library.getName();
				libraryType = library.getContentType();
				hostname = library.getHostName();
				extension = library.getExtension();
				poster = getPoster(library);
				background = getBackground(library);
				
				subcount = library.getTotalFileCount();
				sublength = library.getTotalFileLength();
				sections = library.getSections(squery);
				
			} else if (data != null && data instanceof ISection) { 
				ISection section = (ISection)data;
				IUser usr = section.getManager().getUser();
				
				ILibrary library = section.getLibrary();
				ISectionRoot root = section.getRoot();
				String parent = section.getParentId();
				
				isfolder = section.isFolder();
				id = section.getContentId();
				name = section.getName();
				contentType = section.getContentType();
				permissions = getPermissions(data);
				operations = getOperations(data);
				
				userId = usr.getUserKey();
				userName = usr.getUserName();
				userType = usr instanceof IGroup ? IUser.GROUP : IUser.USER;
				userTitle = usr.getPreference().getNickName();
				owner = section.getOwner();
				libraryId = library.getContentId();
				libraryName = library.getName();
				libraryType = library.getContentType();
				hostname = library.getHostName();
				extension = section.getExtension();
				poster = getPoster(section);
				background = getBackground(section);
				
				parentId = libraryId;
				parentName = libraryName;
				parentType = libraryType;
				
				if (parent != null && parent.length() > 0) {
					IData parentData = getData(user, parent, IData.Access.INFO, accesskey);
					if (parentData != null && parentData instanceof ISection) {
						ISection parentSec = (ISection)parentData;
						parentId = parentSec.getContentId();
						parentType = parentSec.getContentType();
						parentName = parentSec.getName();
					}
				}
				
				if (root != null) {
					rootId = root.getContentId();
					rootType = section.getContentType();
					rootName = section.getName();
				}
				
				if (section instanceof ISectionRoot) {
					ISectionRoot rt = (ISectionRoot)section;
					subcount = rt.getTotalFileCount();
					sublength = rt.getTotalFileLength();
				}
				
				sections = section.getSubSections(squery);
				
			} else {
				throw new ErrorException(ErrorException.ErrorCode.NOT_FOUND, 
						"Section: " + key + " not found");
			}
		} else { 
			IndexSearcher searcher = getManager(user).getSearcher();
			
			id = searcher.getContentId(); 
			name = searcher.getName();
			contentType = searcher.getContentType();
			
			parentId = "";
			permissions = getPermissions(searcher);
			operations = getOperations(searcher);
			
			userId = user.getUserKey();
			userName = user.getUserName();
			userType = user instanceof IGroup ? IUser.GROUP : IUser.USER;
			userTitle = user.getPreference().getNickName();
			libraryId = searcher.getContentId();
			libraryName = searcher.getName();
			libraryType = searcher.getContentType();
			hostname = searcher.getHostName();
			
			subcount = searcher.getTotalFileCount();
			sublength = searcher.getTotalFileLength();
			sections = searcher.search(squery, user);
		}
		
		if (sections != null) { 
			totalcount = sections.getTotalCount();
			sectioncount = sections.getSectionCount();
			
			if (sectionfrom > totalcount)
				sectionfrom = (int)totalcount;
			
			//if (sectioncount + sectionfrom > totalcount)
			//	sectioncount = totalcount - sectionfrom;
			
			//if (sectioncount < 0)
			//	sectioncount = 0;
			
			for (int i=0; i < sections.getSectionCount(); i++) { 
				ISection section = sections.getSectionAt(i);
				NamedList<Object> info = getSectionInfo(section, subfilecount);
				if (section != null && info != null) 
					items.add(section.getContentId(), info);
			}
			
			ISectionGroup[] sgs = sections.getGroups();
			ISectionSort[] sss = sections.getSorts();
			
			for (int i=0; sgs != null && i < sgs.length; i++) { 
				ISectionGroup group = sgs[i];
				NamedList<Object> info = getGroupInfo(group);
				if (group != null && info != null) 
					groups.add(info);
			}
			
			String sortName = sections.getSortName();
			
			for (int i=0; sss != null && i < sss.length; i++) { 
				ISectionSort sort = sss[i];
				NamedList<Object> info = getSortInfo(sort);
				if (sort != null && info != null) {
					if (sortName != null && sortName.startsWith(sort.getName()))
						info.add("sorted", sortName);
					
					sorts.add(info);
				}
			}
		} else { 
			sectioncount = 0;
		}
		
		rsp.add("id", toString(id));
		rsp.add("name", toString(name));
		rsp.add("hostname", toString(hostname));
		rsp.add("type", toString(contentType));
		rsp.add("perms", toString(permissions));
		rsp.add("ops", toString(operations));
		rsp.add("query", toString(query));
		rsp.add("isfolder", isfolder);
		rsp.add("extname", toString(extension));
		rsp.add("poster", toString(poster));
		rsp.add("background", toString(background));
		
		rsp.add("parent_id", toString(parentId));
		rsp.add("parent_name", toString(parentName));
		rsp.add("parent_type", toString(parentType));
		
		rsp.add("root_id", toString(rootId));
		rsp.add("root_name", toString(rootName));
		rsp.add("root_type", toString(rootType));
		
		rsp.add("library_id", toString(libraryId));
		rsp.add("library_name", toString(libraryName));
		rsp.add("library_type", toString(libraryType));
		
		rsp.add("userid", toString(userId));
		rsp.add("username", toString(userName));
		rsp.add("usertype", toString(userType));
		rsp.add("usertitle", toString(userTitle));
		rsp.add("owner", toString(owner));
		
		rsp.add("total_count", totalcount);
		rsp.add("section_from", sectionfrom);
		rsp.add("section_count", sectioncount);
		rsp.add("subcount", subcount);
		rsp.add("sublength", sublength);
		
		rsp.add("groups", groups.toArray());
		rsp.add("sorts", sorts.toArray());
		rsp.add("sections", items);
	}

}
