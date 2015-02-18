package org.javenstudio.lightning.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.TagTextField;
import org.jaudiotagger.tag.id3.AbstractTagFrame;
import org.jaudiotagger.tag.id3.AbstractTagFrameBody;
import org.jaudiotagger.tag.mp4.Mp4TagField;
import org.jaudiotagger.tag.mp4.field.Mp4TagCoverField;
import org.javenstudio.common.util.Logger;
import org.javenstudio.util.StringUtils;

public class SimpleExtractor {
	private static final Logger LOG = Logger.getLogger(SimpleExtractor.class);

	public static interface Collector { 
		public void addTag(String name, String value);
		public void addPic(String mimeType, byte[] data);
	}
	
	public static void main(String[] args) throws Exception { 
		String filename = args[0];
		
		AudioFile f = AudioFileIO.read(new File(filename));
		loadTags(f, new Collector() {
				@Override
				public void addTag(String name, String value) {
					System.out.println(name + " = " + value);
				}
				@Override
				public void addPic(String mimeType, byte[] data) {
					System.out.println(mimeType + " = " + data.length);
				}
			});
	}
	
	private static final Map<String,String> sTagNames = 
			new HashMap<String,String>();
	
	static { 
		addTagName("trkn", "Track");
		addTagName("disk", "Disk");
		addTagName("gnre", "Genre");
		addTagName(((char)0xa9)+"alb", "Album");
		addTagName("aART", "AlbumArtist");
		addTagName(((char)0xa9)+"ART", "Artist");
		addTagName(((char)0xa9)+"nam", "Title");
		addTagName(((char)0xa9)+"day", "Year");
		addTagName("cprt", "Copyright");
		addTagName("soaa", "AlhumArtistSort");
		addTagName("soal", "AlbumSort");
		addTagName("soar", "ArtistSort");
		addTagName("tmpo", "BPM");
		addTagName("catg", "Category");
		addTagName(((char)0xa9)+"cmt", "Comment");
		addTagName("cpil", "Compilation");
		addTagName(((char)0xa9)+"wrt", "Composer");
		addTagName("soco", "ComposerSort");
		addTagName("desc", "Description");
		addTagName(((char)0xa9)+"too", "Encoder");
		addTagName(((char)0xa9)+"gen", "GenreCustom");
		addTagName(((char)0xa9)+"grp", "Grouping");
		addTagName("keyw", "Keyword");
		addTagName(((char)0xa9)+"lyr", "Lyrics");
		addTagName("pgap", "PartOfGaplessAlbum");
		addTagName("purd", "PurchaseDate");
		addTagName("rtng", "Rating");
		addTagName("tvsh", "Show");
		addTagName("sosn", "ShowSort");
		addTagName("sonm", "TitleSort");
		
		addTagName("TCON", "ContentType");
		addTagName("TRCK", "TrackNumber");
		addTagName("TYER", "Year");
		addTagName("TALB", "Album");
		addTagName("TIT2", "Title");
		addTagName("TPE1", "Artist");
		addTagName("AENC", "AudioEncryption");
		addTagName("APIC", "AttachedPicture");
		addTagName("ASPI", "AudioFiles");
		addTagName("CHAP", "Chapter");
		addTagName("COMM", "Comment");
		addTagName("COMR", "Commercial");
		addTagName("CRM", "EncryptedMeta");
		addTagName("CTOC", "TableOfContent");
		addTagName("ENCR", "EncryptionMethod");
		addTagName("EQU2", "Equalisation2");
		addTagName("EQUA", "Equalisation");
		addTagName("ETCO", "EventTimingCodes");
		addTagName("GEOB", "GeneralEncapsulatedObject");
		addTagName("GRID", "GroupIdentification");
		addTagName("IPLS", "InvolvedPeopleList");
		addTagName("LINK", "LinkedInformation");
		addTagName("MCDI", "MusicCDIdentifier");
		addTagName("MLLT", "MPEGLocationLookupTable");
		addTagName("OWNE", "Ownership");
		addTagName("PCNT", "Play counter");
		addTagName("PIC", "AttachedPicture");
		addTagName("POPM", "Popularimeter");
		addTagName("POSS", "PositionSynchronisation");
		addTagName("PRIV", "Private");
		addTagName("RBUF", "RecommendedBufferSize");
		addTagName("RVAD", "RelativeVolumeAdjustment");
		addTagName("RVA2", "RelativeVolumeAdjustment2");
		addTagName("RVRB", "Reverb");
		addTagName("SEEK", "Seek");
		addTagName("SIGN", "Sign");
		addTagName("SYLT", "SynchronisedLyricsText");
		addTagName("SYTC", "SynchronisedTempoCodes");
		addTagName("TBPM", "BeatsPerMinute");
		addTagName("TCMP", "PartOfCompilation");
		addTagName("TCOM", "Composer");
		addTagName("TCOP", "Copyright");
		addTagName("TDAT", "Date");
		addTagName("TDEN", "DEN");
		addTagName("TDLY", "PlaylistDelay");
		addTagName("TDOR", "OriginalReleaseTime");
		addTagName("TDRC", "DRC");
		addTagName("TDRL", "DRL");
		addTagName("TDTG", "TaggingTime");
		addTagName("TENC", "Encoded");
		addTagName("TEXT", "Writer");
		addTagName("TFLT", "FileType");
		addTagName("TIME", "Time");
		addTagName("TIPL", "InvolvedPeopleList");
		addTagName("TIT1", "ContentGroupDescription");
		addTagName("TIT3", "Subtitle");
		addTagName("TKEY", "InitialKey");
		addTagName("TLAN", "Language");
		addTagName("TLEN", "Length");
		addTagName("TMCL", "MCL");
		addTagName("TMED", "MediaType");
		addTagName("TMOO", "MOO");
		addTagName("TOAL", "OriginalTitle");
		addTagName("TOFN", "OriginalFilename");
		addTagName("TOLY", "OriginalWriter");
		addTagName("TOPE", "OriginalArtist");
		addTagName("TORY", "OriginalReleaseYear");
		addTagName("TOWN", "FileOwner");
		addTagName("TPE2", "Band");
		addTagName("TPE3", "Conductor");
		addTagName("TPE4", "Interpreted");
		addTagName("TPOS", "PartOfText");
		addTagName("TPRO", "PRO");
		addTagName("TPUB", "Publisher");
		addTagName("TRCK", "Track");
		addTagName("TRDA", "RecordingDates");
		addTagName("TRSN", "InternetRadioStationName");
		addTagName("TRSO", "InternetRadioStationOwner");
		addTagName("TSIZ", "SizeText");
		addTagName("TSO2", "AlbumArtistSort");
		addTagName("TSOA", "AlbumSort");
		addTagName("TSOC", "ComposerSort");
		addTagName("TSOP", "SOP");
		addTagName("TSOT", "TitleSort");
		addTagName("TSRC", "Src");
		addTagName("TSSE", "Settings");
		addTagName("TSST", "SST");
		addTagName("TXXX", "UserDefined");
		addTagName("UFID", "UFID");
		addTagName("USER", "User");
		addTagName("USLT", "UnsychronisedLyricsTextTranscription");
		addTagName("WCOM", "CommercialInformationURL");
		addTagName("WCOP", "CopyrightURL");
		addTagName("WOAF", "OfficialAudioFileURL");
		addTagName("WOAR", "OfficialArtistURL");
		addTagName("WOAS", "OfficialAudioSourceURL");
		addTagName("WORS", "OfficialRadioStationURL");
		addTagName("WPAY", "PaymentURL");
		addTagName("WXXX", "PublishersURL");
		addTagName("XSOA", "AlbumSort2");
		addTagName("XSOP", "ArtistSort2");
		addTagName("XSOT", "TitleSort2");
	}
	
