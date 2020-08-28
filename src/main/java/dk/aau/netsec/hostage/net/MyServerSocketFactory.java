package dk.aau.netsec.hostage.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import javax.net.ServerSocketFactory;
import dk.aau.netsec.hostage.commons.HelperUtils;
import dk.aau.netsec.hostage.system.Device;


public class MyServerSocketFactory extends ServerSocketFactory {

	private String ipAddress="0.0.0.0";
	
	@Override
	public ServerSocket createServerSocket(int port) throws IOException {
		ServerSocket socket = null;
		if (port > 1023 || port == 0) {
			return bindSocket(port);
		} else if (Device.isRooted()) {
  				if (Device.isPortRedirectionAvailable()) { // use ip tables
					int redirectedPort = HelperUtils.getRedirectedPort(port);
					return bindSocket(redirectedPort);
			}
		}
		return socket;
	}

	private ServerSocket bindSocket(int port) throws IOException {
		ServerSocket socket = new ServerSocket();
		socket.setReuseAddress(true);
		socket.bind(new InetSocketAddress(ipAddress,port));

		return socket;
	}

	@Override
	public ServerSocket createServerSocket(int port, int backlog) throws IOException {
		return createServerSocket(port);
	}

	@Override
	public ServerSocket createServerSocket(int port, int backlog, InetAddress iAddress) throws IOException {
		return createServerSocket(port);
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

}
