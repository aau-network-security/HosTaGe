package de.tudarmstadt.informatik.hostage.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.util.NoSuchElementException;

import de.tudarmstadt.informatik.hostage.system.Device;
import de.tudarmstadt.informatik.hostage.system.PrivilegedPort;
import de.tudarmstadt.informatik.hostage.system.PrivilegedPort.TYPE;

public class MyDatagramSocketFactory {

	public DatagramSocket createDatagramSocket(int port) throws IOException {
		DatagramSocket socket = null;
		//port = 1024;
		if (port > 1023) {
			socket = new DatagramSocket(port);
		} else if (Device.isPorthackInstalled()) {
			FileDescriptor fd = new PrivilegedPort(TYPE.UDP, port).getFD();
			socket = new DatagramSocket();
			try {
				DatagramSocketImpl impl = getImpl(socket);
				injectFD(fd, impl);
				injectLocalPort(port, impl);
				setBound(socket);
			} catch (NoSuchFieldException e) {
			} catch (IllegalAccessException e) {
			} catch (IllegalArgumentException e) {
			}
		}
		return socket;
	}

	private DatagramSocketImpl getImpl(DatagramSocket socket) throws NoSuchFieldException, IllegalAccessException, IllegalArgumentException {
		Field implField = socket.getClass().getDeclaredField("impl");
		implField.setAccessible(true);
		return (DatagramSocketImpl) implField.get(socket);
	}

	private void injectFD(FileDescriptor fd, DatagramSocketImpl impl) throws NoSuchFieldException, IllegalAccessException, IllegalArgumentException {
		Class<?> plainDatagramSocketImplClazz = impl.getClass();
		Class<?> datagramSocketImplClazz = plainDatagramSocketImplClazz.getSuperclass();
		Field fdField = datagramSocketImplClazz.getDeclaredField("fd");
		fdField.setAccessible(true);
		fdField.set(impl, fd);
	}

	private void injectLocalPort(int port , DatagramSocketImpl impl)
			throws NoSuchElementException, NoSuchFieldException, IllegalAccessException {
		Class<?> plainDatagramSocketImplClazz = impl.getClass();
		Class<?> datagramSocketImplClazz = plainDatagramSocketImplClazz.getSuperclass();
		Field localPortField = datagramSocketImplClazz.getDeclaredField("localPort");
		localPortField.setAccessible(true);
		localPortField.set(impl, port);
	}

	private void setBound(DatagramSocket socket) throws NoSuchFieldException, IllegalAccessException, IllegalArgumentException {
		Field boundField = socket.getClass().getDeclaredField("isBound");
		boundField.setAccessible(true);
		boundField.set(socket, true);
	}

}
