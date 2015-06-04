package com.android.soundrecorder;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class RuimeiWaveView extends View implements Runnable{

	private Paint paint1 = new Paint();
	private Paint paint2 = new Paint();
	private Paint paint3 = new Paint();
	private double a3 = -200;
	private double count = -200;
	private Recorder mRecorder;
	
	private boolean isRun = true;
	private int angle = 0;
	float curve = 0;

	public RuimeiWaveView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public RuimeiWaveView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public RuimeiWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		int height = getHeight();
		int width = getWidth();
		
		paint1.setColor(Color.RED);
		paint1.setAntiAlias(true);
		paint1.setStyle(Style.FILL);
		paint1.setAlpha(200);
		
		paint2.setColor(Color.WHITE);
		paint2.setAntiAlias(true);
		paint2.setStyle(Style.FILL);
		paint2.setAlpha(150);
		
		paint3.setColor(Color.WHITE);
		paint3.setAntiAlias(true);
		paint3.setStyle(Style.FILL);
		paint3.setMaskFilter(new BlurMaskFilter(2, BlurMaskFilter.Blur.NORMAL));
		paint3.setAlpha(255);

		double lineX = 0;
		double lineY1 = 0;
		double lineY2 = 0;
		double lineY3 = 0;
		
		if (null != mRecorder) {
	            curve = mRecorder.getMaxAmplitude() / 32768;
	    }
		 
		int half = width / 2;
		double percent = 0.0;
		
		for (int i = 0; i < width; i++) {
			
			lineX = i;
			
			if (i < half) {
				percent = ((double)i)/(double)half; 
			}else{
				percent = ((double)width - (double)i)/(double)half;
			}
//			Log.v("TAG", "percent->" + percent);
			
			if (isRun) {
//				lineY1 = Math.sin((width/2 + angle) * Math.PI / 180);
//				lineY2 = 10 * Math.sin((width/2 + angle) * Math.PI / 180);
//				lineY3 = 20 * Math.sin((i + angle) * Math.PI / 180);
				lineY3 = a3 * Math.sin((i + angle) * Math.PI / 180) * percent;
			} else {
//				lineY1 = 0;
//				lineY2 = 20;
				lineY3 = 0;
			}
//			canvas.drawLine((int) lineX, (int) (lineY1 + height / 1.5),
//					(int) lineX + 1, (int) (lineY2 + height / 1.5), paint1);
//			canvas.drawLine((int) lineX, (int) (lineY2 + height / 1.5),
//					(int) lineX + 1, (int) (lineY3 + height / 1.5), paint2);
			canvas.drawCircle((float)lineX, (float)(height/2 + lineY3), (float)0.5, paint3);
//			canvas.drawLine((int) lineX, (int) (height/ 2),
//					(int) lineX, (int)(height/2 + lineY3 * percent), paint3);
		}

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (isRun) {
			
			angle++;
			if (angle == 360) {
				angle = 0;
			}
			
			if (a3 > count) {
				a3 --;
			}else if (a3 < count) {
				a3 ++;
			}else{
				count = 200 * curve;
				Log.v("TAG", "count->" + count);
			}
			
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void start() {
		isRun = true;
		new Thread(this).start();
	}

	public void stop() {
		isRun = false;
		angle = 0;
	}
	
	public void setRecorder(Recorder recorder) {
        mRecorder = recorder;
        invalidate();
    }
}
