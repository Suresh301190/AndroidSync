package in.ac.iiitd.androidsyncapp;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Helper Functions if any
 * static function, variables, final variables go here. feel free to add any thing.
 * @author Suresh
 *
 */
public abstract class Helper extends Activity{
	
	/**
	 * Configuration which is master to all
	 */
	public static Bundle o_config;
	
	/**
	 * List of Bundles associated with the individual parts
	 */
	public static ArrayList<Bundle> o_sort_config;
	
	/**
	 * Text view to update to Application View of the progress
	 */
	public static TextView o_update;
	
	/**
	 * Maximum Buffer Length
	 */
	public static final int o_buffLength = 0x1000;
	
	/**
	 * Maximum part size default 512KB
	 */
	public static final int o_size = 0x80000; 
	
	/**
	 * To store the working Directory /AndroidSync
	 */
	public static final File o_path = new File(Environment.getExternalStorageDirectory(), "/AndroidSync");
	
	/**
	 * To store the URL temporarily 
	 */
	public static URL o_url;
	
	/**
	 * Boolean array for checking if the prts of file have been downloaded
	 */
	static boolean[] isDone;
	
	/**
	 * To set download Progress
	 */
	public static int o_progress;
	public static SeekBar o_prog_bar;
	
	/**
	 * To check if all process is done
	 * @return true if each part is downloaded
	 */
	public static synchronized boolean o_allDone(){
		boolean ans = true;
		for(int i=0; i<isDone.length; i++){
			ans = ans && isDone[i];
		}
		return ans;
	}
	
	/**
	 * set true if part with id was downloaded successfully
	 * @param id of the part
	 */
	public static synchronized void o_partDone(int id){
		isDone[id] = true;
		o_progress = Math.min(o_progress + o_size, o_config.getInt("len"));
		o_prog_bar.setProgress(100*o_progress/o_config.getInt("len"));
	}
	
	/**
	 * To update the progress on the text view on application
	 * @param s text to append.
	 */
	public static synchronized void o_updateText(String s){
		o_update.setText(o_update.getText() + "\n" + s);
	}
	
	
	// Here on below is your area add your functions below it :)
}
