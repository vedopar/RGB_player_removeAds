
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * plays a wave file using PlaySound class
 * 
 * @author Giulio
 */
public class PlayWaveFile {

    /**
     * <Replace this with one clearly defined responsibility this method does.>
     * 
     * @param args
     *            the name of the wave file to play
     */
    public static void main(String[] args) {

	String filename ="video3-wreckitralph-full.wav";

	// opens the inputStream
	FileInputStream inputStream;
	try {
	    inputStream = new FileInputStream(filename);
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	    return;
	}

	// initializes the playSound Object
	PlaySound playSound = new PlaySound(inputStream);

	// plays the sound
	try {
	    playSound.play();
	} catch (PlayWaveException e) {
	    e.printStackTrace();
	    return;
	}
    }

}
