package org.javenstudio.android.entitydb.content;

import java.util.ArrayList;

import org.javenstudio.android.entitydb.TAccount;
import org.javenstudio.android.entitydb.TAccountQuery;
import org.javenstudio.android.entitydb.TDownload;
import org.javenstudio.android.entitydb.TDownloadQuery;
import org.javenstudio.android.entitydb.TFetch;
import org.javenstudio.android.entitydb.TFetchQuery;
import org.javenstudio.android.entitydb.THost;
import org.javenstudio.android.entitydb.THostQuery;
import org.javenstudio.android.entitydb.TUpload;
import org.javenstudio.android.entitydb.TUploadQuery;

public final class ContentHelper {

	private static final ContentHelper sInstance = new ContentHelper();
	public static ContentHelper getInstance() { return sInstance; }
	
	private ContentHelper() {}
	
	public HostData newHost() { 
		THost data = new THost();
		return new HostDataImpl(data, true);
	}
	
	public HostData getHost(long entityId) { 
		return getHost(entityId, false);
	}
	
	private HostData getHost(long entityId, boolean updateable) { 
		THost entity = THostQuery.queryHost(entityId); 
		if (entity != null) 
			return new HostDataImpl(entity, updateable); 
		
		return null; 
	}
	
	public HostData queryHostByClusterId(String clusterId) {
		if (clusterId == null) return null;
		
		HostData[] hosts = queryHosts();
		if (hosts != null) {
			for (HostData host : hosts) {
				if (host == null) continue;
				
				String id = host.getClusterId();
				if (id != null && id.length() > 0) {
					if (clusterId != null && clusterId.equals(id))
						return host;
				}
			}
		}
		
		return null;
	}
	
	public HostData queryHostByKey(String hostKey) {
		if (hostKey == null) return null;
		
		HostData[] hosts = queryHosts();
		if (hosts != null) {
			for (HostData host : hosts) {
				if (host == null) continue;
				
				String key = host.getHostKey();
				if (key != null && key.length() > 0) {
					if (hostKey != null && hostKey.equals(key))
						return host;
				}
			}
		}
		
		return null;
	}
	
	public HostData queryHostByAddress(String domain, String address, 
			boolean ignoreHasKey) {
		if (domain == null && address == null) 
			return null;
		
		HostData[] hosts = queryHosts();
		if (hosts != null) {
			for (HostData host : hosts) {
				if (host == null) continue;
				
				String hostKey = host.getHostKey();
				if (hostKey != null && hostKey.length() > 0 && ignoreHasKey) 
					continue;
				
				String hostAddr = host.getHostAddr();
				if (hostAddr != null && hostAddr.length() > 0) {
					if (address != null && address.equals(hostAddr))
						return host;
				}
				
				String hostDomain = host.getDomain();
				if (hostDomain != null && hostDomain.length() > 0) {
					if (domain != null && domain.equals(hostDomain))
						return host;
				}
			}
		}
		
		return null;
	}
	
	public HostIterable getHostCursor() { 
		final THostQuery query = new THostQuery(); 
		return new HostIterable(query.queryCursor()); 
	}
	
	public HostData[] queryHostByDomain(String domain) {
		if (domain == null || domain.length() == 0) 
			return null;
		
		THostQuery query = new THostQuery();
		query.setClusterDomain(domain);
		
		HostIterable cursor = new HostIterable(query.queryCursor()); 
		return queryHosts(cursor);
	}
	
	public HostData[] queryHosts() { 
		return queryHosts(getHostCursor());
	}
	
	public HostData[] queryHosts(HostIterable cursor) { 
		if (cursor == null) return null;
		try { 
			ArrayList<HostData> list = new ArrayList<HostData>();
			while (cursor.hasNext()) { 
				HostData data = cursor.next();
				if (data != null) 
					list.add(data);
			}
			return list.toArray(new HostData[list.size()]);
		} finally { 
			cursor.close();
		}
	}
	
