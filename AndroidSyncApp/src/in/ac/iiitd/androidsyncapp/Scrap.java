package in.ac.iiitd.androidsyncapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class Scrap extends Activity{

	private static String o_master = "master";

	protected void demo() {
		// Create Inner Thread Class
		Thread background = new Thread(new Runnable() {

			private final HttpClient Client = new DefaultHttpClient();
			final String URL = "https://dl.dropboxusercontent.com/u/9097066/image.png";

			// After call for background.start this run method call
			public void run() {
				try {
					Log.v(o_master, "Inside Thread Download");

					HttpGet httpget = new HttpGet(URL);
					HttpResponse o_response = Client.execute(httpget);

					Message msgObj = handler.obtainMessage();
					Bundle b = new Bundle();
					b.putInt("status", o_response.getStatusLine().getStatusCode());
					//final double len = o_response;

					if(b.getInt("status") == HttpStatus.SC_OK){
						Log.v(o_master, "Download started");
						final HttpEntity o_entity = o_response.getEntity();
						if(o_entity != null){
							InputStream o_is = null;
							File o_path = Environment.getExternalStorageDirectory();
							OutputStream o_os = new FileOutputStream(o_path + "/image.png");
							byte[] o_buff = new byte[Helper.o_buffLength];
							try {
								int o_bytesRead;

								o_is = o_entity.getContent(); 
								while((o_bytesRead = o_is.read(o_buff, 0, Helper.o_buffLength)) != -1){
									o_os.write(o_buff, 0, o_bytesRead);
								}
								b.putString("isDone", "OK");

								Log.v(o_master, "Download Finished");
							}catch(Exception e){
								Log.v(o_master, e.toString());
							} finally {
								if (o_is != null) {
									o_is.close();  
								}
								if(o_os != null){
									o_os.flush();
									o_os.close();
								}
								Log.v(o_master, "streams closed");
								o_entity.consumeContent();
							}
						}
					}
					msgObj.setData(b);
					handler.sendMessage(msgObj);
					Log.v(o_master, "Message sent to Handler");

				} catch (Throwable t) {
					// just end the background thread
					Log.i("Animation", "Thread  exception " + t);
				}
			}

			private final Handler handler = new Handler(){
				public void handleMessage(Message msg){
					Toast.makeText(getBaseContext(), ""+ msg.getData().getInt("status"), Toast.LENGTH_LONG).show();
					Toast.makeText(getBaseContext(), ""+ msg.getData().getString("isDone"), Toast.LENGTH_LONG).show();
				}
			};
		});

		// Start Thread
		//background.start();  //After call start method thread called run Method
	}
}
