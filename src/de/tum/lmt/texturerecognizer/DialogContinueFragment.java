package de.tum.lmt.texturerecognizer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class DialogContinueFragment extends DialogFragment {
	
	private int step;
	private String messagePart1;
	
	public DialogContinueFragment(int step) {
		this.step = step;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		switch(step) {
			case 1:
				messagePart1 = getString(R.string.next_camera);
				break;
			case 2:
				messagePart1 = getString(R.string.next_logging);
				break;
		}
		
		builder.setMessage(messagePart1 + "\n" + getString(R.string.message_continue))
		   	   .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
			
			  	   @Override
		 		   public void onClick(DialogInterface dialog, int which) {
			  		   
			  		   switch(step) {
			  		   		case 1:
			  		   			Intent intentCam = new Intent(getActivity(), CameraActivity.class);
			  		   			startActivity(intentCam);
			  		   			break;
			  		   		case 2:
			  		   			Intent intentLogging = new Intent(getActivity(), SensorLoggingActivity.class);
			  		   			startActivity(intentLogging);
			  		   			break;
			  		   }
			  		   getActivity().finish();
		   		   }
		   	   })
		   	   /*.setNeutralButton(R.string.button_skip, new DialogInterface.OnClickListener() {
				
		   		   @Override
			   	   public void onClick(DialogInterface dialog, int which) {
		   			   
		   			   switch(step) {
		   			   case 1:
		   				   Intent intentLogging = new Intent(getActivity(), SensorLoggingActivity.class);
		   				   startActivity(intentLogging);
		   				   break;
		   			   case 2:
		   				   showMailDialog();
		   				   break;
		   			   }
			   	   }
			   })*/
		   	   .setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
				
		   		   @Override
			   	   public void onClick(DialogInterface dialog, int which) {
		   			   
			   	   }
			   });
		
		return builder.create();
	}
	
	protected void showMailDialog() {
		
		DialogFragment mailDialog = new DialogMailFragment();
		mailDialog.show(getFragmentManager(), "DialogMailFragment");
	}
	
}
