package in.ac.iiitd.androidsyncapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class DownloadManager extends AsyncTask<Void, Void, Void>{

	private static final String o_master = "master DownloadManager";
	
	/**
	 * Configuration which contains deviceID -- (int) task Scheduled on, id -- (int) Part id, start -- start of part block,
	 * end -- (int) end of part block, isDone -- (String) message if download was successful/ Failed, url -- (String) url from which to download,
	 * size -- (int) content length, path -- directory where the part was stored in sd-card.
	 */
	private Bundle o_config;
	
	@Override
	protected Void doInBackground(Void... params) {
		// TODO Auto-generated method stub
		Log.v(o_master, "Inside Download Manager");
		
		try{
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
						Log.v(o_master, "Scheduled on master id=0");
						o_config.putInt("deviceID", deviceID);
						
						// Set the downloading and running flags.
						Helper.o_isDownloading[deviceID] = true;
						Helper.o_isRunning[o_config.getInt("id")] = true;
						
						new DownloadFile().executeOnExecutor(THREAD_POOL_EXECUTOR, o_config);
					}
					// To run on slave with the respective deviceID
					else if(deviceID != Helper.o_no_devices){
						o_config.putInt("deviceID", deviceID);
						
						Log.v(o_master, "Scheduled on slave id=" + deviceID);
						
						// Set the downloading and running flags.
						Helper.o_isDownloading[deviceID] = true;
						Helper.o_isRunning[o_config.getInt("id")] = true;
						
						new BluetoothComm().executeOnExecutor(THREAD_POOL_EXECUTOR, o_config);
					}
					else{
						// sleep...
						Thread.sleep(Helper.o_sleep);
						//Log.v(o_master, "Sleeping in minor thread for " + Helper.o_sleep/1000 + 's');
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
					//Log.v(o_master, "Sleeping in Major Thread for "  + Helper.o_sleep/1000 + 's');
				}				
			}
		}catch(Exception e){

		}
		return null;
	}

	@Override
	protected void onPostExecute(Void i) {
		// to receive all the parts from slaves in sequential order
		getPartsFromSlave();
		Log.v(o_master, "Merging Started");
		new MergeFile().execute(Helper.o_config);
	}

	
	private void getPartsFromSlave() {
		// TODO Auto-generated method stub
		
		
	}

	/**
	 * To get a part to be downloaded which is not being downloaded by any device
	 * and the part has not been downloaded before
	 * @return Configuration for the part if part is found else "null"
	 */
	private static Bundle o_getFromPool(){
		Bundle b = null;
		
		int i = Helper.o_offer++;
		Helper.o_offer %= Helper.o_no_parts;
		
		for(i %= Helper.o_no_parts; i != Helper.o_offer; i = ++i % Helper.o_no_parts){
			//Log.v(o_master, "Part" + i + " Sent");
			if(!Helper.isDone[i] && !Helper.o_isRunning[i]){
				b = Helper.o_pool_config.get(i);
				Helper.o_offer = ++i % Helper.o_no_parts;
				//Log.v(o_master, "Break out Part" + i + " Sent");
				break;
			}
		}	
		return b;
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
