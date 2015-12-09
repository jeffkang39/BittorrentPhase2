/**
 * Brian Yoo (bgy2 | 140007707)
 * Jeffrey Kang (jk976 | 13900087)
 * Jarrett Mead (jfm168 | 143008288)
 * 
 * CS 352: Internet Technology
 * BitTorrent Client | Phase 1
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import GivenTools.ToolKit;
import GivenTools.TorrentInfo;
import GUI.Gui;

public class Message {
	
	final static int AM_CHOKING = -6;
	final static int AM_INTERESTED = -5;
	final static int PEER_CHOKING = -4;
	final static int PEER_INTERESTED = -3;
	
	final static int NO_MSG = -2;
	final static int KEEP_ALIVE = -1;
	final static int CHOKE = 0;
	final static int UNCHOKE = 1;
	final static int INTERESTED = 2;
	final static int UNINTERESTED = 3;
	final static int HAVE = 4;
	final static int BITFIELD = 5;
	final static int REQUEST = 6;
	final static int PIECE = 7;
	final static byte[] MSG_UNCHOKE = generate_message(UNCHOKE);
	static GuiThread guiThread = null;
	
	public static void sendInterest(DataInputStream input, DataOutputStream output, Socket s) throws IOException {
		// Initialize variables
		byte[] peer_message = new byte[200];
		boolean isUnchoked = true;
		s.setSoTimeout(5000);													// Quit if client listens for 5 seconds
		
		// REPEAT INTEREST UNTIL UNCHOKE RESPONSE
		while (true) {
			try {
				output.write(generate_message(INTERESTED)); output.flush();		// Send Interest Message
				input.read(peer_message);										// Obtain the message from the peer
				isUnchoked = true;
				for (int i = 0; i < 5; i++) {
					if (!(peer_message[i] == MSG_UNCHOKE[i]))
						isUnchoked = false;
				}
				if (isUnchoked)
					break;
			} catch (SocketTimeoutException ste) { System.out.println("Socket Timeout Error. Peer took too long to respond"); break; }
		}
	}

	public static void downloadFile(DataInputStream input,
			DataOutputStream output, Torrent torrent, String fileName,
			Socket s, PeerThread thread) throws IOException{
		// Send the request message
		int index = 0;
		int begin = 0;		// Always 0
		int piecelen;
		int filelen = torrent.getFileLength();
		String event;
		int remaining;
		int hashlen = torrent.getHashesLength();
		URL newURL;
		HttpURLConnection trackerContactConnection;
		s.setSoTimeout(0);
		//byte[] piece = new byte[piecelen];
		FileOutputStream fos = new FileOutputStream(fileName);
		
		while ( thread.getDownload().getRemaining() > 0 && thread.getQuitFlag() == false ) {
			System.out.println("Remaining: " + thread.getDownload().getRemaining());
			System.out.println("" + thread.getDownload().getRemaining());
			System.out.println("" + torrent.getFileLength());
			double td = ((1 - (double)thread.getDownload().getRemaining()/(double)torrent.getFileLength())* 100);
			
			System.out.println(td);
			guiThread.getGui().updateProgressBar((int)td);
			guiThread.getGui().setEditorPane(thread.getDownload().getRemaining(), thread.getDownload().getDownloaded(), td);
			
			//rb.updateProgressBar(50);
			index = thread.rarestPiece();
			if(index == -1) {		// No more pieces.
				break;
			} else if(torrent.getMasterbuffer()[index] != null) {	// Already downloaded this piece.
				continue;
			}
			
			while(true) {		// In case an invalid packet is received and we must try to download the same piece again.
				// Check if the piece is the last packet (i.e. with a lesser piece length)
				// Accommodate this disparity with the byte[] you read into.

				thread.getDownload().setLastBlock(false);
				piecelen = torrent.getPieceLength();
				
				if(index == hashlen-1){
					System.out.println("index is equal to hashlen-1");
					thread.getDownload().setLastBlock(true);
					piecelen = (filelen - index*piecelen);
				}
				byte[] piece = new byte[piecelen];
				output.write(generate_message(REQUEST, index, begin, piecelen, null));
				output.flush();
			
				// Get rid of the first 13 bits that consist of the length prefix, msgID, Index, and begin
				// Use the rest of the bits to compare the SHA-1 Hash of the TorrentInfo and SHA-1 convert hashed of the received piece
				input.readInt();
				input.readByte();
				input.readInt();
				input.readInt();
				input.readFully(piece);
				
				// VERIFY
				// Use info hash to compare to the piece we received
				// Convert byte[] piece to a SHA-1 hash first. Then compare.
				ByteBuffer[] piece_hash = torrent.getPieceHashes();
				ByteBuffer indexedPiece = piece_hash[index];
				byte[] temp = indexedPiece.array();	
				if (Arrays.equals(temp, convertToSHA1Hash(piece))) {
					System.out.println("Valid packet " + (index + 1) + "/" + hashlen + " received by thread: " + thread.getThreadName());
					output.write(generate_message(4, index, -1, -1, null));							// Send HAVE message to Peer.
					
				} else {
					System.out.println("Invalid packet received. index = " + index);
					continue;				// Try to download the same block again.
				}
				
				// Determine how much of the file is downloaded, and how much of the file is left
				// change event to start
				event = "started";
				remaining = thread.getDownload().setTorrentFields(piecelen);
				
				// I need to create an update tracker method.
				
				if (remaining == 0){
					event = "completed";
				}
				
				torrent.setEvent(event);
				
				// This updates the tracker v
				// Contact the tracker with the new information
				newURL = torrent.createURL(torrent);
				trackerContactConnection = (HttpURLConnection) newURL.openConnection();
				trackerContactConnection.setRequestMethod("GET");
				
				// Write the packets to a master buffer.
				torrent.addToMasterBuffer(piece, index);
				
				break;							// Try to download a new block.
			}
		}
		
		fos.close();
		input.close();
		output.close();
		thread.getDownload().decrementIndex();
		if(thread.getQuitFlag() == true){
			thread.getSocket().close();
		}
	}
	
	/**
	 * Obtain the SHA-1 hash from the given byte array
	 * @param hashThis
	 * @return
	 */
	public static byte[] convertToSHA1Hash(byte[] hashThis) {
	    try {
	      byte[] hash = new byte[hashThis.length];
	      MessageDigest md = MessageDigest.getInstance("SHA-1");

	      hash = md.digest(hashThis);
	      //md.update(hash);
	      return hash;
	    } catch (NoSuchAlgorithmException nsae) {
	      System.err.println("SHA-1 algorithm is not available...");
	      System.exit(2);
	    }
	    return null;
	  }
	
	/** Returns an array of booleans representing both
	 *  the client's and the peer's choking and interested states.
	 *   
	 * @param
	 * @return boolean[]
	 */
	public static boolean[] generateStateInfo(){
		
		//Initialize the array's elements to connection's initial states (choking and uninterested).
		boolean[] connection_states = new boolean[4];
		connection_states[AM_CHOKING] = true;
		connection_states[AM_INTERESTED] = false;
		connection_states[PEER_CHOKING] = true;
		connection_states[PEER_INTERESTED] = false;
		
		return connection_states;
	}
	
	/**
	 * Returns a byte array containing a message for the peer
	 * @param id 
	 * @return
	 */
	public static byte[] generate_message(int id){
		
		ByteBuffer message;
		
		switch(id){
		case KEEP_ALIVE:
			message = ByteBuffer.allocate(4);
			message.putInt(0);
			break;
		case CHOKE:
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		case UNCHOKE:
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		case INTERESTED:
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			//message.array();
			break;
		case UNINTERESTED:
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		default:
			return null;
		}
		
		return message.array();
	}
	/**
	 * Returns a byte array containing a message for the peer
	 * @param id Message ID
	 * @param index index of the info hash
	 * @param begin offset of the block
	 * @param length size of the packet piece
	 * @param block data
	 * @return
	 */
	public static byte[] generate_message(int id, int index, int begin, int length, byte[] block){
		
		ByteBuffer message;
		
		switch(id){
		case KEEP_ALIVE:
			message = ByteBuffer.allocate(4);
			message.putInt(0);
			break;
		case CHOKE:
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		case UNCHOKE:
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		case INTERESTED:
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		case UNINTERESTED:
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		case HAVE:
			message = ByteBuffer.allocate(9);
			message.putInt(5);
			message.put((byte) id);
			message.putInt(index);
			break;
		/*
		case BITFIELD:
			break;
		*/
		case REQUEST:
			message = ByteBuffer.allocate(17);
			message.putInt(13);
			message.put((byte) id);
			message.putInt(index);
			message.putInt(begin);
			message.putInt(length);
			break;
		case PIECE:
			message = ByteBuffer.allocate(13+length);
			message.putInt(9+length);
			message.put((byte) id);
			message.putInt(index);
			message.putInt(begin);
			message.put(block);
			break;
		default:
			return null;
		}
		
		return message.array();
	}
	
	/**
	 * Reads in the Peer Message and returns a message ID 
	 * @param dis DataInputStream
	 * @return Integer representing the message ID of the Peer Message
	 */
	public static int readPeerMessage(DataInputStream dis) {
		try {
			int length_prefix = dis.readInt();
			byte msgID = dis.readByte();
			//System.out.println("message: " + length_prefix + msgID);
			if (length_prefix == 0 && msgID == ((byte) 0)) {
				return KEEP_ALIVE;				//keep-alive
			} else {
				return msgID;
			}
		} 
		catch (IOException e) {System.out.println("readPeerMessage() IOException error: " + e.getMessage());}
		return NO_MSG;
	}

}
