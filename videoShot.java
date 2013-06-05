import java.util.ArrayList;


public class videoShot {
	public int begin_frame_id;
	public int shot_id;
	public int cluster_id=-1;
	public int shot_weight=-1;
	public double multiple_value=0;
	public double average_h=0;
	public int ending_frame_id;
	public boolean isAd=false;
	public videoFrame representative;
	
	videoShot(int shot_id,int begin_id,int end_id,videoFrame represent){
		this.shot_id=shot_id;
		this.begin_frame_id=begin_id;
		this.ending_frame_id=end_id;
		this.representative=represent;
		this.shot_weight=(int) ((end_id-begin_id)/50);
		this.shot_weight=this.shot_weight<=0?1:this.shot_weight;
		average_h=represent.getAverageH();

	}
    public double getAudio(ArrayList<videoFrame> al){
    	double av=0;
    	double length=(ending_frame_id-begin_frame_id)*1.0;
    	for(int i=begin_frame_id;i<ending_frame_id;i++){
    		av+=al.get(i).audio_sample/length;
    	}
    	//System.out.print(" "+av+"\n");
    	return Math.ceil(av*100)/100;
    }
}
