package de.tum.lmt.texturerecognizer;

public class Constants {

	//Preferences
	public static final String PREF_KEY_ACCEL_SELECT = "pref_key_accel_select";
	public static final String PREF_KEY_GRAV_SELECT = "pref_key_grav_select";
	public static final String PREF_KEY_GYRO_SELECT = "pref_key_gyro_select";
	public static final String PREF_KEY_MAGNET_SELECT = "pref_key_magnet_select";
	public static final String PREF_KEY_ROTVEC_SELECT = "pref_key_rotvec_select";
	public static final String PREF_KEY_EXTERN_ACCEL = "pref_key_extern_accel";
	public static final String PREF_KEY_MODE_SELECT = "pref_key_mode_select";
	
	//Files and Paths
	public final static String ANALYSIS_FOLDER_NAME = "/Analysis/";
	public final static String DATABASE_FOLDER_NAME = "/Database";
	
	//Calibration Activity
	public static boolean GRAVITY_NOT_PRESENT;
	public static float CALIB_PROXIMITY_THRESHOLD = 4.5f;
	public static float CALIB_FILTER_FORGETTING_FACTOR = 1.0f/100.0f;
	public static float CALIB_THRESH = 0.015f;
	public static long CALIB_TIME_DIFF_SINCE_LAST_GYRO_EVENT = 500;
	
}
