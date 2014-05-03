package in.ac.iiitd.androidsyncapp;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;

/**
 * Helper Functions if any
 * static function, variables, final variables go here. feel free to add any thing.
 * @author Suresh
 *
 */
public abstract class Helper extends Activity{
	
	public static ExecutorService seqExec = Executors.newSingleThreadExecutor();

	public static final int o_maxParts = 8;
	
	public static long o_TimeOut = 20000000000l;
	
	/**To Store the data type of config */
	public static final HashSet<String> _conString = 
			new HashSet<String>(Arrays.asList("url", "isDone", "path", "name"));

	/**
	 * Configuration which contains 
	 * deviceID -- (int) task Scheduled on, 
	 * id -- (int) Part id, 
	 * start -- (int) start of part block,
	 * end -- (int) end of part block, 
	 * isDone -- (String) message if download was successful/ Failed, (Only Master Config)
	 * url -- (String) url from which to download, 
	 * size -- (int) content length, 
	 * path -- (String) directory where the part was stored in sd-card, 
	 * name -- (String) name of the file
	 * crash -- (int) crash count for the respective part
	 * isDone -- (String) Message regarding the part file success/failure.
	 */
	public static Bundle o_config;

	/**
	 * List of Bundles associated with the individual parts
	 * and the main pool from which parts to be downloaded is queried.
	 */
	public static ArrayList<Bundle> o_pool_config;
	
	/**To store list of slaves-id pairs*/
	public static SparseArray<BluetoothDevice> o_slave_pool;
	
	/**UUID for device connections*/
	public static final UUID MASTER_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
	public static final UUID SLAVE_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a77");

	/** Current Config to offer from the pool*/
	public static int o_offer = 0;

	/**Maximum Buffer Length default 16KB */
	public static final int o_buffLength = 0x4000;

	/**
	 * Minimum part size default 512KB
	 */
	public static int o_size = 0x80000;

	/**
	 * To store the working Directory /AndroidSync
	 */
	public static final File o_path = new File(Environment.getExternalStorageDirectory(), "/AndroidSync").getAbsoluteFile();
	
	/**
	 * If process crashes retry how many time.
	 */
	public static final int o_crash_threshold = 5;

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
	
	public static long[] o_scheduledOn;
	
	/**
	 * Default sleep Timer for Major Thread
	 */
	public static final long o_sleep = 5000;
	
	/**
	 * To set download Progress
	 */
	public static int o_progress = 0;

	public static String o_filename;
	
	// Message Types
	public static final int MESSAGE_BROADCAST = 11000;
	public static final int MESSAGE_UNICAST = 11001;
	public static final int HANDSHAKE = 11002;

	// TYPES
	public static final int TYPE_BUNDLE = 1;
	public static final int TYPE_STRING = 2;
	public static final int TYPE_ACK = 3;
	public static final int TYPE_NAK = 4;
	public static final int TYPE_FORWARD = 5;
	public static final int TYPE_URL = 6;
	public static final int TYPE_DOWNLOAD_PART_REQUEST = 7;
	public static final int TYPE_UPDATE_PROGRESS = 8;
	public static final int TYPE_FORWARD_PART = 9;
	public static final int TYPE_PART_FROM_SLAVE = 10;
	public static final int TYPE_ONLY_PART_SLAVE = 11;
	public static final int TYPE_PART_FAILED = 12;
	public static final int TYPE_NAK_PART = 13;
	public static final int TYPE_DOWNLOAD_COMPLETE = 14;
	
	
	public static final int FROM_SLAVE = 111;
	public static final int FROM_MASTER = 112;
		
	
	/**
	 * To check if all process is done
	 * @return true if all parts are downloaded
	 */
	public static boolean o_allDone(){
		boolean ans = true;
		int i;
		long curTime = System.nanoTime();
		for(i=1; i<o_no_devices; i++){
			if(curTime - o_scheduledOn[i] > o_TimeOut){
				Log.v("AndroidSync TimeOut", curTime + " : " + o_scheduledOn[i] + " : " + o_TimeOut);
				o_isRunning[i] = false;
			}
		}
		for(i=0; i<o_no_parts; i++){
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
		o_isRunning[id] = false;
		o_progress = Math.min(o_progress + o_size, o_config.getInt("len"));		
	}

	public static void reset() {
		// TODO Auto-generated method stub
		o_size = 0x80000;
		o_pool_config = null;
		o_config = null;
		o_slave_pool = null;
		o_isDownloading = new boolean[7];
		o_isRunning = new boolean[7];
		isDone = new boolean[o_maxParts];
		o_progress = 0;
		o_size = 0x80000;
		o_offer = 0;
		o_url = null;		
		o_no_devices = 1;
		o_no_parts = 0;	
		o_scheduledOn = new long[7];
	}
	
	static{
		//Make directory /AndroidSync
		Helper.o_path.mkdir();
		isDone = new boolean[o_maxParts];
		o_isDownloading = new boolean[7];
		o_isRunning = new boolean[7];
		o_scheduledOn = new long[7];
	}
	
	public void shutdown(){
		
	}
	
	

	// Here on below is your area add your functions below it :)
}
