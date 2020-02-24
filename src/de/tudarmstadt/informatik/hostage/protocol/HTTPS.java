package de.tudarmstadt.informatik.hostage.protocol;

import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import de.tudarmstadt.informatik.hostage.Hostage;

/**
 * HTTPS protocol. Extends HTTP. Implementation of RFC document 2818. It can handle the
 * following requests: GET, HEAD, TRACE, POST, DELETE. For all other requests
 * '400 Bad Request' will be replied.
 * 
 * @author Wulf Pfeiffer
 */
public class HTTPS extends HTTP implements SSLProtocol {

	@Override
	public int getPort() {
		return 443;
	}

	@Override
	public SSLContext getSSLContext() {
		String keyStoreName = "https_cert.bks";
		char keyStorePassword[] = "password".toCharArray();
		KeyStore keyStore;
		KeyManagerFactory keyManagerFactory = null;
		try {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(
					Hostage.getContext().getAssets().open(keyStoreName),
					keyStorePassword);
			keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory
					.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, keyStorePassword);
		} catch (Exception e) {
			e.printStackTrace();
		}
		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("SSLv3");
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sslContext;
	}

	@Override
	public boolean isSecure() {
		return true;
	}

	@Override
	public String toString() {
		return "HTTPS";
	}

}
