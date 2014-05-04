package in.ac.iiitd.androidsyncapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.Bundle;
import android.os.Handler;
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
			conn = (HttpURLConnection) (new URL(o_config.getString("url"))).openConnection();

			int start  = o_config.getInt("start"), end = o_config.getInt("end");
			
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Range", "bytes=" + start + '-' + end);
			
			double percent1 = ((double) end - start)/100;
			Log.v(TAG, "bytes=" + o_config.getInt("start") + '-' + o_config.getInt("end"));
			Log.v(TAG, "Response Code : " + conn.getResponseCode());

			if(conn.getResponseCode() == HttpURLConnection.HTTP_PARTIAL){
				//Log.v(TAG, "Partial Download possible... Downloading");
				oh_DownloadFile.obtainMessage(Helper.TYPE_DOWNLOAD_BAR_SET, "Part").sendToTarget();
				
				o_os = new FileOutputStream(new File(o_config.getString("path")));

				o_buff = new byte[Helper.o_buffLength];
				try {
					int o_bytesRead, tot_read = 0;
					o_is = conn.getInputStream(); 
					while((o_bytesRead = o_is.read(o_buff, 0, Helper.o_buffLength)) != -1){
						o_os.write(o_buff, 0, o_bytesRead);
						tot_read += o_bytesRead;
						if(tot_read > percent1){
							oh_DownloadFile.obtainMessage(Helper.TYPE_DOWNLOAD_BAR_UPDATE, (int) percent1, -1).sendToTarget();
							tot_read %= percent1;
						}
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
				o_config.putString("isDone", "Partial Downloading Not possible\n@Furious-Zombie-Salt");
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.v(TAG, e.toString() + " Crash count : " + o_config.getInt("crash"));
			int c;
			if((c = o_config.getInt("crash")) < Helper.o_crash_threshold){
				o_config.putInt("crash", c+1);
				run();
			}else{
				oh_DownloadFile.obtainMessage(Helper.TYPE_NAK_PART, o_config.getInt("id"),
						o_config.getInt("deviceID"), null).sendToTarget();
				
				o_config.putString("isDone", "Download Failed... Try Again");
			}
		}
	}
}
