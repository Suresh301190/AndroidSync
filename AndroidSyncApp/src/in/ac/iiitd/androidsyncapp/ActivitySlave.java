package in.ac.iiitd.androidsyncapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

public class ActivitySlave extends Activity{

	private static final String TAG = "AndroidSync ActivitySlave";
	
	private static TextView o_update;
	
	private BluetoothComm bcomm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "switched to slave");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_slave);
		o_update = (TextView) findViewById(R.id.o_slave_updates);

		bcomm = new BluetoothComm(new Bundle(), oh_Slave, null);
		bcomm.start();
	}
	
	private final static Handler oh_Slave = new Handler(){
		
		@Override
		public void handleMessage(Message msg){
			o_update.append("\n" + (String) msg.obj);
		}		
	};

	// Copy over the saved code from previous file

}
