package in.ac.iiitd.androidsyncapp;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityMaster extends Activity{
	
	private static int activityID = 5687;
	private static final int enableBT = ++activityID, enableDiscovery = ++activityID;
	private static BluetoothAdapter myBT;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_master);
		myBT = BluetoothAdapter.getDefaultAdapter();
	}
	
	public void enableBT(View view){
		if(noBT()) return;

		if(!myBT.isEnabled()){
			Intent enableBTIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBTIntent, enableBT);

			if(!myBT.isEnabled()) return;	// if BT failed to Turn On
		}		
	}
	
	public void showPairedDevices(View view){
		if(noBT()) return;
		if(!myBT.isEnabled()){
			enableBT(view);
			if(!myBT.isEnabled()){
				return;
			}
		}
		
		/*
		 * get Devices then update the TextView to show all the paired devices
		 */
		
		Set<BluetoothDevice> pairedBT = myBT.getBondedDevices();
		if(pairedBT.size() > 0){
			TextView tv = (TextView) findViewById(R.id.textView1);
			tv.setText(pairedBT.toString());
		}
		else{
			Toast.makeText(getApplicationContext(), 
					"No Paired Devices", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent){
		super.onActivityResult(requestCode, resultCode, intent);

		if(enableBT == requestCode){
			if(resultCode == RESULT_OK){
				Toast.makeText(getApplicationContext(), 
						"BT Turned On", Toast.LENGTH_LONG).show();
			}
			else{
				Toast.makeText(getApplicationContext(), 
						"BT Failed to Turn On", Toast.LENGTH_LONG).show();
			}
		}
		if(enableDiscovery == resultCode){
			if(resultCode == RESULT_OK){
				Toast.makeText(getApplicationContext(), 
						"BT Discovery Turned On", Toast.LENGTH_LONG).show();
			}
			else{
				Toast.makeText(getApplicationContext(), 
						"BT Not Discoverable", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	/*
	 * to check if device contains Bluetooth
	 */
	private boolean noBT(){
		if(null == myBT){
			Toast.makeText(getApplicationContext(), "No BT available"
					, Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}
}
