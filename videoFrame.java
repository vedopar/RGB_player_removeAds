import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;


public class videoFrame {
	//BufferedImage frame;
	final public static int width=352;
	final public static int height=288;
	final public static int dims=37;
	final public static int divisor=10;
	final public static int average=width*height/dims;
	public static int frameNumber = 0;
	public int id;
	private int totalFramesAmount;
	public static byte[] bytes=new byte[width*height*3]; 
	static byte[] bytes_audio=null;
	double audio_sample;
	long[] histogram;
	//DecimalFormat df = new DecimalFormat("###.##");

	
	public videoFrame(InputStream is,InputStream is_audio,int totalFramesAmount,int audio_frame_length){
		this.totalFramesAmount=totalFramesAmount;
		//bytes = new byte[width*height*3];
		if(bytes_audio==null)
			bytes_audio=new byte[audio_frame_length];
		histogram=new long[dims];
			for(int j=0;j<dims;j++)
				histogram[j]=0;
		readFrame(is,is_audio) ;
	}
	/*
	public void showHist(){

			for(int j=0;j<dims;j++){
				System.out.print(histogram[j]+" ");
			}

		System.out.print("\n");
	}*/
	
	public static void writeFrame(InputStream is_v,InputStream is_a,OutputStream os_v,OutputStream os_a,boolean isAd,int audio_frame_length){
		try {
			if(isAd){
				is_v.skip(bytes.length);
				is_a.skip( audio_frame_length);
				return;
			}
			
			if(bytes_audio==null)
				bytes_audio=new byte[audio_frame_length];
			int offset = 0;
	        int numRead = 0;
	        int offset_audio = 0;
            int numRead_audio = 0;
		
			while (offset < bytes.length && (numRead=is_v.read(bytes, offset, bytes.length-offset)) >= 0)
			    offset += numRead;
	         os_v.write(bytes) ;
	         while (offset_audio < bytes_audio.length && (numRead_audio=is_a.read(bytes_audio, offset_audio, bytes_audio.length-offset_audio)) >= 0)
					offset_audio += numRead_audio;
	         os_a.write(bytes_audio) ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    private void readFrame(InputStream is,InputStream is_audio) {

    	if(frameNumber<totalFramesAmount) {
    	    int offset = 0;
            int numRead = 0;
            int offset_audio = 0;
            int numRead_audio = 0;
            try {
				while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0)
				    offset += numRead;
				while (offset_audio < bytes_audio.length && (numRead_audio=is_audio.read(bytes_audio, offset_audio, bytes_audio.length-offset_audio)) >= 0)
					offset_audio += numRead_audio;
				//audio_sample=Math.abs((bytes_audio[1]<<8)|(bytes_audio[0] & 0xff));
				//bytes_audio=null;
				
			    //nBytesRead = is_audio.read(abData, 0, abData.length);
			    //numRead_audio=is_audio.read(bytes_audio, 0, bytes_audio.length);
		      
		      if (numRead_audio >= 0)
		      {
		          double sum=0;
		          
		          for (int i=1;i<bytes_audio.length;i=i+2)
					 {
					  sum=sum+Math.abs((bytes_audio[i]<<8)|(bytes_audio[i-1] & 0xff));
					
					 }
		      
		          //audio_sample=((int)(sum*100))/100.0; //System.out.println(sum);
		          audio_sample=sum/48000;
			}
            }catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            //frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    		int ind = 0;
    		for(int y = 0; y < height; y++){
    			for(int x = 0; x < width; x++){
    				double r2 = (bytes[ind]& 0xff)/255.0;
    				double g2 = (bytes[ind+height*width] & 0xff)/255.0;
    				double b2 =( bytes[ind+height*width*2] & 0xff)/255.0 ; 
    				 double minRGB = Math.min(r2,Math.min(g2,b2));
    		    	 double maxRGB = Math.max(r2,Math.max(g2,b2));

    		    	 // Colors other than black-gray-white:
    		    	 double d = (r2==minRGB) ? g2-b2 : ((b2==minRGB) ? r2-g2 : b2-r2);
    		    	 double h = (r2==minRGB) ? 3 : ((b2==minRGB) ? 1 : 5);
    		    	  double computeH= 60*(h - d/(maxRGB - minRGB));
    		    	  int index=(int) (computeH/divisor);
    		    	  histogram[index]++;
    				ind++;
    			}
    		}
    		this.id=frameNumber;
    		frameNumber++;
    		//bytes=null;
    		//frame=null;

    			for(int j=0;j<dims;j++)
    				histogram[j]-=average;

    	}
    	else{
    		System.out.println("reach the end");
    		System.exit(1);
    	}
}
    public void getH (int r,int g,int b) {

    	 double r2=r/255.0;
    	 double g2=g/255.0;
    	 double b2=b/255.0;
    	 double minRGB = Math.min(r2,Math.min(g2,b2));
    	 double maxRGB = Math.max(r2,Math.max(g2,b2));


    	 // Colors other than black-gray-white:
    	 double d = (r2==minRGB) ? g2-b2 : ((b2==minRGB) ? r2-g2 : b2-r2);
    	 double h = (r2==minRGB) ? 3 : ((b2==minRGB) ? 1 : 5);
    	  double computeH= 60*(h - d/(maxRGB - minRGB));
    	  int index=(int) (computeH/divisor);
    	  histogram[index]++;
    	}
    
    
    public double getAverageH(){
    	double av=0;
		for(int i=0;i<videoFrame.dims;i++){
			av+=i*(histogram[i]/totalFramesAmount);
		}
	return av;
    }
}
