/**
 * Brian Yoo (bgy2 | 140007707)
 * Jeffrey Kang (jk976 | 13900087)
 * Jarrett Mead (jfm168 | 143008288)
 * 
 * CS 352: Internet Technology
 * BitTorrent Client | Phase 1
 */

package RUBTClient;
import java.io.BufferedInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import GivenTools.*;

public class Torrent {
	
	private String announce_url;
	private byte[] info_hash;
	private ByteBuffer[] piece_hashes;
	private String peer_id;
	private String ip;
	private int port;
	private int uploaded;
	private int downloaded;
	private int left;
	private int file_length;
	private int piece_length;
	private int small_piece_length;
	private int hashes_length;
	private int min_interval;
	private String event;
	private byte[][] masterBuffer;
	
	
	// Constructor
	public Torrent (TorrentInfo ti) {
		String announce_url = ti.announce_url.toString();
		String parsedURL = parseAnnounceURL(announce_url);
		String[] split = parsedURL.split(":");
		
		this.announce_url = announce_url;
		this.info_hash = ti.info_hash.array();
		this.piece_hashes = ti.piece_hashes;
		this.peer_id = generatePeerID();
		this.ip = split[0];
		this.port = 6881;
		this.uploaded = 0;
		this.downloaded = 0;
		this.left = 0;
		this.file_length = ti.file_length;
		this.piece_length = ti.piece_length;
		this.small_piece_length = file_length - ((file_length/piece_length)*piece_length);
		this.hashes_length = ti.piece_hashes.length;
		this.min_interval = 120;
		this.event = "";
		this.masterBuffer = new byte[hashes_length][];
	}
	/** 
	 * Returns the metadata from the given .torrent file into a TorrentInfo Object
	 * @param torrentName name of the .torrent file
	 * @return TorrentInfo object that holds the metadata for the .torrent file
	 * @throws FileNotFoundException
	 */
	public static TorrentInfo getTorrentInfo(String torrentName) throws FileNotFoundException {
		// Error Check.
		if (torrentName.equals(""))
			return null;
		
		// Create File object f using args[0]
		File f = new File(torrentName);
		FileInputStream fis = new FileInputStream(f);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		// Obtain the metadata from .torrent and store into a byte array.
		byte[] buf = new byte[1024];
		try {
			// Read the file, and write to the byte buffer
			for (int readNum; (readNum = fis.read(buf)) != -1;) {
				bos.write(buf, 0, readNum); // Offset will be zero
			}
		} catch (IOException ex) { System.out.println("IO exception Error: " + ex.getMessage()); }
		byte[] data = bos.toByteArray();

		// Decode the byte array and return a TorrentInfo Object.
		try {
			TorrentInfo ti = new TorrentInfo(data);
			return ti;
		} catch (BencodingException e) { System.out.println("Bencoding Exception Error: " + e.getMessage()); }
		return null;
	}
	
	/**
	 * Returns a Torrent object
	 * @param ti TorrentInfo Object
	 * @return Torrent Object
	 */
	public static Torrent createTorrent(TorrentInfo ti) {
		if (ti == null) {
			System.out.println("Unable to create Torrent Object. Null received.");
			return null;
		}
		return new Torrent(ti);
	}
	
	/**
	 * Returns a HttpURLConnection that holds the Tracker connection
	 * @param torrent Torrent object containing the metadata of the .torrent file
	 * @return HttpURLConnection the tracker connection
	 * @throws MalformedURLException
	 */
	public HttpURLConnection getTrackerConnection(Torrent torrent) throws MalformedURLException{
		// Error Check.
		if (torrent == null)
			System.out.println("Unable to create a tracker connection. Null received.");
		
		// Make connection with tracker.
		URL url = createURL(torrent);
		int tryPort = 6881;
		HttpURLConnection trackerConnection = null;
		while (tryPort < 6890) {
			try {
				System.out.println("Attempting connection to the tracker with port: " + this.port + "...");
				trackerConnection = (HttpURLConnection) url.openConnection();
				System.out.println("Successful tracker connection");
				return trackerConnection;
			} catch (IOException e) {
				System.out.println("Connection failure with port: " + tryPort);
				tryPort++;
				torrent.port = tryPort;
				url = createURL(torrent);
			}
		}
		return trackerConnection;
	}
	
