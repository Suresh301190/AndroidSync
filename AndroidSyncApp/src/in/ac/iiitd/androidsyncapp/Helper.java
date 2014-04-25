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

	public static final int o_maxParts = 2;
	/**
	 * Configuration which is master to all contains deviceID -- (int) task Scheduled on, id -- (int) Part id, start -- start of part block,
	 * end -- (int) end of part block, isDone -- (String) message if download was successful/ Failed, url -- (String) url from which to download,
	 * size -- (int) content length, path -- (String) directory where the part was stored in sd-card, name -- (String) name of the file.
	 */
	public static Bundle o_config;

	/**
	 * List of Bundles associated with the individual parts
	 */
	public static ArrayList<Bundle> o_pool_config;

	/**
	 * Current Config to offer from the pool
	 */
	public static int o_offer = 0;

	/**
	 * Text view to update to Application View of the progress
	 */
	public static TextView o_update;

	/**
	 * Maximum Buffer Length
	 */
	public static final int o_buffLength = 0x1000;

	/**
	 * Minimum part size default 512KB
	 */
	public static int o_size = 0x80000;

	/**
	 * To store the working Directory /AndroidSync
	 */
	public static final File o_path = new File(Environment.getExternalStorageDirectory(), "/AndroidSync");

	/**
	 * To store the URL temporarily 
	 */
	public static URL o_url;

	/**
	 * Boolean array for checking if the part'i' of file have been downloaded
	 */
	static boolean[] isDone;
	
	/**
	 * No. of parts
	 */
	public static int o_no_parts; 

	/**
	 * No. of devices currently being used to download the file
	 */
	public static int o_no_devices = 0;

	/**
	 * True if part'i' is currently being downloaded by any device...
	 */
	public static boolean[] o_isRunning;
	
	/**
	 * If Device is currently Downloading some part
	 */
	public static boolean[] o_isDownloading;
	
	/**
	 * Default sleep Timer for Major Thread
	 */
	public static long o_sleep = 1000;
	/**
	 * To set download Progress
	 */
	public static int o_progress = 0;
	public static SeekBar o_prog_bar;

	/**
	 * To check if all process is done
	 * @return true if all parts are downloaded
	 */
	public static boolean o_allDone(){
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
