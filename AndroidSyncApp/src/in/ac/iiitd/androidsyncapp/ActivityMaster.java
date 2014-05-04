package in.ac.iiitd.androidsyncapp;

import java.io.File;
import java.net.URLConnection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityMaster extends Activity{

	/**
	 * Some random sequence for starting
	 */
	private static int o_activityID = 5687;

	/**
	 * Response codes for various activity
	 */
	private static final int o_enableBT = ++o_activityID, o_enableDiscovery = ++o_activityID;
	private static final String o_master = "AndroidSync ActivityMaster";

	public static final ExecutorService execMaster = Executors.newCachedThreadPool();

	public static ProgressBar o_progBar;

	private BluetoothComm bcomm;

	/**
	 * Contains Default bluetooth adapter if none conatins null
	 */
	public static BluetoothAdapter o_myBT; 

	private boolean def = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(o_master, "Switched to Master");
		setContentView(R.layout.activity_master);
		o_myBT = BluetoothAdapter.getDefaultAdapter();
		o_progBar = (ProgressBar) findViewById(R.id.o_progMaster);	
	}

	public void download_file(View view){
		try{
			if(def) Helper.reset();
			//Helper.reset();
			Log.v(o_master, "Clicked Download in Master");

			if(def) Helper.o_config = new Bundle();
			if(def) Helper.o_config.putString("url", ((EditText) findViewById(R.id.o_URL_box_m)).getText().toString());

			Log.v(o_master, Helper.o_config.getString("url"));

			if(Helper.o_config.getString("url").length() == 0){
				o_showToast("Downloading from default URL");
				//Log.v(o_master, "Background thread started");

				//o_config.putString("url", "https://dl.dropboxusercontent.com/u/108785914/TechReport.pdf");
				Helper.o_config.putString("url", "https://dl.dropbox.com/u/9097066/image.png");
				//Helper.o_config.putString("url", "https://dl.dropbox.com/u/9097066/barfi.mp4");
				//Helper.o_config.putString("url", "https://dl.dropbox.com/u/9097066/amc.mp3");
				Helper.o_config.putInt("id", 1);

				execMaster.execute(new StartOfMain(oh_Master, Executors.newSingleThreadExecutor()));
				//new StartOfMain(oh_Master, Helper.seqExec).start();
				//bcomm = new BluetoothComm(oh_Master, null);
				//bcomm.connect(o_myBT.getRemoteDevice("41:2F:C6:0A:F5:52"));		// Mom's
				//bcomm.connect(o_myBT.getRemoteDevice("18:26:66:6B:33:1D"), HANDSHAKE);		// Ritika			
			}
			else{
				o_showToast("Downloading from URL");
				Helper.o_config.putInt("id", 1);
				execMaster.execute(new StartOfMain(oh_Master, Executors.newSingleThreadExecutor()));
			}
		}catch(Exception e){
			Log.v(o_master, e.toString());
			e.printStackTrace();
		}
	}

	public void sendMsg(View view){
		//for(int i=1; i<= Helper.o_no_devices; i++){
		bcomm.sendMessage("It Works Yipee");
		//bcomm2.sendMessage("It Works Yipee");
		//}

		/*
		Bundle b = new Bundle();
		b.putString("device", o_master);
		b.putInt("id", 3);
		bcomm.sendBundle(b, 1, HANDSHAKE);
		//*/
	}

	@SuppressLint("HandlerLeak")
	public final Handler oh_Master = new Handler(){

		@Override
		public void handleMessage(Message msg){
			//o_progBar.setProgress(msg.arg1);

			switch(msg.what){
			case Helper.TYPE_UPDATE_PROGRESS:
				o_progBar.setProgress(msg.arg1);
				//o_update.append("\n" + msg.obj);
				o_showToast((String) msg.obj);
				break;

			case Helper.TYPE_FORWARD_PART:
				Helper.o_partDone(((Bundle) msg.obj).getInt("id"));
				o_showToast("Received Part" + ((Bundle) msg.obj).getInt("id") + " from slave");
				break;

			case Helper.TYPE_ONLY_PART_SLAVE:
				o_showToast("Downloaded Part" + msg.arg1);
				Helper.o_partDone(msg.arg1);
				Helper.o_isDownloading[msg.arg2] = false;
				break;

			case Helper.TYPE_NAK_PART:
				Helper.o_isRunning[msg.arg1] = false;
				break;

			case Helper.TYPE_DOWNLOAD_BAR_UPDATE:
				o_progBar.setProgress(msg.arg1);
				break;

			case Helper.TYPE_DOWNLOAD_BAR_SET:
				o_progBar.setProgress(0);
				o_progBar.setMax(msg.arg1);
				break;

			case Helper.TYPE_FROM_MASTER:
				o_showToast("Success Connected to " + msg.obj);
				break;
			}
		}
	};

	public void imageURL(View view){
		Helper.reset();
		Helper.o_config = new Bundle();
		Helper.o_config.putString("url", "https://dl.dropbox.com/u/9097066/image.png");
		((EditText) findViewById(R.id.o_URL_box_m)).setText(Helper.o_config.getString("url"));
		def = false;
	}

	public void videoURL(View view){
		Helper.reset();
		Helper.o_config = new Bundle();
		Helper.o_config.putString("url", "https://dl.dropboxusercontent.com/u/108785914/BlueSwedeHQ.mp4");
		((EditText) findViewById(R.id.o_URL_box_m)).setText(Helper.o_config.getString("url"));

		def = false;
	}

	public void mp3URL(View view){
		Helper.reset();
		Helper.o_config = new Bundle();
		Helper.o_config.putString("url", "https://dl.dropboxusercontent.com/u/108785914/BlueSwede.mp3");
		((EditText) findViewById(R.id.o_URL_box_m)).setText(Helper.o_config.getString("url"));
		def = false;
	}

	public void playVideo(View view) {
		File videoFile = new File (Helper.o_config.getString("path"));

		if (videoFile.exists()) {
			Uri fileUri = Uri.fromFile(videoFile);
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setDataAndType(fileUri, URLConnection.guessContentTypeFromName(fileUri.toString()));
			startActivity(intent);
		} else {
			Toast.makeText(this, "Video file does not exist", Toast.LENGTH_LONG).show();
		}

	}

	public void reset(View view){
		
	}

	/**
	 * To enable the bluetooth if present
	 * @param view default view
	 */
	public void enableBT(View view){
		Log.v(o_master, "In Enable BT");
		if(o_noBT()) return;

		if(!o_myBT.isEnabled()){
			Intent enableBTIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBTIntent, o_enableBT);

			if(!o_myBT.isEnabled()) return;	// if BT failed to Turn On
		}		
	}

	public void showPairedDevices(View view){
		if(o_noBT()) return;
		if(!o_myBT.isEnabled()){
			enableBT(view);
			if(!o_myBT.isEnabled()){
				return;
			}
		}

		/*
		 * get Devices then update the TextView to show all the paired devices
		 */

		Set<BluetoothDevice> o_pairedBT = o_myBT.getBondedDevices();
		if(o_pairedBT.size() > 0){
			TextView tv = (TextView) findViewById(R.id.textView1);
			tv.setText(o_pairedBT.toString());
		}
		else{
			o_showToast("No Paired Devices");
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent){
		super.onActivityResult(requestCode, resultCode, intent);

		if(o_enableBT == requestCode){
			if(resultCode == RESULT_OK){
				o_showToast("BT Turned On");
			}
			else{
				o_showToast("BT Failed to Turn On");
			}
		}
		if(o_enableDiscovery == resultCode){
			if(resultCode == RESULT_OK){
				o_showToast("BT Discovery Turned On");
			}
			else{
				o_showToast("BT Not Discoverable");
			}
		}
	}

	/*
	 * to check if device contains Bluetooth
	 */
	private boolean o_noBT(){
		if(null == o_myBT){
			o_showToast("No BT available");
			return true;
		}
		return false;
	}

	private void o_showToast(String o_msg){
		Toast.makeText(getApplicationContext(), o_msg
				, Toast.LENGTH_LONG).show();
	}
}
