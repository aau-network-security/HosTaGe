package de.tudarmstadt.informatik.hostage.ui.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;


/**
 * @author Alexander Brakowski
 * @created 14.01.14 18:04
 */
public class ProfileListItem {
    public String label;
    public String text;

    public boolean activated;
    public Bitmap icon;

	public boolean isBackVisible = false;

    public ProfileListItem(String text, String label, Bitmap icon){
        this.text = text;
        this.label = label;
        this.activated = false;
        this.icon = icon;
    }

    public ProfileListItem(String text, String label, int icon){
        this(text, label, BitmapFactory.decodeResource(MainActivity.getContext().getResources(), icon));
    }
}
