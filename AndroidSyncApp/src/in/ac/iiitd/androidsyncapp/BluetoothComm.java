package in.ac.iiitd.androidsyncapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

/**
 * Sends the configuration to slave and keeps Pinging every 2-3s for status
 * if it receives ACK it means the file is being downloaded by slave
 * if it Receives NAK then it changes the status the 
 * o_isDownloading[deviceID] = o_isRunning[id] = false, so the part is added back to o_pool_config
 * if No ACK or NAK from slave it is assumed it is dead and above process is repeted
 * @author Suresh
 *
 */
public class BluetoothComm extends AsyncTask<Bundle, Void, Void>{

	private static final String o_master = "master BluetoothComm";
	
	/**
	 * Configuration which contains deviceID -- (int) task Scheduled on, id -- (int) Part id, start -- start of part block,
	 * end -- (int) end of part block, isDone -- (String) message if download was successful/ Failed, url -- (String) url from which to download,
	 * size -- (int) content length, path -- (String) directory where the part was stored in sd-card, name -- (String) name of the file.
	 */
	private Bundle o_config;
	
	@Override
	protected Void doInBackground(Bundle... params) {
		// TODO Auto-generated method stub
		Log.v(o_master, "doInBackground");
		
		o_config = params[0];
		
		return null;
	}
	
	@Override
	protected void onPostExecute(Void i){
		if(Helper.isDone[o_config.getInt("id")]){
			Helper.o_partDone(o_config.getInt("id"));
		}
		else{
			Helper.o_updateText("part" + o_config.getInt("id") + " failed...");
		}
	}

}
