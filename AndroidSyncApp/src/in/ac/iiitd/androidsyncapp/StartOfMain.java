package in.ac.iiitd.androidsyncapp;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

/**
 * Actually Runs the downloading in parts in parallel by first creating config files for each thread
 * which are stored in o_pool_config Bundles.
 * @author Suresh
 */
public class StartOfMain extends Thread{

	private static final String TAG = "AndroidSync StartOfMain";

	private final ExecutorService seq_StartOfMain;

	private BluetoothComm bcomm;

	public final Handler oh_Start;

	public StartOfMain(Handler h, ExecutorService es){
		oh_Start = h;
		seq_StartOfMain = es;
	}

	/* 
	 * tag:^(?!(NativeCrypto|SIM|Signal|ADB|Network|Wifi|Buffer|Power|State|wpa|Provider|Input|Surface|Location|dalvik|Status|Alarm|Phone|IPC|wifi|Clock|Tele|Bluetooth|Cell|Gsm|Key|Window|Settings|Launcher|lights|Lights|Open|asset|Graphic|lib|Connectivity|Wallpaper|WebView|Battery))
	 */

	@Override
	public void run() {
		// TODO Auto-generated method stub

		try {
			Log.v(TAG, "Started");


			// find no. of connected devices
			findDevices();

			//*

			String o_s = Helper.o_config.getString("url");
			Log.v(TAG, "Setting URL");
			Helper.o_url = new URL(o_s);

			Helper.o_config.putString("ext", o_s.substring(o_s.length()-4, o_s.length()));
			//Log.v(o_master, "Extension : " + Helper.o_config.getString("ext"));

			Helper.o_config.putString("name", o_s.substring(o_s.lastIndexOf('/')));

			Log.v(TAG, "Requesting URL");
			HttpURLConnection conn = (HttpURLConnection) (new URL(Helper.o_config.getString("url"))).openConnection();
			
			conn.setRequestMethod("HEAD");
			Log.v(TAG, "Connection status : " + conn.getResponseCode());
			if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){

				final int len = conn.getContentLength();
				Helper.o_config.putInt("len", len);
				Helper.o_pool_config = new ArrayList<Bundle>();

				// Initialize No. of Parts
				Helper.o_no_parts = Math.min((int) Math.ceil(1.0*len/Helper.o_size), Helper.o_maxParts);

				Helper.isDone = new boolean[Helper.o_no_parts];
				Helper.o_isRunning = new boolean[Helper.o_no_parts];

				Helper.o_config.putInt("parts", Helper.o_no_parts);

				// Set the block size for part if not default
				if(Helper.o_no_parts == Helper.o_maxParts) {
					Helper.o_size = len/Helper.isDone.length;
				}

				Log.v(TAG, "No. of Parts : " + Helper.o_no_parts + " Content len : " + len);

				for(int i=0, offset = 0; i<Helper.isDone.length; i++, offset++) {
					Bundle b = new Bundle();
					//Crash count
					//To store path in bundle
					b.putInt("crash", 0);
					b.putInt("id", i);
					b.putInt("start", offset);
					b.putInt("end", Math.min(offset+=Helper.o_size, len));
					b.putString("path", Helper.o_path + "/tmp" + b.getInt("id"));// + o_config.getString("ext"));
					b.putString("url", Helper.o_config.getString("url"));
					Helper.o_pool_config.add(b);		
				}

				if(bcomm.isFinished()){
					Log.v(TAG, "Download Manager Started");
					ActivityMaster.execMaster.execute(new DownloadManager(oh_Start, bcomm));
				}
				else{
					Log.v(TAG, "Error");
					oh_Start.obtainMessage(Helper.TYPE_STRING, -1, -1, "Error Occured Please try again :)");
				}	
			}
			//*/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.v(TAG, e.toString());
		}

		//*/
	}

	private void findDevices() {
		// TODO Auto-generated method stub

		Helper.o_slave_pool = new SparseArray<BluetoothDevice>();
		try{
			Log.v(TAG, "getting paired devices");
			bcomm = new BluetoothComm(oh_Start, seq_StartOfMain);
			Set<BluetoothDevice> o_bonded = ActivityMaster.o_myBT.getBondedDevices();
			for(BluetoothDevice o_bon: o_bonded){
				bcomm.connect(o_bon, Helper.HANDSHAKE);
			}

			/*
			for(int i=1; i<=Helper.o_no_devices; i++){
				Log.v(TAG, "Sending to Device " + i);
				bcomm.sendMessage("Master is A117 Bitches", i);
			}
			//*/
		}catch(Exception e){
			Log.v(TAG, e.toString() + "Exception to find Devices");
			e.printStackTrace();
		}
	}
}
