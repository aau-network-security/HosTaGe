package de.tudarmstadt.informatik.hostage.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketImpl;

import javax.net.ServerSocketFactory;


import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.system.Device;
import de.tudarmstadt.informatik.hostage.system.PrivilegedPort;


public class MyServerSocketFactory extends ServerSocketFactory {

	@Override
	public ServerSocket createServerSocket(int port) throws IOException {
		ServerSocket socket = null;
		if (port > 1023 || port == 0) {
			socket = new ServerSocket();
			socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(port));
		} else if (Device.isRooted()) {
			if (Device.isPorthackInstalled()) {
				FileDescriptor fd = new PrivilegedPort(PrivilegedPort.TYPE.TCP, port).getFD();
				socket = new ServerSocket();
				try {
					SocketImpl impl = getImpl(socket);
					injectFD(fd, impl);
					setBound(socket);
				} catch (NoSuchFieldException e) {
				} catch (IllegalAccessException e) {
				} catch (IllegalArgumentException e) {
				}
			} else if (Device.isPortRedirectionAvailable()) { // use ip tables
				int redirectedPort = HelperUtils.getRedirectedPort(port);
				socket = new ServerSocket();
				socket.setReuseAddress(true);
				socket.bind(new InetSocketAddress(redirectedPort));
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

	private SocketImpl getImpl(ServerSocket socket) throws NoSuchFieldException, IllegalAccessException, IllegalArgumentException {
		Field implField = socket.getClass().getDeclaredField("impl");
		implField.setAccessible(true);
		return (SocketImpl) implField.get(socket);
	}

	private void injectFD(FileDescriptor fd, SocketImpl impl) throws NoSuchFieldException, IllegalAccessException, IllegalArgumentException {
		Class<?> plainServerSocketImplClazz = impl.getClass();
		Class<?> plainSocketImplClazz = plainServerSocketImplClazz.getSuperclass();
		Class<?> socketImplClazz = plainSocketImplClazz.getSuperclass();
		Field fdField = socketImplClazz.getDeclaredField("fd");
		fdField.setAccessible(true);
		fdField.set(impl, fd);
	}

	private void setBound(ServerSocket socket) throws NoSuchFieldException, IllegalAccessException, IllegalArgumentException {
		Field boundField = socket.getClass().getDeclaredField("isBound");
		boundField.setAccessible(true);
		boundField.set(socket, true);
	}

}
