package org.javenstudio.falcon.publication.table;

import org.javenstudio.falcon.publication.IPublication;
import org.javenstudio.falcon.publication.IPublicationService;
import org.javenstudio.falcon.publication.PublicationManager;

public final class TPublicationHelper {

	public static IPublicationService createApp(PublicationManager manager) { 
		return new TPublicationService(manager, 
			PublicationManager.TYPE_APP, 
			new String[] {
				IPublication.WINAPP, IPublication.IOSAPP, IPublication.ANDROIDAPP, 
				IPublication.WEBAPP, IPublication.DRAFT, IPublication.TRASH
			}, new TNameType[] { 
				TNameType.ATTR_PUBLISHID, 
				TNameType.ATTR_SERVICETYPE, 
				TNameType.ATTR_CHANNEL,
				TNameType.ATTR_CHANNELFROM, 
				TNameType.ATTR_PUBLISHTYPE, 
				TNameType.ATTR_STREAMID, 
				TNameType.ATTR_REPLYID,
				TNameType.ATTR_CATEGORY,
				TNameType.ATTR_LANGUAGE,
				TNameType.ATTR_OWNER,
				TNameType.ATTR_FLAG,
				TNameType.ATTR_STATUS,
				TNameType.ATTR_RATING,
				TNameType.ATTR_CREATEDTIME,
				TNameType.ATTR_PUBLISHTIME,
				TNameType.ATTR_UPDATETIME
			}, new TNameType[] { 
				TNameType.HEADER_AUTHOR,
				TNameType.HEADER_APPNAME,
				TNameType.HEADER_PACKAGENAME,
				TNameType.HEADER_PLATFORM,
				TNameType.HEADER_VERSIONCODE,
				TNameType.HEADER_VERSION,
				TNameType.HEADER_COMPANYNAME,
				TNameType.HEADER_COMPANYSITE,
				TNameType.HEADER_POSTER
			}, new TNameType[] { 
				TNameType.CONTENT_BODY,
				TNameType.CONTENT_SOURCE,
				TNameType.CONTENT_ATTACHMENTS,
				TNameType.CONTENT_SCREENSHOTS
			});
	}
	
	public static IPublicationService createSubscription(PublicationManager manager) { 
		return new TPublicationService(manager, 
			PublicationManager.TYPE_SUBSCRIPTION, 
			new String[] {
				IPublication.DEFAULT, IPublication.DRAFT, IPublication.TRASH
			}, new TNameType[] { 
				TNameType.ATTR_PUBLISHID, 
				TNameType.ATTR_SERVICETYPE, 
				TNameType.ATTR_CHANNEL,
				TNameType.ATTR_CHANNELFROM, 
				TNameType.ATTR_PUBLISHTYPE, 
				TNameType.ATTR_STREAMID, 
				TNameType.ATTR_REPLYID,
				TNameType.ATTR_CATEGORY,
				TNameType.ATTR_LANGUAGE,
				TNameType.ATTR_OWNER,
				TNameType.ATTR_FLAG,
				TNameType.ATTR_STATUS,
				TNameType.ATTR_RATING,
				TNameType.ATTR_CREATEDTIME,
				TNameType.ATTR_PUBLISHTIME,
				TNameType.ATTR_UPDATETIME
			}, new TNameType[] { 
				TNameType.HEADER_TITLE,
				TNameType.HEADER_SUBTITLE,
				TNameType.HEADER_SUBJECT,
				TNameType.HEADER_LINK,
				TNameType.HEADER_TAGS,
				TNameType.HEADER_HEADERLINES,
				TNameType.HEADER_CONTENTTYPE
			}, new TNameType[] { 
				TNameType.CONTENT_BODY,
				TNameType.CONTENT_SOURCE,
				TNameType.CONTENT_ATTACHMENTS
			});
	}
	
