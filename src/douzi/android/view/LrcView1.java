package douzi.android.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.AvoidXfermode.Mode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * LrcView can display LRC file and Seek it.
 * Use SurfaceView for improve performance and scroll smooth.
 * @author douzifly
 *
 */
public class LrcView1 extends SurfaceView implements SurfaceHolder.Callback{
	
	public final static String TAG = "LrcView1";
	
	/** normal display mode*/
	public final static int DISPLAY_MODE_NORMAL = 0;
	/** seek display mode */
	public final static int DISPLAY_MODE_SEEK = 1;
	/** scale display mode ,scale font size*/
	public final static int DISPLAY_MODE_SCALE = 2;

	private List<LrcRow> mLrcRows; 	// all lrc rows of one lrc file
	private ILrcBuilder mLrcBuilder; 
	private int mMinSeekFiredOffset = 10; // min offset for fire seek action, px;
	private int mHignlightRow = 0;   // current singing row , should be highlighted.
	private int mHignlightRowColor = Color.YELLOW; 
	private int mNormalRowColor = Color.WHITE;
	private int mSeekLineColor = Color.CYAN;
	private int mSeekLineTextColor = Color.CYAN;
	private int mSeekLineTextSize = 15;
	private int mMinSeekLineTextSize = 13;
	private int mMaxSeekLineTextSize = 18;
	private int mLrcFontSize = 23; 	// font size of lrc 
	private int mMinLrcFontSize = 15;
	private int mMaxLrcFontSize = 35;
	private int mPaddingY = 10;		// padding of each row
	private int mSeekLinePaddingX = 0; // Seek line padding x
	private int mDisplayMode = DISPLAY_MODE_NORMAL;
	private OnLrcViewListener mLrcViewListener;
	private int mPalyTimerDuration = 50;
	private String mLoadingLrcTip = "Downloading lrc...";
	
	private Paint mPaint;
	
	public LrcView1(Context context,AttributeSet attr){
		super(context,attr);
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setTextSize(mLrcFontSize);
		getHolder().addCallback(this);
		setZOrderOnTop(true);
		getHolder().setFormat(PixelFormat.TRANSPARENT);
	}
	
	public void setOnLrcViewSeekListener(OnLrcViewListener l){
		mLrcViewListener = l;
	}
	
	public void setLoadingTipText(String text){
		mLoadingLrcTip = text;
	}
	
	public void setLrcBuilder(ILrcBuilder lrcBuilder){
		mLrcBuilder = lrcBuilder;
	}
	
	private void checkLrcBuilder(){
		if(mLrcBuilder == null){
			mLrcBuilder = new DefaultLrcBuilder();
		}
	}
	
	
	private Timer mTimer;
	private TimerTask mTask;
	private int mPrevHighlihgtRow = -1;
	private long mStartTime; // mills when start play ,used to calculate play passed time
	private long mPassedTime; // mills between now and start
	private long mSeekOffset = 0; // seek offset
	
	public void timerSeekTo(long newBegin){
		mSeekOffset = newBegin;
		mStartTime = new Date().getTime();
		// sometimes ,may be seek to last row and timer has been stopped,so restart timer
		beginLrcPlay();
	}
	
	class LrcTask extends TimerTask{
		boolean firstRun = true;
		
		@Override
		public void run() {
			if(firstRun){
				firstRun = false; 
				Date date = new Date();
				mStartTime = date.getTime();
			}
			if(mHignlightRow == mLrcRows.size() - 1){
				// last row
				stopLrcPlay();
			}
			Date date = new Date();
			mPassedTime = date.getTime() - mStartTime + mSeekOffset;
			//Log.d(TAG,"timePassed:"+mPassedTime);
			int i = mHignlightRow;
			for(; i < mLrcRows.size() ; i++){
				long time = mLrcRows.get(i).time;
				if(time < mPassedTime + mPalyTimerDuration && time > mPassedTime - mPalyTimerDuration){
					mHignlightRow = i;
					if(mHignlightRow != mPrevHighlihgtRow){
					    Log.i(TAG, "hrow:" + mHignlightRow + " phrow:" + mPrevHighlihgtRow + " anim invalidate");
					    int height = getHeight();
					    if(height <= 0){
					        return;
					    }
					    mTargetHighlightRowY = height / 2 - mLrcFontSize;
					    if(mCurrentHighlightRowY == mTargetHighlightRowY){
					        // after seek or scale , mCurrentHighlightRowY == mTargetHighlightRowY, and this time,
					        // don't do animation
					        mAnimating = false;
					        mCurrentHighlightRowY = 0;
					        // do not invalidate here
					    }else{
					        mAnimating = true;
					        mCurrentHighlightRowY = mTargetHighlightRowY + mLrcFontSize;
					        invalidate();
					    }        
						mPrevHighlihgtRow = mHignlightRow;
					}
					break;
				}
			}
		}
	};
	
