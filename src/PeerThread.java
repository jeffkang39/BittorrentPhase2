
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import GivenTools.ToolKit;
import GivenTools.TorrentInfo;

public class PeerThread implements Runnable{
	
	private DataInputStream input;
	private DataOutputStream output;
	private Socket socket;
	private String threadName;
	private Thread t;
	private Download dl;
	private static boolean flag;
	
	public PeerThread(String threadName, Download dl) {
		this.threadName = threadName;
		this.dl = dl;
		this.flag = false;
	}
	
	public void run() {
		byte[] handshake = Peer.createHandshake(dl.getTorrentInfo(), dl.getTorrent());
		try {
			if (!Peer.verifyHandshake(handshake, dl.getTorrentInfo(), socket)) {
				System.out.println("Failed Handshake.");
				System.out.println("Peer " + threadName + " did not contain the expected info_hash.");
				close();
			}
			else {
				System.out.println("Handshake verified with thread: " + threadName);
				
				// Send Interest message to Peer. Unchoke Peer
				Message.sendInterest(input, output, socket);
				
				// Download the packets into a local file
				System.out.println("DOWNLOAD FILEsfdf1a");
				Message.downloadFile(input, output, dl.getTorrent(), dl.getFileName(), socket, this);
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
	
	// Get Methods
	public String getThreadName() {return this.threadName;}
	public Socket getSocket() {return this.socket;}
	public Thread getThread() {return this.t;}
	public DataInputStream getDataInputStream() {return this.input;}
	public DataOutputStream getDataOutputStream() {return this.output;}
	public Download getDownload() {return dl;}
	public boolean getBooleanFlag(){return this.flag;}
	
	// Set Methods
	public void setSocket(Socket s) {this.socket = s;}
	public void setDataInputStream(DataInputStream dis) {this.input = dis;}
	public void setDataOutputStream(DataOutputStream dos) {this.output = dos;}
	public void setBooleanFlagToTrue(){this.flag = true;}
}