	public void deleteHost(long entityId) throws ContentException { 
		HostData data = getHost(entityId, true);
		if (data != null) 
			data.commitDelete();
	}
	
	
	public AccountData newAccount() { 
		TAccount data = new TAccount();
		return new AccountDataImpl(data, true);
	}
	
	public AccountData getAccount(long entityId) { 
		return getAccount(entityId, false);
	}
	
	private AccountData getAccount(long entityId, boolean updateable) { 
		TAccount entity = TAccountQuery.queryAccount(entityId); 
		if (entity != null) 
			return new AccountDataImpl(entity, updateable); 
		
		return null; 
	}
	
	public AccountIterable getAccountCursor() { 
		final TAccountQuery query = new TAccountQuery(); 
		return new AccountIterable(query.queryCursor()); 
	}
	
	public AccountData[] queryAccounts() { 
		return queryAccounts(getAccountCursor());
	}
	
	public AccountData[] queryAccounts(AccountIterable cursor) { 
		if (cursor == null) return null;
		try { 
			ArrayList<AccountData> list = new ArrayList<AccountData>();
			while (cursor.hasNext()) { 
				AccountData data = cursor.next();
				if (data != null) 
					list.add(data);
			}
			return list.toArray(new AccountData[list.size()]);
		} finally { 
			cursor.close();
		}
	}
	
	public void deleteAccount(long entityId) throws ContentException { 
		AccountData data = getAccount(entityId, true);
		if (data != null) 
			data.commitDelete();
	}
	
	
	public UploadData newUpload() { 
		TUpload data = new TUpload();
		return new UploadDataImpl(data, true);
	}
	
	public UploadData getUpload(long entityId) { 
		return getUpload(entityId, false);
	}
	
	private UploadData getUpload(long entityId, boolean updateable) { 
		TUpload entity = TUploadQuery.queryUpload(entityId); 
		if (entity != null) 
			return new UploadDataImpl(entity, updateable); 
		
		return null; 
	}
	
	public UploadData[] queryUploadByUri(String uri) {
		if (uri == null || uri.length() == 0) 
			return null;
		
		TUploadQuery query = new TUploadQuery();
		query.setContentUri(uri);
		
		UploadIterable cursor = new UploadIterable(query.queryCursor()); 
		return queryUploads(cursor);
	}
	
	public UploadIterable getUploadCursor(String accountId) { 
		final TUploadQuery query = new TUploadQuery(); 
		if (accountId != null) query.setDestAccountId(accountId);
		return new UploadIterable(query.queryCursor()); 
	}
	
	public UploadData[] queryUploads(String accountId) { 
		return queryUploads(getUploadCursor(accountId));
	}
	
	public UploadData[] queryUploads(UploadIterable cursor) {
		if (cursor == null) return null;
		try { 
			ArrayList<UploadData> list = new ArrayList<UploadData>();
			while (cursor.hasNext()) { 
				UploadData data = cursor.next();
				if (data != null) 
					list.add(data);
			}
			return list.toArray(new UploadData[list.size()]);
		} finally { 
			cursor.close();
		}
	}
	
	public void deleteUpload(long entityId) throws ContentException { 
		UploadData data = getUpload(entityId, true);
		if (data != null) 
			data.commitDelete();
	}
	
	
	public DownloadData newDownload() { 
		TDownload data = new TDownload();
		return new DownloadDataImpl(data, true);
	}
	
	public DownloadData getDownload(long entityId) { 
		return getDownload(entityId, false);
	}
	
	private DownloadData getDownload(long entityId, boolean updateable) { 
		TDownload entity = TDownloadQuery.queryDownload(entityId); 
		if (entity != null) 
			return new DownloadDataImpl(entity, updateable); 
		
		return null; 
	}
	
	public DownloadData[] queryDownloadByUri(String uri) {
		if (uri == null || uri.length() == 0) 
			return null;
		
		TDownloadQuery query = new TDownloadQuery();
		query.setContentUri(uri);
		
		DownloadIterable cursor = new DownloadIterable(query.queryCursor()); 
		return queryDownloads(cursor);
	}
	
