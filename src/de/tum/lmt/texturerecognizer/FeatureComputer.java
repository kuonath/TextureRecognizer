package de.tum.lmt.texturerecognizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import de.tum.lmt.texturerecognizer.DialogSensorFragment.iOnDialogButtonClickListener;
import android.graphics.Bitmap;
import android.util.Log;

public class FeatureComputer {
	
	private String mFeaturePath;
	
	private Bitmap mSurfacePicture;
	
	private boolean mDatabaseMode;
	
	private Bitmap mMacroImage;
	
	private double mWeightMacro;
	private double mWeightMicro;
	private double mWeightFineness;
	
	private double mMacroAmplitude;
	private double mMicroAmplitude;
	private double mFinenessAmplitude;
	
	private double mHardness;
	private long mImpactDuration;
	
	private double mNoiseDistribution;
	
	public interface iOnFeaturesFinishedListener {
		public void onFeaturesComputed(long duration);
	}
	
	public FeatureComputer(String featurePath, Bitmap surfacePicture, boolean databaseMode) {
		mFeaturePath = featurePath;
		mSurfacePicture = surfacePicture;
		mDatabaseMode = databaseMode;
	}
	
	private void initWeights() {
		mWeightMacro = 0.4;
		mWeightMicro = 0.4;
		mWeightFineness = 0.2;
	}
	
	public void computeFeatures() {
		
		initWeights();
		
		computeHardnessAndImpactDuration();
		
		computeMacroscopicImage();
		
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
	
	private void computeHardnessAndImpactDuration() {
		mHardness = 1;
		mImpactDuration = 500;
	}
	
	private void computeMacroscopicImage() {
		mMacroImage = mSurfacePicture;
		
		savePictureToFile(mFeaturePath, mMacroImage);
	}
	
	private void savePictureToFile(String pathToFile, Bitmap picture) {
		
		File pictureFile = new File(pathToFile + "macro.jpg");
		
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		
		picture.compress(Bitmap.CompressFormat.JPEG, Constants.JPG_COMPRESSION_LEVEL, bytes);

		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(pictureFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			fos.write(bytes.toByteArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		mNoiseDistribution = 0.5;
	}
	
	private void calculateWeightMacroRoughness() {
		mWeightMacro = 0.4;
	}
	
	private void calculateWeightMicroRoughness() {
		mWeightMicro = 0.4;
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
			mMacroAmplitude = roundDigits(mMacroAmplitude, precisionDigitParameter);
			mMicroAmplitude = roundDigits(mMicroAmplitude, precisionDigitParameter);
			mFinenessAmplitude = roundDigits(mFinenessAmplitude, precisionDigitParameter);
			mNoiseDistribution = roundDigits(mNoiseDistribution, precisionDigitParameter);
			mWeightMacro = roundDigits(mWeightMacro, precisionWeights);
			mWeightMicro = roundDigits(mWeightMicro, precisionWeights);
			mWeightFineness = roundDigits(mWeightFineness, precisionWeights);
			mHardness = roundDigits(mHardness, precisionDigitParameter);
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
	
	private double roundDigits(double toRound, int precision) {
		
		double factor = Math.pow(10, precision);
		double value = toRound * factor;
		value = Math.round(value);
		return value /= factor;
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
