import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;

public class MyPlayer extends Frame{
	private int width=352;
	private int height=288;
	private float fps=25;
	private long frame_duration;
	//private int perFramesAmount=fps;
	private String videoName;
	private String audioName;
	//private BufferedImage[] images;
	private BufferedImage image;
	private Object cond;
	private Lock mutex;
	//private Lock cond;
	private boolean isReadyToShow;
	private long frameNumber = 0;
	private long totalFramesAmount;
	
	//for audio
	private AudioInputStream audioInputStream = null;
	private SourceDataLine dataLine = null;
	private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb
	int readBytes = 0;
	byte[] audioBuffer = new byte[EXTERNAL_BUFFER_SIZE];
	
	
	MyPlayer(String videoName,String audioName){
		this.videoName=videoName;
		this.audioName=audioName;
		cond=new Object();
		//mutex=new ReentrantLock();
		mutex=new ReentrantLock();
		isReadyToShow=false;
		//images=new BufferedImage[perFramesAmount];
		
		this.setSize(width, height+22);
		Dimension window = this.getSize();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation((screen.width - window.width) / 2, (screen.height - window.height) / 2);
			
		try {
			audioInit();
		} catch (PlayWaveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		playAudio pa=null;
		pa = new playAudio();
		
		showFrame sf = new showFrame();
		this.add(sf);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		new Thread(sf).start(); //built a new thread for video
		new Thread(pa).start(); //built a new thread for audio
		this.setVisible(true);
	}

	class playAudio implements Runnable{
		
		playAudio(){

		}

		@Override
		public void run() {
			try {
			    while (readBytes != -1) {
				readBytes = audioInputStream.read(audioBuffer, 0,
					audioBuffer.length);
				if (readBytes >= 0){
				    dataLine.write(audioBuffer, 0, readBytes);
				    //System.out.println("This is level "+dataLine.getLevel());
				}
			    }
			} catch (IOException e1) {
			    try {
					throw new PlayWaveException(e1);
				} catch (PlayWaveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} finally {
			    // plays what's left and and closes the audioChannel
			    dataLine.drain();
			    dataLine.close();
			}
		}
		
	}
	
	class showFrame extends Panel implements Runnable {
		//private static final long serialVersionUID = 1L;
		byte[] bytes;
		InputStream is;
		
        public showFrame() {
        	
			File file = new File(videoName);			
			try {
				is = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long len = file.length();
			totalFramesAmount = len/(width*height*3);
			System.out.println(len+" "+totalFramesAmount);
			
			//totalFramesAmount=30;
	        bytes = new byte[width*height*3];    
            this.setSize(width, height);
            this.setVisible(true);
            
        }

        public void paint(Graphics g) {
            g.drawImage(image, 0, 0, this);
        }
        
        public void run() {

        	while(frameNumber<totalFramesAmount) {
	    	    int offset = 0;
	            int numRead = 0;
	            try {
					while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0)
					    offset += numRead;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            image=null;
	            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);	    		
	    		int ind = 0;
	    		for(int y = 0; y < height; y++){
	    			for(int x = 0; x < width; x++){
	    				byte r = bytes[ind];
	    				byte g = bytes[ind+height*width];
	    				byte b = bytes[ind+height*width*2]; 
	    				
	    				//int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
	    				int pix = 0xff000000 | ((b & 0xff) << 16) | ((g & 0xff) << 8) | (r & 0xff);
	    				image.setRGB(x,y,pix);
	    				ind++;
	    			}
	    		}
	    		System.out.println("this is number "+frameNumber);
	    		frameNumber++;
		        		this.repaint();
		        		
		        		try {
							Thread.sleep(29);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
        	}
        }
    }
	
	void audioInit()throws PlayWaveException{
		// opens the inputStream
		FileInputStream inputStream;
		try {
		    inputStream = new FileInputStream(audioName);
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		    return;
		}
		// initializes the playSound Object
		try {
			InputStream bufferedIn = new BufferedInputStream(inputStream);
		    audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
		} catch (UnsupportedAudioFileException e1) {
		    throw new PlayWaveException(e1);
		} catch (IOException e1) {
		    throw new PlayWaveException(e1);
		}

		// Obtain the information about the AudioInputStream
		AudioFormat audioFormat = audioInputStream.getFormat();
		Info info = new Info(SourceDataLine.class, audioFormat);
		try {
		    dataLine = (SourceDataLine) AudioSystem.getLine(info);
		    dataLine.open(audioFormat, EXTERNAL_BUFFER_SIZE);
		} catch (LineUnavailableException e1) {
		    throw new PlayWaveException(e1);
		}		
		//fps=24;//audioInputStream.getFormat().getFrameRate();
		//frame_duration=(long) ((audioInputStream.getFrameLength()+0.0)*1000 / (fps*10398));
		
		dataLine.start();
		
		//dataLine.getLevel();
	}
	
	public static void main(String[] args) throws IOException, InterruptedException{
		String videoName = args[0];
	   	String audioName = args[1];
	   	new MyPlayer(videoName,audioName);
	}
}
