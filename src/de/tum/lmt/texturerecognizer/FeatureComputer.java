package de.tum.lmt.texturerecognizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import de.tum.lmt.texturerecognizer.DialogSensorFragment.iOnDialogButtonClickListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;

public class FeatureComputer {
	
	private String mFeaturePath;
	
	private Bitmap mSurfacePicture;
	
	private boolean mDatabaseMode;
	
	private Bitmap mMacroImage;
	
	private float mImpactBegins; //in seconds, can be converted to sound or sensor index with known sampling rate
	private float mImpactEnds;
	private float mMovementBegins;
	private float mMovementEnds;
	
	private double mWeightMacro;
	private double mWeightMicro;
	private double mWeightFineness;
	
	private double mMacroAmplitude;
	private double mMicroAmplitude;
	private double mFinenessAmplitude;
	
	private double mHardness;
	private long mImpactDuration;
	
	private double mNoiseDistribution;
	
	/*private SensorLog mAccelLog;
	private SensorLog mGravLog;
	private SensorLog mGyroLog;
	private SensorLog mMagnetLog;
	private SensorLog mRotVecLog;*/
	
	private List<Double> mSoundData;
	private List<Double> mSoundDataMovement;
	private List<Double> mSoundDataImpact;
	private List<float[]> mAccelData;
	private List<Float> mAccelDataMovement;
	private List<Float> mAccelDataImpact;
	
	private Calculator mCalculator;
	
	public interface iOnFeaturesFinishedListener {
		public void onFeaturesComputed(long duration);
	}
	
	public FeatureComputer(String featurePath, List<Double> soundData, SensorLog accelLog, boolean databaseMode) {
		
		mCalculator = new Calculator();
		
		mFeaturePath = featurePath;
		mSoundData = soundData;
		mAccelData = accelLog.getValues();
		mDatabaseMode = databaseMode;
		
		mSoundDataMovement = new ArrayList<Double>();
		mSoundDataImpact = new ArrayList<Double>();
		mAccelDataMovement = new ArrayList<Float>();
		mAccelDataImpact = new ArrayList<Float>();
	}
	
	public void computeFeatures() {
		
		initWeights();
		
		getIntervals(); // get Indices to split sensor and sound data into "no contact", "impact" and "movement"
						   // splitting happens in the next two methods; so far only one data segment after 1s is used (movement)
		
		prepareSoundData(); //extract movement data (currently fixed to 1s to end) 
		
		prepareSensorData(); //extract movement data (currently fixed to 1s to end); use only one axis or use absolute acceleration (currently only y-axis)
		
		computeHardnessAndImpactDuration();
		
		
		
		computeMacroAmplitude();
		computeMicroAmplitude();
		computeFinenessAmplitude();
		
		computeNoiseDistribution();
		
		calculateWeightMacroRoughness();
		calculateWeightMicroRoughness();
		calculateWeightFineness();
		
		if(!mDatabaseMode) {
			renormalizeWeights();
		}
		
		printAllFeatures();
		
		//onFeaturesComputed(mImpactDuration);
	}
	
	private void initWeights() {
		
		Log.i("Features", "initWeights()");
		
		mWeightMacro = 0.4;
		mWeightMicro = 0.4;
		mWeightFineness = 0.2;
	}
	
	private void getIntervals() {
		
		Log.i("Features", "getIntervals()");
		
		mImpactBegins = 0.5f;
		mImpactEnds = 1.0f;
		mMovementBegins = 1.0f;
		//mMovementEnds = // currently movement ends at the end of the recording (last index of lists)
	}
	
	private void prepareSoundData() {
				
		Log.i("Features", "prepareSoundData()");
		
		mSoundDataImpact = mSoundData;
		mSoundDataMovement = mSoundData;
		
		int beginImpactIndex = (int)(mImpactBegins * Constants.RECORDER_SAMPLING_RATE);
		int endImpactIndex = (int)(mImpactEnds * Constants.RECORDER_SAMPLING_RATE);
		
		for(int i = (mSoundDataImpact.size() - 1); i > endImpactIndex; i--) {
			mSoundDataImpact.remove(i);
		}
		
		if(beginImpactIndex < mSoundDataImpact.size()) {
			for(int i = (beginImpactIndex - 1); i >= 0; i--) {
				mSoundDataImpact.remove(i);
			}
		}
		
		int beginMovementIndex = (int)(mMovementBegins * Constants.RECORDER_SAMPLING_RATE);
		
		if(beginMovementIndex < mSoundDataMovement.size()) {
			for(int i = (beginMovementIndex - 1); i >= 0; i--) {
				mSoundDataMovement.remove(i); // remove elements before start of movement
			}
		}
	}
	
