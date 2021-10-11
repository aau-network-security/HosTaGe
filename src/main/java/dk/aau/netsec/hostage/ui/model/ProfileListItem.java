package dk.aau.netsec.hostage.ui.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import dk.aau.netsec.hostage.ui.activity.MainActivity;


/**
 * @author Alexander Brakowski
 * @created 14.01.14 18:04
 */
public class ProfileListItem {
    public final String label;
    public final String text;

    public final boolean activated;
    public final Bitmap icon;

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
