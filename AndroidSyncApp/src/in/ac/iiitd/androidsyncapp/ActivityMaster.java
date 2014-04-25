package in.ac.iiitd.androidsyncapp;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
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
	private static final String o_master = "master";

	/**
	 * Contains Default bluetooth adapter if none conatins null
	 */
	private static BluetoothAdapter o_myBT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_master);
		o_myBT = BluetoothAdapter.getDefaultAdapter();
		Helper.o_update = (TextView) findViewById(R.id.o_dl_update);
		Helper.o_prog_bar = (SeekBar) findViewById(R.id.o_progress);
	}

	public void download_file(View view){
		try{
			Log.v(o_master, "Clicked Download in Master");

			Helper.o_config = new Bundle();
			Helper.o_config.putString("url", ((EditText) findViewById(R.id.o_URL_box_m)).getText().toString());

			Log.v(o_master, Helper.o_config.getString("url"));

			if(Helper.o_config.getString("url").length() == 0){
				o_showToast("Downloading from default URL");
				Log.v(o_master, "Background thread started");

				//o_config.putString("url", "https://dl.dropboxusercontent.com/u/108785914/TechReport.pdf");
				Helper.o_config.putString("url", "https://dl.dropbox.com/u/9097066/image.png");
				Helper.o_config.putInt("id", 1);

				new StartOfMain().execute();

			}
		}catch(Exception e){
			Log.v(o_master, e.toString());
		}
	}

	/**
	 * Checks for URL if something can be downloaded ?
	 * @param URL to be checked
	 * @return true if URL is valid
	 */
	boolean isValid(String URL) {
		// TODO Auto-generated method stub	
		Log.v(o_master, "isValid()");

		return true;
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
