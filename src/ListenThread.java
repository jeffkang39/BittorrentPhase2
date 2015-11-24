
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import GivenTools.TorrentInfo;

public class ListenThread implements Runnable{
	
	final static int CHOKE = 0;
	final static int UNCHOKE = 1;
	final static int INTERESTED = 2;
	final static int UNINTERESTED = 3;
	final static int HAVE = 4;
	final static int REQUEST = 6;
	final static int PIECE = 7;

	private int port;
	private Thread t;
	private String threadName;
	private TorrentInfo ti;
	private Torrent torrent;
	private ServerSocket serverSocket;
	private boolean flag;
	//private Socket socket;
	//private DataInputStream is;
	//private DataOutputStream os;
	private static ArrayList<UploadThread> uploadList;
	
	
	public ListenThread(int port, TorrentInfo ti, Torrent torrent) throws IOException{ 
		this.threadName = "listening thread";
		this.ti = ti;
		this.torrent = torrent;	
		this.uploadList = new ArrayList<UploadThread>();
		this.port = port;
		this.serverSocket = new ServerSocket(port);
		this.flag = true;
	}

	@Override
	public void run() {
		Socket socket = null;
		while(flag == true){
			try {
				System.out.println("Awaiting incoming connection . . .");
				socket = serverSocket.accept();
				System.out.println("Successful upload connection to seed ip: " + socket.getInetAddress());
				
				// Create Handshake
				byte[] handshake = Peer.createHandshake(this.ti, this.torrent); 
				
				// Failed handshake. Sever the connection. Try another accept.
				if (!Peer.verifyHandshake(handshake, ti, socket)) {
					System.out.println("Failed handshake with incoming connection of IP address: " + socket.getInetAddress());
					socket.close();
					
				// Otherwise Continue.
				} else {
					System.out.println("Successfully verified handshake with incoming connection of IP address: " + socket.getInetAddress());
					
					String IPaddress = socket.getInetAddress().toString();
					UploadThread uThread = new UploadThread(IPaddress, torrent, socket, this);
					// Stupid needless method that checks for two of the same ip trying to connect.
					if (!uploadListContains(uThread)) {
						uThread.start();
						uploadList.add(uThread);
					}
				}
			} catch (IOException e) {

				System.out.println("Listen thread IO Exception Error:" + e.getMessage());
			}
		}
		
	}
	
	//starts run
	public void start(){
		if (t == null) 
			t = new Thread(this, this.threadName);
		t.start();
	}
	
	public void close() throws IOException{
		this.serverSocket.close();
	//	System.out.println("sdfsfsfsfssfsfsd");
		this.flag = false;
	}
	
	public static void startListenThread(int port, TorrentInfo ti, Torrent torrent) throws IOException {
		ListenThread lt = new ListenThread(port, ti, torrent);				// Listen Thread
		lt.start();
	}
	
	private boolean uploadListContains(UploadThread uThread) {
		if (uThread == null) return false;
		
		for (int i = 0; i < uploadList.size(); i++) {
			if (uploadList.get(i).getThreadName().equals(uThread.getThreadName())) {
				return true;
			}
		}
		return false;
	}
	
	public static void uploadListRemove(String threadName) {
		if (threadName.equals("") || threadName.isEmpty()) return;
		
		for (int i = 0; i < uploadList.size(); i++) {
			if (uploadList.get(i).getThreadName().equals(threadName)) {
				uploadList.remove(i);
			}
		}
	}
	
	// Get Method
	public int getPort() {return this.port;}
	public TorrentInfo getTorrentInfo() {return this.ti;}
	public Torrent getTorrent() {return this.torrent;}
	public Thread getThread() {return this.t;}
	public ArrayList<UploadThread> getUploadList() {return this.uploadList;}
	
	// Set Method
	public void setPort(int port) {this.port = port;}
	public void setTorrentInfo(TorrentInfo ti) {this.ti = ti;}
	public void setTorrent(Torrent torrent) {this.torrent = torrent;}
}
