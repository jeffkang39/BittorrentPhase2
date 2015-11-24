
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import GivenTools.TorrentInfo;

public class ThreadHandler {

	// Start method runs all threads in list
	public static void startThreads(ArrayList<PeerThread> threadList, Torrent torrent, TorrentInfo ti, Download dl){
		for(int i = 0; i < threadList.size(); i++){
			System.out.println("Starting thread: " + threadList.get(i).getThreadName());
			threadList.get(i).start();
		}
		ListenThread lt;
		try {
			lt = new ListenThread(torrent.getPort(), ti, torrent);
			lt.start();
			UserInputThread uit = new UserInputThread(dl,threadList, lt, torrent);
			uit.start();
			UpdateTrackerThread tut = new UpdateTrackerThread(torrent);
			tut.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Creates IO streams
	public static void createIOStreams(ArrayList<PeerThread> threadList){
		if (threadList == null) {System.out.println("Cannot create IO Streams. Null parameter."); return;}
		
		for(int i = 0; i < threadList.size() ;i++){
			DataInputStream dis = threadList.get(i).getDataInputStream();
			DataOutputStream dos = threadList.get(i).getDataOutputStream();
			try {
				dis = new DataInputStream(threadList.get(i).getSocket().getInputStream());
				dos = new DataOutputStream(threadList.get(i).getSocket().getOutputStream());
				threadList.get(i).setDataInputStream(dis);
				threadList.get(i).setDataOutputStream(dos);
				
			} catch (IOException e) {System.out.println("Error. Could not create IO Streams.");}
		}
	}
	
	//close threads	and socket
	public static void closeStreams(ArrayList<PeerThread> threadList) throws IOException{
		if (threadList == null) {System.out.println("Cannot close IO Streams. Null parameter."); return;}
		
		for(int i = 0; i < threadList.size() ;i++){
			System.out.println("Closing " + threadList.get(i).getThreadName());
			threadList.get(i).getSocket().close();
			threadList.get(i).getDataInputStream().close();
			threadList.get(i).getDataOutputStream().close();
		}
	}
	
	//helper function
	public static void debug(String msg){
		if(true)
			System.out.println(msg);
	}
	
}
