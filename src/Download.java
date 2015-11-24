
import GivenTools.TorrentInfo;

public class Download {

	private int index = 0;
	private int downloaded = 0;
	private int remaining = Integer.MIN_VALUE;
	private String fileName = "";
	private TorrentInfo ti = null;
	private Torrent torrent = null;
	private boolean lastBlock = false;
	
	public Download(String fileName, TorrentInfo ti, Torrent torrent) {

		this.ti = ti;
		this.torrent = torrent;
		this.fileName = fileName;
		if(remaining == Integer.MIN_VALUE)
			setRemaining(torrent.getFileLength());
	}

	public String getFileName() {return this.fileName;}
	public TorrentInfo getTorrentInfo() {return this.ti;}
	public Torrent getTorrent() {return this.torrent;}
	public boolean lastBlock() {return lastBlock;}
	
	public synchronized int getRemaining(int size) {
		remaining -= size;
		return remaining;
		}
	
	public synchronized int getDownloaded(int size) {
		downloaded += size;
		return downloaded;
		}
	public synchronized int getDownloaded(){
		return downloaded;
	}
	public synchronized int getRemaining(){
		return remaining;
	}
	
	public synchronized void resumeDownloaded(int newDownloaded){
		downloaded = newDownloaded;
	}
	
	/*
	 * By incrementing index whenever a thread calls getIndex()
	 * rather than incrementing it manually AFTER a piece is downloaded,
	 * we prevent two threads from "getting" the same index and therefore
	 * downloading the same piece and then possibly skipping over an index
	 * number when they both increment.
	 * -Jarrett
	 */
	// getIndex() increments index and returns the index before it was incremented.
	public synchronized int getIndex() {
		int retVal = index;
		index++;
		return retVal;
	}
	public synchronized void decrementIndex(){
		index = index - 1;
	}
	public synchronized void resumeIndex(int newIndex){
		index = newIndex;
	}
	
	public synchronized int getIndexNoInc() {return index;}
	
	public synchronized void setRemaining(int length) {remaining = length;}
	public synchronized void resumeRemaining(int newRemaining){
		remaining = newRemaining;
	}
	public void setLastBlock() {lastBlock = true;}
	
	public synchronized int setTorrentFields(String event, int piecelen) {

		int d = getDownloaded(piecelen);
		int r = getRemaining(piecelen);
		torrent.setEvent(event);
		torrent.setDownloaded(d);
		torrent.setLeft(r);
		
		return r;
	}

	// Inc/Dec Methods
	public synchronized void resetRemaining() {remaining = Integer.MIN_VALUE;}
	public synchronized void resetDownloaded() {downloaded = 0;}
}