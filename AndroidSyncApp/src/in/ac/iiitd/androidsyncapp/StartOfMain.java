package in.ac.iiitd.androidsyncapp;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

/**
 * Actually Runs the downloading in parts in parallel by first creating config files for each thread
 * which are stored in o_sort_config Bundles.
 * @author Suresh
 *
 */
public class StartOfMain extends AsyncTask<Void, Void, Void>{

	private static final String o_master = "master StartOfMain";
	
	@Override
	protected Void doInBackground(Void... voids) {
		// TODO Auto-generated method stub
		
		Log.v(o_master, "doInBackground");
		try {
			String o_s = Helper.o_config.getString("url");
			Helper.o_url = new URL(o_s);
			
			Helper.o_config.putString("ext", o_s.substring(o_s.length()-4, o_s.length()));
			Log.v(o_master, "Extension : " + Helper.o_config.getString("ext"));
			
			Helper.o_config.putString("name", o_s.substring(o_s.lastIndexOf('/')));
			
			HttpURLConnection conn = (HttpURLConnection) (new URL(Helper.o_config.getString("url"))).openConnection();
				conn.setRequestMethod("HEAD");
				Log.v(o_master, "Connection status : " + conn.getResponseCode());
				if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
					
					final int len = conn.getContentLength();
					Helper.o_config.putInt("len", len);
					Helper.o_pool_config = new ArrayList<Bundle>();
					
					//Make directory /AndroidSync
					Helper.o_path.mkdir();
					
					// Initialize No. of Parts
					Helper.o_no_parts = Math.min((int) Math.ceil(1.0*len/Helper.o_size), Helper.o_maxParts);
					
					Helper.isDone = new boolean[Helper.o_no_parts];
					Helper.o_isRunning = new boolean[Helper.o_no_parts];
					
					Helper.o_config.putInt("parts", Helper.o_no_parts);
					
					// Set the block size for part if not default
					if(Helper.o_no_parts == Helper.o_maxParts) {
						Helper.o_size = len/Helper.isDone.length;
					}
					
					Log.v(o_master, "No. of Parts : " + Helper.o_no_parts + " Content len : " + len);
					
					// find no. of connected devices
					Helper.o_no_devices = findDevices();
					Helper.o_isDownloading = new boolean[Helper.o_no_devices];
					
					Log.v(o_master, "Parts Size : " + Helper.o_size + " No. of Devices : " + Helper.o_no_devices);
					
					for(int i=0, offset = 0; i<Helper.isDone.length; i++, offset++) {
						Bundle b = new Bundle(Helper.o_config);
						b.putInt("id", i);
						b.putInt("start", offset);
						b.putInt("end", Math.min(offset+=Helper.o_size, len));
						Helper.o_pool_config.add(b);			
					}
					
					Log.v(o_master, "All Bundles created for execution");
					
					broadcastBundle();
					
					new DownloadManager().executeOnExecutor(THREAD_POOL_EXECUTOR);
					
					Log.v(o_master, "All Parts running in background");				
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.v(o_master, e.toString());
			}
		
		return null;
	}
	
	private void broadcastBundle() {
		// TODO Auto-generated method stub
		
		// Send the bundle to every slave...
		
		
	}

	private int findDevices() {
		// TODO Auto-generated method stub
	
		// contact to BT interface and get list of devices
		
		// Increment for Master
		return ++Helper.o_no_devices;
	}

	@Override
	protected void onPostExecute(Void v){
		Log.v(o_master, "Download Manager Started");
	}

}
