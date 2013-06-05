import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class MyProgram{
	
	private ArrayList<videoFrame> frames;
	private ArrayList<videoShot> shots;
	File file_video = new File("video3-wreckitralph-full.rgb");
	File file_audio = new File("video3-wreckitralph-full.wav");
	InputStream is_video;
	InputStream is_audio;
	String videoName;
	String audioName;
	String outputVideoName;
	String outputAudioName;
	//int testNum=100;
	public int totalFramesAmount;
	public int audio_frame_length;
	public double threshold;
	
	//for audio
	
	MyProgram(String videoName,String audioName,String outputVideoName,String outputAudioName){
		this.videoName=videoName;
		this.audioName=audioName;
		this.outputAudioName=outputAudioName;
		this.outputVideoName=outputVideoName;
		file_video = new File(videoName);
		file_audio = new File(audioName);
		frames=new ArrayList<videoFrame>();
		shots=new ArrayList<videoShot>();
		try {
			is_video = new FileInputStream(file_video);
			long len_video = file_video.length();
			is_audio = new FileInputStream(file_audio);
			long len_audio = file_audio.length();
			totalFramesAmount = (int) (len_video/(videoFrame.width*videoFrame.height*3));
			audio_frame_length=(int) (len_audio/totalFramesAmount);
			System.out.println("the audio frame length is "+audio_frame_length);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//for test purpose
		//totalFramesAmount=3000;
		
		int intraframe=0;
		int shot_id=0;
		for(int i=0;videoFrame.frameNumber<totalFramesAmount;i++){
			videoFrame vf=new videoFrame(is_video,is_audio,totalFramesAmount,audio_frame_length);
			//vf.showHist();
			frames.add(vf);
			if(i>=2){
				double sim=calcSimilarity(frames.get(intraframe),frames.get(i-1));
				//if(sim<0.5 && calcSimilarity(frames.get(i-1),frames.get(i))>0.9){
				if(sim<=0.5 && calcSimilarity(frames.get(i),frames.get(i-1))>=0.5){	
					final videoShot vs=new videoShot(shot_id,intraframe,i-2,frames.get(intraframe));
					shots.add(vs);
					intraframe=i-1;	
					shot_id++;
				}
			}
		}
		final videoShot vs=new videoShot(shot_id,intraframe,totalFramesAmount-1,frames.get(intraframe));
		shots.add(vs);
		
		//close all the input files
		try {
			is_video.close();
			is_audio.close();
			is_video=null;
			is_audio=null;
			file_video=null;
			file_audio=null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("\n");

		//analysis shots and distinguish ads
		double last_average_h=0;
		double av_multiple=0;
		for(int x=0;x<shots.size();x++){
			videoShot i=shots.get(x);
			double variance_h=0;
			if(x!=0){
				variance_h=Math.abs(i.average_h-last_average_h);
				last_average_h=i.average_h;
			}
			double tmp_av_average_h=variance_h/i.shot_weight;
			double tmp_av_audio_sample=i.representative.audio_sample/i.shot_weight;
			i.multiple_value=tmp_av_average_h*tmp_av_audio_sample;
			System.out.print(" "+i.begin_frame_id+"-"+i.ending_frame_id+" "+i.shot_weight+"\t"+i.multiple_value+"\n");
			//System.out.print(" "+i.begin_frame_id+"-"+i.ending_frame_id+" "+i.shot_weight+"\t"+tmp_av_average_h+"\t"
			//+tmp_av_audio_sample+"\t"+tmp_av_multiple+"\n");
			av_multiple+=i.multiple_value;
		}
		threshold=av_multiple/shots.size();
		System.out.println(threshold);
		distinguishAds();
		
		writeToFile();
		//System.out.println(av_average_h+"\t"+av_audio_sample+"\t"+av_multiple);
		
		//kmeansCluster();
	}
	
	void writeToFile(){
		//int frame_length=videoFrame.width*videoFrame.height*3;
		  try{
			  // Create file 
			  file_video = new File(videoName);
			  is_video = new FileInputStream(file_video);
			  file_audio = new File(audioName);
			  is_audio = new FileInputStream(file_audio);
			  OutputStream os_video=new FileOutputStream(new File(outputVideoName));
			  OutputStream os_audio=new FileOutputStream(new File(outputAudioName));
			  for(int i=0;i<shots.size();i++){
				  videoShot vs=shots.get(i);
				  int size=vs.ending_frame_id-vs.begin_frame_id;
				  System.out.println("here writing shot "+i);
				  int j=0;
				  while(j++<size){
					  videoFrame.writeFrame(is_video,is_audio, os_video,os_audio, vs.isAd,audio_frame_length);
				  }
			  }
			  //Close the output stream
			  os_video.close();
			  os_video=null;
			  os_audio.close();
			  os_audio=null;
			  }catch (Exception e){//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
			  }
	}
	
	void distinguishAds(){
		for(int i=0;i<shots.size();i++){
			int begin_shot=0;
			int ending_shot=0;
			if(shots.get(i).multiple_value>=threshold/8){
				begin_shot=ending_shot=i;
				int frame_count=shots.get(i).shot_weight;
				int above_count=0;
				if(shots.get(i).multiple_value>=threshold/2)
					above_count=shots.get(i).shot_weight;
				while(ending_shot++<shots.size()-2){
					if(shots.get(ending_shot).multiple_value>threshold/2)
						above_count+=shots.get(ending_shot).shot_weight;
					frame_count+=shots.get(ending_shot).shot_weight;
					if(14<frame_count && shots.get(ending_shot+1).multiple_value<threshold/2){
						ending_shot++;
						break;
					}
				}
				if(above_count>=3){
					for(int j=begin_shot;j<ending_shot;j++)
						shots.get(j).isAd=true;
						System.out.println(shots.get(begin_shot).begin_frame_id+"-"+shots.get(ending_shot-1).ending_frame_id);
					i=ending_shot;
				}
			}
		}
	}
	
	public double calcSimilarity(videoFrame f1,videoFrame f2){
		double similarity=0;

			long molecular=0;
			long denominator_factor1=0;
			long denominator_factor2=0;
			for(int j=0;j<videoFrame.dims;j++){
				molecular+=f1.histogram[j]*f2.histogram[j];
				denominator_factor1+=(f1.histogram[j]*f1.histogram[j]);
				denominator_factor2+=(f2.histogram[j]*f2.histogram[j]);
			}
			if(molecular==0 || denominator_factor1<=0 || denominator_factor2<=0 ||Math.sqrt(denominator_factor1*denominator_factor2)<=0 )
				similarity=1;
			else{
				similarity=molecular/Math.sqrt(denominator_factor1);
				similarity=similarity/Math.sqrt(denominator_factor2);
			}		

		System.out.println("similarity "+f1.id+" to "+f2.id+" is "+similarity+" "+f2.audio_sample);
		return similarity;
	}
	/*
	double calcDistance(long[] n1,long[]n2,int id,double average_index){
		double distance=0;
		long molecular=0;
		long denominator_factor1=0;
		long denominator_factor2=0;
		for(int j=0;j<videoFrame.dims;j++){
			molecular+=n1[j]*n2[j];
			denominator_factor1+=(n1[j]*n1[j]);
			denominator_factor2+=(n2[j]*n2[j]);
		}
		if(molecular<0)
			System.out.println("molecular is under zero");
		distance= (Math.sqrt(denominator_factor1)/molecular);
		distance=distance*Math.sqrt(denominator_factor2);	
		//if(average_index!=-1){
			//double dis=average_index-id;
			//distance=Math.sqrt(distance*distance+dis*dis*0.25);
			//distance+=Math.abs(dis);
		//}
		//System.out.println(distance);
		return distance;
	}
	*/
        public static void main(String[] args) throws IOException, InterruptedException{

    		String videoName = args[0];
    	   	String audioName = args[1];
    		String outputVideoName = args[2];
    	   	String outputAudioName = args[3];
		//String videoName = "video3-wreckitralph-full.rgb";
	   	//String audioName = "video3-wreckitralph-full.wav";
	   	//String outputVideoName="testoutput.rgb";
	   	//String outputAudioName="testoutput.wav";
	   	new MyProgram(videoName,audioName,outputVideoName,outputAudioName);
	}
        /*
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
    		
    		//dataLine.start();
    		
    		//dataLine.getLevel();
    	}
        
        void kmeansCluster(){
        	long[][] clusters_center=new long[2][videoFrame.dims];
        	for(int i=0;i<videoFrame.dims;i++)
        		clusters_center[0][i]=shots.get(0).representative.histogram[i];
        	for(int i=0;i<videoFrame.dims;i++)
        		clusters_center[1][i]=shots.get(shots.size()-1).representative.histogram[i];
        	
        	ArrayList<videoShot> cluster0=new ArrayList<videoShot>();     	
        	ArrayList<videoShot> cluster1=new ArrayList<videoShot>();
        	
        	
        	double[] clusters_average_index=new double[2];
        	clusters_average_index[0]=-1;
        	clusters_average_index[1]=-1;
        	
        	boolean isChanged=true;
        	int counter=0;
        	while(isChanged && counter++<2000){
        		isChanged=false;
        		for(videoShot vs:shots){
        			int choice=calcDistance(vs.representative.histogram,clusters_center[0],vs.shot_id,clusters_average_index[0])
        					> calcDistance(vs.representative.histogram,clusters_center[1],vs.shot_id,clusters_average_index[1]) ? 0:1;
        			ArrayList<videoShot> which_cluster= choice==0 ? cluster0:cluster1;
        			ArrayList<videoShot> another_cluster= choice==1 ? cluster0:cluster1;
        			switch(vs.cluster_id){
	        			case -1:{
	        				isChanged=true;
	        				vs.cluster_id=choice;	  
	        				which_cluster.add(vs);
	        				break;
	        			}
	        			case 0:{
	        				if(choice!=0){
	            				isChanged=true;
	            				vs.cluster_id=choice;
		        				another_cluster.remove(vs);
		        				which_cluster.add(vs);
	        				}
	        				break;
	        			}
	        			case 1:{
	        				if(choice!=1){
	            				isChanged=true;
	            				vs.cluster_id=choice;
		        				another_cluster.remove(vs);
		        				which_cluster.add(vs);
	        				}
	        				break;
	        			}
        			}       			
        		}
        			//calculate the new centers
        			for(int i=0;i<videoFrame.dims;i++){
        				clusters_center[0][i]=0;
        				clusters_center[1][i]=0;
        			}
        			clusters_average_index[0]=0;
        			clusters_average_index[1]=0;
        			int size=(int) (cluster0.size()*totalFramesAmount/100);
        			for(videoShot vs:cluster0){
        				for(int i=0;i<videoFrame.dims;i++)
        					clusters_center[0][i]+=(vs.representative.histogram[i]*vs.shot_weight/size);
        				clusters_average_index[0]+=vs.shot_id;
        			}
        			clusters_average_index[0]/=cluster0.size();
        			size=(int) (cluster1.size()*totalFramesAmount/100);
        			for(videoShot vs:cluster1){
        				for(int i=0;i<videoFrame.dims;i++)
        					clusters_center[1][i]+=(vs.representative.histogram[i]*vs.shot_weight/size);
        				clusters_average_index[1]+=vs.shot_id;
        			}
        			clusters_average_index[1]/=cluster1.size();
        		}
        	
        	//print clusters
        	for(videoShot vs:cluster0)
        		System.out.print(" "+vs.shot_id);
        	System.out.println("");
        	for(videoShot vs:cluster1)
        		System.out.print(" "+vs.shot_id);
        }*/
}
