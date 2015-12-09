package GUI;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

import javax.net.ssl.SSLEngineResult.Status;
import javax.swing.*;

public class Gui {

	   private JFrame frame;
	   private JLabel headerLabel;
	   private static JLabel statusLabel;
	   private JPanel controlPanel;
	   
	   private static JEditorPane editorPane1;
	   private static JEditorPane editorPane2;
	   private static JEditorPane editorPane3;
	   private static JEditorPane editorPane4;
	   private static JEditorPane editorPane5;
	   private static JEditorPane editorPane6;
	   static JProgressBar jb;
	   private boolean flag = false;
	   private static String torrentName;
	   private static String fileName;
	   private boolean quitFlag = false;
	   private static int port = 0;
	   
	   public Gui(){
	      prepareGUI();
	      showEditorPane();
	      showProgressBar();
	      showButton();
		   editorPane1.setText("File Name :                                  Torrent Name : "  );           
		   editorPane2.setText("Port : ");		      
		   editorPane3.setText("Total Bytes Uploaded : " + 0);
		   editorPane4.setText("Remaining Bytes : " + 0);		  
		   editorPane5.setText("Downloaded Bytes : " + 0);
		   editorPane6.setText("Percentage : ");
		   
	   }

	   private void prepareGUI(){
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
	   
	   private void showProgressBar(){
		   jb = new JProgressBar();
		   jb.setVisible(true);
		   controlPanel.add(jb);
	   }
	   private void showEditorPane(){
		   
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
	   
	   public void setEditorPane(int remaining, int downloaded, double percentage){
		   this.editorPane4.setText("Remaining Bytes : " + remaining);
		   this.editorPane5.setText("Downloaded Bytes : " + downloaded);
		   this.editorPane6.setText("Percentage : " + new DecimalFormat("##.##").format(percentage) + "%");
		   
	   } 
	   
	   private void setFlag(boolean b){
		   this.flag = b;
	   }
	   public boolean getFlag(){
		   return this.flag;
	   }
	   
	   private void setQuitFlag(boolean b){
		   quitFlag = b;
	   }
	   public boolean getQuitFlag(){
		   return quitFlag;
	   }
	  
	   private void showButton(){

	      //headerLabel.setText("Bittorent"); 
		  
	      JButton startButton = new JButton("Start");        
	      JButton quitButton = new JButton("Quit");
	      
	      startButton.addActionListener(new ActionListener() {
	         public void actionPerformed(ActionEvent e) {
	            statusLabel.setText("Starting...");
	            setEditable();
	            setUneditable();
	            jb.setVisible(true);
	            setFlag(true);
	         }          
	      });

	      quitButton.addActionListener(new ActionListener() {
	         public void actionPerformed(ActionEvent e) {
	            statusLabel.setText("Quitting...");
	            setQuitFlag(true);
	            System.out.println(quitFlag);
	    
	         }
	      });

	      controlPanel.add(startButton);
	      controlPanel.add(quitButton);
	      frame.setVisible(true);  
	   }
	   
	   public void setEditable(){
		   editorPane1.setEditable(true);
		   editorPane2.setEditable(true);
		   editorPane3.setEditable(true);
		   editorPane4.setEditable(true);
		   editorPane5.setEditable(true);
		   editorPane6.setEditable(true);
		   
	   }
	   
	   public void setUneditable(){
		   editorPane1.setEditable(false);
		   editorPane2.setEditable(false);
		   editorPane3.setEditable(false);
		   editorPane4.setEditable(false);
		   editorPane5.setEditable(false);
		   editorPane6.setEditable(false);
		   
	   }
	   public static void setStrings(String t, String f){
		   editorPane1.setText("File Name : " + f + " Torrent Name : " + t);
           
		   	
	   }
	   public static void setUploaded(int uploaded){
		   editorPane3.setText("Total Bytes Uploaded : " + uploaded);
	   }
	   public static void setport(int p){
		   editorPane2.setText("Port : " + p);		      
		   
	   }
	   public static void updateProgressBar(int progress) {		
		   if(progress == 99){
		   			
		   			statusLabel.setText("Download Complete");
		 		   editorPane4.setText("Remaining Bytes : " + 0);
				   editorPane6.setText("Percentage : " + new DecimalFormat("##.##").format(100.00) + "%");
				   
		   		}
		   		
			  jb.setValue(progress);
			  System.out.println("progress.... " + progress);
		   }	
}