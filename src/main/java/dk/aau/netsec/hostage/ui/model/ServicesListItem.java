package dk.aau.netsec.hostage.ui.model;

/**
 * @author Daniel Lazar
 * @created 06.02.14
 *
 * defines a service list item
 */
public class ServicesListItem {
	public final String protocol;
	public int attacks;
	public final int port;

	public final boolean activated;

    /**
     * constructor of a service list item
     *
     * @param protocol protocol of this item, e.g. ftp
     */
	public ServicesListItem(String protocol,int port){
		this.protocol = protocol;
		this.port = port;
		this.activated = false;
	}
}
