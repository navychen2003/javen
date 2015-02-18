package org.javenstudio.falcon.publication.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javenstudio.falcon.publication.IPublication;

final class TPublicationStream {

	static class TStream {
		private final String mStreamId;
		private final Map<String,Set<String>> mPublications = 
				new HashMap<String,Set<String>>();
		
		public TStream(String streamId) {
			if (streamId == null) throw new NullPointerException();
			mStreamId = streamId;
		}
		
		public String getStreamId() { return mStreamId; }
	}
	
	private final Map<String,TStream> mStreams = 
			new HashMap<String,TStream>();
	
	public TPublicationStream() {}
	
	public synchronized void putPublication(TPublication publish) {
		if (publish == null) return;
		
		String publishId = publish.getId();
		String streamId = publish.getAttrString(IPublication.ATTR_STREAMID);
		String channelName = publish.getAttrString(IPublication.ATTR_CHANNEL);
		
		if (streamId == null || streamId.length() == 0)
			return;
		
		TStream stream = mStreams.get(streamId);
		if (stream == null) {
			stream = new TStream(streamId);
			mStreams.put(streamId, stream);
		}
		
		for (Set<String> list : stream.mPublications.values()) {
			list.remove(publishId);
		}
		
		Set<String> set = stream.mPublications.get(channelName);
		if (set == null) {
			set = new HashSet<String>();
			stream.mPublications.put(channelName, set);
		}
		
		set.add(publishId);
	}
	
	public synchronized void removePublication(TPublication publish) {
		String publishId = publish.getId();
		String streamId = publish.getAttrString(IPublication.ATTR_STREAMID);
		
		TStream stream = mStreams.get(streamId);
		if (stream == null) return;
		
		for (Set<String> list : stream.mPublications.values()) {
			list.remove(publishId);
		}
	}
	
	public synchronized String[] getPublicationIds(String streamId, 
			String channelName) {
		if (streamId == null) return null;
		
		TStream stream = mStreams.get(streamId);
		if (stream == null) return null;
		
		ArrayList<String> list = new ArrayList<String>();
		
		for (Map.Entry<String, Set<String>> entry : stream.mPublications.entrySet()) {
			String channel = entry.getKey();
			Set<String> publishs = entry.getValue();
			
			if (channelName == null || channelName.length() == 0 || 
				channelName.equals(channel)) {
				if (publishs != null) {
					for (String id : publishs) {
						list.add(id);
					}
				}
			}
		}
		
		return list.toArray(new String[list.size()]);
	}
	
}
