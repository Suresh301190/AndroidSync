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
			OutputStream o_os = new FileOutputStream(new File(Helper.o_path + "/Final_file" + bundles[0].getString("ext")));
			InputStream o_is;
			byte[] o_buff = new byte[Helper.o_buffLength];
			
			for(Bundle o_config:bundles){
				o_is = new FileInputStream(o_config.getString("path"));
				int len;
				while((len = o_is.read(o_buff, 0, Helper.o_buffLength)) != -1){
					o_os.write(o_buff, 0, len);
				}
				o_is.close();
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
	}
}