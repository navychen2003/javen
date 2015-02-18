package org.javenstudio.falcon.publication.table;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.publication.IPublication;
import org.javenstudio.falcon.publication.IPublicationQuery;

final class TPublicationCache {
	private static final Logger LOG = Logger.getLogger(TPublicationCache.class);

	private final TPublicationService mService;
	private final TPublicationStream mStreams = new TPublicationStream();
	
	private final Map<String,WeakReference<TPublication>> mPublications = 
			new HashMap<String,WeakReference<TPublication>>();
	
	public TPublicationCache(TPublicationService service) {
		if (service == null) throw new NullPointerException();
		mService = service;
	}
	
	public TPublicationService getService() { return mService; }
	
	private void put(TPublication publish) {
		if (publish == null) return;
		
		synchronized (mPublications) {
			if (LOG.isDebugEnabled())
				LOG.debug("put: cache publish: " + publish);
			
			mPublications.put(publish.getId(), 
					new WeakReference<TPublication>(publish));
			mStreams.putPublication(publish);
		}
	}
	
	private TPublication get(String publishId) {
		if (publishId == null) return null;
		
		synchronized (mPublications) {
			WeakReference<TPublication> ref = mPublications.get(publishId);
			return ref != null ? ref.get() : null;
		}
	}
	
	public synchronized void close() {
		if (LOG.isDebugEnabled()) LOG.debug("close");
		
		synchronized (mPublications) {
			mPublications.clear();
		}
	}
	
	public void flushPublications() throws ErrorException {
		if (LOG.isDebugEnabled()) LOG.debug("flushPublications");
		
		TPublicationTable.flushPublications(getService());
	}
	
	public TPublication getPublication(String publishId) throws ErrorException {
		if (publishId == null) throw new NullPointerException();
		
		synchronized (mPublications) {
			TPublication publish = get(publishId);
			if (publish == null) {
				if (LOG.isDebugEnabled())
					LOG.debug("getPublication: load publish: " + publishId);
				
				publish = TPublicationTable.getPublication(getService(), publishId);
				if (publish != null) put(publish);
			}
			
			return publish;
		}
	}
	
	public TPublication deletePublication(String publishId) throws ErrorException {
		if (publishId == null) throw new NullPointerException();
		
		synchronized (mPublications) {
			TPublication publish = getPublication(publishId);
			if (publish != null) {
				if (LOG.isDebugEnabled())
					LOG.debug("deletePublication: delete publish: " + publishId);
				
				TPublicationTable.deletePublication(publish);
				mPublications.remove(publish.getId());
				mStreams.removePublication(publish);
			}
			
			return publish;
		}
	}
	
	public TPublication movePublication(String publishId, String channelTo) 
			throws ErrorException {
		if (publishId == null || channelTo == null) 
			throw new NullPointerException();
		
		synchronized (mPublications) {
			TPublication publish = getPublication(publishId);
			if (publish != null) {
				if (LOG.isDebugEnabled())
					LOG.debug("movePublication: move publish: " + publishId);
				
				TPublicationTable.movePublication(publish, channelTo);
				mPublications.remove(publish.getId());
				
				publish = getPublication(publishId);
				mStreams.putPublication(publish);
			}
			
			return publish;
		}
	}
	
	public TPublication savePublication(TPublication publish) throws ErrorException {
		if (publish == null) throw new NullPointerException();
		
		synchronized (mPublications) {
			if (LOG.isDebugEnabled())
				LOG.debug("savePublication: save publish: " + publish);
			
			TPublicationTable.savePublication(publish);
			mPublications.remove(publish.getId());
			
			publish = getPublication(publish.getId());
			mStreams.putPublication(publish);
			
			return publish;
		}
	}
	
	public TPublicationSet getPublications(String streamId, String channelName) 
			throws ErrorException {
		if (streamId == null) return null;
		
		String[] publishIds = mStreams.getPublicationIds(streamId, channelName);
		
		final Set<TPublication> publishSet = newSet();
		
		if (publishIds != null) {
			for (String publishId : publishIds) {
				TPublication publish = getPublication(publishId);
				if (publish != null)
					publishSet.add(publish);
			}
		}
		
		TPublication[] publishs = publishSet.toArray(new TPublication[publishSet.size()]);
		return new TPublicationSet(publishs, (int)publishs.length, (int)0);
	}
	
	public TPublicationSet getPublications(IPublicationQuery query) throws ErrorException {
		if (query == null) throw new NullPointerException();
		
		if (LOG.isDebugEnabled())
			LOG.debug("getPublications: query=" + query);
		
		String[] channelNames = query.getChannelNames();
		
		String owner = query.getOwner();
		String streamId = query.getStreamId();
		String status = query.getStatus();
		String flag = query.getFlag();
		
		long timeStart = query.getTimeStart();
		long timeEnd = query.getTimeEnd();
		int rowSize = query.getRowSize();
		
		int resultStart = (int)query.getResultStart();
		int resultCount = query.getResultCount();
		
		if (resultStart < 0) resultStart = 0;
		if (resultCount < 0) resultCount = 0;
		
		IPublicationQuery.GroupBy groupby = query.getGroupBy();
		int groupRowSize = query.getGroupRowSize();
		
		TPublication[] publishs = queryPublications(owner, 
				channelNames, streamId, status, flag, 
				rowSize, timeStart, timeEnd);
		
		if (groupby == IPublicationQuery.GroupBy.STREAM)
			publishs = groupbyStream(publishs, groupRowSize);
		
		return createSet(publishs, resultStart, resultCount);
	}
	
