package in.ac.iiitd.androidsyncapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class DownloadFile extends AsyncTask<Bundle, Integer, Integer>{

	private final static String o_download = "master Download File";

	/**
	 * Contains All the configuration for downloading the file from the url, 
	 * like start offset, ending offset, id, size.
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

	@Override
	protected Integer doInBackground(Bundle... b) {
		// TODO Auto-generated method stub
		Log.v(o_download, "doInBackgroung Thread");
		
		try {
			o_config = b[0];
			conn = (HttpURLConnection) Helper.o_url.openConnection();
			
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Range", "bytes=" + o_config.getInt("start") + '-' + o_config.getInt("end"));
			
			Log.v(o_download, "bytes=" + o_config.getInt("start") + '-' + o_config.getInt("end"));
			Log.v(o_download, "Response Code : " + conn.getResponseCode());

			if(conn.getResponseCode() == HttpURLConnection.HTTP_OK || conn.getResponseCode() == HttpURLConnection.HTTP_PARTIAL){
				Log.v(o_download, "Partial Download possible... Downloading");

				//Make directory /AndroidSync
				Helper.o_path.mkdir();

				o_os = new FileOutputStream(new File(Helper.o_path + "/image" + o_config.getInt("id")));// +o_config.getString("ext")));

				//To store path in bundle
				o_config.putString("path", Helper.o_path.getAbsolutePath() + "/image" + o_config.getInt("id"));// +o_config.getString("ext"));

				o_buff = new byte[Helper.o_buffLength];
				try {
					int o_bytesRead;
					o_is = conn.getInputStream(); 
					while((o_bytesRead = o_is.read(o_buff, 0, Helper.o_buffLength)) != -1){
						o_os.write(o_buff, 0, o_bytesRead);
					}               

					Log.v(o_download, "Downloaded Part" + o_config.getInt("id"));
					o_config.putString("isDone", "Downloaded Part" + o_config.getInt("id"));
					Helper.o_partDone(o_config.getInt("id"));

				}catch(Exception e){
					Log.v(o_download, e.toString());
				} finally {
					if (o_is != null) {
						o_is.close();  
					}
					if(o_os != null){
						o_os.flush();
						o_os.close();
					}
					Log.v(o_download, "streams closed");
				}
			}
			else{
				o_config.putString("isDone", "Part" + o_config.getInt("id") + " failed... Trying Again");					
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.v(o_download, e.toString());
			o_config.putString("isDone", "Download Failed... Trying Again");
		}

		return null;
	}

	@Override
	protected void onPostExecute(Integer i){
		Helper.o_updateText(o_config.getString("isDone"));
	}

}
