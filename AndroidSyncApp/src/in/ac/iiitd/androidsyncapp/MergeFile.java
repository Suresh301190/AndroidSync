package in.ac.iiitd.androidsyncapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

/**
 * Merges the files on the "/AndroidSync/" folder by path info contained in Bundles "path" 
 * in back ground
 * @author Suresh
 *
 */
public class MergeFile extends AsyncTask<Bundle, Void, Void>{

	private static final String o_merge = "master Merge File";

	@Override
	protected Void doInBackground(Bundle... bundles) {
		// TODO Auto-generated method stub
		Log.v(o_merge, "MergeFile --- doInBackground");
		
		try {
			// open the main file for writing
			OutputStream o_os = new FileOutputStream(new File(Helper.o_path + bundles[0].getString("name")));
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
			Log.v(o_merge, e.toString());
			Helper.o_config.putString("isDone", "Curruption... Sorry Retry Again");
		}		
		return null;
	}
	
	@Override
	protected void onPostExecute(Void i){
		Helper.o_updateText(Helper.o_config.getString("isDone"));
		Log.v(o_merge, Helper.o_config.getString("isDone"));
	}
}
