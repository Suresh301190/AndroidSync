package in.ac.iiitd.androidsyncapp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class DownloadManager extends Thread{

	private static final String TAG = "AndroidSync DownloadManager";

	/**
	 * Configuration which contains 
	 * deviceID -- (int) task Scheduled on, 
	 * id -- (int) Part id, 
	 * start -- start of part block,
	 * end -- (int) end of part block, 
	 * isDone -- (String) message if download was successful/ Failed, 
	 * url -- (String) url from which to download, 
	 * size -- (int) content length, 
	 * path -- (String) directory where the part was stored in sd-card, 
	 * name -- (String) name of the file
	 * crash -- (int) crash count for the respective part
	 * isDone -- (String) Message regarding the part file success/failure.
	 */
	private Bundle o_config;
	
	private final static ExecutorService execDownloadQueue = Executors.newCachedThreadPool();

	private final ExecutorService seq_DownloadManager;

	public final BluetoothComm bcomm;

	private final Handler oh_Manager;
	
	public DownloadManager(Handler h, BluetoothComm comm){
		oh_Manager = h;
		bcomm = comm;	
		seq_DownloadManager = Executors.newSingleThreadExecutor();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		//Log.v(TAG, "Inside Download Manager");
		Log.v(TAG, "No. of Devices found " + Helper.o_no_devices);
		try{

			Runnable broadCastBundle = new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					Log.v(TAG, "Broad Casting Bundles");
					// Send the bundle to every slave...
					for(int i=1; i<Helper.o_no_devices; i++){
						bcomm.sendBundle(Helper.o_config, i, Helper.TYPE_BUNDLE);
					}
				}
			};

			//seq_DownloadManager.execute(broadCastBundle);

			int deviceID = 0;

			// Major Loop/Service
			while(true){

				// get a free config 
				o_config = o_getFromPool();

				// If we have a Part and finding some idle device to schedule to...
				while(o_config != null){

					deviceID = findIdle();

					// To run on master itself
					if(deviceID == 0){
						Log.v(TAG, "Scheduled on master id=0");
						o_config.putInt("deviceID", deviceID);
						o_config.putInt("crash", o_config.getInt("crash" + 1));

						// Set the downloading and running flags.
						Helper.o_isDownloading[deviceID] = true;
						Helper.o_isRunning[o_config.getInt("id")] = true;
						Helper.o_scheduledOn[deviceID] = System.nanoTime();
						new DownloadFile(o_config, oh_Manager).start();
					}
					// To run on slave with the respective deviceID
					//*
					else if(deviceID != Helper.o_no_devices){
						o_config.putInt("deviceID", deviceID);

						Log.v(TAG, "Scheduled on slave id=" + deviceID);

						// Set the downloading and running flags. 
						Helper.o_isDownloading[deviceID] = true;
						Helper.o_isRunning[o_config.getInt("id")] = true;

						Helper.o_scheduledOn[deviceID] = System.nanoTime();
						bcomm.sendBundle(o_config, deviceID, Helper.TYPE_DOWNLOAD_PART_REQUEST);
					} //*/
					else{
						// sleep...
						Thread.sleep(Helper.o_sleep);
						Log.v(TAG, "Sleeping in minor thread for " + Helper.o_sleep/1000 + 's');
					}

					// get a free config
					o_config = o_getFromPool();
				}

				// if all parts done
				if(Helper.o_allDone()){
					break;
				}
				else{
					Thread.sleep(Helper.o_sleep);
					Log.v(TAG, "Sleeping in Major Thread for "  + Helper.o_sleep/1000 + 's');
				}				
			}
		}catch(Exception e){
			Log.v(TAG, e.toString());
		}

		onPostExecute();
	}

	protected void onPostExecute() {
		// to receive all the parts from slaves in sequential order
		Log.v(TAG, "Merging Started");
		new MergeFile(oh_Manager, bcomm).start();
	}

	/**
	 * To get a part to be downloaded which is not being downloaded by any device
	 * and the part has not been downloaded before
	 * @return Configuration for the part if part is found else "null"
	 */
	private static Bundle o_getFromPool(){

		for(int i = 0; i <Helper.o_no_parts; ++i){
			//Log.v(o_master, "Part" + i + " Sent");
			if(!Helper.isDone[i] && !Helper.o_isRunning[i]) {
				return Helper.o_pool_config.get(i);
			}
		}	
		return null;
	}

	/**
	 * Find a device which is idle
	 * @return id of the idle device, else No. of devices
	 */
	private static int findIdle() {
		// TODO Auto-generated method stub
		for(int i=0; i<Helper.o_no_devices; i++){
			if(!Helper.o_isDownloading[i]){
				return i;
			}
		}
		return Helper.o_no_devices;
	}
}