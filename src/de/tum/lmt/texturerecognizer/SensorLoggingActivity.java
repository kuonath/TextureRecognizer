package de.tum.lmt.texturerecognizer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class SensorLoggingActivity extends Activity {
	
	private static final String TAG = SensorLoggingActivity.class.getSimpleName();
	
	//Bluetooth
	private static final int REQUEST_ENABLE_BT = 0;
	
	private File parentLoggingDir = MainActivity.getLoggingDir();
	private String sensorLoggingPath;// = parentLoggingDir.getAbsolutePath() + File.separator + Constants.sensorLogginFolderName;
	private File sensorLoggingDir;
	private Map<Integer,String> mMap;
	List<SensorDataWrapper> wrapperList = new ArrayList<SensorDataWrapper>();
	// a couple of native libraries for sensor processing

	/** A native method that is implemented by the
	 * native library, which is packaged
	 * with this application.
	 */
	public native void startLoggingSensorNDK(String dir, int sensorSpeed, double[] offsetValues, String[] sensorNames, int numberSensorsToLog);

	/**
	 * a native library method to stop logging, finishes writing data into files 
	 */
	public native void stopLoggingSensorNDK();
	
	/* this is used to load the 'logger' library on application
	 * startup. The library has already been unpacked into logger
	 */
	static {
		System.loadLibrary(Constants.nameNativeLibrary);
	}

	private AudioRecorderWAV recorder;

	private CheckBox checkboxSensorsLogging;
	private CheckBox checkboxAudioLogging;

	private ImageButton buttonStartLogging;
	private ImageButton buttonStopLogging;
	private ImageButton buttonOkLogging;
	private ImageButton buttonCancelLogging;

	private TextView description1;
	private TextView description2;
	private TextView description3;

	private Vibrator vibrator;

	private boolean mIsLogging = false;
	private boolean mLoggingFinished = false;

	private String mPrefPathToStorage;
	private int mPrefSensorSpeed;
	private String[] sensorNames;
	private int numberOfSensorsToLog = 0;

	private File mActivityStreamFile;
	private BufferedOutputStream mOutActivity;

	private Handler mTimerHandler;
	private Handler mTimedUpdateHandler;
	private Runnable mTimedUpdateRunnable;

	private String mGapFiller;

	// init data
	class Sensor {
		int nameID;
		String name;
		Sensor(int nameID_, String name_) {
			nameID = nameID_;
			name = name_;
		}
	}

	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logging);

		sensorLoggingPath = parentLoggingDir.getAbsolutePath() + File.separator + Constants.SENSOR_LOGGING_FOLDER_NAME;

		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		recorder = new AudioRecorderWAV();

		sensorLoggingDir = new File(sensorLoggingPath);

		mTimerHandler = new Handler();

		checkboxSensorsLogging = (CheckBox) findViewById(R.id.checkbox_sensors_logging);
		checkboxAudioLogging = (CheckBox) findViewById(R.id.checkbox_audio_logging);

		description1 = (TextView) findViewById(R.id.textview_instructions_logging_1);
		description2 = (TextView) findViewById(R.id.textview_instructions_logging_2);
		description3 = (TextView) findViewById(R.id.textview_instructions_logging_3);

		buttonStartLogging = (ImageButton) findViewById(R.id.button_start_logging);
		buttonStartLogging.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				description2.setText("Preparing sensors...");

				mTimerHandler.postDelayed( new Runnable() {
					@Override
					public void run() {
						startLogging();
					}
				}, Constants.DURATION_TO_LOG);
			}
		});

		buttonStopLogging = (ImageButton) findViewById(R.id.button_stop_logging);
		buttonStopLogging.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				stopLogging();
				
				
				
			}
		});

		buttonOkLogging = (ImageButton) findViewById(R.id.button_features_logging);
		buttonOkLogging.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				showFeaturesDialog();
			}
		});

		buttonCancelLogging = (ImageButton) findViewById(R.id.button_cancel_logging);
		buttonCancelLogging.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				if(mLoggingFinished) {
					mGapFiller = getString(R.string.gapFiller_after) + " " + getString(R.string.logging);
					showCancelDialog(mGapFiller);
				}
				else {
					mGapFiller = getString(R.string.gapFiller_before) + " " + getString(R.string.logging);
					showCancelDialog(mGapFiller);
				}
			}
		});
		buttonOkLogging.setClickable(false);
		// init stuff



		mMap = new HashMap<Integer,String>();
		for (int i=0; i<Constants.requiredSensorNames.length; i++) {
			mMap.put(i, Constants.requiredSensorNames[i]);
		}

	}

	protected void showFeaturesDialog() {
		DialogFragment featuresDialog = new DialogFeaturesFragment();
		featuresDialog.show(getFragmentManager(), "DialogFeaturesFragment");
	}

	protected void showCancelDialog(String gapFiller) {
		DialogFragment cancelDialog = new DialogCancelFragment(gapFiller, mLoggingFinished);
		cancelDialog.show(getFragmentManager(), "DialogCancelFragment");
	}

	@Override 
	protected void onPause(){
		super.onPause();
		if (mIsLogging) {
			stopLogging();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_ENABLE_BT) {
			if(resultCode == Activity.RESULT_OK) {
				Toast toast = Toast.makeText(getApplicationContext(), R.string.toast_enabled_bt, Toast.LENGTH_SHORT);
				toast.show();
			} 
			else {
				Toast toast = Toast.makeText(getApplicationContext(), R.string.toast_error_bt, Toast.LENGTH_SHORT);
				toast.show();
			}
		}
	}

	boolean checkIfInsideStringArray(String s, String[] sArray) {
		for (String it : sArray) {
			if (it.contains(s)) {
				return true;
			}
		}
		return false;
	}


	private void stopLoggingNoSensors(String message) {
		mLoggingFinished = false;

		description2.setText("");
		description3.setText("Something went wrong: " + message);
		Toast.makeText(getApplicationContext(), "Sensor is missing " + message + ". Please check again.", Toast.LENGTH_LONG).show();
		buttonOkLogging.setEnabled(true);
		buttonOkLogging.setClickable(true);
		buttonStopLogging.setEnabled(false);
	}


	private void startLogging() {

		if(!sensorLoggingDir.exists()) {
			sensorLoggingDir.mkdirs();
		}
		mPrefPathToStorage = MainActivity.getPrefPathToStorage();
		mPrefSensorSpeed = MainActivity.getPrefSensorSpeed();
		sensorNames = MainActivity.getSensorNames();
		numberOfSensorsToLog = MainActivity.getNumberOfSensorsToLog();

		buttonOkLogging.setEnabled(false);

		// check if sensor names has the sensors we need


		if (sensorNames == null) {
			stopLoggingNoSensors("Sensor names null");
			return;
		}
		if (sensorNames.length == 0) {
			stopLoggingNoSensors("Sensors are empty");
			return;
		}

		// check if we have accel stuff etc.
		for (Integer mMapId : mMap.keySet()) {
			if (!checkIfInsideStringArray(mMap.get(mMapId), sensorNames)) {
				stopLoggingNoSensors(mMap.get(mMapId));
				return;
			}
		}


		if(!mIsLogging && !mLoggingFinished && (checkboxSensorsLogging.isChecked() || checkboxAudioLogging.isChecked()) && 
				sensorNames.length>0) {
			Log.i(TAG, "logging sensors on native level to folder: " + sensorLoggingDir.getAbsolutePath());

			vibrator.vibrate(500);

			buttonStartLogging.setEnabled(false);
			buttonStopLogging.setEnabled(true);

			if(checkboxAudioLogging.isChecked()) {
				recorder.startRecording();
			}

			if(checkboxSensorsLogging.isChecked()) {
				startLoggingSensorNDK(sensorLoggingDir.getAbsolutePath(), mPrefSensorSpeed, SensorCalibrationActivity.mOffsetValues, 
						sensorNames, numberOfSensorsToLog);
			}

			mIsLogging = true;

			int number_sensors = 0;
			if (sensorNames!= null) {
				number_sensors = sensorNames.length;
			}

			if (number_sensors == 0) {
				description3.setText(R.string.warning_logging);
			}
			else {
				description2.setText(R.string.status_logging);
			}

			// timed updating of interesting information
			mTimedUpdateHandler = new Handler();
			mTimedUpdateRunnable = new Runnable() {
				@Override
				public void run() {
					description3.setText(getString(R.string.details_logging, mPrefSensorSpeed, mPrefPathToStorage));
					mTimedUpdateHandler.postDelayed(this, 500);
				}
			};
			mTimedUpdateHandler.postDelayed(mTimedUpdateRunnable, 500);

			mTimerHandler.postDelayed( new Runnable() {
				@Override
				public void run() {
					stopLogging();
				}
			}, Constants.DURATION_TO_LOG);
		}
		else if(!checkboxAudioLogging.isChecked() && !checkboxSensorsLogging.isChecked()) {
			description2.setText(getString(R.string.no_box_checked));
		}
	}

	private void stopLogging() {

		if(mIsLogging) {

			description2.setText("Logging finished, parsing the necessary data...");

			mTimedUpdateHandler.removeCallbacks(mTimedUpdateRunnable);

			vibrator.vibrate(500);

			if(checkboxAudioLogging.isChecked()) {
				recorder.stopRecording();
			}

			if(checkboxSensorsLogging.isChecked()) {
				stopLoggingSensorNDK();
			}

			mIsLogging = false;
			mLoggingFinished = true;

			dropDataPoints();

			buttonOkLogging.setEnabled(true);
			buttonOkLogging.setClickable(true);
			buttonStopLogging.setEnabled(false);

			description2.setText("");
			description3.setText(getString(R.string.textview_instructions_logging_3));
			
			
			
	
			
		}
	}

	private boolean checkIfInside(File file, ArrayList<Sensor> sensors) {
		for (Sensor current : sensors) {
			if (file.getName().contains(current.name)) {
				return true;
			}
		}
		return false;
	}

	private void dropDataPoints() {

		File[] fileList = sensorLoggingDir.listFiles();

		List<List<String>> sensorDataList = new ArrayList<List<String>>();

		ArrayList<Sensor> sensors = new ArrayList<Sensor>();
		for (Integer s : mMap.keySet()) {
			sensors.add(new Sensor(s, mMap.get(s)));
			sensorDataList.add(new ArrayList<String>());
		}

		for(File file : fileList)
		{
			if (file.length()==0) { // check if file has been filled up, if not then skip it
				Log.e(TAG, "File " + file.getPath().toString() + " has zero length");
				Toast.makeText(getBaseContext(), "File " + file.getPath().toString() + 
						" has zero length. Maybe you need to disable it in sensor settings?..", Toast.LENGTH_LONG).show();
				continue;
			}

			String tempString = null;
			BufferedReader br = null;
			Log.i(TAG, "File " + file);

			if (checkIfInside(file, sensors)) {
				try {
					br = new BufferedReader(new FileReader(file));

					for (Sensor current : sensors) {
						if (file.getName().contains(current.name)) {
							while(((tempString = br.readLine()) != null)) {
								sensorDataList.get(current.nameID).add(tempString);
							}					
						}
					}
				} catch (Throwable t) {
					Log.e(TAG, "load()", t);
				} finally {
					if(br != null) {
						try {
							br.close();
						} catch (IOException e) {
							Log.e(TAG, "br.close()", e);
						}
					}
				}
			}
			else {
				file.delete();
			}
		}

		//description2.setText(getString(R.string.textview_magn_data_length, sensorDataList.get(Constants.MAGNDATA).size()));

		cropListsToSmallestSize(sensorDataList);

		for(File file : fileList)
		{
			Log.i(TAG, "Now appending files, " + file);

			// find if at least one is there			
			if (checkIfInside(file, sensors)) {
				for (int i=0; i<sensors.size(); i++) {
					String name = sensors.get(i).name;
					SensorDataWrapper m = wrapperList.get(i);
					if (file.getName().contains(name)) {
						writeToFile(file, name, m);
					}
				}


			}
		}
	}

	boolean writeToFile(File file, String name, SensorDataWrapper m) {
		// write to file
		BufferedWriter bw = null;
		StringBuilder sb = new StringBuilder();
		try {
			bw = new BufferedWriter(new FileWriter(file));
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}
		if((file.getName().contains(name))) {
			for (int indexMeas=0; indexMeas<m.dataParsed.size(); indexMeas++) {
				String s = m.dataParsed.get(indexMeas).toString() + "\n";
				if (file.getName().contains(Constants.requiredSensorNames[Constants.ACCELDATA])) {
					//Log.i(TAG, "Pushing to accel for " + file.getName().toString());
					Constants.pushToAccelArray(m.dataParsed.get(indexMeas).meas_y);
				}
				sb.append(s);
			}
			try {
				if(bw != null) {
					bw.write(sb.toString());
					bw.close();
					return true;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}		
		}
		return false;
	}

	private void cropListsToSmallestSize(List<List<String>> sensorDataList) {

		for(List<String> element : sensorDataList) {
			SensorDataWrapper curr = new SensorDataWrapper(element);
			curr.removeNonMonotonicData();
			wrapperList.add(curr);
			Log.i(TAG, "cropListsToSmallestSize Parsed sensor " + curr.dataParsed.size());
		}

		long firstTS = wrapperList.get(0).dataParsed.get(0).ts;
		for (SensorDataWrapper w : wrapperList) {
			w.interpolate(Constants.period, Constants.durationInterested, firstTS, Constants.precision_to_write_sensor_data);
			Log.i(TAG, "Parsed string array, duration " + w.timeDuration + ", size " + w.dataParsed.size());
		}

		for (SensorDataWrapper w : wrapperList) {
			w.check();
		}

	}
}