package dk.aau.netsec.hostage.ui.model;

/**
 * Holds the data for an navigation item in the navigation drawer
 *
 * @author Alexander Brakowski
 * @created 13.01.14 16:37
 */
public class DrawerListItem {

	/**
	 * The icon of the item
	 */
    public final int icon;

	/**
	 * The text of the item
	 */
    public final int text;

    public DrawerListItem(int text, int icon){
        this.text = text;
        this.icon = icon;
    }
}