	/**
	 * Returns a URL object based on the metadata stored within the Torrent object
	 * @param torrent Torrent Object containing the information within the .torrent file
	 * @return URL object
	 */
	public URL createURL(Torrent torrent) {
		// Error Check.
		if (torrent == null)
			System.out.println("Unable to create URL. Null received.");
		
		String announce_url = torrent.announce_url;
		//String parsedURL = parseAnnounceURL(announce_url);
		//String[] split = parsedURL.split(":");
		String info_hash = toHexString(torrent.info_hash);
		String peer_id = this.peer_id;
		String port = Integer.toString(this.port);
		//String ip = split[0];
		String uploaded = Integer.toString(torrent.uploaded);
		String downloaded = Integer.toString(torrent.downloaded);
		String left = Integer.toString(torrent.file_length - torrent.downloaded);
		String event = torrent.event;
		
		String str = announce_url + 
				"?info_hash=" + info_hash +
				"&peer_id=" + peer_id + 
				"&port=" + port +
				"&uploaded=" + uploaded +
				"&downloaded=" + downloaded +
				"&left=" + left +
				"&event=" + event;
		
		// Attempt to create the URL
		try { return new URL(str);
		} catch (MalformedURLException e) { System.out.println("Unable to create URL. Malformed URL Exception: " + e.getMessage()); }
		return null;
	}
	
	
	/**
	 * Returns a URL based on the given parameters
	 * @param ti
	 * @param downloaded_amt
	 * @param left_amt
	 * @param eventmsg
	 * @return URL object
	 * @throws MalformedURLException
	 */
	public URL createURL(TorrentInfo ti, int downloaded_amt, int left_amt, String eventmsg) throws MalformedURLException{
		// Error Check.
		if (ti == null)
			System.out.println("Unable to obtain Torrent file metadata. Null received.");
		
		String announce_url = ti.announce_url.toString();
		//String parsedURL = parseAnnounceURL(announce_url);
		//String[] split = parsedURL.split(":");
		String info_hash = toHexString(ti.info_hash.array());
		String peer_id = this.peer_id;
		String port = Integer.toString(this.port);
		//String ip = split[0];
		String uploaded = "0";
		String downloaded = Integer.toString(downloaded_amt);
		String left = Integer.toString(ti.file_length - downloaded_amt);
		String event = eventmsg;
		
		// The part of the url that changes.
		this.uploaded = 0;
		this.downloaded = downloaded_amt;
		this.left = ti.file_length - downloaded_amt;
		
		// Concat all metadata.
		String str = announce_url + 
				"?info_hash=" + info_hash +
				"&peer_id=" + peer_id + 
				"&port=" + port +
				"&uploaded=" + uploaded +
				"&downloaded=" + downloaded +
				"&left=" + left +
				"&event=" + event;
		//System.out.println(str);
		return new URL(str);
	}
	
	public static String generatePeerID() {
		char[] chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		String peerid = "";
		
		// Peer ID must be length 20. Generate a random Peer ID that does not start with RUBT.
		while (true) {
			for (int i = 0; i < 20; i++) {
				peerid = peerid + chars[(int)(Math.random()*35)];
			}
			String sub = peerid.substring(0,4);
			if (!sub.equals("RUBT"))
				break;
		}
		return peerid;
	}
	
	public static String toHexString(byte[] arr) {
		// Error Check
		if (arr == null) {
			System.out.println("Null input. Cannot convert byte array to Hex String.");
			return "";
		}
		
		// Convert byte array to Hex. Append % at end to 
		final StringBuilder builder = new StringBuilder();
	    for(byte b : arr) {
	    	builder.append("%");
	        builder.append(String.format("%02X", b));
	    }
	    return builder.toString();
	    
	    // If a divider is not required, replace body with following:
	    // String str = DatatypeConverter.printHexBinary(arr);
	    // return str;
	}
	/**
	 * Takes in the annouce_url and parses the string down to the IP and Port 
	 * @param url String object to be parsed
	 * @return null if there is an error
	 */
	public static String parseAnnounceURL(String url) {
		String text = url;
		String pattern = "\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d{1,5})?";

		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(text);
		while (matcher.find()) {
			return matcher.group();
		}
		return null;
	}
	

