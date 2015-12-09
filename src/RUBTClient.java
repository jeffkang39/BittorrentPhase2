/**
 * Brian Yoo (bgy2 | 140007707)
 * Jeffrey Kang (jk976 | 13900087)
 * Jarrett Mead (jfm168 | 143008288)
 * 
 * CS 352: Internet Technology
 * BitTorrent Client | Phase 3
 */

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import GivenTools.TorrentInfo;
import GUI.Gui;

public class RUBTClient {	
	
   private static JFrame frame;
   private static JLabel headerLabel;
   private static JLabel statusLabel;
   private static JPanel controlPanel;
   
   private static JEditorPane editorPane1;
   private static JEditorPane editorPane2;
   private static JEditorPane editorPane3;
   private static JEditorPane editorPane4;
   private static JEditorPane editorPane5;
   private static JEditorPane editorPane6;
   static JProgressBar jb;
   private static String arg;
   public static String args[];
   
   //instantiate gui in the main method, everything in rubtclient becomes a method.
   
   
   public static void main(String args1[]) throws Exception {
		
	   args = args1;
	   RUBTClient RB = new RUBTClient();
	   System.out.println("hi");
		
	}
   public RUBTClient() throws ClassNotFoundException, IOException, InterruptedException{
	      GuiThread gui = new GuiThread();
	      gui.start();
	      Message.guiThread = gui;
	      boolean b = true;
	      while(b){
	    	  Thread.sleep(100);
	    	  
	    	  if(gui.getGui().getFlag() == true){
	    		  System.out.println("Starting");
	    		  b = false;
	    		  start();
	    	  }
	      }
   }
   public static void start() throws IOException, ClassNotFoundException, InterruptedException{
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
		Message.guiThread.getGui().setStrings(torrentName, fileName);
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
		//trackerConnection.setRequestMethod("GET");
		
		// MULTITHREADING 
		/* 
		 * 1. Obtain a list of our desired peer(s)
		 * 2. Iterate through that list of peer(s) and open a connection for each
		 * 3. Transfer data (i.e. download and upload) for each peer
		 */
		ArrayList<PeerThread> threadList = new ArrayList<PeerThread>();
		ArrayList<HashMap<ByteBuffer, Object>> peerList = Peer.getTargetPeerList(trackerConnection);
		
		int pi = 0;
		int pj = 0;
		while(pi < peerList.size())
		{
			HashMap<ByteBuffer, Object> eachPeer = (HashMap<ByteBuffer, Object>) peerList.get(pi);
			String currPeerID = Peer.objectBBToString(eachPeer.get(Peer.PEER_ID));
			
			PeerThread t = new PeerThread(currPeerID, dl);
			
			if(Peer.openTCPConnection(eachPeer) != null){
				threadList.add(t);				
				threadList.get(pj).setSocket(Peer.openTCPConnection(eachPeer));				
				pj++;
			}
			pi++;
		}
		
		// Create IO streams.
		ThreadHandler.createIOStreams(threadList);
		
		// Start Threads
		ThreadHandler.startThreads(threadList, torrent, ti, dl);							// Peer Threads
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
		else if(threadList.get(0).getQuitFlag() == true){ //if q is HIT
			System.out.println("QUIT@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
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
		}else{		//finished downloading
			
			Save s = new Save(dl.getIndexNoInc(), torrent.getDownloaded(),torrent.getMasterbuffer(), torrent.getInfoHash(), dl.getRemaining(), true);
			try{
				FileOutputStream fos1 = new FileOutputStream("RUBTClient.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fos1);
				oos.writeObject(s);
				oos.close();
			}catch(IOException i){
				i.printStackTrace();		
			}
			
			if(lastarr != null){
				for (int i = 0; i < lastarr.length; i++){
					
					fos.write(lastarr[i]);
				}
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
	


	
	private static void prepareGUI(){
      frame = new JFrame("BitTorrent");
      headerLabel = new JLabel("BitTorrent", JLabel.CENTER);
    
      headerLabel.setFont(new Font("Serif", Font.PLAIN, 20));
      frame.add(headerLabel);
      frame.setSize(600,620);
      frame.setLocation(700,200);
      frame.setLayout(new GridLayout(0,1));
      frame.setResizable(true);
      
      frame.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent windowEvent){
            System.exit(0);
         }
      });
        
      statusLabel = new JLabel("",JLabel.CENTER);

      statusLabel.setSize(250,100);

      controlPanel = new JPanel();
      controlPanel.setLayout(new FlowLayout());

      frame.add(controlPanel);
      frame.add(statusLabel);
      frame.setVisible(true);  
   }
   
   private static void showProgressBar(){
	   jb = new JProgressBar();
	   jb.setVisible(true);
	   controlPanel.add(jb);
   }
   private static void showEditorPane(){
	   
	   editorPane1 = new JEditorPane();
	   editorPane1.setSize(550, 100);
	   editorPane2 = new JEditorPane();
	   editorPane2.setSize(550, 100);
	   editorPane3 = new JEditorPane();
	   editorPane3.setSize(550, 100);
	   editorPane4 = new JEditorPane();
	   editorPane4.setSize(550, 100);
	   editorPane5 = new JEditorPane();
	   editorPane5.setSize(550, 100);
	   editorPane6 = new JEditorPane();
	   editorPane6.setSize(550, 100);		   
	   
	   setUneditable();
	   
	   controlPanel.add(editorPane1);
	   controlPanel.add(editorPane2);
	   controlPanel.add(editorPane3);
	   controlPanel.add(editorPane4);
	   controlPanel.add(editorPane5);
	   controlPanel.add(editorPane6);
	   
	   frame.setVisible(true);
	   
   }
   private static void showButton(){

      //headerLabel.setText("Bittorent"); 
	  
      JButton startButton = new JButton("Start");        
      JButton quitButton = new JButton("Quit");
      
      startButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            statusLabel.setText("Starting...");
            setEditable();
            editorPane1.setText("hello");
            setUneditable();
            jb.setVisible(true);
            updateProgressBar(10);
            try {
				start();
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

         }          
      });

      	quitButton.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
            statusLabel.setText("Quitting...");
         }
      });

      controlPanel.add(startButton);
      controlPanel.add(quitButton);
      frame.setVisible(true);  
   }
   
   public static void setEditable(){
	   editorPane1.setEditable(true);
	   editorPane2.setEditable(true);
	   editorPane3.setEditable(true);
	   editorPane4.setEditable(true);
	   editorPane5.setEditable(true);
	   editorPane6.setEditable(true);
	   
   }
   
   public static void setUneditable(){
	   editorPane1.setEditable(false);
	   editorPane2.setEditable(false);
	   editorPane3.setEditable(false);
	   editorPane4.setEditable(false);
	   editorPane5.setEditable(false);
	   editorPane6.setEditable(false);
	   
   }
   
   public static void updateProgressBar(int progress) {		   
	  jb.setValue(progress);
	  System.out.println("progress.... " + progress);
   }	
   


}
