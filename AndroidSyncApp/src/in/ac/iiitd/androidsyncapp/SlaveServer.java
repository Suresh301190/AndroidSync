package in.ac.iiitd.androidsyncapp;

import java.net.MalformedURLException;
import java.net.URL;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

class SlaveServer extends Thread{
	
	private static final String TAG ="AndroidSync SlaveServer";	

	private static final Handler oh_SlaveServer = new Handler(){

		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
			case Helper.TYPE_BUNDLE:
				Log.v(TAG, "Handling Bundle");
				Helper.o_config = (Bundle) msg.obj;
				
				try {
					Helper.o_url = new URL(Helper.o_config.getString("url"));
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ActivitySlave.oh_Slave.obtainMessage(Helper.TYPE_URL, -1, -1, Helper.o_config.get("url")).sendToTarget();
				/*
				Bundle b = new Bundle(Helper.o_config);
				b.putInt("start", 0);
				b.putInt("end", Helper.o_config.getInt("len"));
				b.putInt("deviceID", 1);
				new DownloadFile((Bundle) msg.obj, oh_SlaveServer).start();
				//*/
				break;
			case Helper.TYPE_FORWARD:
				
				break;
			case Helper.TYPE_DOWNLOAD_PART_REQUEST:
				new DownloadFile((Bundle) msg.obj, oh_SlaveServer).start();
				break;
				
			case Helper.TYPE_UPDATE_PROGRESS:
				// Redirect Message to Slave Activity
				ActivitySlave.oh_Slave.obtainMessage(Helper.TYPE_UPDATE_PROGRESS, Helper.o_progress, -1, msg.obj);
				break;
				
			case Helper.TYPE_FORWARD_PART:
				Bundle b = (Bundle) msg.obj;
				ActivitySlave.oh_Slave.obtainMessage(Helper.TYPE_STRING, -1, -1, b.getString("isDone"));
				Log.v(TAG, "Sending Part" + b.getInt("part") + " to Master");				
				//bcomm.sendFile((Bundle) msg.obj, Helper.TYPE_FORWARD_PART);
				bcomm.sendFile(b.getString("path"), b.getInt("id"), b.getInt("deviceID"));
			}
		}	
	};

	private static final BluetoothComm bcomm;
	
	static{
		bcomm = new BluetoothComm(oh_SlaveServer, null);
	}
	public static BluetoothDevice master;

	@Override
	public void run(){
		bcomm.start();
	}
	
	/**Closes all the streams of Bluetooth Communication */
	public void close(){
		bcomm.reset();
	}
}