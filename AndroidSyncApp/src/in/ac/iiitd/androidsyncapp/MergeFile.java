package in.ac.iiitd.androidsyncapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Merges the files on the "/AndroidSync/" folder by path info contained in Bundles "path" 
 * in back ground
 * @author Suresh
 *
 */
public class MergeFile extends Thread{

	private static final String TAG = "AndroidSync MergeFile";
	
	private final Handler oh_MergeFile;
	
	public MergeFile(Handler h){
		oh_MergeFile = h;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		//Log.v(TAG, "MergeFile --- doInBackground");
		
		try {
			// open the main file for writing
			OutputStream o_os = new FileOutputStream(new File(Helper.o_path + Helper.o_config.getString("name")));
			InputStream o_is;
			byte[] o_buff = new byte[Helper.o_buffLength];
			
			// for each part read and write to file in sequential order
			for(Bundle o_config:Helper.o_pool_config){
				o_is = new FileInputStream(o_config.getString("path"));
				int len;
				
				// THis actually writes
				while((len = o_is.read(o_buff, 0, Helper.o_buffLength)) != -1){
					o_os.write(o_buff, 0, len);
				}
				o_is.close();
				new File(o_config.getString("path")).delete();
			}			
			o_os.close();
			
			Helper.o_config.putString("isDone", "Success Please view the file");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.v(TAG, e.toString());
			Helper.o_config.putString("isDone", "Curruption... Sorry Retry Again");
		}		
		onPostExecute();
	}
	
	protected void onPostExecute(){
		
		Message msg = oh_MergeFile.obtainMessage();
		msg.arg1 = 100;
		msg.obj = Helper.o_config.getString("isDone");
		oh_MergeFile.sendMessage(msg);
		Log.v(TAG, Helper.o_config.getString("isDone"));
		Helper.reset();
	}


}