	private static void addTagName(String name, String tagname) { 
		if (name == null || tagname == null) return;
		synchronized (sTagNames) { 
			sTagNames.put(name, tagname);
		}
	}
	
	private static String toTagName(String name) { 
		if (name == null) return name;
		synchronized (sTagNames) { 
			String tagname = sTagNames.get(name);
			if (tagname != null) return tagname;
		}
		return name;
	}
	
	public static boolean loadTags(File file, String extname, 
			Collector collector) throws IOException { 
		if (file == null || extname == null || collector == null)
			return false;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadTags: file=" + file + " extname=" + extname);
		
		try {
			AudioFile afile = AudioFileIO.getDefaultAudioFileIO()
					.readFile(file, extname);
			
			return loadTags(afile, collector);
		} catch (IOException e) { 
			if (e instanceof FileNotFoundException) { 
				if (LOG.isDebugEnabled())
					LOG.debug("loadTags: not found: " + e, e);
				return false;
			}
			throw e;
		} catch (CannotReadException e) { 
			if (LOG.isDebugEnabled())
				LOG.debug("loadTags: unsupported: " + e, e);
			return false;
		} catch (Throwable e) { 
			throw new IOException(e.toString(), e);
		}
	}
	
	public static boolean loadTags(AudioFile file, Collector collector) {
		if (file == null || collector == null) return false;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadTags: audioFile=" + file);
		
		boolean result = false;
		
		AudioHeader header = file.getAudioHeader();
		if (header != null) { 
			collector.addTag("EncodingType", header.getEncodingType());
			collector.addTag("BitRate", header.getBitRate());
			collector.addTag("SampleRate", header.getSampleRate());
			collector.addTag("Format", header.getFormat());
			collector.addTag("Channels", header.getChannels());
			collector.addTag("Duration", ""+(header.getTrackLength()));
			collector.addTag("BitsPerSample", ""+header.getBitsPerSample());
			result = true;
		}
		
		Tag tag = file.getTag();
		if (tag == null) return result;
		
		Iterator<TagField> it = tag.getFields();
		while (it.hasNext()) { 
			TagField field = it.next();
			if (field == null) continue;
			
			final String id = field.getId();
			final String tagName = toTagName(id);
			
			if (field instanceof Mp4TagField) {
				if (field instanceof Mp4TagCoverField) { 
					Mp4TagCoverField cover = (Mp4TagCoverField)field;
					byte[] data = cover.getData();
					if (data != null) {
						String mimeType = "image/png";
						switch (cover.getFieldType()) { 
						case COVERART_PNG:
							mimeType = "image/png";
							break;
						case COVERART_JPEG:
							mimeType = "image/jpeg";
							break;
						case COVERART_GIF:
							mimeType = "image/gif";
							break;
						case COVERART_BMP:
							mimeType = "image/bmp";
							break;
						default:
							mimeType = "image/png";
							break;
						}
						collector.addPic(mimeType, data);
					}
				} else { 
					final String tagValue = field.toString();
					collector.addTag(tagName, tagValue);
				}
				result = true;
			} else if (field instanceof TagTextField) { 
				if (id.equals("APIC") || id.equals("PIC")) { 
					if (field instanceof AbstractTagFrame) {
						AbstractTagFrame frame = (AbstractTagFrame)field;
						AbstractTagFrameBody body = frame.getBody();
						if (body != null) {
							Object mimeTypeObj = (String)body.getObjectValue("MIMEType");
							Object dataObj = body.getObjectValue("PictureData");
							if (mimeTypeObj != null && mimeTypeObj instanceof String && 
								dataObj != null && dataObj instanceof byte[]) { 
								String mimeType = (String)mimeTypeObj;
								byte[] data = (byte[])dataObj;
								collector.addPic(mimeType, data);
							}
						}
					}
				} else { 
					String tagValue = field.toString();
					if (tagValue != null && tagValue.startsWith("Text=")) { 
						tagValue = StringUtils.trimChars(tagValue.substring(5), " \t\r\n\"\';,");
						collector.addTag(tagName, tagValue);
					}
				}
				result = true;
			}
		}
		
		return result;
	}
	
}
