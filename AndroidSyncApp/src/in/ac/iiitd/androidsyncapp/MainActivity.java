package in.ac.iiitd.androidsyncapp;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.SeekBar;

public class MainActivity extends Activity {

	public static SeekBar bar;
	private static MediaPlayer mp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		bar = (SeekBar) findViewById(R.id.seekBar1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void startMP(View view){		
		new Thread(new BackgroundTask()).start();
		//new Thread(new MedaiStart()).start();
	}
	
	public void switchMaster(View view){
		Intent menuIntent = new Intent(this, ActivityMaster.class);
		startActivity(menuIntent);
	}

	class MedaiStart implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try{
				
				mp = MediaPlayer.create(getApplicationContext(), R.raw.video_file_1);
				mp.start();
			}catch(Exception e){
				e.printStackTrace();
			}
			mp.release();
			mp = null;
		}
	}
	
	
}
