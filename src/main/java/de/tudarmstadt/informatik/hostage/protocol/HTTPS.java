package de.tudarmstadt.informatik.hostage.protocol;

import com.google.android.gms.security.ProviderInstaller;

import java.security.KeyStore;


import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;


/**
 * HTTPS protocol. Extends HTTP. Implementation of RFC document 2818. It can handle the
 * following requests: GET, HEAD, TRACE, POST, DELETE. For all other requests
 * '400 Bad Request' will be replied.
 * 
 * @author Wulf Pfeiffer
 */
public class HTTPS extends HTTP implements SSLProtocol {

	private int port = 443;

	@Override
	public int getPort() { return port; }

	@Override
	public void setPort(int port){ this.port = port;}

	@Override
	public SSLContext getSSLContext() {
		SSLContext sslContext = null;
		try {
			ProviderInstaller.installIfNeeded(MainActivity.getContext());
			sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(getKeyManager().getKeyManagers(), null, null);
			sslContext.createSSLEngine();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sslContext;
	}

	/**
	 * KeyManager Factory
	 * @return self signed certificate
	 */
	private KeyManagerFactory getKeyManager(){
		String keyStoreName = "https_cert.bks";
		char[] keyStorePassword = "password".toCharArray();
		KeyStore keyStore;
		KeyManagerFactory keyManagerFactory = null;
		try {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(Hostage.getContext().getAssets().open(keyStoreName), keyStorePassword);
			keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, keyStorePassword);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return keyManagerFactory;
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