	private TPublicationSet createSet(TPublication[] publishs, 
			int resultStart, int resultCount) {
		ArrayList<TPublication> list = new ArrayList<TPublication>();
		int resultEnd = resultStart + resultCount;
		int totalCount = 0;
		
		if (publishs != null) {
			totalCount = publishs.length;
			for (int i = resultStart; i < publishs.length && i < resultEnd; i++) {
				TPublication publish = publishs[i];
				list.add(publish);
			}
		}
		
		TPublication[] results = list.toArray(new TPublication[list.size()]);
		return new TPublicationSet(results, (int)totalCount, (int)resultStart);
	}
	
	private TPublication[] groupbyStream(TPublication[] publishs, int groupRowSize) {
		if (publishs == null) return publishs;
		
		StreamSet[] streams = toStreamSet(publishs);
		ArrayList<TPublication> list = new ArrayList<TPublication>();
		
		if (streams != null) {
			Arrays.sort(streams, new Comparator<StreamSet>() {
					@Override
					public int compare(StreamSet o1, StreamSet o2) {
						long t1 = o1.mPublishTime;
						long t2 = o2.mPublishTime;
						
						String id1 = o1.mStreamId;
						String id2 = o2.mStreamId;
						
						return t1 > t2 ? -1 : (t1 < t2 ? 1 : id1.compareTo(id2));
					}
				});
			
			for (StreamSet streamSet : streams) {
				int rowCount = 0;
				for (TPublication publish : streamSet.mPublications) {
					if (publish != null) {
						list.add(publish);
						rowCount ++;
						if (groupRowSize > 0 && rowCount >= groupRowSize)
							break;
					}
				}
			}
		}
		
		return list.toArray(new TPublication[list.size()]);
	}
	
	private StreamSet[] toStreamSet(TPublication[] publishs) {
		if (publishs == null) return null;
		
		Map<String,StreamSet> streams = new HashMap<String,StreamSet>();
		
		for (TPublication publish : publishs) {
			if (publish == null) continue;
			
			String streamId = publish.getAttrString(IPublication.ATTR_STREAMID);
			if (streamId == null || streamId.length() == 0)
				continue;
			
			StreamSet streamSet = streams.get(streamId);
			if (streamSet == null) {
				streamSet = new StreamSet(streamId);
				streams.put(streamId, streamSet);
			}
			
			streamSet.put(publish);
		}
		
		return streams.values().toArray(new StreamSet[streams.size()]);
	}
	
	private TPublication[] queryPublications(String owner, 
			String[] channelNames, String streamId, String status, 
			String flag, int rowSize, long minStamp, long maxStamp) 
			throws ErrorException {
		final Set<TPublication> publishSet = newSet();
		
		if (channelNames != null && channelNames.length > 0) {
			for (String channelName : channelNames) {
				if (channelName != null && channelName.length() > 0) {
					scanPublications(publishSet, owner, channelName, null, 
							status, flag, rowSize, minStamp, maxStamp);
				}
			}
		} else if (streamId != null && streamId.length() > 0) {
			scanPublications(publishSet, owner, null, streamId, 
					status, flag, rowSize, minStamp, maxStamp);
			scanPublications(publishSet, owner, null, streamId, 
					status, flag, rowSize, minStamp, maxStamp);
			
		} else {
			scanPublications(publishSet, owner, null, null, 
					status, flag, rowSize, minStamp, maxStamp);
		}
		
		return publishSet.toArray(new TPublication[publishSet.size()]);
	}
	
	private void scanPublications(final Set<TPublication> publishSet, 
			String owner, String channelName, String streamId, 
			String status, String flag, int rowSize, 
			long minStamp, long maxStamp) throws ErrorException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("scanPublications: owner=" + owner 
					+ ", channelName=" + channelName 
					+ ", status=" + status + ", flag=" + flag
					+ ", rowSize=" + rowSize + ", minstamp=" + minStamp 
					+ ", maxstamp=" + maxStamp);
		}
		
		TPublicationTable.scanPublications(getService(), 
			owner, channelName, streamId, status, flag, 
			rowSize, minStamp, maxStamp, 
			new TPublicationTable.Collector() {
				@Override
				public void addPublication(TPublication publish) throws ErrorException {
					if (publish == null) return;
					put(publish); publishSet.add(publish);
				}
			});
	}
	
	static class StreamSet {
		private final String mStreamId;
		private final TreeSet<TPublication> mPublications;
		private long mPublishTime = 0;
		
		public StreamSet(String streamId) {
			if (streamId == null) throw new NullPointerException();
			mStreamId = streamId;
			mPublications = newSet();
		}
		
		public void put(TPublication publish) {
			if (publish == null) return;
			if (!mStreamId.equals(publish.getAttrString(IPublication.ATTR_STREAMID))) return;
			mPublications.add(publish);
			long publishTime = publish.getAttrLong(IPublication.ATTR_PUBLISHTIME);
			if (publishTime > mPublishTime)
				mPublishTime = publishTime;
		}
	}
	
	static TreeSet<TPublication> newSet() {
		return new TreeSet<TPublication>(new Comparator<TPublication>() {
				@Override
				public int compare(TPublication o1, TPublication o2) {
					long t1 = o1.getAttrLong(IPublication.ATTR_PUBLISHTIME);
					long t2 = o2.getAttrLong(IPublication.ATTR_PUBLISHTIME);
					
					String id1 = o1.getId();
					String id2 = o2.getId();
					
					return t1 > t2 ? -1 : (t1 < t2 ? 1 : id1.compareTo(id2));
				}
			});
	}
	
}
