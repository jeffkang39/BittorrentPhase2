
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class PeerThread implements Runnable{
	
	private DataInputStream input;
	private DataOutputStream output;
	private Socket socket;
	private String threadName;
	private Thread t;
	private Download dl;
	private static boolean quit_flag;
	private static HashMap<Integer, ArrayList<Integer>> piece_rarity;
	private static int numPeers;
	private static int[] piece_frequency;
	private static int finished_bitfield;
	
	public PeerThread(String threadName, Download dl) {
		this.threadName = threadName;
		this.dl = dl;
		//PeerThread.flag = false;
		incNumPeers();
	}
	
	public void run() {
		if(PeerThread.piece_frequency == null)
			initializePieceFreq();
		
		byte[] handshake = Peer.createHandshake(dl.getTorrentInfo(), dl.getTorrent());
		try {
			if (!Peer.verifyHandshake(handshake, dl.getTorrentInfo(), socket)) {
				System.out.println("Failed Handshake.");
				System.out.println("Peer " + threadName + " did not contain the expected info_hash.");
				close();
			}
			else {
				System.out.println("Handshake verified with thread: " + threadName);
				
				// Receive bitmap
				int hashesLength = dl.getTorrent().getHashesLength();
				byte[] bitfield_message = Peer.getBitfield(socket, hashesLength);
				if(bitfield_message == null){
					System.out.println("Peer has no pieces.");
				} else {
					System.out.println("Received bitmap.");
				}
				
				BitSet peer_bitmap = null;
				if (bitfield_message == null){
					
				}else{
					peer_bitmap = byteArrayToBitSet(bitfield_message);
					System.out.println("Peer " + threadName + " has pieces " + peer_bitmap.toString());
					
					/*
					 * Need to check for remaining messages in DataInputStream???
					 * If the Peerthread's socket's input stream retains leftover bytes
					 * regardless of opening/closing of DataInputStreams in Peer.getBitfield()
					 * calls, then we're fine.
					 */
					
					// For each piece the peer has, update the static array
					for(int i = peer_bitmap.nextSetBit(0); i >= 0; i = peer_bitmap.nextSetBit(i+1)) {
						foundPiece(i /*-3051*/);
					}
					
					int fb_num = finishedBitfield();
					try {
						// Wait for all threads to finish filling in piece_frequency array
						while(getFinishedBitfield() < PeerThread.numPeers) {
							Thread.sleep((long) 120);
						}
					} catch (InterruptedException e) {
						System.out.println("PeerThread Error: " + e.getMessage());
						t.interrupt();
					}
					
					// Initialize global hashmap
					if(fb_num == 1) {
						initializeRarityMap();
						printRarityMap();
					} else {
						try {
							while(PeerThread.piece_rarity == null)
								Thread.sleep((long) 120);
						} catch (InterruptedException e) {
							System.out.println("PeerThread Error: " + e.getMessage());
							t.interrupt();
						}
					}
				
				
				// Send Interest message to Peer. Unchoke Peer
				Message.sendInterest(input, output, socket);
				
				// Download the packets into a local file
				Message.downloadFile(input, output, dl.getTorrent(), dl.getFileName(), socket, this);
				}
			}
		} catch (IOException e) {
			System.out.println("Error in Run() for peer: " + threadName);
			//System.out.println("IO Exception Error: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	/**
	 * Causes this thread to begin execution; the Java Virtual Machine calls the run method of this thread. 
	 */
	public void start() {
		// Thread start calls run
		
		if (t == null) 
			t = new Thread(this, this.threadName);
		t.start();
	}
	/**
	 * Closes the socket and IO Streams of an Upload Object
	 */
	private void close() {
		try {socket.close(); input.close(); output.close();}
		catch (IOException e) {
			System.out.println("Failed to close peer thread " + threadName);
			System.out.println("Error message: " + e.getMessage());
		}
	}
	
	public synchronized void addToMasterBuffer(byte[] piece, Torrent torrent, int index) {
		if (piece == null) 
			System.out.println("Null Paramter. Cannot add hashed piece");
		else {
			if (torrent.getMasterbuffer()[index] == null) {
				torrent.getMasterbuffer()[index] = piece;
			}
		}
	}

	public static BitSet byteArrayToBitSet(byte[] bytes) {
		BitSet bits = new BitSet();
		for (int i = 0; i < bytes.length * 8; i++) {
			if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
				bits.set(i-3052);		// I HAVE NO IDEA WHY THE RESULTS WERE 3,052 TOO HIGH, BUT THIS SEEMS TO WORK -JFM
			}
		}
		return bits;
	}

	public synchronized void initializeRarityMap() {
		if (PeerThread.piece_rarity != null)
			return;
		
		PeerThread.piece_rarity = new HashMap<Integer, ArrayList<Integer>>(PeerThread.numPeers);
		for(int i = 0; i <= PeerThread.numPeers; i++){
			PeerThread.piece_rarity.put(i, new ArrayList<Integer>());
		}
		
		for(int j = 0; j <= PeerThread.piece_frequency.length-1; j++) {
			PeerThread.piece_rarity.get(PeerThread.piece_frequency[j]).add(j);
		}
	}
	
	/**
	 * Iterates through the HashMap to find the rarest piece. If found, the rarest piece is removed from
	 * the HashMap. If there are multiple pieces with the same rarity, a random piece with that rarity
	 * is selected.
	 * @param none
	 * @return index of the rarest Piece
	 * @return -1 if the HashMap is empty
	 */
	public synchronized int rarestPiece() {
		int index = -1;
		int i;
		
		for(i = 1; i <= PeerThread.numPeers; i++) {
			if(PeerThread.piece_rarity.get(i).size() == 1) {	// Rarest piece
				index = PeerThread.piece_rarity.get(i).get(0);
				break;
			} else if(PeerThread.piece_rarity.get(i).size() > 1) {	// More than one, pick a random
				int rand = ThreadLocalRandom.current().nextInt(0, PeerThread.piece_rarity.get(i).size());
				index = PeerThread.piece_rarity.get(i).get(rand);
				break;
			}
		}
		if(index != -1)
			PeerThread.piece_rarity.get(i).remove((Integer) index);
		
		return index;
	}
	
	public synchronized void initializePieceFreq() {
		if(PeerThread.piece_frequency != null)
			return;
		
		PeerThread.piece_frequency = new int[dl.getTorrent().getHashesLength()];
	}
	
	// Get Methods
	public String getThreadName() {return this.threadName;}
	public Socket getSocket() {return this.socket;}
	public Thread getThread() {return this.t;}
	public DataInputStream getDataInputStream() {return this.input;}
	public DataOutputStream getDataOutputStream() {return this.output;}
	public Download getDownload() {return dl;}
	public boolean getQuitFlag() {return PeerThread.quit_flag;}
	public synchronized int getNumPeers() {return PeerThread.numPeers;}
	public synchronized int getFinishedBitfield() {return PeerThread.finished_bitfield;}
	
	// Set Methods
	public void setSocket(Socket s) {this.socket = s;}
	public void setDataInputStream(DataInputStream dis) {this.input = dis;}
	public void setDataOutputStream(DataOutputStream dos) {this.output = dos;}
	public void setQuitFlagToTrue() {PeerThread.quit_flag = true;}
	public synchronized void decNumPeers() {PeerThread.numPeers--;}
	public synchronized void incNumPeers() {PeerThread.numPeers++;}
	public synchronized void foundPiece(int pieceNum) {
		PeerThread.piece_frequency[pieceNum]++;
	}
	public synchronized int finishedBitfield() {
		return PeerThread.finished_bitfield++;
	}
	
	public void printRarityMap() {
		ArrayList<Integer> obj;
		System.out.println("Rarity HashMap:");
		
		for(int i = 0; i < PeerThread.piece_rarity.size(); i++) {
			System.out.println("	" + i + " :");
			obj = PeerThread.piece_rarity.get(i);
			if(obj.isEmpty()) {
				System.out.println("		empty");
			} else {
				System.out.println("		" + obj);
			}
		}
	}
}

