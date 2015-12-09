package RUBTClient;

public class Save implements java.io.Serializable {
	
	private int index;
	private int downloaded;
	private int remaining;
	private byte[][] masterBuffer;
	private static final long serialVersionUID = 1L;
	private byte[] info_hash;
	private boolean finish;
	
	public Save(int index, int downloaded, byte[][] masterBuffer, byte[] info_hash, int remaining, boolean finish){
		
		this.index = index;
		this.downloaded = downloaded;
		this.masterBuffer = masterBuffer;
		this.info_hash = info_hash;
		this.remaining = remaining;
		this.finish = finish;
	}
	
	public int getIndex(){
		return index;
	}
	public int getDownloaded(){
		return downloaded;
	}
	public byte[][] getMasterBuffer(){
		return masterBuffer;
	}
	public byte[] getInfoHash(){
		return info_hash;
	}
	public int getRemaining(){
		return remaining;
	}
	public boolean getFinish(){
		return finish;
	}
}
