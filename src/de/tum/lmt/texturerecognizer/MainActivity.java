package de.tum.lmt.texturerecognizer;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements DialogSensorFragment.iOnDialogButtonClickListener {

	private static final String TAG = MainActivity.class.getSimpleName();

	private static final int SETTINGS_CODE = 0;
	
	private ImageButton mButtonStart;
	private Button mButtonNewTexture;
	private Button mButtonAddToEntry;
	private ImageButton mButtonSettings;
	private static boolean mPrefMode = false;
	private String mPathToStorage;
	private static String mPrefPathToStorage;
	private static File mLoggingDir;
	private static String[] sensorNames;
	private static int numberOfSensorsToLog = 0;//sensorNames.length;
	private static int mPrefSensorSpeed;
	public static File file;
	
	private boolean mAccelAvailable = false;
	private boolean mGravAvailable = false;
	private boolean mGyroAvailable = false;
	private boolean mMagnetAvailable = false;
	private boolean mRotVecAvailable = false;
	
	private boolean mUseAccel = false;
	private boolean mUseGrav = false;
	private boolean mUseGyro = false;
	private boolean mUseMagnet = false;
	private boolean mUseRotVec = false;
	private boolean mUseExternAccel = false;
	
	@Override
	public void onFinishSensorDialog(String buttonClicked) {
		
		if (buttonClicked.equals(getString(R.string.close))) {
            finish();
        } else if (buttonClicked.equals(getString(R.string.ok))) {
            
        	updateSettings();
        }
	}
	
	/**
	 * taken from http://stackoverflow.com/questions/3394765/how-to-check-available-space-on-android-device-on-mini-sd-card
	 * @return
	 */
	public static long megabytesAvailable() {
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		long bytesAvailable = (long)stat.getBlockSizeLong() *(long)stat.getAvailableBlocksLong();
		long megAvailable = bytesAvailable / 1048576;
		Log.i(TAG, "Megs :"+megAvailable);
		return megAvailable;
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (megabytesAvailable() < 500) { // MB
			Toast.makeText(getBaseContext(),  "Not enough space on the SDcard, please clean up. Only " + megabytesAvailable() + " MB left", 
					Toast.LENGTH_LONG).show();
		}
        
        SensorManager sensorManager = (SensorManager) this.getSystemService(this.SENSOR_SERVICE);
        
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
        	mAccelAvailable = true;
        }
        if(sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
        	mGravAvailable = true;
        }
        if(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
        	mGyroAvailable = true;
        }
        if(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
        	mMagnetAvailable = true;
        }
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) {
        	mRotVecAvailable = true;
        }
        
        DialogFragment sensorDialog = new DialogSensorFragment(getApplicationContext(), mAccelAvailable, mGravAvailable, mGyroAvailable, mMagnetAvailable, mRotVecAvailable);
        sensorDialog.show(getFragmentManager(), "sensorDialog");
    }

    @Override
	protected void onResume() {
		super.onResume();

		//updatePrefs();

		mButtonStart = (ImageButton) findViewById(R.id.button_start);
		mButtonStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				mLoggingDir = loggingDir();

				Intent calibrationIntent = new Intent(MainActivity.this, SensorCalibrationActivity.class);
				startActivity(calibrationIntent);
				//Intent Skip = new Intent(MainActivity.this, SensorLoggingActivity.class);
				//startActivity(Skip);
			}
		});

		mButtonSettings = (ImageButton) findViewById(R.id.button_prefs);
		mButtonSettings.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent settingsActivity = new Intent(MainActivity.this, SettingsActivity.class);
				
				settingsActivity.putExtra("accelAvailable", mAccelAvailable);
				settingsActivity.putExtra("gravAvailable", mGravAvailable);
				settingsActivity.putExtra("gyroAvailable", mGyroAvailable);
				settingsActivity.putExtra("magnetAvailable", mMagnetAvailable);
				settingsActivity.putExtra("rotVecAvailable", mRotVecAvailable);
				
				startActivityForResult(settingsActivity, SETTINGS_CODE);
			}
		});

		mButtonNewTexture = (Button) findViewById(R.id.button_new_texture);
		mButtonNewTexture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				mLoggingDir = loggingDir();
				showNewEntryDialog(mLoggingDir);
			}
		});

		mButtonAddToEntry = (Button) findViewById(R.id.button_add_to_entry);
		mButtonAddToEntry.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				mLoggingDir = loggingDir();
				showTextureChoiceDialog(mLoggingDir);
			}
		});

		if(mPrefMode) {
			mButtonNewTexture.setVisibility(View.VISIBLE);
			mButtonAddToEntry.setVisibility(View.VISIBLE);
			mButtonStart.setVisibility(View.GONE);
		}
		else {
			mButtonStart.setVisibility(View.VISIBLE);
			mButtonNewTexture.setVisibility(View.GONE);
			mButtonAddToEntry.setVisibility(View.GONE);
		}   	

		// show used memory
		{
			final Runtime runtime = Runtime.getRuntime();
			final long usedMemInMB=(runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
			final long maxHeapSizeInMB=runtime.maxMemory() / 1048576L;
			Log.i(TAG, "TRActivity, Used in MB " + Long.toString(usedMemInMB) + ", total  " + Long.toString(maxHeapSizeInMB));
		}
	}
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	if(requestCode == SETTINGS_CODE) {
    		
    		updateSettings();
    		
    	}
    }

	protected void showNewEntryDialog(File dir) {

		/*DialogFragment newEntryDialog = new DialogNewEntryFragment(dir, 0);
		newEntryDialog.show(getFragmentManager(), "DialogNewEntryFragment");*/
	}

	protected void showTextureChoiceDialog(File dir) {

		/*DialogFragment textureChoiceDialog = new DialogTextureChoiceFragment(dir);
		textureChoiceDialog.show(getFragmentManager(), "DialogDataDescTextureFragment");*/
	}

	private static String get2DigitString(int val){
		String valStr=""+val;
		if(val<10){
			valStr="0"+val;
		}
		return valStr;
	}

	private File loggingDir() {
		File dir;

		mPathToStorage = mPrefPathToStorage;
		String prefix = Environment.getExternalStorageDirectory().getPath();
		mPathToStorage =  prefix + "/" + mPathToStorage;

		if(!mPrefMode) {
			mPathToStorage += Constants.ANALYSIS_FOLDER_NAME;
		}
		else {
			mPathToStorage += Constants.DATABASE_FOLDER_NAME;
		}

		File logPath = new File(mPathToStorage);
		if (!logPath.exists())
			logPath.mkdirs();
		if(!logPath.exists())
		{
			((TextView) findViewById(R.id.textview_error)).setText("Not logging! Storage directory " + logPath.getPath() + 
					" does not exist.");
			return null;
		}
		else {
			Log.i(TAG, "storage dir: " + logPath.getAbsolutePath());
		}

		Calendar rightNow = Calendar.getInstance();

		if(!mPrefMode) {
			dir = getLogFolder(mPathToStorage, rightNow);
		}
		else {
			dir = new File(mPathToStorage);
		}

		if (!dir.exists()) {
			Log.i(TAG, "does not exist, creating folder " + dir.getName() + "...");
			if (!dir.mkdirs())
				Log.i(TAG, "failure on creating");
		}

		return dir;
	}

	private static File getLogFolder(String pathToStorage, Calendar rightNow){

		return new File(pathToStorage + rightNow.get(Calendar.YEAR)
				+get2DigitString(rightNow.get(Calendar.MONTH)+1)
				+get2DigitString(rightNow.get(Calendar.DAY_OF_MONTH))+"_"
				+get2DigitString(rightNow.get(Calendar.HOUR_OF_DAY))
				+get2DigitString(rightNow.get(Calendar.MINUTE))
				+get2DigitString(rightNow.get(Calendar.SECOND)));
	}
	
	private void updateSettings() {
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if(sharedPrefs.getBoolean(Constants.PREF_KEY_ACCEL_SELECT, false)) {
            Log.i("Settings", "use Accel");
            mUseAccel = true;
        } else {
        	mUseAccel = false;
        }
        
        if(sharedPrefs.getBoolean(Constants.PREF_KEY_GRAV_SELECT, false)) {
        	Log.i("Settings", "use Grav");
        	mUseGrav = true;
        } else {
        	mUseGrav = false;
        }
	
        if(sharedPrefs.getBoolean(Constants.PREF_KEY_GYRO_SELECT, false)) {
        	Log.i("Settings", "use Gyro");
        	mUseGyro = true;
        } else {
        	mUseGyro = false;
        }
        
        if(sharedPrefs.getBoolean(Constants.PREF_KEY_MAGNET_SELECT, false)) {
        	Log.i("Settings", "use Magnet");
        	mUseMagnet = true;
        } else {
        	mUseMagnet = false;
        }
        
        if(sharedPrefs.getBoolean(Constants.PREF_KEY_ROTVEC_SELECT, false)) {
        	Log.i("Settings", "use Grav");
        	mUseRotVec = true;
        } else {
        	mUseRotVec = false;
        }
        
        if(sharedPrefs.getBoolean(Constants.PREF_KEY_EXTERN_ACCEL, false)) {
        	Log.i("Settings", "use extern Accel");
        	mUseExternAccel = true;
        } else {
        	mUseExternAccel = false;
        }
        
        if(sharedPrefs.getBoolean(Constants.PREF_KEY_MODE_SELECT, false)) {
        	Log.i("Settings", "database mode on");
        	mPrefMode = true;
        } else {
        	mPrefMode = false;
        }
	}

	boolean checked = false;

	public static File getLoggingDir() {
		return mLoggingDir;
	}

	public static void setLoggingDir(File file) {
		mLoggingDir = file;
	}

	public static boolean getPrefMode() {
		return mPrefMode;
	}

	public static int getPrefSensorSpeed() {
		return mPrefSensorSpeed;
	}

	public static String[] getSensorNames() {
		return sensorNames;
	}

	public static String getPrefPathToStorage() {
		return mPrefPathToStorage;
	}

	public static int getNumberOfSensorsToLog() {
		return numberOfSensorsToLog;
	}
}