	/**
	 * Return the encoded tracker response
	 * @param trackerConnection HttpURLConnection object establish by the URL from the TorrentInfo
	 * @return the encoded tracker response as a byte array
	 */
	public static byte[] getTrackerResponse(HttpURLConnection trackerConnection) {
		// Error check
		if (trackerConnection == null) return null;
		
		// Open IO streams and write data from connection to a buffer
		BufferedInputStream bis = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[trackerConnection.getContentLength()];
		try { bis = new BufferedInputStream(trackerConnection.getInputStream());
		} catch (IOException e) { System.out.println("Buffered Input Stream Error: " + e.getMessage()); }
		
		// Write Tracker Response to ByteArrayOutputStream
		try {
			// Read the file, and write to the byte buffer
			for (int readNum; (readNum = bis.read(buf, 0, buf.length)) != -1;) {
				baos.write(buf, 0, readNum); // Offset will be zero
			}
		} catch (IOException ex) {
			System.out.println("IO exception Error: " + ex.getMessage());
		}
				
		byte[] encoded_response = baos.toByteArray();
		return encoded_response;
	}
	
	/**
	 * Decode a byte array to a HashMap object.
	 * @param encoded_response Byte array to be decoded into a HashMap
	 * @return HashMap object
	 */
	public static HashMap<ByteBuffer, Object> decodeEncodedRepsonse(byte[] encoded_response) {
		if (encoded_response == null) return null;	// Error check.
		
		HashMap<ByteBuffer, Object> decoded_response = null;
		try {
			decoded_response = (HashMap<ByteBuffer, Object>) Bencoder2.decode(encoded_response);
		} catch (BencodingException e) {
			System.out.println("Bencoding Exception error: " + e.getMessage());
		}
		return decoded_response;
	}
	
	public synchronized void addToMasterBuffer(byte[] piece, int index) {
		if (piece == null) 
			System.out.println("Null Paramter. Cannot add hashed piece");
		else {
			if (masterBuffer[index] == null) {
				masterBuffer[index] = piece;
			}
		}
	}
	
	// Get Methods
	public String getAnnounceURL() {return this.announce_url;}
	public byte[] getInfoHash() {return this.info_hash;}
	public ByteBuffer[] getPieceHashes() {return this.piece_hashes;}
	public String getPeerID() {return this.peer_id;}
	public String getIP() {return this.ip;}
	public int getPort() {return this.port;}
	public int getUploaded() {return this.uploaded;}
	public int getDownloaded() {return this.downloaded;}
	public int getLeft() {return this.left;}
	public int getFileLength() {return this.file_length;}
	public int getPieceLength() {return this.piece_length;}
	public int getHashesLength() {return this.hashes_length;}
	public String getEvent() {return this.event;}
	public int getSmallPieceLength() {return small_piece_length;}
	public synchronized byte[][] getMasterbuffer() {return masterBuffer;}
	
	// Set Methods
	public void resumeMasterbuffer(byte[][] resumeMB){
		masterBuffer = resumeMB;
	}
	public void setDownloaded(int num) {
		if (num < 0){ 
			System.out.println("Invalid parameter. Cannot set downloaded to a negative value");
			return;
		}
		this.downloaded = num;
	}
	public void resumeDownloaded(int newDownloaded){
		downloaded = newDownloaded;
	}
	public void setUploaded(int num) {
		if (num < 0){ 
			System.out.println("Invalid parameter. Cannot set uploaded to a negative value");
			return;
		}
		this.uploaded = num;
	}
	public void setLeft(int num) {
		/*if (num < 0){ 
			System.out.println("Invalid parameter. Cannot set left to a negative value");
			return;
		}*/
		this.uploaded = num;
	}
	public void resumeLeft(int newLeft){
		left = newLeft;
	}
	public void setEvent(String event) {
		if (!(event.equals("started") || event.equals("stopped") || event.equals("completed") || event.equals("empty"))) {
			System.out.println("Invalid parameter. Must set event to a valid key");
			return;
		}
		this.event = event;
	}
	public void setMinInterval(int min_interval) {
		this.min_interval = min_interval;
	}


}
