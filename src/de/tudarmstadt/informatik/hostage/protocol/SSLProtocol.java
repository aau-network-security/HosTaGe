package de.tudarmstadt.informatik.hostage.protocol;

import javax.net.ssl.SSLContext;

/**
 * Interface for secure protocols.
 * 
 * @author Wulf Pfeiffer
 */
public interface SSLProtocol extends Protocol {

	/**
	 * Returns the SSL Context to be used.
	 * 
	 * @return the used SSLContext
	 */
	SSLContext getSSLContext();
}
