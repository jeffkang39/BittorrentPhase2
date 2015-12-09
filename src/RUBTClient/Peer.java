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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import GivenTools.Bencoder2;
import GivenTools.BencodingException;
import GivenTools.ToolKit;
import GivenTools.TorrentInfo;

public class Peer {
	final static ByteBuffer PEERS = ByteBuffer.wrap(new byte[] {'p','e','e','r','s'});
	final static ByteBuffer IP = ByteBuffer.wrap(new byte[] {'i','p'});
	final static ByteBuffer PEER_ID = ByteBuffer.wrap(new byte[] {'p','e','e','r',' ','i','d'});
	final static ByteBuffer PORT = ByteBuffer.wrap(new byte[] {'p', 'o', 'r', 't'});
	final static ByteBuffer MIN_INTERVAL = ByteBuffer.wrap(new byte[] {'m', 'i', 'n', ' ', 'i', 'n', 't', 'e', 'r','v', 'a', 'l'});
	/**
	 * Obtains the list of peers specified at location 128.6.171.130 and 128.6.171.131
	 * @param trackerConnection HttpURLConnection object 
	 * @return ArrayList<HashMap<ByteBuffer, Object>> object containing peers specified at location 128.6.171.130 and 128.6.171.131
	 */
	public static ArrayList<HashMap<ByteBuffer, Object>> getTargetPeerList(HttpURLConnection trackerConnection) {
		// Open IO streams and write data from connection to a buffer
		BufferedInputStream bis = null;
		try { bis = new BufferedInputStream(trackerConnection.getInputStream());
		} catch (IOException e2) { System.out.println("Unable to open input stream"); }
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		// Write Tracker Response to ByteArrayOutputStream
		byte[] buf = new byte[trackerConnection.getContentLength()];
		try {
			// Read the file, and write to the byte buffer
			for (int readNum; (readNum = bis.read(buf, 0, buf.length)) != -1;) {
				baos.write(buf, 0, readNum); // Offset will be zero
			}
		} catch (IOException ex) {
			System.out.println("IO exception Error: " + ex.getMessage());
		}
		/*
		 * Tracker Response.
		 * 1. Take the Encoded response and decode it into the HashMap that holds all of the dictionaries (peers)
		 * 		(i.e. This decoded object is a HashMap of HashMaps)
		 * 2. Covert that decoded HashMap into an ArrayList. Each index holds an individual dictionary (peer)
		 * 3. Use a loop to convert each index entry back to a HashMap to obtain each dictionary (peer)
		 * 4. Extract the target peer(s)
		 * 5. Store those peers into a list to open TCP connections with later
		 */
		byte[] encoded_response = baos.toByteArray();					// Record the encoded response
		HashMap<ByteBuffer, Object> decoded_response = null;
		try {
			decoded_response = (HashMap<ByteBuffer, Object>) Bencoder2.decode(encoded_response);
		} catch (BencodingException e1) {
			System.out.println("Bencoding Exception Error: " + e1.getMessage());
		}
		
		ToolKit.print(decoded_response);

		// Search through the list of peers and extract peers located at: 128.6.171.130, 128.6.171.131
		ArrayList<HashMap<ByteBuffer, Object>> targetPeerList = new ArrayList<HashMap<ByteBuffer, Object>>();
		ArrayList<Object> peerList = (ArrayList<Object>) decoded_response.get(PEERS);
		for(int i = 0; i < peerList.size(); i++)
		{
			HashMap<ByteBuffer, Object> eachPeer = (HashMap<ByteBuffer, Object>) peerList.get(i);
			// Extract the target peer(s) located at IP Addresses: 128.6.171.130 and 128.6.171.131
			String currPeerIP = objectBBToString(eachPeer.get(IP));
			if (currPeerIP.equals("128.6.171.130") || currPeerIP.equals("128.6.171.131")) {
				targetPeerList.add(eachPeer);
			}
		}
		return targetPeerList;
	}
	
	public static Socket openTCPConnection(HashMap<ByteBuffer, Object> peer) {
		// Declare variables for peer.
		String peerIP = "";
		int port = -1;
		
		peerIP = objectBBToString(peer.get(IP));
		port = (int) peer.get(PORT);
		
		try {
			Socket s = new Socket(peerIP, port);
			System.out.println("Successful connection to peer: " + peerIP);
			return s;
		} catch (IOException e) {
			System.out.println("Unable to create a connection to the peer");
		}
		return null;
	}
	
