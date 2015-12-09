import GUI.Gui;

public class GuiThread implements Runnable {
	private Thread t;
	private Gui g;
	
	public GuiThread(){
		this.g = new Gui();
	}
	
	public void run(){
		
	}
	
	public void start() {
		// Thread start calls run
		
		if (t == null) 
			t = new Thread(this);
		t.start();
	}
	public Gui getGui(){
		return g;
	}
}