	public void beginLrcPlay(){
		if(mLrcRows == null || mLrcRows.size() == 0){
			return;
		}
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
	
	/**
	 * Asynchronous load LRC file
	 * @param lrcFile
	 */
	public void beginLoadLrc(File lrcFile){
		checkLrcBuilder();
		new LoadLrcTask().execute(lrcFile);
	}
	
	class LoadLrcTask extends AsyncTask<File, Void, Void>{

		@Override
		protected Void doInBackground(File... params) {
			try {
				//assume network download
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mLrcRows = mLrcBuilder.getLrcRows(params[0]);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			invalidate();
			if(mLrcViewListener != null){
				mLrcViewListener.didLrcLoad(mLrcRows != null);
			}
		}
		
	}
	
	private int mTargetHighlightRowY = 0;  // when change highlight row , final y offset
	private int mCurrentHighlightRowY = -1; // when change highlight row , do smooth scroll, record current
	                                       // y offset 
	
	private boolean mAnimating = false;    // indicate whether is smooth scrolling.
	protected void drawLrc(Canvas canvas) {
		//Log.d(TAG,"drawLrc");
		canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
		
		
		final int height = getHeight(); // height of this view
		final int width = getWidth() ; // width of this view
		if(mLrcRows == null || mLrcRows.size() == 0){
			if(mLoadingLrcTip != null){
				// draw tip when no lrc.
				mPaint.setColor(mHignlightRowColor);
				mPaint.setTextSize(mLrcFontSize);
				mPaint.setTextAlign(Align.CENTER);
				canvas.drawText(mLoadingLrcTip, width / 2, height / 2 - mLrcFontSize, mPaint);
			}
			return;
		}

		int rowY = 0; // vertical point of each row. 
		final int rowX = width / 2;
		int rowNum = 0;
		
		// 1, draw highlight row at center.
		// 2, draw rows above highlight row.
		// 3, draw rows below highlight row.
		
		// 1 highlight row
		String highlightText = mLrcRows.get(mHignlightRow).content;
		
		if(!mAnimating){
            mCurrentHighlightRowY = mTargetHighlightRowY;
        }
		
		if(mCurrentHighlightRowY <=0 ){
		    return;
		}
		
		
		//Log.d(TAG, "mCurrentHighlightRowY:" + mCurrentHighlightRowY + " tagetY:" + mTargetHighlightRowY + " anim:" + mAnimating ) ;
		mPaint.setColor(mHignlightRowColor);
		mPaint.setTextSize(mLrcFontSize);
		mPaint.setTextAlign(Align.CENTER);
		canvas.drawText(highlightText, rowX, mCurrentHighlightRowY, mPaint);

		if(mDisplayMode == DISPLAY_MODE_SEEK){
			// draw Seek line and current time when moving.
			mPaint.setColor(mSeekLineColor);
			canvas.drawLine(mSeekLinePaddingX, mCurrentHighlightRowY, width - mSeekLinePaddingX, mCurrentHighlightRowY, mPaint);
			mPaint.setColor(mSeekLineTextColor);
			mPaint.setTextSize(mSeekLineTextSize);
			mPaint.setTextAlign(Align.LEFT);
			canvas.drawText(mLrcRows.get(mHignlightRow).strTime, 0, mCurrentHighlightRowY, mPaint);
		}
		
		// 2 above rows
		mPaint.setColor(mNormalRowColor);
		mPaint.setTextSize(mLrcFontSize);
		mPaint.setTextAlign(Align.CENTER);
		rowNum = mHignlightRow - 1;
		rowY = mCurrentHighlightRowY - mPaddingY - mLrcFontSize;
		while( rowY > -mLrcFontSize && rowNum >= 0){
			String text = mLrcRows.get(rowNum).content;
			canvas.drawText(text, rowX, rowY, mPaint);
			rowY -=  (mPaddingY + mLrcFontSize);
			rowNum --;
		}
		
		// 3 below rows
		rowNum = mHignlightRow + 1;
		rowY = mCurrentHighlightRowY + mPaddingY + mLrcFontSize;
		while( rowY < height && rowNum < mLrcRows.size()){
			String text = mLrcRows.get(rowNum).content;
			canvas.drawText(text, rowX, rowY, mPaint);
			rowY += (mPaddingY + mLrcFontSize);
			rowNum ++;
		}
		
		if(mAnimating){
			if(mCurrentHighlightRowY == mTargetHighlightRowY){
				Log.i(TAG, "stop animating");
				mAnimating = false;
				mCurrentHighlightRowY = 0;
				return;
			}
			//Log.d(TAG, "animating");
			invalidate();
			mCurrentHighlightRowY--;
		}
		
	}
	
	private void seekTo(int row){
		LrcRow lrcRow = mLrcRows.get(row);
		timerSeekTo(lrcRow.time);
		if(mLrcViewListener != null){
			mLrcViewListener.onSeek(row, lrcRow);
		}
	}
	
	private float mLastMotionY;
	private PointF mPointerOneLastMotion = new PointF();
	private PointF mPointerTwoLastMotion = new PointF();
	private boolean mIsFirstMove = false; // whether is first move , some events can't not detected in touch down, 
	                                      // such as two pointer touch, so it's good place to detect it in first move
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		if(mLrcRows == null || mLrcRows.size() == 0){
			return super.onTouchEvent(event);
		}
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG,"down,mLastMotionY:"+mLastMotionY);
			mLastMotionY = event.getY();
			mIsFirstMove = true;
			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
			
			if(event.getPointerCount() == 2){
			//	Log.d(TAG, "two move");
				doScale(event);
				return true;
			}
			//Log.d(TAG, "one move");
			// single pointer mode ,seek
			if(mDisplayMode == DISPLAY_MODE_SCALE){
				 //if scaling but pointer become not two ,do nothing.
				return true;
			}
			
			doSeek(event);
			break;	
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			if(mDisplayMode == DISPLAY_MODE_SEEK){
				seekTo(mHignlightRow);
			}
			mDisplayMode = DISPLAY_MODE_NORMAL;
			invalidate();
			break;
		}
		return true;
	}

	private void doScale(MotionEvent event) {
		if(mDisplayMode == DISPLAY_MODE_SEEK){
			// if Seeking but pointer become two, become to scale mode
			mDisplayMode = DISPLAY_MODE_SCALE;
			Log.d(TAG, "two move but teaking ...change mode");
			return;
		}
		mAnimating = false;
		// two pointer mode , scale font
		if(mIsFirstMove){
			mDisplayMode = DISPLAY_MODE_SCALE;
			invalidate();
			mIsFirstMove = false;
			setTwoPointerLocation(event);
		}
		int scaleSize = getScale(event);
		Log.d(TAG,"scaleSize:" + scaleSize);
		if(scaleSize != 0){
			setNewFontSize(scaleSize);
			invalidate();
		}		
		setTwoPointerLocation(event);
	}

	private void doSeek(MotionEvent event) {
		float y = event.getY();
		float offsetY = y - mLastMotionY; // touch offset.
		if(Math.abs(offsetY) < mMinSeekFiredOffset){
			// move to short ,do not fire seek action
			return;
		}
		mAnimating = false;
		mDisplayMode = DISPLAY_MODE_SEEK;
		int rowOffset = Math.abs((int) offsetY / mLrcFontSize); // highlight row offset. 
		//Log.d(TAG, "move new hightlightrow : " + mHignlightRow + " offsetY: " + offsetY + " rowOffset:" + rowOffset);
		if(offsetY < 0){
			// finger move up
			mHignlightRow += rowOffset;
		}else if(offsetY > 0){
			// finger move down
			mHignlightRow -= rowOffset;
		}
		mHignlightRow = Math.max(0, mHignlightRow);
		mHignlightRow = Math.min(mHignlightRow, mLrcRows.size() - 1);
		
		if(rowOffset > 0){
			mLastMotionY = y;
			invalidate();
		}
	}

	private void setTwoPointerLocation(MotionEvent event) {
		mPointerOneLastMotion.x = event.getX(0);
		mPointerOneLastMotion.y = event.getY(0);
		mPointerTwoLastMotion.x = event.getX(1);
		mPointerTwoLastMotion.y = event.getY(1);
	}
	
	private void setNewFontSize(int scaleSize){
		mLrcFontSize += scaleSize;
		mSeekLineTextSize += scaleSize;
		mLrcFontSize = Math.max(mLrcFontSize, mMinLrcFontSize);
		mLrcFontSize = Math.min(mLrcFontSize, mMaxLrcFontSize);
		mSeekLineTextSize = Math.max(mSeekLineTextSize, mMinSeekLineTextSize);
		mSeekLineTextSize = Math.min(mSeekLineTextSize, mMaxSeekLineTextSize);
	}
	
	// get font scale offset
	private int getScale(MotionEvent event){
		Log.d(TAG,"scaleSize getScale");
		float x0 = event.getX(0);
		float y0 = event.getY(0);
		float x1 = event.getX(1);
		float y1 = event.getY(1);
		float maxOffset =  0; // max offset between x or y axis,used to decide scale size
		
		boolean zoomin = false; 
		
		float oldXOffset = Math.abs(mPointerOneLastMotion.x - mPointerTwoLastMotion.x);
		float newXoffset = Math.abs(x1 - x0);
		
		float oldYOffset = Math.abs(mPointerOneLastMotion.y - mPointerTwoLastMotion.y);
		float newYoffset = Math.abs(y1 - y0);
		
		maxOffset = Math.max(Math.abs(newXoffset - oldXOffset), Math.abs(newYoffset - oldYOffset));
		if(maxOffset == Math.abs(newXoffset - oldXOffset)){
			zoomin = newXoffset > oldXOffset ? true : false;
		}else{
			zoomin = newYoffset > oldYOffset ? true : false;
		}
		
		Log.d(TAG,"scaleSize maxOffset:" + maxOffset);
		
		if(zoomin)
			return (int)(maxOffset / 10);
		else 
			return -(int)(maxOffset / 10);
	}
	
	
	/** one row of LRC data */
	public static class LrcRow implements Comparable<LrcRow>{
		public final static String TAG = "LrcRow";
		
		/** begin time of this lrc row */
		public long time;
		/** content of this lrc */
		public String content;
		
		public String strTime;
		
		public LrcRow(){}
		
		public LrcRow(String strTime,long time,String content){
			this.strTime = strTime;
			this.time = time;
			this.content = content;
			Log.d(TAG,"strTime:" + strTime + " time:" + time + " content:" + content);
		}
		
		/**
		 *  create LrcRows by standard Lrc Line , if not standard lrc line,
		 *  return false<br />
		 *  [00:00:20] balabalabalabala
		 */
		public static List<LrcRow> createRows(String standardLrcLine){
			try{
				if(standardLrcLine.indexOf("[") != 0 || standardLrcLine.indexOf("]") != 9 ){
					return null;
				}
				int lastIndexOfRightBracket = standardLrcLine.lastIndexOf("]");
				String content = standardLrcLine.substring(lastIndexOfRightBracket + 1, standardLrcLine.length());
				
				// times [mm:ss.SS][mm:ss.SS] -> *mm:ss.SS**mm:ss.SS*
				String times = standardLrcLine.substring(0,lastIndexOfRightBracket + 1).replace("[", "-").replace("]", "-");
				String arrTimes[] = times.split("-");
				List<LrcRow> listTimes = new ArrayList<LrcView1.LrcRow>();
				for(String temp : arrTimes){
					if(temp.trim().length() == 0){
						continue;
					}
					LrcRow lrcRow = new LrcRow(temp, timeConvert(temp), content);
					listTimes.add(lrcRow);
				}
				return listTimes;
			}catch(Exception e){
				Log.e(TAG,"createRows exception:" + e.getMessage());
				return null;
			}
		}
		
		private static long timeConvert(String timeString){
			timeString = timeString.replace('.', ':');
			String[] times = timeString.split(":");
			// mm:ss:SS
			return Integer.valueOf(times[0]) * 60 * 1000 +
					Integer.valueOf(times[1]) * 1000 +
					Integer.valueOf(times[2]) ;
		}

		public int compareTo(LrcRow another) {
			return (int)(this.time - another.time);
		}
		
		
		
	}
	
	/** default lrc builder,convert raw lrc string to lrc rows */
	public static class DefaultLrcBuilder implements  ILrcBuilder{
		static final String TAG = "DefaultLrcBuilder";
		public List<LrcRow> getLrcRows(String rawLrc) {
			Log.d(TAG,"getLrcRows by rawString");
			if(rawLrc == null || rawLrc.length() == 0){
				Log.e(TAG,"getLrcRows rawLrc null or empty");
				return null;
			}
			StringReader reader = new StringReader(rawLrc);
			BufferedReader br = new BufferedReader(reader);
			String line = null;
			List<LrcRow> rows = new ArrayList<LrcView1.LrcRow>();
			try{
				do{
					line = br.readLine();
					Log.d(TAG,"lrc raw line:" + line);
					if(line != null && line.length() > 0){
						List<LrcRow> lrcRows = LrcRow.createRows(line);
						if(lrcRows != null && lrcRows.size() > 0){
							for(LrcRow row : lrcRows){
								rows.add(row);
							}
						}
					}
					
				}while(line != null);
				if( rows.size() > 0 ){
					// sort by time:
					Collections.sort(rows);
				}
				
			}catch(Exception e){
				Log.e(TAG,"parse exceptioned:" + e.getMessage());
				return null;
			}finally{
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				reader.close();
			}
			return rows;
		}
		public List<LrcRow> getLrcRows(File rawLrcFile) {
			Log.d(TAG,"getLrcRows by rawFile");
			if(rawLrcFile == null || !rawLrcFile.exists() || !rawLrcFile.canRead()){
				Log.e(TAG,"getLrcRows rawFile null or not exists or can not read");
				return null;
			}
			FileInputStream fis = null;
			try{
				int fileLen = (int)rawLrcFile.length();
				byte[] buffer = new byte[(int)rawLrcFile.length()];
				fis = new FileInputStream(rawLrcFile);
				int totalReaded = 0;
				int readThisTime;
				while(totalReaded < fileLen){
					readThisTime = fis.read(buffer, totalReaded, fileLen - totalReaded);
					if(readThisTime == -1){
						break;
					}else{
						totalReaded += readThisTime;
					}
				}
				String rawLrcContent = new String(buffer);
				return getLrcRows(rawLrcContent);
			}catch(Exception e){
				return null;
			}finally{
				if(fis != null){
					try {
						fis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		
	}
	
	public static interface ILrcBuilder{
		public List<LrcRow> getLrcRows(String rawLrc);
		public List<LrcRow> getLrcRows(File rawLrcFile);
	}
	
	/** callback defines for LrcView */
	public static interface OnLrcViewListener{
		/** when sought */
		public void onSeek(int position,LrcRow row);
		
		/** called after lrc load complete,maybe failed */
		public void didLrcLoad(boolean sucess);
	}

	private RendererThread mRendererThread;
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		invalidate();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if(mRendererThread == null){
			mRendererThread = new RendererThread("Renderer");
			mRendererThread.start();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if(mRendererThread != null){
			mRendererThread.interrupt();
			mRendererThread = null;
		}
	}
	
	
	private class RendererThread extends HandlerThread{
		
		public RendererThread(String name) {
			super(name);
		}
		
		@Override
		public synchronized void start() {
			super.start();
			hander = new RendererHandler(getLooper());
		}
		
		private RendererHandler hander;
		
		public void invalidate(){
			if(hander != null){
				hander.sendEmptyMessage(0);
			}
		}
		
		public void invalidateAndResetCurrentY(){
		    if(hander != null){
                hander.sendEmptyMessage(1);
            }
		}
	}
	
	private class RendererHandler extends Handler{
		
		public RendererHandler(Looper looper){
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			
			SurfaceHolder holder = getHolder();
			if(holder == null){
				return;
			}
			Canvas canvas = holder.lockCanvas();
			if(canvas == null){
				return;
			}
			drawLrc(canvas);
			holder.unlockCanvasAndPost(canvas);
		}
	}
	
	@Override
	public void invalidate() {
		if(mRendererThread != null){
			mRendererThread.invalidate();
		}
	}
	
}
