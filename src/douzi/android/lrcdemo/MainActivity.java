package douzi.android.lrcdemo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import douzi.android.view.LrcView;
import douzi.android.view.LrcView.LrcRow;
import douzi.android.view.LrcView.OnLrcViewListener;

public class MainActivity extends Activity {

	public final static String TAG = "MainActivity";
	LrcView mLrcView;
	MediaPlayer mPlayer;
	private boolean mIsPlaying = false;
	
	
    public String getFromAssets(String fileName){ 
        try { 
             InputStreamReader inputReader = new InputStreamReader( getResources().getAssets().open(fileName) ); 
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line="";
            String Result="";
            while((line = bufReader.readLine()) != null){
            	if(line.trim().equals(""))
            		continue;
            	Result += line + "\r\n";
            }
            return Result;
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return "";
    } 
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLrcView = new LrcView(this, null);
//        mLrcView.setBackgroundResource(R.drawable.jb_bg);
        setContentView(mLrcView);
        String path = "file:///android_asset/test.lrc";
		Log.d(TAG, "lrc path:" + path);
        
        String mp3Path = "/sdcard/xuebuhui.mp3";
        
        String lrc = getFromAssets("test.lrc");
        Log.d(TAG, "lrc:" + lrc);
        
        mLrcView.loadLrc(lrc);
        
        startPlay();
    
        mPlayer = new MediaPlayer();
        try {
			mPlayer.setDataSource(mp3Path);
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mPlayer.setOnPreparedListener(new OnPreparedListener() {
				
				public void onPrepared(MediaPlayer arg0) {
					Log.d(TAG,"onPrepared");
					startPlay();
				}
			});
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        mLrcView.setOnLrcViewSeekListener(new OnLrcViewListener() {
			
			public void onSeek(int position, LrcRow row) {
				mPlayer.seekTo((int)row.time);
			}

			public void didLrcLoad(boolean sucess) {
				Log.d(TAG, "lrc loaded:" + sucess);
				setTitle("Lrc Downloaded");
				if(mIsPlaying){
					if(mPlayer != null){
						mLrcView.timerSeekTo(mPlayer.getCurrentPosition());
					}
				}
			}

		});
        
        mPlayer.prepareAsync();
        setTitle("Lrc Downloading");
    }
    
    private  void startPlay(){
    	if(mIsPlaying == false){
//    		mPlayer.start();
    		mLrcView.beginLrcPlay();
    		mIsPlaying = true;
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if(mPlayer != null){
    		mPlayer.stop();
    		mPlayer.release();
    		mLrcView.stopLrcPlay();
    		mPlayer = null;
    	}
    }
    
}