	public DownloadIterable getDownloadCursor(String accountId) { 
		final TDownloadQuery query = new TDownloadQuery(); 
		if (accountId != null) query.setSourceAccountId(accountId);
		return new DownloadIterable(query.queryCursor()); 
	}
	
	public DownloadData[] queryDownloads(String accountId) { 
		return queryDownloads(getDownloadCursor(accountId));
	}
	
	public DownloadData[] queryDownloads(DownloadIterable cursor) { 
		if (cursor == null) return null;
		try { 
			ArrayList<DownloadData> list = new ArrayList<DownloadData>();
			while (cursor.hasNext()) { 
				DownloadData data = cursor.next();
				if (data != null) 
					list.add(data);
			}
			return list.toArray(new DownloadData[list.size()]);
		} finally { 
			cursor.close();
		}
	}
	
	public void deleteDownload(long entityId) throws ContentException { 
		DownloadData data = getDownload(entityId, true);
		if (data != null) 
			data.commitDelete();
	}
	
	
	public FetchData newFetch() { 
		TFetch data = new TFetch();
		return new FetchDataImpl(data, true);
	}
	
	public FetchData queryFetch(long entityId) { 
		return queryFetch(entityId, false);
	}
	
	private FetchData queryFetch(long entityId, boolean updateable) { 
		TFetch entity = TFetchQuery.queryFetch(entityId); 
		if (entity != null) 
			return new FetchDataImpl(entity, updateable); 
		
		return null; 
	}
	
	public FetchData queryFetch(String contentUri) { 
		if (contentUri == null || contentUri.length() == 0) 
			return null;
		
		final TFetchQuery query = new TFetchQuery(); 
		query.setContentUri(contentUri);
		
		FetchIterable cursor = new FetchIterable(query.queryCursor()); 
		try { 
			while (cursor.hasNext()) { 
				return cursor.next();
			}
		} finally { 
			cursor.close();
		}
		
		return null;
	}
	
	public FetchIterable getFetchCursor() { 
		final TFetchQuery query = new TFetchQuery(); 
		return new FetchIterable(query.queryCursor()); 
	}
	
	public FetchData[] queryFetchs() { 
		return queryFetchs(getFetchCursor());
	}
	
	public FetchData[] queryFetchs(FetchIterable cursor) { 
		if (cursor == null) return null;
		try { 
			ArrayList<FetchData> list = new ArrayList<FetchData>();
			while (cursor.hasNext()) { 
				FetchData data = cursor.next();
				if (data != null) 
					list.add(data);
			}
			return list.toArray(new FetchData[list.size()]);
		} finally { 
			cursor.close();
		}
	}
	
	public void deleteFetch(long entityId) throws ContentException { 
		FetchData data = queryFetch(entityId, true);
		if (data != null) 
			data.commitDelete();
	}
	
	public void updateFetchDirtyWithAccount(String account) { 
		if (account == null) return;
		
		FetchData[] datas = queryFetchs();
		for (int i=0; datas != null && i < datas.length; i++) { 
			FetchData data = datas[i];
			if (data == null) continue;
			
			String name = data.getAccount();
			if (name != null && name.equals(account)) { 
				updateFetchDirty(data);
			}
		}
	}
	
	public void updateFetchDirtyWithPrefix(String prefix) { 
		if (prefix == null) return;
		
		FetchData[] datas = queryFetchs();
		for (int i=0; datas != null && i < datas.length; i++) { 
			FetchData data = datas[i];
			if (data == null) continue;
			
			String name = data.getPrefix();
			if (name != null && name.equals(prefix)) { 
				updateFetchDirty(data);
			}
		}
	}
	
	private void updateFetchDirty(FetchData data) { 
		if (data == null) return;
		
		FetchData update = data.startUpdate();
		update.setStatus(FetchData.STATUS_DIRTY);
		update.setUpdateTime(System.currentTimeMillis());
		update.commitUpdates();
	}
	
}
