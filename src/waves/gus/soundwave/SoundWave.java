package waves.gus.soundwave;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.android.soundrecorder.LogUtils;
import com.android.soundrecorder.Recorder;

public class SoundWave extends View {
	final int LINE_SIZE = 6;
	Path basePath;
	Paint basePaint;
	private Path[] path;
	private Paint[] paint;
	Context context;

	DisplayMetrics display = this.getResources().getDisplayMetrics();
	private float width = display.widthPixels, height = display.heightPixels; // height
																				// is
																				// measured
																				// from
																				// the
																				// top

	float space = 1f;
	float theta = 0f;
	final float baseAmplitude = 5f; // what all noise will "restore" to --
									// initial amplitude
	final float maxAmplitude = 140; // amplitude limit
	float currentAmplitude = baseAmplitude;
	float degrade = 0.01f; // degradation factor that brings currentAmplitude
							// back to baseAmplitude
	float period = width / 1.0f; // distance between waves
	float dx = ((float) (2 * Math.PI) / period) * space; // space between points
	float[][] y_values = new float[LINE_SIZE][(int) (width / space)]; // 4
																		// waves,
	float[] delta_x = new float[LINE_SIZE];
	// increase or
	// decrease as
	// you
	// like

	float current_amp = 0f; // net amplitude from mic; allows us to subtly draw
							// sine
	private Recorder mRecorder;

	public SoundWave(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;

		basePath = new Path();

		path = new Path[LINE_SIZE];
		paint = new Paint[LINE_SIZE];
		for (int i = 0; i < LINE_SIZE; ++i) {
			path[i] = new Path();
		}

		float k = 0.29f;
		delta_x[0] = k;
		for( int i = 1; i < LINE_SIZE; ++i ) {
			k = k - 0.035f;
			delta_x[i] = delta_x[i-1] + k;
		}
	//	delta_x[0] = 0.29f;
	//	delta_x[1] = 0.29f + 0.27f;
	//	delta_x[2] = 0.29f + 0.27f + 0.25f;
	//	delta_x[3] = 0.29f + 0.27f + 0.25f + 0.23f;
	//	delta_x[4] = 0.29f + 0.27f + 0.25f + 0.23f + 0.21f;
	//	delta_x[5] = 0.29f + 0.27f + 0.25f + 0.23f + 0.21f + 0.19f;
		// delta_x[6] = 0.29f + 0.27f + 0.25f + 0.23f + 0.21f + 0.19f + 0.17f;

		basePaint = new Paint();
		basePaint.setAntiAlias(true); /* makes lines look smoooooth */
		basePaint.setColor(Color.WHITE);
		basePaint.setStyle(Paint.Style.STROKE);
		basePaint.setStrokeJoin(Paint.Join.ROUND);
		basePaint.setStrokeWidth(0.5f);

		paint[0] = new Paint();
		paint[0].setAntiAlias(true); /* makes lines look smoooooth */
		paint[0].setColor(Color.WHITE);
		paint[0].setStyle(Paint.Style.STROKE);
		paint[0].setStrokeJoin(Paint.Join.ROUND);
		paint[0].setStrokeWidth(1.5f);

		path[0].moveTo(0, height / 2);

		for (int i = 1; i < LINE_SIZE; ++i) {
			paint[i] = new Paint();
			paint[i].setAntiAlias(true); /* makes lines look smoooooth */
			paint[i].setColor(Color.WHITE);
			paint[i].setStyle(Paint.Style.STROKE);
			paint[i].setStrokeJoin(Paint.Join.ROUND);
			paint[i].setStrokeWidth(0.5f);

			path[i].moveTo(0, height / 2);
		}
	}

	void calc_wave() {
		theta -= 0.22; // speed of oscillations
		// theta = 0.0f;

		getAmplitudeValue2();
		for (int j = 0; j < LINE_SIZE; ++j) {

			float x = theta + delta_x[j];
			// float x = theta + j * 0.3f;
			// float x = theta;

			for (int i = 0; i < y_values[j].length; i++) {
				y_values[j][i] = (float) Math.sin(x) * currentAmplitude;
				x += dx;
			}
		}
	}

	public void setRecorder(Recorder recorder) {
		mRecorder = recorder;
		invalidate();
	}