	private void prepareSensorData() {
		
		Log.i("Features", "prepareSensorData()");
		
		for(int i = 0; i < mAccelData.size(); i++) {
			mAccelDataImpact.add(mAccelData.get(i)[1]); // use only y-axis
		}
		
		for(int i = 0; i < mAccelData.size(); i++) {
			mAccelDataMovement.add(mAccelData.get(i)[1]); // use only y-axis
		}
		
		int beginImpactIndex = (int)(mImpactBegins * Constants.SAMPLE_RATE_ACCEL);
		int endImpactIndex = (int)(mImpactEnds * Constants.SAMPLE_RATE_ACCEL);
		
		for(int i = (mAccelDataImpact.size() - 1); i > endImpactIndex; i--) {
			mAccelDataImpact.remove(i);
		}
		
		if(beginImpactIndex < mAccelDataImpact.size()) {
			for(int i = (beginImpactIndex - 1); i >= 0; i--) {
				mAccelDataImpact.remove(i);
			}
		}
		
		int beginMovementIndex = (int)(mMovementBegins * Constants.SAMPLE_RATE_ACCEL);
		
		if(beginMovementIndex < mAccelDataMovement.size()) {
			for(int i = (beginMovementIndex - 1); i >= 0; i--) {
				mAccelDataMovement.remove(i); // remove elements before start of movement
			}
		}
	}
	
	private void computeHardnessAndImpactDuration() {
		mHardness = 1;
		mImpactDuration = 500;
	}
	

	//from leparlon on http://stackoverflow.com/questions/3373860/convert-a-bitmap-to-grayscale-in-android
	private Bitmap toGrayscale(Bitmap bmpOriginal) {        
	    int width, height;
	    height = bmpOriginal.getHeight();
	    width = bmpOriginal.getWidth();    

	    
	    Log.i("TAG","width/height:" + width + "..." + height);
	    Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, bmpOriginal.getConfig());
	    
