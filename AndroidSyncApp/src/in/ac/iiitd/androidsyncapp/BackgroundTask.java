package in.ac.iiitd.androidsyncapp;

import android.media.MediaPlayer;


public class BackgroundTask implements Runnable{
	
	MediaPlayer mp;
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		for(int i=0; i<=100; i+=10){
			final int value = i;
			try{
				Thread.sleep(1000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			MainActivity.bar.setProgress(value);
		}
	}
	
}
