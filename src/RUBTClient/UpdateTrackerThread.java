package RUBTClient;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.Timer;
import java.util.logging.Logger;

import GivenTools.TorrentInfo;

public class UpdateTrackerThread implements Runnable{

	private static Thread t;
	private String threadName;
	private Torrent torrent;
	private static boolean running;
	
	public UpdateTrackerThread(Torrent torrent) {
		this.threadName = "update tracker thread";
		this.torrent = torrent;
		UpdateTrackerThread.running = true;
	}

	    public static void terminate() {
	    	
	    	t.interrupt();
	    	running = false;
	        
	    }
	    
	    public void run() {
	        while (running) {
	        	try {
					Thread.sleep((long) 120000);
					if(Thread.interrupted() == true){
						System.out.println("hi");
					}
					// Updates the tracker.
					HttpURLConnection trackerConnection = torrent.getTrackerConnection(torrent);
					DataInputStream dis = new DataInputStream(trackerConnection.getInputStream());
					System.out.println("Successfully update the tracker");
					
					// DID WE ACTUALLY UPDATE THE TRACKER??? -JFM
					
				} catch (InterruptedException | IOException e) {
					System.out.println("UpdateTrackerThread Error: " + e.getMessage());
			    	t.interrupt();
				}
	        }
	    }
	
	/**
	 * Causes this thread to begin execution; the Java Virtual Machine calls the run method of this thread. 
	 */
	public void start() {
		if (t == null) 
			t = new Thread(this, this.threadName);
		t.start();
	}
	
	
	public Thread getThread() {return UpdateTrackerThread.t;}

}