	/**
	 * Convert a ByteBuffer of type Object to a String
	 * @param o object
	 * @return String
	 */
	public static String objectBBToString(Object o)
    {
		if (o == null) return "";		// Error check.
		
		String str = "";
		try {
			str = new String((((ByteBuffer) o).array()));
		} catch (ClassCastException e){
			System.out.println("Cannot cast this object to a string.");
			System.out.println("Object must be type ByteBuffer");
		}
        return str;
    }
	
	public static byte[] createHandshake(TorrentInfo ti, Torrent torrent) {	
		/* 
		 * COMMUNICATING WITH THE PEER
		 * Handshaking between peers begins with byte 19, followed by string "BitTorrent protocol"
		 * After fixed headers, there are 8 reserved bytes which are set to 0
		 * Next is 20 byte SHA-1 hash of the bencoded form of the info value from TorrentInfo
		 * Next 20 bytes are the peer_id generated by the client
		 */
		// Create handshake byte arrays
		String protocol = "BitTorrent protocol00000000";
		byte[] protocolLengthByte = new byte[] {19};								//Protocol string length single byte
		byte[] protocolBytes = protocol.getBytes(Charset.forName("UTF-8"));			//Protocol followed by 8 reserved bytes
		byte[] hashBytes = ti.info_hash.array();									//info_hash from metainfo file
		byte[] peerIDBytes = torrent.getPeerID().getBytes(Charset.forName("UTF-8"));//Peer_ID
		
		// Wrap byte arrays in ByteBuffers to combine them and make them big-endian
		final ByteBuffer PLB = ByteBuffer.wrap(protocolLengthByte);
		final ByteBuffer PB = ByteBuffer.wrap(protocolBytes);
		final ByteBuffer HB = ByteBuffer.wrap(hashBytes);
		final ByteBuffer PIDB = ByteBuffer.wrap(peerIDBytes);
		ByteBuffer HandShake = ByteBuffer.allocate(68);							//allocate handshake bytebuffer the length of the handshake
		HandShake = HandShake.put(PLB);
		HandShake = HandShake.put(PB);
		HandShake = HandShake.put(HB);
		HandShake = HandShake.put(PIDB);
		HandShake = HandShake.order(ByteOrder.BIG_ENDIAN);

		// Convert HandShake to a byte array to send to peer
		byte[] HS_arr = null;
		try {
			HS_arr = HandShake.array();
		} catch (UnsupportedOperationException e){
			System.out.println("Unable to convert HandShake ByteBuffer to byte array.");
			System.out.println(HandShake.toString());
		}
		return HS_arr;
	}
	
	public static boolean verifyHandshake(byte[] handshake, TorrentInfo ti, Socket s) throws IOException {
		// Open IO Streams
		DataInputStream input = new DataInputStream(s.getInputStream());
		DataOutputStream output = new DataOutputStream(s.getOutputStream());
		
		// HANDSHAKE
		output.write(handshake);										// Send out HandShake to peer
		byte[] HS_Response = new byte[68];
		try {
			input.readFully(HS_Response);									// Read in peer's response. Takes in response.
		} catch (IOException e){
			System.out.println("Unable to read handshake response.");
		}
		output.flush();
		
		//Check info_hash and peer_id of response
		byte[] response_hash = null;
		response_hash = java.util.Arrays.copyOfRange(HS_Response, 28, 48);		//copy info_hash section of handshake response into its own byte array
		
		//If info_hash is not the one client is currently serving, sever the connection.
		if(!java.util.Arrays.equals(ti.info_hash.array(), response_hash)){
			s.close();
			input.close();
			output.close();
			return false;
		}
		return true;
	}
	
	public static byte[] getBitfield(Socket s, int hashesLength) throws IOException {
		byte[] bitfield = null;
		byte msgID;
		
		// Open IO Streams
		DataInputStream input = new DataInputStream(s.getInputStream());

		try {
			input.readInt();
			msgID = input.readByte();
			if(msgID == ((byte) Message.BITFIELD)){
				// Get Bitfield
				bitfield = new byte[hashesLength];
				System.out.println(input.read(bitfield, 0, 55));
			} else {
				System.out.println("ERROR: LOST A PEER MESSAGE");
			}
		} catch (IOException e){
			System.out.println("Unable to read bitfield response.");
		}
		
		return bitfield;
	}
}
