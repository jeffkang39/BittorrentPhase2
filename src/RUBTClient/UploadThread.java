package RUBTClient;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class UploadThread implements Runnable{
	final static byte[] MSG_UNCHOKE = Message.generate_message(1);
	
	private Thread t;
	private String threadName;
	private Socket socket;
	private Torrent torrent;
	private DataInputStream dis;
	private DataOutputStream dos;
	//private ListenThread LS;
	private int totalUploaded = 0;
	private static boolean flag;
	
	public UploadThread(String threadName, Torrent torrent, Socket s, ListenThread LS) {
		this.threadName = threadName;
		this.socket = s;
		this.torrent = torrent;
		//this.LS = LS;
		UploadThread.flag = true;
		try {
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			System.out.println("Upload thread IO Exception Error: " + e.getMessage());
			close();
		}
	}

	@Override
	public void run() {
		try {
			// Send Have messages to seed
			sendHave(torrent, dos);
			int peerMsgID = Message.readPeerMessage(dis);
			// No message 
			if (peerMsgID == Message.NO_MSG) {
				System.out.println("No response from IP address: " + threadName);
				System.out.println("Closing upload thread..");
			}
			// Interest
			else if (peerMsgID == Message.INTERESTED) {
				System.out.println("Received INTEREST message by IP address: " + threadName);
				
				sendUnchoke();
				while (UploadThread.flag == true) {
					// Wait and try to receive the requests.
					peerMsgID = Message.readPeerMessage(dis);
					if (peerMsgID == Message.REQUEST) {
						int index = dis.readInt();
						int begin = dis.readInt();
						int length = dis.readInt();
						
						// Send requested piece.
						System.out.println("Uploading requested piece to " + threadName + " at index: " + index + ", offset: " + begin);
						if (sendPiece(index, begin, length)){
							// Updates the torrent information for the tracker update.
							int amtUploaded = torrent.getUploaded();
							amtUploaded += length;
							torrent.setUploaded(amtUploaded);
						}
						
						// Update tracker with what you just uploaded
					}
					else if (peerMsgID == Message.UNINTERESTED) {
						System.out.println("NOT INTERESTED message received");
						System.out.println("Closing upload thread..");
						break;
					}
					else {
						System.out.println("Did not receive INTERESTED" + peerMsgID);
						System.out.println("Closing upload thread..");
						break;
					}
				}
			}
			// Not interested
			else if (peerMsgID == Message.UNINTERESTED) {
				System.out.println("Received NOT INTERESTED message by IP address: " + threadName);
				System.out.println("Closing upload thread..");
			}
			// Any other message
			else {
				System.out.println("Did not receive INTEREST message by IP address: " + threadName);
				System.out.println("Closing upload thread..");
			}
		} catch (IOException e){
			System.out.println("Upload Thread, IO Exception error: " + e.getMessage());
			ListenThread.uploadListRemove(threadName);
			close();		
		}
		ListenThread.uploadListRemove(threadName);
		close();
	}
	
	/**
	 * Causes this thread to begin execution; the Java Virtual Machine calls the run method of this thread. 
	 */
	public void start() {
		if (t == null) 
			t = new Thread(this, this.threadName);
		t.start();
	}
	
	/**
	 * Closes the socket and IO Streams of an Upload Object
	 */
	public void toggleFlag(){
		System.out.println("Closing upload threads");
		flag = false;
	}
	private void close() {
		try {socket.close();dis.close();dos.close();} 
		catch (IOException e) {
			System.out.println("Failed to close Upload thread " + threadName);
			System.out.println("Error message: " + e.getMessage());
		}
	}
	/**
	 * Writes a series of have messages to a peer through a socket
	 * @param torrent Torrent object
	 * @param dos DataOutputStream object
	 * @throws IOException
	 */
	private synchronized static void sendHave(Torrent torrent, DataOutputStream dos) throws IOException {
		// So far this just get a synchronized current version of MasterBuffer.
		// And then prints out.
		byte[][] temp = torrent.getMasterbuffer();
		for (int i = 0; i < temp.length; i++) {
			if (temp[i] != null) 
				dos.write(Message.generate_message(4, i, -1, -1, null));
		}
		dos.flush();
	}
	/**
	 * Writes and sends an unchoke message to a peer
	 */
	private void sendUnchoke() {
		byte[] unchoke = Message.generate_message(1);
		try {dos.write(unchoke); dos.flush();}
		catch (IOException e) { System.out.println("Could not send unchoke message");}
	}
	
	/**
	 * Returns a packet of the file correlating to the parameters given.
	 * @param index the index of the piece_hashes of the torrent file
	 * @param begin the offset of the piece
	 * @param length the length of the piece
	 * @return byte array of the piece
	 */
	private boolean sendPiece(int index, int begin, int length) {
		int pieceLength = torrent.getPieceLength();
		byte[] piece = null;
		
		byte [] temp = torrent.getMasterbuffer()[index];
		if ((begin + length) > pieceLength) {
			System.out.println("Could not retreive requested piece. Returning null.");
			return false;
		}
		else {
			piece = Arrays.copyOfRange(temp, begin, length+begin);
			try {
				dos.writeInt(9+length);
				dos.writeByte(7);
				dos.writeInt(index);
				dos.writeInt(begin);
				dos.write(piece);
				dos.flush();
				return true;
			} catch (IOException e) {
				System.out.println("Error. Could not send piece message to seed." + e.getMessage());
				return false;
			}
		}
	}
	
	// Get Methods
	public String getThreadName() {return this.threadName;}
	public int getTotalUploaded() {return this.totalUploaded;}

}
