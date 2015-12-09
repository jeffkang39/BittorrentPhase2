
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class UserInputThread implements Runnable{
	private Scanner sc;
	private Thread t;
	private ArrayList<DataInputStream> inputList;
	private ArrayList<DataOutputStream> outputList;
	//private Download dl;
	private ArrayList<PeerThread> threadList;
	private ListenThread lt;
	private Torrent torrent;
	
	public UserInputThread(Download dl, ArrayList<PeerThread> threadList, ListenThread lt, Torrent torrent) {
		sc = new Scanner(System.in);
		inputList = new ArrayList<DataInputStream>();
		outputList = new ArrayList<DataOutputStream>();
		//this.dl = dl;
		this.threadList = threadList;
		this.lt = lt;
		this.torrent = torrent;
	}
	
	public void run() {
		String quitString1 = new String("q");
		String quitString2 = new String("Q");
		System.out.println("User inputthreadQDFSFSDSFS " + Message.guiThread.getGui().getQuitFlag());
		
		while(true){
			if(!sc.hasNext())
				continue;
			String nextString = sc.next();
			System.out.println("User ssssinputthreadQDFSFSDSFS " + Message.guiThread.getGui().getQuitFlag());
			if(nextString.equals(quitString1) || nextString.equals(quitString2) || Message.guiThread.getGui().getQuitFlag() == true){
			//	System.out.println(dl.getIndexNoInc()); //save this index
				System.out.println("User ssssinputthreadQDFSFSDSFS QUITTTT" + Message.guiThread.getGui().getQuitFlag());
				System.out.println("Quit Success!");
				triggerFlag();
				UpdateTrackerThread.terminate();
				try {
					lt.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for (int i = 0; i < lt.getUploadList().size(); i++) {
					
					lt.getUploadList().get(i).toggleFlag();
					
				}
				
				torrent.setEvent("stopped");
				break;
				
			//	System.exit(0);
			}
		}
	}
	
	/**
	 * Causes this thread to begin execution; the Java Virtual Machine calls the run method of this thread. 
	 */
	public void start() {
		// Thread start calls run
		
		if (t == null) 
			t = new Thread(this);
		t.start();
	}
	
	public void addInputStream(DataInputStream s){
		inputList.add(s);
	}
	
	public void addOutputStream(DataOutputStream s){
		outputList.add(s);
	}
	
	public void triggerFlag(){
		try{//System.out.println("hello????");
		threadList.get(0).setQuitFlagToTrue();
		}
		catch(IndexOutOfBoundsException e){
			
		}
	}
	
	public void closeSockets(){
		
	}
}