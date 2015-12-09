package de.tum.lmt.texturerecognizer;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 *  this activity is simply used to perform calibration of accelerometer, while the phone is in stationary position, later the calculated offset values are taken into account when reading off the measurement values
 * @author ga56den
 *
 */
public class SensorCalibrationActivity extends Activity implements SensorEventListener{

	private static final String TAG = SensorCalibrationActivity.class.getSimpleName();
	private static final int STEP = 1;
	private ImageButton buttonOkCalib;
	private ImageButton buttonCancelCalib;
	private SensorManager sensorManager;
	private boolean closeToFloor = false, lyingOnTheFloor = false, finalValues = false, proximitySensorAvailable = false;
	public double offsetX = 0.0, offsetY = 0.0, offsetZ = 0.0;
	public static double offsetValues[] = {0.0, 0.0, 0.0};
	private long timestampLastGyro; // 500 ms
	private int accelerometerMinDelay, gyroMinDelay=-1; 
	private String mGapFiller;
	private String finalOffsetX, finalOffsetY, finalOffsetZ;
	TextView description, description_2, offSetX, offSetY, offSetZ;
	private Vibrator mVibrator;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calibration);
		description=((TextView) findViewById(R.id.textview_instructions_calib_1));
		description_2=((TextView) findViewById(R.id.textview_instructions_calib_2));
		offSetX=((TextView) findViewById(R.id.textview_offsetX));
		offSetY=((TextView) findViewById(R.id.textview_offsetY));
		offSetZ =((TextView) findViewById(R.id.textview_offsetZ));		

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		buttonOkCalib =  (ImageButton) findViewById(R.id.button_ok_calib);

		buttonOkCalib.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onPause();
				if(!MainActivity.getPrefMode()) {
					showContinueDialog();
				} 
				else {
					/*Intent intentCam = new Intent(SensorCalibrationActivity.this, CameraActivity.class);
					startActivity(intentCam);
					finish();*/
				}
			}
		});

		buttonCancelCalib = (ImageButton) findViewById(R.id.button_cancel_calib);

		buttonCancelCalib.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onPause();

				if(finalValues) {
					mGapFiller = getString(R.string.gapFiller_after) + " " + getString(R.string.calibration);
					showCancelDialog(mGapFiller);
				}
				else {
					mGapFiller = getString(R.string.gapFiller_before) + " " + getString(R.string.calibration);
					showCancelDialog(mGapFiller);
				}
				//SensorCalibrationActivity.this.finish();*/
			}
		});


		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		timestampLastGyro = System.currentTimeMillis();

		// show used memory
       	{
	       	final Runtime runtime = Runtime.getRuntime();
			final long usedMemInMB=(runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
			final long maxHeapSizeInMB=runtime.maxMemory() / 1048576L;
			Log.i(TAG, "SCActivity, Used in MB " + Long.toString(usedMemInMB) + ", total  " + Long.toString(maxHeapSizeInMB));
       	}
	}





	protected void showContinueDialog() {

		//DialogFragment continueDialog = new DialogContinueFragment(STEP);
		//continueDialog.show(getFragmentManager(), "DialogContinueFragment");
	}

	protected void showCancelDialog(String gapFiller) {

		//DialogFragment cancelDialog = new DialogCancelFragment(gapFiller, finalValues);
		//cancelDialog.show(getFragmentManager(), "DialogCancelFragment");
	}

	@Override
	public void onResume() {
		super.onResume();
		// register this class as a listener for the gyro, proximity and accelerometer sensors
		if (sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
			proximitySensorAvailable = true;
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_FASTEST);
			Log.i(TAG, "Registered proximity sensor");
		}
		else if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
			proximitySensorAvailable = false;
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_FASTEST);
			Log.i(TAG, "Registered light sensor");
		}
		else {
			Log.i(TAG, "Light nor proximity sensors are present here!");
		}

		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
		if (!Constants.GRAVITY_NOT_PRESENT) {
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
			gyroMinDelay = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE).getMinDelay();
		}
		accelerometerMinDelay = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).getMinDelay();
		
		if (Constants.GRAVITY_NOT_PRESENT) {
			description.setText("No gravity found, please continue to next screen for data collection");
			offSetX.setText("0.0 (default)");
			offSetY.setText("0.0 (default)");
			offSetZ.setText("0.0 (default)");
		}
		else {
			description.setText(getString(R.string.textview_instructions_calib_1, accelerometerMinDelay/1000, gyroMinDelay/1000));
		}
		Log.i(TAG, "onResume");
	}

	@Override
	public void onPause() {
		// unregister listener
		super.onPause();
		sensorManager.unregisterListener(this);


	}


	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent arg0) {		
		if (arg0.sensor.getType() == Sensor.TYPE_ACCELEROMETER && closeToFloor	&& !finalValues) {
			lyingOnTheFloor = true;
			getAccelerometer(arg0);
		}
		if (arg0.sensor.getType() == Sensor.TYPE_PROXIMITY) {
			Log.i(TAG, "proximity value " + arg0.values[0]);

			if (arg0.values[0] < Constants.CALIB_PROXIMITY_THRESHOLD) {
				closeToFloor = true;
				timestampLastGyro = System.currentTimeMillis();
				if(mVibrator != null)
				{
					mVibrator.vibrate(200);
				}
			}
			else {
				closeToFloor = false;
			}
		}
		if (arg0.sensor.getType() == Sensor.TYPE_LIGHT) {
			Log.i(TAG, "light value " + arg0.values[0]);
			if (arg0.values[0] < 8.0)
				closeToFloor = true;
			else
				closeToFloor = false;
		}
		
		if (!Constants.GRAVITY_NOT_PRESENT) {
		
			if (arg0.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
				// as soon as we get the gyro event, we assume that we are not stationary anymore, which means calibration finished
				if (arg0.values[0] > Constants.CALIB_THRESH || arg0.values[1] > Constants.CALIB_THRESH || arg0.values[2] > 
				Constants.CALIB_THRESH && (arg0.timestamp - timestampLastGyro/1e6) > Constants.CALIB_TIME_DIFF_SINCE_LAST_GYRO_EVENT) {
					if (lyingOnTheFloor) {
						description.setText("");
						description_2.setText(R.string.textview_instructions_calib_2);
						buttonOkCalib.setEnabled(true);
						offsetValues[0] = -offsetX;
						offsetValues[1] = offsetY;
						offsetValues[2] = offsetZ;
						finalValues = true;
						lyingOnTheFloor = false;
						if(mVibrator != null)
						{
							mVibrator.vibrate(500);
						}
					}
				}
			}
		}
		else {
			if (arg0.sensor.getType() == Sensor.TYPE_PROXIMITY && lyingOnTheFloor) {
				description.setText("");
				description_2.setText(R.string.textview_instructions_calib_2);
				buttonOkCalib.setEnabled(true);
				offsetValues[0] = -offsetX;
				offsetValues[1] = offsetY;
				offsetValues[2] = offsetZ;
				finalValues = true;
				lyingOnTheFloor = false;
				if(mVibrator != null)
				{
					mVibrator.vibrate(500);
				}
			}
		}
	}

	private void getAccelerometer(SensorEvent event) {
		float[] values = event.values;
		// Movement	    
		offsetX = (1 - Constants.CALIB_FILTER_FORGETTING_FACTOR) * offsetX + Constants.CALIB_FILTER_FORGETTING_FACTOR * values[0];
		offsetY = (1 - Constants.CALIB_FILTER_FORGETTING_FACTOR) * offsetY + Constants.CALIB_FILTER_FORGETTING_FACTOR * values[1];
		if (values[2]<0.0)
			offsetZ = (1 - Constants.CALIB_FILTER_FORGETTING_FACTOR) * offsetZ + Constants.CALIB_FILTER_FORGETTING_FACTOR * (SensorManager.GRAVITY_EARTH + values[2]);
		else
			offsetZ = (1 - Constants.CALIB_FILTER_FORGETTING_FACTOR) * offsetZ + Constants.CALIB_FILTER_FORGETTING_FACTOR * (-SensorManager.GRAVITY_EARTH + values[2]);
		Log.i(TAG, "getting: " + values[2] + ", summing: " + offsetZ);
		if (!finalValues) {
			finalOffsetX = getString(R.string.textview_offsetX) + offsetX + " m/s^2";
			finalOffsetY = getString(R.string.textview_offsetY) + offsetY + " m/s^2";
			finalOffsetZ = getString(R.string.textview_offsetZ) + offsetZ + " m/s^2";
			offSetX.setText(finalOffsetX);
			offSetY.setText(finalOffsetY);		
			offSetZ.setText(finalOffsetZ);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		sensorManager.unregisterListener(this);
		finish();
	}
}
