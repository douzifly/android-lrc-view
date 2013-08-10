package douzi.android.lrcdemo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import douzi.android.view.DefaultLrcBuilder;
import douzi.android.view.ILrcBuilder;
import douzi.android.view.ILrcView;
import douzi.android.view.LrcRow;
import douzi.android.view.LrcView;

public class MainActivity extends Activity {

	public final static String TAG = "MainActivity";
	ILrcView mLrcView;
    private int mPalyTimerDuration = 1000;
    private Timer mTimer;
    private TimerTask mTask;
	
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
        setContentView((View) mLrcView);
        //file:///android_asset/test.lrc;
        String lrc = getFromAssets("test.lrc");
        Log.d(TAG, "lrc:" + lrc);
        
        ILrcBuilder builder = new DefaultLrcBuilder();
        List<LrcRow> rows = builder.getLrcRows(lrc);
        
        mLrcView.setLrc(rows);
        beginLrcPlay();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    }
    
      
    // emulate music play
    public void beginLrcPlay(){
        if(mTimer == null){
            mTimer = new Timer();
            mTask = new LrcTask();
            mTimer.scheduleAtFixedRate(mTask, 0, mPalyTimerDuration);
        }
    }
    
    public void stopLrcPlay(){
        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
    }
    
    class LrcTask extends TimerTask{
        
        long beginTime = -1;
        
        @Override
        public void run() {
            if(beginTime == -1) {
                beginTime = System.currentTimeMillis();
            }
            
            final long timePassed = System.currentTimeMillis() - beginTime;
            MainActivity.this.runOnUiThread(new Runnable() {
                
                public void run() {
                    mLrcView.seekLrcToTime(timePassed);
                }
            });
           
        }
    };
}
