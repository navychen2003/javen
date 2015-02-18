package org.javenstudio.falcon.message.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class TMessageStream {

	static class TStream {
		private final String mStreamId;
		private final Map<String,Set<String>> mMessages = 
				new HashMap<String,Set<String>>();
		
		public TStream(String streamId) {
			if (streamId == null) throw new NullPointerException();
			mStreamId = streamId;
		}
		
		public String getStreamId() { return mStreamId; }
	}
	
	private final Map<String,TStream> mStreams = 
			new HashMap<String,TStream>();
	
	public TMessageStream() {}
	
	public synchronized void putMessage(TMessage message) {
		if (message == null) return;
		
		String messageId = message.getMessageId();
		String streamId = message.getStreamId();
		String folderName = message.getFolder();
		
		if (streamId == null || streamId.length() == 0)
			return;
		
		TStream stream = mStreams.get(streamId);
		if (stream == null) {
			stream = new TStream(streamId);
			mStreams.put(streamId, stream);
		}
		
		for (Set<String> list : stream.mMessages.values()) {
			list.remove(messageId);
		}
		
		Set<String> set = stream.mMessages.get(folderName);
		if (set == null) {
			set = new HashSet<String>();
			stream.mMessages.put(folderName, set);
		}
		
		set.add(messageId);
	}
	
	public synchronized void removeMessage(TMessage message) {
		String messageId = message.getMessageId();
		String streamId = message.getStreamId();
		
		TStream stream = mStreams.get(streamId);
		if (stream == null) return;
		
		for (Set<String> list : stream.mMessages.values()) {
			list.remove(messageId);
		}
	}
	
	public synchronized String[] getMessageIds(String streamId, 
			String folderName) {
		if (streamId == null) return null;
		
		TStream stream = mStreams.get(streamId);
		if (stream == null) return null;
		
		ArrayList<String> list = new ArrayList<String>();
		
		for (Map.Entry<String, Set<String>> entry : stream.mMessages.entrySet()) {
			String folder = entry.getKey();
			Set<String> messages = entry.getValue();
			
			if (folderName == null || folderName.length() == 0 || 
				folderName.equals(folder)) {
				if (messages != null) {
					for (String id : messages) {
						list.add(id);
					}
				}
			}
		}
		
		return list.toArray(new String[list.size()]);
	}
	
}
