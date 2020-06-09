package de.tudarmstadt.informatik.hostage.ui.model;

/**
 * @author Daniel Lazar
 * @created 06.02.14
 *
 * defines a service list item
 */
public class ServicesListItem {
	public String protocol;
	public int attacks;

	public boolean activated;

    /**
     * constructor of a service list item
     *
     * @param protocol protocol of this item, e.g. ftp
     */
	public ServicesListItem(String protocol){
		this.protocol = protocol;
		this.activated = false;
	}
}