	    Canvas c = new Canvas(bmpGrayscale);
	    Paint paint = new Paint();
	    ColorMatrix cm = new ColorMatrix();
	    cm.setSaturation(0);
	    ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
	    paint.setColorFilter(f);
	    c.drawBitmap(bmpOriginal, 0, 0, paint);
	    return bmpGrayscale;
	}
	

	
	private void computeMacroAmplitude() {
		mMacroAmplitude = 1;
	}
	
	private void computeMicroAmplitude() {
		mMicroAmplitude = 1;
	}
	
	private void computeFinenessAmplitude() {
		mFinenessAmplitude = 1;
	}
	
	private void computeNoiseDistribution() {
				
		ListIterator<Double> itBegin = mSoundDataMovement.listIterator(0);
		ListIterator<Double> itEnd = mSoundDataMovement.listIterator(mSoundDataMovement.size()-1);
		
		double maxAbs = mCalculator.getAbsoluteMaximumDouble(itBegin, itEnd);
		
		Log.i("Features", "Max Sound: " + maxAbs);
		
		double distThresh = 0.1 * maxAbs;
		
		List<Integer> binarySignal = new ArrayList<Integer>();
		
		int numberOfOnes = 0;
		int numberOfZeros = 0;
		
		itBegin = mSoundDataMovement.listIterator(0);
		
		for(ListIterator<Double> lit = itBegin; (lit.hasNext() && lit != itEnd); ) {
			
			double nextValue = lit.next();
			
			if(nextValue > distThresh) {
				binarySignal.add(1);
				numberOfOnes++;
			} else {
				binarySignal.add(0);
				numberOfZeros++;
			}
		}
		
		mNoiseDistribution = (double)numberOfOnes / (double)numberOfZeros;
		
		if(!mDatabaseMode && (mNoiseDistribution > 1.0)) {
			mNoiseDistribution = 1.0;
		}
		
		Log.i("Features", "noise distribution: " + mNoiseDistribution);

		
		mNoiseDistribution = 0.5;
	}
	
	private void calculateWeightMacroRoughness() {
		
		ListIterator<Float> itBegin = mAccelDataMovement.listIterator(0);
		ListIterator<Float> itEnd = mAccelDataMovement.listIterator(mAccelDataMovement.size()-1);
		
		double variance = mCalculator.varianceFloat(itBegin, itEnd);
		
		mWeightMacro = 3.0 * variance;
		
		if(!mDatabaseMode && mWeightMacro > 1.0) {
			mWeightMacro = 1.0;
		}
		
		Log.i("Features", "mWeightMacro: " + mWeightMacro);
		
	}
	
	private void calculateWeightMicroRoughness() {
		
		ListIterator<Double> itBegin = mSoundDataMovement.listIterator(0);
		ListIterator<Double> itEnd = mSoundDataMovement.listIterator(mSoundDataMovement.size()-1);
		
		double bandpower = mCalculator.bandPowerDouble(itBegin, itEnd);
		
		mWeightMicro = 5 * bandpower;
		if (!mDatabaseMode && mWeightMicro > 1.0) {
			mWeightMicro = 1.0;
		}
		
		Log.i("Features", "mWeightMicro: " + mWeightMicro);
		
	}
	
	private void calculateWeightFineness() {
		mWeightFineness = 0.2;
	}
	
	private void renormalizeWeights() {
		
		double sumOfWeights = mWeightMacro + mWeightMicro + mWeightFineness;
		
		if(Math.abs(sumOfWeights - 1) > 0.001) {
			mWeightMacro /= sumOfWeights;
			mWeightMicro /= sumOfWeights;
			mWeightFineness /= sumOfWeights;
		}
				
		Log.i("Features", "final weights: " + mWeightMacro + ", " + mWeightMicro + ", " + mWeightFineness);
	}
	
	private void printAllFeatures() {
		int precisionDigitParameter = 2;
		int precisionWeights = 3;

		//only round when not in database mode
		if(!mDatabaseMode) {
			mMacroAmplitude = mCalculator.roundDigits(mMacroAmplitude, precisionDigitParameter);
			mMicroAmplitude = mCalculator.roundDigits(mMicroAmplitude, precisionDigitParameter);
			mFinenessAmplitude = mCalculator.roundDigits(mFinenessAmplitude, precisionDigitParameter);
			mNoiseDistribution = mCalculator.roundDigits(mNoiseDistribution, precisionDigitParameter);
			mWeightMacro = mCalculator.roundDigits(mWeightMacro, precisionWeights);
			mWeightMicro = mCalculator.roundDigits(mWeightMicro, precisionWeights);
			mWeightFineness = mCalculator.roundDigits(mWeightFineness, precisionWeights);
			mHardness = mCalculator.roundDigits(mHardness, precisionDigitParameter);
			//impact duration does not have to be rounded
		}
		
		if(!mDatabaseMode) {
			String delimiter = "#";
			
			StringBuilder sb = new StringBuilder();
			
			sb.append(Double.toString(mMacroAmplitude));
			sb.append(delimiter);
			sb.append(Double.toString(mMicroAmplitude));
			sb.append(delimiter);
			sb.append(Double.toString(mFinenessAmplitude));
			sb.append(delimiter);
			sb.append(Double.toString(mNoiseDistribution));
			sb.append(delimiter);
			sb.append(Double.toString(mWeightMacro));
			sb.append(delimiter);
			sb.append(Double.toString(mWeightMicro));
			sb.append(delimiter);
			sb.append(Double.toString(mWeightFineness));
			sb.append(delimiter);
			sb.append(Double.toString(mHardness));
			sb.append(delimiter);
			sb.append(Long.toString(mImpactDuration));
			
			String featureString = sb.toString();
			
			writeStringToFile(mFeaturePath, featureString);
		}
	}
	
	private void writeStringToFile(String pathToFile, String stringToWrite) {
		
		File featureFile = new File(pathToFile + "parameters.txt");
		
		if(!featureFile.exists()) {
			try {
				featureFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(featureFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
		try {
			myOutWriter.append(stringToWrite);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			myOutWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			fOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
