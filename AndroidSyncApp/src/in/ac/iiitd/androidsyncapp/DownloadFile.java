package in.ac.iiitd.androidsyncapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class DownloadFile extends Thread{

	private final static String TAG = "AndroidSync DownloadFile";

	/**
	 * Configuration which contains 
	 * deviceID -- (int) task Scheduled on, 
	 * id -- (int) Part id, 
	 * start -- start of part block,
	 * end -- (int) end of part block, 
	 * isDone -- (String) message if download was successful/ Failed, 
	 * url -- (String) url from which to download, 
	 * size -- (int) content length, 
	 * path -- (String) directory where the part was stored in sd-card, 
	 * name -- (String) name of the file
	 * crash -- (int) crash count for the respective part
	 * isDone -- (String) Message regarding the part file success/failure.
	 */
	private Bundle o_config;

	/**
	 * To read the content from the url
	 */
	private InputStream o_is;

	/**
	 * TO write the content to the file in /AndroidSync
	 */
	private OutputStream o_os;

	/**
	 * To manage the connection
	 */
	private HttpURLConnection conn;

	/**
	 * Temporary Buffer to read from URL and write to file 
	 */
	byte[] o_buff;

	private final Handler oh_DownloadFile;

	public DownloadFile(Bundle b, Handler h){
		o_config = b;
		oh_DownloadFile = h;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		try {
			conn = (HttpURLConnection) Helper.o_url.openConnection();

			conn.setRequestMethod("GET");
			conn.setRequestProperty("Range", "bytes=" + o_config.getInt("start") + '-' + o_config.getInt("end"));

			//Log.v(o_download, "bytes=" + o_config.getInt("start") + '-' + o_config.getInt("end"));
			//Log.v(o_download, "Response Code : " + conn.getResponseCode());

			if(conn.getResponseCode() == HttpURLConnection.HTTP_PARTIAL){
				//Log.v(TAG, "Partial Download possible... Downloading");

				//To store path in bundle
				o_config.putString("path", Helper.o_path + "/tmp" + o_config.getInt("id"));// + o_config.getString("ext"));

				o_os = new FileOutputStream(new File(o_config.getString("path")));

				o_buff = new byte[Helper.o_buffLength];
				try {
					int o_bytesRead;
					o_is = conn.getInputStream(); 
					while((o_bytesRead = o_is.read(o_buff, 0, Helper.o_buffLength)) != -1){
						o_os.write(o_buff, 0, o_bytesRead);
					}               

					Log.v(TAG, "Downloaded Part" + o_config.getInt("id"));
					o_config.putString("isDone", "Downloaded Part" + o_config.getInt("id"));
					
					oh_DownloadFile.obtainMessage(Helper.TYPE_FORWARD_PART, -1, -1, o_config).sendToTarget();

					oh_DownloadFile.obtainMessage(Helper.TYPE_UPDATE_PROGRESS, 
							100*Helper.o_progress/Helper.o_config.getInt("len"), -1, o_config.getString("isDone")).sendToTarget();

					//Helper.o_updateText(o_config.getString("isDone"));
					Helper.o_isDownloading[o_config.getInt("deviceID")] = false;

				}catch(Exception e){

				} finally {
					if (o_is != null) {
						o_is.close();  
					}
					if(o_os != null){
						o_os.flush();
						o_os.close();
					}
					//Log.v(TAG, "streams closed");
				}

			}
			else{
				o_config.putString("isDone", "Partial Downloading Not possible");					
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.v(TAG, e.toString() + " Crash count : " + o_config.getInt("crash"));
			if(o_config.getInt("crash") < Helper.o_crash_threshold){
				run();
			}else{
				o_config.putString("isDone", "Download Failed... Try Again");
			}
		}
	}
}
