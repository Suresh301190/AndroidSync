package in.ac.iiitd.androidsyncapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ActivitySlave extends Activity{

	private static final String TAG = "AndroidSync ActivitySlave";

	private static TextView os_update, os_url;
	
	private SlaveServer sServer;
	
	private static ProgressBar os_progBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "switched to slave");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_slave);
		os_update = (TextView) findViewById(R.id.o_slave_updates);
		os_url = (TextView) findViewById(R.id.os_urlTextView);
		os_progBar = (ProgressBar) findViewById(R.id.os_progress);
		sServer = new SlaveServer();
		sServer.start();
		//bcomm = new BluetoothComm(oh_Slave, null);
		//bcomm.start();
	}

	public final static Handler oh_Slave = new Handler(){

		@Override
		public void handleMessage(Message msg){
						
			switch(msg.what){
			case Helper.TYPE_STRING: 
				os_update.append("\n" + msg.obj);
				break;
			case Helper.TYPE_BUNDLE:
								
			case Helper.TYPE_URL:
				os_url.setText(Helper.o_config.getString("url"));
				break;
				
			case Helper.TYPE_DOWNLOAD_COMPLETE:
				os_update.setText((String) msg.obj);
				break;
				
			case Helper.TYPE_DOWNLOAD_BAR_UPDATE:
				os_progBar.incrementProgressBy(msg.arg1);
				break;
				
			case Helper.TYPE_DOWNLOAD_BAR_SET:
				os_progBar.setProgress(0);
				break;
				
			case Helper.TYPE_FROM_MASTER:
				os_update.setText("Success Connected to " + msg.obj);
				break;
				
			}
		}	
	};
	
	public void reset(View view){
		sServer.close();
	}
	
	protected void o_showToast(String o_msg){
		Toast.makeText(getApplicationContext(), o_msg
				, Toast.LENGTH_LONG).show();
	}
}
