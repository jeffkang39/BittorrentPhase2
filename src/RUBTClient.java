/**
 * Brian Yoo (bgy2 | 140007707)
 * Jeffrey Kang (jk976 | 13900087)
 * Jarrett Mead (jfm168 | 143008288)
 * 
 * CS 352: Internet Technology
 * BitTorrent Client | Phase 1
 */

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;

import GivenTools.TorrentInfo;

public class RUBTClient {	
	  
	public static void main(String args[]) throws Exception {
		

		//check infohash to see			
		
		// Error Check
		if (args.length != 2) {
			System.out.println("Invalid number of arguments");
			System.out.println("Correct format: java -cp . <torrent file name> <file name to be saved>");
			return;
		}
		// Take in the first two arguments
		String torrentName = args[0];
		String fileName = args[1];

		// Obtain the metadata from the .torrent file
		TorrentInfo ti = Torrent.getTorrentInfo(torrentName);		// Get the Torrent file metadata.
		Torrent torrent = Torrent.createTorrent(ti);
		torrent.setEvent("started");
		
		 File varTmpDir = new File("RUBTClient.ser"); 
		 boolean exists = varTmpDir.exists();
		 Save Load = null;
		
		// Create Download object for single download (for now).
			Download dl = new Download(fileName, ti, torrent);
			
		boolean finished = false;
		 if(exists == true){
				
				FileInputStream fi = new FileInputStream("RUBTClient.ser");
				ObjectInputStream in = new ObjectInputStream(fi);
				Load = (Save) in.readObject();		
				
				in.close();
				fi.close();
				Thread.sleep(5000);
				//check infohash
				finished = Load.getFinish();
				if(Arrays.equals(Load.getInfoHash(), torrent.getInfoHash())){
					dl.resumeIndex(Load.getIndex());
					dl.resumeDownloaded(Load.getDownloaded());
					dl.resumeRemaining(Load.getRemaining());
					
					torrent.resumeDownloaded(Load.getDownloaded());
					torrent.resumeLeft(Load.getRemaining());
					torrent.resumeMasterbuffer(Load.getMasterBuffer());
				}else{//if torrent info has does not match
					
				}
		 }		
		
		// Open connection, Send GET request to tracker
		HttpURLConnection trackerConnection = torrent.getTrackerConnection(torrent);
		DataInputStream dis = new DataInputStream(trackerConnection.getInputStream());
		//trackerConnection.setRequestMethod("GET");
		
		// MULTITHREADING 
		/* 
		 * 1. Obtain a list of our desired peer(s)
		 * 2. Iterate through that list of peer(s) and open a connection for each
		 * 3. Transfer data (i.e. download and upload) for each peer
		 */
		ArrayList<PeerThread> threadList = new ArrayList<PeerThread>();
		ArrayList<HashMap<ByteBuffer, Object>> peerList = Peer.getTargetPeerList(trackerConnection);
		for(int i = 0; i < peerList.size(); i++)
		{
			HashMap<ByteBuffer, Object> eachPeer = (HashMap<ByteBuffer, Object>) peerList.get(i);
			String currPeerID = Peer.objectBBToString(eachPeer.get(Peer.PEER_ID));
			
			PeerThread t = new PeerThread(currPeerID, dl);
			threadList.add(t);
			threadList.get(i).setSocket(Peer.openTCPConnection(eachPeer));
		}
		
		// Create IO streams.
		ThreadHandler.createIOStreams(threadList);
		
		// Start Threads
		ThreadHandler.startThreads(threadList, torrent, ti, dl);								// Peer Threads
		/*ListenThread lt = new ListenThread(torrent.getPort(), ti, torrent);				// Listen Thread
		lt.start();
		
		UserInputThread uit = new UserInputThread(dl,threadList, lt,torrent);
		uit.start();*/
		// TrackerUpdateThread.startUpdateTrackerThread
		
		// Join loop, wait till all peer threads are done running
		for (int i = 0; i < threadList.size(); i++) {
			threadList.get(i).getThread().join();
		}
		// Create FOS and get Master Buffer
		FileOutputStream fos = new FileOutputStream(fileName);
		byte[][] lastarr = torrent.getMasterbuffer();
		
		// Write to File
		if(finished == true){
			Save s = new Save(dl.getIndexNoInc(), torrent.getDownloaded(),torrent.getMasterbuffer(), torrent.getInfoHash(), dl.getRemaining(), true);
			try{
				FileOutputStream fos1 = new FileOutputStream("RUBTClient.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fos1);
				oos.writeObject(s);
				oos.close();
			}catch(IOException i){
				i.printStackTrace();		
			}
			for (int i = 0; i < dl.getIndexNoInc(); i++){
				fos.write(lastarr[i]);			
			}
			
			
		}
		else if(threadList.get(0).getBooleanFlag() == true){ //if q is HIT
		
			Save s = new Save(dl.getIndexNoInc(), torrent.getDownloaded(),torrent.getMasterbuffer(), torrent.getInfoHash(), dl.getRemaining(), false);
			try{
				FileOutputStream fos1 = new FileOutputStream("RUBTClient.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fos1);
				oos.writeObject(s);
				oos.close();
			}catch(IOException i){
				i.printStackTrace();		
			}
			
			for (int i = 0; i < dl.getIndexNoInc(); i++){
				fos.write(lastarr[i]);
				
				
			}
		}else{//finished downloading
			
			Save s = new Save(dl.getIndexNoInc(), torrent.getDownloaded(),torrent.getMasterbuffer(), torrent.getInfoHash(), dl.getRemaining(), true);
			try{
				FileOutputStream fos1 = new FileOutputStream("RUBTClient.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fos1);
				oos.writeObject(s);
				oos.close();
			}catch(IOException i){
				i.printStackTrace();		
			}
			
			for (int i = 0; i < lastarr.length; i++){
				
				fos.write(lastarr[i]);
			}
		}
		fos.close();
		
	}
	
	public static void printByteArray(byte[] b) {
			if (b == null)
				return;
			for(int q=0; q<b.length; q++){
				System.out.println("Element " + q + ": " + b[q]);
			}
	}
	
}
