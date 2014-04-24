package in.ac.iiitd.androidsyncapp;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class StartOfMain extends AsyncTask<Void, Void, Void>{

	private static final String o_master = "master StartOfMain";
	
	@Override
	protected Void doInBackground(Void... voids) {
		// TODO Auto-generated method stub
		Log.v(o_master, "doInBackground");
		try {
			HttpURLConnection conn = (HttpURLConnection) (new URL(Helper.o_config.getString("url"))).openConnection();
				conn.setRequestMethod("HEAD");
				Log.v(o_master, "Connection status : " + conn.getResponseCode());
				if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
					
					final int len = conn.getContentLength();
					Helper.o_config.putInt("len", len);
					Helper.o_sort_config = new ArrayList<Bundle>();
					
					Helper.isDone = new boolean[(int) Math.ceil(len/Helper.o_size)];
					Helper.o_config.putInt("parts", Helper.isDone.length);
					
					Log.v(o_master, "No. of Parts : " + Helper.isDone.length + " Content len : " + len);
					for(int i=0; i<Helper.isDone.length; i++){
						Bundle b = new Bundle(Helper.o_config);
						b.putInt("id", i);
						b.putInt("start", i*Helper.o_size);
						b.putInt("end", Math.min((i+1)*Helper.o_size, len));
						Helper.o_sort_config.add(b);			
					}
					
					Log.v(o_master, "All Bundles created for execution");
					
					for(Bundle o_config:Helper.o_sort_config){
						new DownloadFile().execute(o_config);
					}
					
					Log.v(o_master, "All Parts running in background");
					
					while(true){
						if(Helper.o_allDone()){
							break;
						}
						Thread.sleep(1000);
					}					
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.v(o_master, e.toString());
			}
		
		return null;
	}
	
	@Override
	protected void onPostExecute(Void v){
		Log.v(o_master, "Parts Downloaded Successfully");
		new MergeFile().execute((Bundle[]) Helper.o_sort_config.toArray());
	}

}