	public static IPublicationService createPost(PublicationManager manager) { 
		return new TPublicationService(manager, 
			PublicationManager.TYPE_POST, 
			new String[] {
				IPublication.DEFAULT, IPublication.DRAFT, IPublication.TRASH
			}, new TNameType[] { 
				TNameType.ATTR_PUBLISHID, 
				TNameType.ATTR_SERVICETYPE, 
				TNameType.ATTR_CHANNEL,
				TNameType.ATTR_CHANNELFROM, 
				TNameType.ATTR_PUBLISHTYPE, 
				TNameType.ATTR_STREAMID, 
				TNameType.ATTR_REPLYID,
				TNameType.ATTR_CATEGORY,
				TNameType.ATTR_LANGUAGE,
				TNameType.ATTR_OWNER,
				TNameType.ATTR_FLAG,
				TNameType.ATTR_STATUS,
				TNameType.ATTR_RATING,
				TNameType.ATTR_CREATEDTIME,
				TNameType.ATTR_PUBLISHTIME,
				TNameType.ATTR_UPDATETIME
			}, new TNameType[] { 
				TNameType.HEADER_TITLE,
				TNameType.HEADER_SUBTITLE,
				TNameType.HEADER_SUBJECT,
				TNameType.HEADER_LINK,
				TNameType.HEADER_TAGS,
				TNameType.HEADER_HEADERLINES,
				TNameType.HEADER_CONTENTTYPE
			}, new TNameType[] { 
				TNameType.CONTENT_BODY,
				TNameType.CONTENT_SOURCE,
				TNameType.CONTENT_ATTACHMENTS
			});
	}
	
	public static IPublicationService createFeatured(PublicationManager manager) { 
		return new TPublicationService(manager, 
			PublicationManager.TYPE_FEATURED, 
			new String[] {
				IPublication.POPULAR, IPublication.EDITORCHOICE, 
				IPublication.POSTER, IPublication.DRAFT, IPublication.TRASH
			}, new TNameType[] { 
				TNameType.ATTR_PUBLISHID, 
				TNameType.ATTR_SERVICETYPE, 
				TNameType.ATTR_CHANNEL,
				TNameType.ATTR_CHANNELFROM, 
				TNameType.ATTR_PUBLISHTYPE, 
				TNameType.ATTR_STREAMID, 
				TNameType.ATTR_REPLYID,
				TNameType.ATTR_CATEGORY,
				TNameType.ATTR_LANGUAGE,
				TNameType.ATTR_OWNER,
				TNameType.ATTR_FLAG,
				TNameType.ATTR_STATUS,
				TNameType.ATTR_RATING,
				TNameType.ATTR_CREATEDTIME,
				TNameType.ATTR_PUBLISHTIME,
				TNameType.ATTR_UPDATETIME
			}, new TNameType[] { 
				TNameType.HEADER_TITLE,
				TNameType.HEADER_SUBTITLE,
				TNameType.HEADER_SUBJECT,
				TNameType.HEADER_LINK,
				TNameType.HEADER_TAGS,
				TNameType.HEADER_HEADERLINES,
				TNameType.HEADER_CONTENTTYPE
			}, new TNameType[] { 
				TNameType.CONTENT_BODY,
				TNameType.CONTENT_SOURCE,
				TNameType.CONTENT_ATTACHMENTS
			});
	}
	
	public static IPublicationService createComment(PublicationManager manager) { 
		return new TPublicationService(manager, 
			PublicationManager.TYPE_COMMENT, 
			new String[] {
				IPublication.DEFAULT, IPublication.DRAFT, IPublication.TRASH
			}, new TNameType[] { 
				TNameType.ATTR_PUBLISHID, 
				TNameType.ATTR_SERVICETYPE, 
				TNameType.ATTR_CHANNEL,
				TNameType.ATTR_CHANNELFROM, 
				TNameType.ATTR_PUBLISHTYPE, 
				TNameType.ATTR_STREAMID, 
				TNameType.ATTR_REPLYID,
				TNameType.ATTR_CATEGORY,
				TNameType.ATTR_LANGUAGE,
				TNameType.ATTR_OWNER,
				TNameType.ATTR_FLAG,
				TNameType.ATTR_STATUS,
				TNameType.ATTR_RATING,
				TNameType.ATTR_CREATEDTIME,
				TNameType.ATTR_PUBLISHTIME,
				TNameType.ATTR_UPDATETIME
			}, new TNameType[] { 
				TNameType.HEADER_TITLE,
				TNameType.HEADER_SUBTITLE,
				TNameType.HEADER_SUBJECT,
				TNameType.HEADER_LINK,
				TNameType.HEADER_TAGS,
				TNameType.HEADER_HEADERLINES,
				TNameType.HEADER_CONTENTTYPE
			}, new TNameType[] { 
				TNameType.CONTENT_BODY,
				TNameType.CONTENT_SOURCE,
				TNameType.CONTENT_ATTACHMENTS
			});
	}
	
}
