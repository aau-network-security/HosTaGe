package dk.aau.netsec.hostage.protocol;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.ui.activity.MainActivity;

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
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public SSLContext getSSLContext() {
        SSLContext sslContext = null;
        try {
            ProviderInstaller.installIfNeeded(MainActivity.getContext());
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(getKeyManager().getKeyManagers(), null, null);
            sslContext.createSSLEngine();
        } catch (KeyManagementException | NoSuchAlgorithmException | GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        }
        return sslContext;
    }

    /**
     * KeyManager Factory
     *
     * @return self signed certificate
     */
    private KeyManagerFactory getKeyManager() {
        String keyStoreName = "https_cert.bks";
        char[] keyStorePassword = "password".toCharArray();
        KeyStore keyStore;
        KeyManagerFactory keyManagerFactory = null;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(Hostage.getContext().getAssets().open(keyStoreName), keyStorePassword);
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword);
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
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
