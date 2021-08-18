package dk.aau.netsec.hostage.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import java.util.ArrayList;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.ui.activity.MainActivity;

//import android.content.Intent;
/**
 * Created by Julien on 16.02.14.
 */
@SuppressLint("ValidFragment")
public class ChecklistDialog extends DialogFragment {
	
	private ArrayList<Integer> mSelectedItems;
	private ArrayList<String> itemTitles;
    private ChecklistDialogListener mListener;
    
    private int selectedIndex;
    private boolean isMultipleChoice;
    
    public String title;
	
	/**
     * The ChecklistDialogListener will be called if the user clicked a "cancel" or "ok" button.
     * */
    @SuppressLint("ValidFragment")
	public interface ChecklistDialogListener {
        /**
         * Called if the user tapped "ok"
         * @param dialog {@link ChecklistDialog ChecklistDialog}
         */
        void onDialogPositiveClick(ChecklistDialog dialog);

        /**
         * Called if the user tapped "cancel".
         * @param dialog {@link ChecklistDialog ChecklistDialog}
         */
        void onDialogNegativeClick(ChecklistDialog dialog);
    }

    /**
     * Returns the dialog title
     * @return title String
     */
    public String getTitle(){
    	return this.title;
    }

    /**
     * Returns true if the checklist dialog is a multiple choice dialog.
     * @return boolean isMultipleChoice
     * */
    public boolean isMultipleChoice(){
    	return this.isMultipleChoice;
    }

    /*CONSTRUCTOR*/
    /**
     * The Constructor Method
     * @param  title String
     * @param itemTitles ArrayList<String> item titles list
     * @param selected boolean[] an array of booleans describing the position of all the selected titles.
     * @param isMultipleChoice boolean isMultipleChoice
     * @param listener ChecklistDialogListener an user "event" listener
     *
     * */
    public ChecklistDialog(String title, ArrayList<String> itemTitles, boolean[] selected, boolean isMultipleChoice , ChecklistDialogListener listener){
    	mListener = listener;
    	this.mSelectedItems = new ArrayList<Integer>();
	    
    	this.isMultipleChoice = isMultipleChoice;
	    this.title = title;
	    this.itemTitles = itemTitles;

	    
	    
	    boolean[] selectedArray = new boolean[this.itemTitles.size()];

	    if(this.isMultipleChoice){
		    for(int i = 0; i < this.itemTitles.size(); i++){
		    	boolean isSelected = selected[i];
		    	selectedArray[i] = isSelected;
		    	if(isSelected) this.mSelectedItems.add(i);
		    }
	    } else {
		    for(int i = 0; i < this.itemTitles.size(); i++){
		    	boolean isSelected = selected[i];
		    	selectedArray[i] = isSelected;
		    	if(isSelected) this.selectedIndex = i;
		    }
	    }
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        if(this.mListener == null)
        try {
            
            if (activity.getClass().equals(MainActivity.class)){
            	mListener = (ChecklistDialogListener) (((MainActivity)activity).getDisplayedFragment());
            } else {
            	mListener = (ChecklistDialogListener) activity;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ChecklistDialogListener");
        }
    }

    /**
     * Return the selected titles.
     * @return ArrayList<String>
     */
    public ArrayList<String> getSelectedItemTitles(){
    	ArrayList<String> list = new ArrayList<String>();
        if (this.mSelectedItems == null){
            return  list;
        }
    	for(Integer i : this.mSelectedItems){
    		list.add(this.itemTitles.get(i.intValue()));
    	}
        if (this.mSelectedItems.size() == 0 && !this.isMultipleChoice){
            list.add(this.itemTitles.get(this.selectedIndex));
       }
    	return list;
    }


	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    
	    CharSequence[] titles = this.itemTitles.toArray(new CharSequence[this.itemTitles.size()]);
	    
	    boolean[] selectedArray = new boolean[this.itemTitles.size()];
	    for(Integer selection : this.mSelectedItems){
	    	selectedArray[selection.intValue()] = true;
	    }

	    if(this.isMultipleChoice){
	        builder.setTitle(title).setMultiChoiceItems(titles, selectedArray,
                    new DialogInterface.OnMultiChoiceClickListener() {
             public void onClick(DialogInterface dialog, int which,
                     boolean isChecked) {
          		   if (isChecked) {
                     	mSelectedItems.add(which);
                 	} else if (mSelectedItems.contains(which)) {
                     	mSelectedItems.remove(Integer.valueOf(which));
                 	}
             }
         }).setPositiveButton(R.string.button_title_apply, new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int id) {
                 mListener.onDialogPositiveClick(ChecklistDialog.this);
             }
         })
         .setNegativeButton(R.string.button_title_cancel, new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int id) {
                 mListener.onDialogNegativeClick(ChecklistDialog.this);
             }
         });
	        
	    } else {
	    	
	    	 builder.setTitle(title).setSingleChoiceItems(titles, this.selectedIndex, 
	    			 new DialogInterface.OnClickListener() {
	             public void onClick(DialogInterface dialog, int id) {
	                 mSelectedItems.clear();
	            	 mSelectedItems.add(id);
	             }
	         })
	         .setPositiveButton(R.string.button_title_apply, new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int id) {
                 mListener.onDialogPositiveClick(ChecklistDialog.this);
             }
	         })
	         .setNegativeButton(R.string.button_title_cancel, new DialogInterface.OnClickListener() {
	             public void onClick(DialogInterface dialog, int id) {
	                 mListener.onDialogNegativeClick(ChecklistDialog.this);
	             }
	         });
	    }
	    
	    return builder.create();
	}
}


