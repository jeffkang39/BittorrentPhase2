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

public class Message {
	
	final static byte[] MSG_UNCHOKE = generate_message(1);
	final static int AM_CHOKING = 0;
	final static int AM_INTERESTED = 1;
	final static int PEER_CHOKING = 2;
	final static int PEER_INTERESTED = 3;
	final static int KEEP_ALIVE = -1;
	final static int CHOKE = 0;
	final static int UNCHOKE = 1;
	final static int INTERESTED = 2;
	final static int UNINTERESTED = 3;
	final static int HAVE = 4;
	final static int REQUEST = 6;
	final static int PIECE = 7;
	/*
	final static ByteBuffer keep_alive = ByteBuffer.wrap(new byte[] {0, 0, 0, 0}).order(ByteOrder.BIG_ENDIAN);
	final static ByteBuffer choke = ByteBuffer.wrap(new byte[] {0, 0, 0, 1}).order(ByteOrder.BIG_ENDIAN).put(ByteBuffer.wrap(new byte[] {0}));
	//final static ByteBuffer unchoke = ByteBuffer.wrap(new byte[] {0, 0, 0, 1}).order(ByteOrder.BIG_ENDIAN).put(ByteBuffer.wrap(new byte[] {1}));
	final static ByteBuffer unchoke = ByteBuffer.wrap(new byte[] {0, 0, 0, 1, 1});
	//final static ByteBuffer interested = ByteBuffer.wrap(new byte[] {0, 0, 0, 1}).order(ByteOrder.BIG_ENDIAN).put(ByteBuffer.wrap(new byte[] {2}));
	final static ByteBuffer interested = ByteBuffer.wrap(new byte[] {0, 0, 0, 1, 2});
	final static ByteBuffer not_interested = ByteBuffer.wrap(new byte[] {0, 0, 0, 1}).order(ByteOrder.BIG_ENDIAN).put(ByteBuffer.wrap(new byte[] {3}));
	final static ByteBuffer have_prefix = ByteBuffer.wrap(new byte[] {0, 0, 0, 5}).order(ByteOrder.BIG_ENDIAN).put(ByteBuffer.wrap(new byte[] {4}));
	final static ByteBuffer request_prefix = ByteBuffer.wrap(new byte[] {0, 0, 1, 3}).order(ByteOrder.BIG_ENDIAN).put(ByteBuffer.wrap(new byte[] {6}));
	*/
	
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
		int piecelen = torrent.getPieceLength();
		int lgpiecelen = torrent.getPieceLength();
		int smlpiecelen = torrent.getSmallPieceLength();
		int filelen = torrent.getFileLength();
		String event;
		int downloaded;
		int remaining;
		int hashlen = torrent.getHashesLength();
		URL newURL;
		HttpURLConnection trackerContactConnection;
		s.setSoTimeout(0);
		//byte[] piece = new byte[piecelen];

		FileOutputStream fos = new FileOutputStream(fileName);
		
		while ( (index = thread.getDownload().getIndex()) < hashlen && thread.getBooleanFlag() == false ) {
			while(true) {
				byte[] piece = new byte[piecelen];
				// Check if the piece is the last packet (i.e. with a lesser piece length)
				// Accommodate this disparity with the byte[] you read into.
			
				/* 
				 * We were having trouble downloading the last block because our request message
				 * was using "remaining" for the length field. For some reason, when the second
				 * thread starts running, "remaining" starts having problems. This caused us to
				 * request a full piece when less than 2^15 bytes are remaining.
				 * -Jarrett
				 * 
				if(remaining < piecelen){
					//System.out.println("LAST PIECE");
					piece = new byte[remaining];
					output.write(generate_message(REQUEST, index, begin, remaining, null));
				 */
				if(index == hashlen-1){
					thread.getDownload().setLastBlock();
					//System.out.println("LAST BLOCK: thread = " + thread.getThreadName() + ", piece = " + (index+1));
					piece = new byte[(filelen - index*piecelen)];
					output.write(generate_message(REQUEST, index, begin, (filelen - index*piecelen), null));
					piecelen = (filelen - index*piecelen);
				} else {
					output.write(generate_message(REQUEST, index, begin, piecelen, null));
				}
				output.flush();
			
				// Get rid of the first 13 bits that consist of the length prefix, msgID, Index, and begin
				// Use the rest of the bits to compare the SHA-1 Hash of the TorrentInfo and SHA-1 convert hashed of the received piece
				input.readInt();
				input.readByte();
				input.readInt();
				input.readInt();
				input.readFully(piece);
				
				//System.out.println("heeeeeeeeeeeeeeeeey"  + s.getLocalAddress() + s.getLocalSocketAddress());
				//PeerThread.incrementIndex();
				//System.out.println(threadName + " " + PeerThread.getIndex());
				
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
					System.out.println("Invalid packet received");
					continue;				// Try to download the same block again.
				}
				
				// Determine how much of the file is downloaded, and how much of the file is left
				// change event to start
				event = "started";
				remaining = thread.getDownload().setTorrentFields(event, piecelen);
				if(thread.getDownload().lastBlock()){
					//System.out.println("Thread: " + thread.getThreadName() + ", remaining: " + remaining + ", Should be1: " + (filelen - ((thread.getDownload().getIndexNoInc()-2)*lgpiecelen + smlpiecelen)));
				} else {
					//System.out.println("Thread: " + thread.getThreadName() + ", remaining: " + remaining + ", Should be2: " + (filelen - ((thread.getDownload().getIndexNoInc()-1)*lgpiecelen)));
				}
				
				// I need to create an update tracker method.
				
				if (remaining == 0){
					//System.out.println("FINAL BLOCK: thread = " + thread.getThreadName() + ", piece = " + (index+1));
					event = "completed";
				}
				
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
		if(thread.getBooleanFlag() == true){
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
		case -1: //keep-alive
			message = ByteBuffer.allocate(4);
			message.putInt(0);
			break;
		case 0: //choke
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		case 1: //unchoke
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		case 2: //interested
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			//message.array();
			break;
		case 3: //uninterested
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
		case -1: //keep-alive
			message = ByteBuffer.allocate(4);
			message.putInt(0);
			break;
		case 0: //choke
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		case 1: //unchoke
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		case 2: //interested
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		case 3: //uninterested
			message = ByteBuffer.allocate(5);
			message.putInt(1);
			message.put((byte) id);
			break;
		case 4: //have
			message = ByteBuffer.allocate(9);
			message.putInt(5);
			message.put((byte) id);
			message.putInt(index);
			break;
		case 6: //request
			message = ByteBuffer.allocate(17);
			message.putInt(13);
			message.put((byte) id);
			message.putInt(index);
			message.putInt(begin);
			message.putInt(length);
			break;
		case 7: //piece
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
			if (length_prefix == 0 && msgID == ((byte) 0))
				return 0;				//keep-alive
			switch(msgID) {
				case 0: return 0;	//choke
				case 1: return 1;	//unchoke
				case 2: return 2;	//interested
				case 3: return 3;	//uninterested
				case 4: return 4;	//have
				case 5: return 5;	//bitfield
				case 6: return 6;	//request
				case 7: return 7;	//piece
			}
		} 
		catch (IOException e) {System.out.println("readPeerMessage() IOException error: " + e.getMessage());}
		return -1;
	}

}
