package de.tudarmstadt.informatik.hostage.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import javax.net.ServerSocketFactory;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.system.Device;


public class MyServerSocketFactory extends ServerSocketFactory {

	private String ipAddress="0.0.0.0";
	
	@Override
	public ServerSocket createServerSocket(int port) throws IOException {
		ServerSocket socket = null;
		if (port > 1023 || port == 0) {
			socket = new ServerSocket();
			socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(ipAddress,port));
		} else if (Device.isRooted()) {
  				if (Device.isPortRedirectionAvailable()) { // use ip tables
					int redirectedPort = HelperUtils.getRedirectedPort(port);
					socket = new ServerSocket();
					socket.setReuseAddress(true);
					socket.bind(new InetSocketAddress(ipAddress,redirectedPort));
			}
		}
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