	void getAmplitudeValue2() {

		current_amp = (float) mRecorder.getMaxAmplitude(); // current amplitude
															// of audio pickup

		// LogUtils.v("soundrecorder", "getAmplitudeValue  1 " + current_amp
		// );
		degrade = 0.001f;// +0.001f*(currentAmplitude-baseAmplitude);
							// //degradation should be lower when amplitude is
							// high, to avoid jitter between waves

		if (currentAmplitude - degrade >= baseAmplitude) { // don't go below the
															// base amplitude
			currentAmplitude -= degrade;
		}

		if (current_amp == 0) {
		} // no audio (extremely rare) or lapse (likely) in audio pickup
		else {
			// float increase = (current_amp/35000)*maxAmplitude; //increase
			// 35000 for lower sensitivity, decrease for greater sensitivity

			float t = 0;
			float step1 = 10000.0f;
			float step2 = 20000.0f;
			float step3 = 30000.0f;
			// float delta_step1 = (1.0f / 35000.0f);
			// float delta_step2 = (1.0f / 70000.0f);
			// float delta_step3 = (1.0f / 140000.0f);
			// float delta_step4 = (1.0f / 180000.0f);

			float delta_step1 = (1.0f / 25000.0f);
			float delta_step2 = (1.0f / 60000.0f);
			float delta_step3 = (1.0f / 100000.0f);
			float delta_step4 = (1.0f / 100000.0f);

			if (current_amp < step1) {
				t = current_amp * delta_step1;
			} else if (current_amp < step2) {
				t = step1 * delta_step1 + (current_amp - step1) * delta_step2;
			} else if (current_amp < step3) {
				t = step1
						* delta_step1
						+ (step2 - step1)
						* delta_step2
						+ (current_amp - step2 + 2000 - (float) (Math.random() * 4000)

						) * delta_step3;
			} else {
				t = step1
						* delta_step1
						+ (step2 - step1)
						* delta_step2
						+ (step3 - step2)
						* delta_step3
						+ (current_amp - step3 + 4000 - (float) (Math.random() * 8000))
						* delta_step4;
			}

			float increase = t * maxAmplitude; // increase
												// 35000 for
												// lower
												// sensitivity,
												// decrease
												// for
												// greater

			// sensitivity
			float y = 0.0f;
			// float y = baseAmplitude + increase;
			float target = baseAmplitude + increase;

			float delta = (target - currentAmplitude);
			float delta_y = 6.0f;
			if (Math.abs(delta) < delta_y) {
				y = target;
			} else {
				if (target > currentAmplitude) {
					y = currentAmplitude + delta_y;
				} else {
					y = currentAmplitude - delta_y;
				}
			}

			LogUtils.v("soundrecorder", "getAmplitudeValue " + current_amp
					+ " increase: " + increase + " target: " + target + " " + y);

		//	LogUtils.v("soundrecorder",
		//			"getAmplitudeValue currentAmplitude start : "
		//					+ currentAmplitude);

			if (y > maxAmplitude) {
				currentAmplitude = maxAmplitude;// - (float) (Math.random() *
												// 30);
			} // don't exceed maximum amplitude
			else if (y < baseAmplitude) {
				currentAmplitude = baseAmplitude;
			} else {
				currentAmplitude = y; // jump to the spot
			}

		//	LogUtils.v("soundrecorder",
		//			"getAmplitudeValue currentAmplitude after : "
		//					+ currentAmplitude);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		calc_wave(); // fill array
		draw_wave(canvas); // draw array
		invalidate();
	}

	void draw_wave(Canvas canvas) {
		float h = height * 0.75f;

		basePath.moveTo(0, h / 2); // s
		basePath.quadTo(0, h / 2, 1080, h / 2);
		canvas.drawPath(basePath, basePaint);
		basePath.reset();
	 
//		int k = 1;
//		if(current_amp < 10) {
//			
//		} else {
//			k = LINE_SIZE;
//		}

		for (int j = 0; j < LINE_SIZE; ++j) {
			path[j].moveTo(0, h / 2 + y_values[j][0]); // start position
			for (int i = 1; i < y_values[j].length - 1; i += 2) {
				path[j].quadTo(i * space, h / 2 + y_values[j][i], (i + 1)
						* space, h / 2 + y_values[j][i + 1]); // create bezier
																// between
																// critical //
																// points
			}
			canvas.drawPath(path[j], paint[j]);
			path[j].reset();
		}
	}
}